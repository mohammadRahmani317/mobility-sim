package ir.srbiau.cloudsim.mobilitysim.scheduling;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CriticalPathExtractor {

    public List<CloudletMobility> extract(User workflow) {
        List<CloudletMobility> tasks = workflow.getCloudlets();
        Map<CloudletMobility, List<CloudletMobility>> successors = new HashMap<>();
        tasks.forEach(task -> successors.put(task, new ArrayList<>()));
        tasks.forEach(task -> task.getParents().forEach(parent -> {
            if (!successors.containsKey(parent)) {
                throw new IllegalArgumentException("A parent task belongs to another workflow");
            }
            successors.get(parent).add(task);
        }));

        Map<CloudletMobility, Path> memo = new HashMap<>();
        Set<CloudletMobility> visiting = new HashSet<>();
        // Visit every task so a cycle in a disconnected component cannot be hidden.
        tasks.forEach(task -> bestFrom(task, successors, memo, visiting));

        return tasks.stream()
                .filter(task -> task.getParents().isEmpty())
                .map(memo::get)
                .max((left, right) -> Double.compare(left.score, right.score))
                .map(path -> path.tasks)
                .orElseGet(List::of);
    }

    private Path bestFrom(CloudletMobility task,
                          Map<CloudletMobility, List<CloudletMobility>> successors,
                          Map<CloudletMobility, Path> memo,
                          Set<CloudletMobility> visiting) {
        if (memo.containsKey(task)) {
            return memo.get(task);
        }
        if (!visiting.add(task)) {
            throw new IllegalArgumentException("Workflow DAG contains a cycle");
        }

        Path bestChild = successors.get(task).stream()
                .map(child -> bestFrom(child, successors, memo, visiting))
                .max((left, right) -> Double.compare(left.score, right.score))
                .orElse(new Path(0.0, List.of()));
        visiting.remove(task);

        List<CloudletMobility> pathTasks = new ArrayList<>();
        pathTasks.add(task);
        pathTasks.addAll(bestChild.tasks);
        Path result = new Path(task.getSdmsPriority() + bestChild.score, List.copyOf(pathTasks));
        memo.put(task, result);
        return result;
    }

    private record Path(double score, List<CloudletMobility> tasks) {}
}
