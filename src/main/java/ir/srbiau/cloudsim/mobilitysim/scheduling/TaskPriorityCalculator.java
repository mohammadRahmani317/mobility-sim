package ir.srbiau.cloudsim.mobilitysim.scheduling;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskPriorityCalculator {
    // The paper does not prescribe usable numeric weights for this adaptation.
    // Equal, configurable-in-one-place weights are therefore an explicit assumption.
    public static final double COMMUNICATION_WEIGHT = 0.2;
    public static final double TIME_PRESSURE_WEIGHT = 0.2;
    public static final double SUCCESSOR_WEIGHT = 0.2;
    public static final double EXECUTION_WEIGHT = 0.2;
    public static final double RESOURCE_AVAILABILITY_WEIGHT = 0.2;

    public Map<CloudletMobility, Double> calculatePriorities(User workflow, List<VmMobility> vms) {
        if (vms.isEmpty()) {
            throw new IllegalArgumentException("SDMS requires at least one VM");
        }

        List<CloudletMobility> tasks = workflow.getCloudlets();
        Map<CloudletMobility, List<CloudletMobility>> successors = successors(tasks);
        Map<CloudletMobility, Double> communication = new HashMap<>();
        Map<CloudletMobility, Double> pressure = new HashMap<>();
        Map<CloudletMobility, Double> successorCount = new HashMap<>();
        Map<CloudletMobility, Double> execution = new HashMap<>();
        Map<CloudletMobility, Double> availability = new HashMap<>();

        double fastestMips = vms.stream().mapToDouble(VmMobility::getTotalMips).max().orElseThrow();
        Map<CloudletMobility, Double> earliestFinish = new HashMap<>();
        for (CloudletMobility task : topologicalOrder(tasks)) {
            double earliestStart = task.getParents().stream()
                    .mapToDouble(parent -> earliestFinish.getOrDefault(parent, 0.0))
                    .max().orElse(0.0);
            double fastestExecution = task.getCloudletTotalLength() / fastestMips;
            earliestFinish.put(task, earliestStart + fastestExecution);
            double remaining = Math.max(workflow.getDeadline() - earliestStart, 1.0e-9);
            pressure.put(task, fastestExecution / remaining);

            double averageExecution = vms.stream()
                    .filter(vm -> vm.getNumberOfPes() >= task.getNumberOfPes())
                    .mapToDouble(vm -> task.getCloudletTotalLength() / vm.getTotalMips())
                    .average().orElse(Double.MAX_VALUE / 4);
            execution.put(task, averageExecution);

            long compatible = vms.stream()
                    .filter(vm -> vm.getNumberOfPes() >= task.getNumberOfPes()).count();
            // Cloudlets only expose PE demand in this project. The fraction of compatible
            // VMs is the practical SRR/resource-availability approximation.
            availability.put(task, compatible / (double) vms.size());
            successorCount.put(task, (double) successors.get(task).size());

            double dataMb = workflow.getWorkflowEdges().stream()
                    .filter(edge -> edge.sourceCloudletId() == task.getCloudletId()
                            || edge.targetCloudletId() == task.getCloudletId())
                    .mapToDouble(edge -> edge.dataSize()).sum();
            communication.put(task, dataMb);
        }

        Map<CloudletMobility, Double> result = new HashMap<>();
        for (CloudletMobility task : tasks) {
            double priority = COMMUNICATION_WEIGHT * normalize(communication, task)
                    + TIME_PRESSURE_WEIGHT * normalize(pressure, task)
                    + SUCCESSOR_WEIGHT * normalize(successorCount, task)
                    + EXECUTION_WEIGHT * normalize(execution, task)
                    + RESOURCE_AVAILABILITY_WEIGHT * normalize(availability, task);
            task.setSdmsPriority(priority);
            result.put(task, priority);
        }
        return result;
    }

    private double normalize(Map<CloudletMobility, Double> values, CloudletMobility task) {
        double min = values.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return max == min ? 0.0 : (values.get(task) - min) / (max - min);
    }

    private Map<CloudletMobility, List<CloudletMobility>> successors(List<CloudletMobility> tasks) {
        Map<CloudletMobility, List<CloudletMobility>> result = new HashMap<>();
        tasks.forEach(task -> result.put(task, new ArrayList<>()));
        tasks.forEach(task -> task.getParents().forEach(parent -> {
            if (!result.containsKey(parent)) {
                throw new IllegalArgumentException("A parent task belongs to another workflow");
            }
            result.get(parent).add(task);
        }));
        return result;
    }

    static List<CloudletMobility> topologicalOrder(List<CloudletMobility> tasks) {
        List<CloudletMobility> remaining = new ArrayList<>(tasks);
        List<CloudletMobility> ordered = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<CloudletMobility> ready = remaining.stream()
                    .filter(task -> ordered.containsAll(task.getParents())).toList();
            if (ready.isEmpty()) {
                throw new IllegalArgumentException("Workflow DAG contains a cycle or foreign parent");
            }
            ordered.addAll(ready);
            remaining.removeAll(ready);
        }
        return ordered;
    }
}
