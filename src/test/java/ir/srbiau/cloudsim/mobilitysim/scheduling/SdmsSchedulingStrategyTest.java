package ir.srbiau.cloudsim.mobilitysim.scheduling;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.UserLevel;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdmsSchedulingStrategyTest {

    @Test
    void prtUsesDagFactorsInsteadOfCloudletLengthAlone() {
        User workflow = workflow(100);
        CloudletMobility root = task(0, 1000, workflow);
        CloudletMobility child = task(1, 1000, workflow);
        CloudletMobility leaf = task(2, 1000, workflow);
        workflow.addCloudlet(root);
        workflow.addCloudlet(child);
        workflow.addCloudlet(leaf);
        workflow.addWorkflowEdge(root, child, 100);
        workflow.addWorkflowEdge(child, leaf, 10);

        Map<CloudletMobility, Double> priorities = new TaskPriorityCalculator()
                .calculatePriorities(workflow, List.of(vm(0, 1000), vm(1, 2000)));

        assertTrue(priorities.values().stream().allMatch(value -> value >= 0 && value <= 1));
        assertNotEquals(priorities.get(root), priorities.get(leaf));
    }

    @Test
    void criticalPathIsThePathWithHighestTotalPrt() {
        User workflow = workflow(100);
        CloudletMobility root = task(0, 1000, workflow);
        CloudletMobility high = task(1, 1000, workflow);
        CloudletMobility low = task(2, 1000, workflow);
        CloudletMobility sink = task(3, 1000, workflow);
        List.of(root, high, low, sink).forEach(workflow::addCloudlet);
        workflow.addWorkflowEdge(root, high, 1);
        workflow.addWorkflowEdge(root, low, 1);
        workflow.addWorkflowEdge(high, sink, 1);
        workflow.addWorkflowEdge(low, sink, 1);
        root.setSdmsPriority(0.2);
        high.setSdmsPriority(0.9);
        low.setSdmsPriority(0.1);
        sink.setSdmsPriority(0.2);

        assertEquals(List.of(root, high, sink), new CriticalPathExtractor().extract(workflow));
        assertThrows(IllegalArgumentException.class,
                () -> workflow.addWorkflowEdge(sink, root, 1));
    }

    @Test
    void deadlineRejectsEveryAssignmentWhenItIsImpossible() {
        User workflow = workflow(0.1);
        workflow.addCloudlet(task(0, 10_000, workflow));
        SdmsSchedulingStrategy strategy = new SdmsSchedulingStrategy();

        strategy.assignCloudletsToVms(List.of(workflow), List.of(vm(0, 1000)), 7);

        assertTrue(workflow.isSchedulingFailed());
        assertEquals(0, strategy.getLastCandidateCount());
    }

    @Test
    void sdmsCreatesValidDependencySafeVmAssignmentsAndTiming() {
        User workflow = workflow(100);
        CloudletMobility root = task(0, 2000, workflow);
        CloudletMobility child = task(1, 3000, workflow);
        workflow.addCloudlet(root);
        workflow.addCloudlet(child);
        workflow.addWorkflowEdge(root, child, 20);

        new SdmsSchedulingStrategy().assignCloudletsToVms(
                List.of(workflow), List.of(vm(0, 1000), vm(1, 2000)), 7);

        assertFalse(workflow.isSchedulingFailed());
        assertTrue(root.getGuestId() >= 0);
        assertTrue(child.getGuestId() >= 0);
        assertEquals(7, root.getUserId());
        assertTrue(child.getPredictedEst() >= root.getPredictedEft());
        assertTrue(child.getPredictedEft() <= workflow.getDeadline());
    }

    private User workflow(double deadline) {
        User user = new User(0, UserLevel.EASY, "test");
        user.setDeadline(deadline);
        return user;
    }

    private CloudletMobility task(int id, long length, User user) {
        UtilizationModelFull utilization = new UtilizationModelFull();
        return new CloudletMobility(id, length, 1, 1, 1,
                utilization, utilization, utilization, user);
    }

    private VmMobility vm(int id, int mips) {
        return new VmMobility(id, 7, mips, 1, 2048, 1000, 10_000,
                "Xen", new CloudletSchedulerSpaceShared());
    }
}
