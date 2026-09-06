package ir.srbiau.cloudsim.mobilitysim.service;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;
import ir.srbiau.cloudsim.mobilitysim.scheduling.SdmsSchedulingStrategy;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudActionTags;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Minimal broker extension that turns the model's parent list into real CloudSim
 * execution constraints. It intentionally leaves VM creation and cloudlet execution
 * to the standard CloudSim broker/datacenter implementation.
 */
public class WorkflowDatacenterBroker extends DatacenterBroker {
    private final Set<CloudletMobility> completed = new HashSet<>();
    private final Set<CloudletMobility> released = new HashSet<>();
    private int expectedCloudlets;

    public WorkflowDatacenterBroker(String name) throws Exception {
        super(name);
    }

    public void setExpectedCloudlets(int expectedCloudlets) {
        this.expectedCloudlets = expectedCloudlets;
    }

    @Override
    protected void submitCloudlets() {
        releaseReadyCloudlets();
    }

    @Override
    protected void processCloudletReturn(SimEvent event) {
        CloudletMobility cloudlet = (CloudletMobility) event.getData();
        getCloudletReceivedList().add(cloudlet);
        completed.add(cloudlet);
        cloudletsSubmitted--;

        if (getCloudletReceivedList().size() == expectedCloudlets) {
            clearDatacenters();
            finishExecution();
            return;
        }
        releaseReadyCloudlets();
    }

    @Override
    protected void processOtherEvent(SimEvent event) {
        if (event.getTag() == CloudActionTags.VM_BROKER_EVENT
                && event.getData() instanceof CloudletMobility cloudlet) {
            submitOne(cloudlet);
            return;
        }
        super.processOtherEvent(event);
    }

    private void releaseReadyCloudlets() {
        List<CloudletMobility> waiting = new ArrayList<>(this.<CloudletMobility>getCloudletList());
        for (CloudletMobility task : waiting) {
            if (!released.contains(task) && completed.containsAll(task.getParents())) {
                released.add(task);
                getCloudletList().remove(task);
                double delay = communicationDelay(task);
                if (delay > 0) {
                    schedule(getId(), delay, CloudActionTags.VM_BROKER_EVENT, task);
                } else {
                    submitOne(task);
                }
            }
        }
    }

    private void submitOne(CloudletMobility task) {
        // Isolate one ready task because the standard method submits every item in its list.
        List<CloudletMobility> stillWaiting = new ArrayList<>(this.<CloudletMobility>getCloudletList());
        getCloudletList().clear();
        getCloudletList().add(task);
        super.submitCloudlets();
        getCloudletList().addAll(stillWaiting);
    }

    private double communicationDelay(CloudletMobility child) {
        double readyAt = CloudSim.clock();
        for (CloudletMobility parent : child.getParents()) {
            if (parent.getGuestId() == child.getGuestId()) {
                continue;
            }
            VmMobility parentVm = findVm(parent.getGuestId());
            VmMobility childVm = findVm(child.getGuestId());
            User workflow = child.getUser();
            double transferFinish = parent.getExecFinishTime()
                    + SdmsSchedulingStrategy.communicationTime(
                    workflow, parent, child, parentVm, childVm);
            readyAt = Math.max(readyAt, transferFinish);
        }
        return Math.max(0.0, readyAt - CloudSim.clock());
    }

    private VmMobility findVm(int vmId) {
        return this.<VmMobility>getGuestList().stream()
                .filter(vm -> vm.getId() == vmId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Assigned VM " + vmId + " is unavailable"));
    }
}
