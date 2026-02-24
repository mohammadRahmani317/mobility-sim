package ir.srbiau.cloudsim.mobilitysim.scheduling;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("RoundRobin")
public class RoundRobin implements VmSchedulingStrategy {

    private int currentVmIndex = 0;

    @Override
    public void assignCloudletsToVms(List<User> users, List<VmMobility> vms, Integer brokerId) {
        for (User user : users) {
            VmMobility vm = vms.get(currentVmIndex);
            for (CloudletMobility cloudlet : user.getCloudlets()) {
                cloudlet.setGuestId(vm.getId());
                cloudlet.setUserId(brokerId);
            }
            currentVmIndex = (currentVmIndex + 1) % vms.size();
        }
    }
}