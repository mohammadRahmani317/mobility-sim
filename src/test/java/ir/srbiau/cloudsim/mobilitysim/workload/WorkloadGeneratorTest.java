package ir.srbiau.cloudsim.mobilitysim.workload;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadGeneratorTest {

    @Test
    void generatedWorkloadsContainReproducibleAcyclicDependenciesAndDeadlines() {
        List<User> first = new WorkloadGenerator(42L).generateUsers();
        List<User> second = new WorkloadGenerator(42L).generateUsers();

        assertEquals(first.size(), second.size());
        assertTrue(first.stream().allMatch(user -> user.getDeadline() > 0));
        assertTrue(first.stream().allMatch(user -> !user.getWorkflowEdges().isEmpty()));
        assertEquals(first.stream().flatMap(user -> user.getWorkflowEdges().stream()).toList(),
                second.stream().flatMap(user -> user.getWorkflowEdges().stream()).toList());

        for (User user : first) {
            Set<CloudletMobility> visited = new HashSet<>();
            Set<CloudletMobility> visiting = new HashSet<>();
            user.getCloudlets().forEach(task -> assertFalse(hasCycle(task, visited, visiting)));
            assertTrue(user.getWorkflowEdges().stream().allMatch(edge -> edge.dataSize() > 0));
        }
    }

    private boolean hasCycle(CloudletMobility task, Set<CloudletMobility> visited,
                             Set<CloudletMobility> visiting) {
        if (visiting.contains(task)) return true;
        if (!visited.add(task)) return false;
        visiting.add(task);
        for (CloudletMobility parent : task.getParents()) {
            if (hasCycle(parent, visited, visiting)) return true;
        }
        visiting.remove(task);
        return false;
    }
}
