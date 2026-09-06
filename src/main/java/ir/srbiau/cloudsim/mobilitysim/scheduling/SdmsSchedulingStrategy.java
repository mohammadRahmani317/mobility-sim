package ir.srbiau.cloudsim.mobilitysim.scheduling;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SdmsSchedulingStrategy implements VmSchedulingStrategy {
    /** Engineering bound added to keep exhaustive assignment practical for HARD workflows. */
    public static final int MAX_SCHEDULE_CANDIDATES = 3000;

    private final TaskPriorityCalculator priorityCalculator;
    private final CriticalPathExtractor criticalPathExtractor;
    private int lastCandidateCount;

    public SdmsSchedulingStrategy() {
        this(new TaskPriorityCalculator(), new CriticalPathExtractor());
    }

    SdmsSchedulingStrategy(TaskPriorityCalculator priorityCalculator,
                           CriticalPathExtractor criticalPathExtractor) {
        this.priorityCalculator = priorityCalculator;
        this.criticalPathExtractor = criticalPathExtractor;
    }

    @Override
    public void assignCloudletsToVms(List<User> users, List<VmMobility> vms, Integer brokerId) {
        if (vms.isEmpty()) {
            throw new IllegalArgumentException("At least one VM is required");
        }
        lastCandidateCount = 0;
        for (User workflow : users) {
            scheduleWorkflow(workflow, vms, brokerId);
        }
    }

    private void scheduleWorkflow(User workflow, List<VmMobility> vms, int brokerId) {
        workflow.setSchedulingFailed(false);
        priorityCalculator.calculatePriorities(workflow, vms);
        List<CloudletMobility> criticalPath = criticalPathExtractor.extract(workflow);
        List<CloudletMobility> order = dependencySafePriorityOrder(workflow.getCloudlets(), criticalPath);

        List<CandidateSchedule> candidates = new ArrayList<>();
        List<VmMobility> fastestFirst = vms.stream()
                .sorted(Comparator.comparingDouble(VmMobility::getTotalMips).reversed())
                .toList();
        search(workflow, order, fastestFirst, 0, new HashMap<>(), new HashMap<>(),
                new HashMap<>(), new HashMap<>(), candidates);
        lastCandidateCount += candidates.size();

        candidates.sort(Comparator.comparingDouble(CandidateSchedule::utility));
        for (CandidateSchedule candidate : candidates) {
            if (applySchedule(order, candidate, vms, brokerId)) {
                return;
            }
        }
        workflow.setSchedulingFailed(true);
    }

    private void search(User workflow,
                        List<CloudletMobility> order,
                        List<VmMobility> vms,
                        int taskIndex,
                        Map<CloudletMobility, VmMobility> assignments,
                        Map<CloudletMobility, Double> starts,
                        Map<CloudletMobility, Double> finishes,
                        Map<VmMobility, Double> vmAvailable,
                        List<CandidateSchedule> candidates) {
        if (candidates.size() >= MAX_SCHEDULE_CANDIDATES) {
            return;
        }
        if (taskIndex == order.size()) {
            double makespan = finishes.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double serialTime = assignments.entrySet().stream()
                    .mapToDouble(entry -> executionTime(entry.getKey(), entry.getValue())).sum();
            double utility = calculateUtility(workflow, vms, makespan, serialTime);
            candidates.add(new CandidateSchedule(Map.copyOf(assignments), Map.copyOf(starts),
                    Map.copyOf(finishes), utility));
            return;
        }

        CloudletMobility task = order.get(taskIndex);
        for (VmMobility vm : vms) {
            if (vm.getNumberOfPes() < task.getNumberOfPes()) {
                continue;
            }
            double est = vmAvailable.getOrDefault(vm, 0.0);
            for (CloudletMobility parent : task.getParents()) {
                VmMobility parentVm = assignments.get(parent);
                double ready = finishes.get(parent);
                if (parentVm != vm) {
                    ready += communicationTime(workflow, parent, task, parentVm, vm);
                }
                est = Math.max(est, ready);
            }
            double eft = est + executionTime(task, vm);
            if (eft > workflow.getDeadline()) {
                continue; // Deadline-aware branch pruning.
            }

            assignments.put(task, vm);
            starts.put(task, est);
            finishes.put(task, eft);
            Double previousAvailability = vmAvailable.put(vm, eft);
            search(workflow, order, vms, taskIndex + 1, assignments, starts, finishes,
                    vmAvailable, candidates);
            assignments.remove(task);
            starts.remove(task);
            finishes.remove(task);
            if (previousAvailability == null) {
                vmAvailable.remove(vm);
            } else {
                vmAvailable.put(vm, previousAvailability);
            }
            if (candidates.size() >= MAX_SCHEDULE_CANDIDATES) {
                return;
            }
        }
    }

    private double calculateUtility(User workflow, List<VmMobility> vms,
                                    double candidateMakespan, double candidateSerialTime) {
        VmMobility best = vms.stream().max(Comparator.comparingDouble(VmMobility::getTotalMips)).orElseThrow();
        double referenceSerial = workflow.getCloudlets().stream()
                .mapToDouble(task -> executionTime(task, best)).sum();
        Map<CloudletMobility, Double> referenceFinish = new HashMap<>();
        for (CloudletMobility task : TaskPriorityCalculator.topologicalOrder(workflow.getCloudlets())) {
            double parentFinish = task.getParents().stream()
                    .mapToDouble(referenceFinish::get).max().orElse(0.0);
            referenceFinish.put(task, parentFinish + executionTime(task, best));
        }
        double referenceParallel = referenceFinish.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0);

        // Equation 20 is ambiguous about sort direction. Ratios represent slowdown,
        // so this implementation follows the paper's prose: lower utility is better.
        return ratio(candidateMakespan, referenceParallel) + ratio(candidateSerialTime, referenceSerial);
    }

    private double ratio(double value, double baseline) {
        return baseline == 0.0 ? 0.0 : value / baseline;
    }

    private boolean applySchedule(List<CloudletMobility> order, CandidateSchedule candidate,
                                  List<VmMobility> availableVms, int brokerId) {
        Set<Integer> availableIds = new HashSet<>();
        availableVms.forEach(vm -> availableIds.add(vm.getId()));
        boolean valid = candidate.assignments.entrySet().stream().allMatch(entry ->
                availableIds.contains(entry.getValue().getId())
                        && entry.getValue().getNumberOfPes() >= entry.getKey().getNumberOfPes());
        if (!valid) {
            return false;
        }
        for (CloudletMobility task : order) {
            task.setGuestId(candidate.assignments.get(task).getId());
            task.setUserId(brokerId);
            task.setPredictedEst(candidate.starts.get(task));
            task.setPredictedEft(candidate.finishes.get(task));
        }
        return true;
    }

    private List<CloudletMobility> dependencySafePriorityOrder(List<CloudletMobility> tasks,
                                                               List<CloudletMobility> criticalPath) {
        Set<CloudletMobility> critical = new HashSet<>(criticalPath);
        Comparator<CloudletMobility> priority = Comparator
                .comparing((CloudletMobility task) -> critical.contains(task)).reversed()
                .thenComparing(CloudletMobility::getSdmsPriority, Comparator.reverseOrder())
                .thenComparingInt(CloudletMobility::getCloudletId);
        List<CloudletMobility> remaining = new ArrayList<>(tasks);
        List<CloudletMobility> result = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<CloudletMobility> ready = remaining.stream()
                    .filter(task -> result.containsAll(task.getParents()))
                    .sorted(priority).toList();
            if (ready.isEmpty()) {
                throw new IllegalArgumentException("Workflow DAG contains a cycle");
            }
            result.addAll(ready);
            remaining.removeAll(ready);
        }
        return result;
    }

    static double executionTime(CloudletMobility task, VmMobility vm) {
        return task.getCloudletTotalLength() / vm.getTotalMips();
    }

    public static double communicationTime(User workflow, CloudletMobility parent,
                                           CloudletMobility child, VmMobility parentVm, VmMobility childVm) {
        double bandwidthMbps = Math.min(parentVm.getBw(), childVm.getBw());
        if (bandwidthMbps <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        // Edge data is MB and VM bandwidth is interpreted as Mbit/s, matching the
        // conventional CloudSim example inputs. Multiplication by 8 converts MB to Mbit.
        return workflow.getEdgeDataSize(parent, child) * 8.0 / bandwidthMbps;
    }

    int getLastCandidateCount() {
        return lastCandidateCount;
    }

    private record CandidateSchedule(Map<CloudletMobility, VmMobility> assignments,
                                     Map<CloudletMobility, Double> starts,
                                     Map<CloudletMobility, Double> finishes,
                                     double utility) {}
}
