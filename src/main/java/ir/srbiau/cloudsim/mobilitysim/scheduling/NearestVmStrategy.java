package ir.srbiau.cloudsim.mobilitysim.scheduling;

import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NearestVmStrategy implements VmSchedulingStrategy {

    @Override
    public void assignCloudletsToVms(List<User> users, List<VmMobility> vms, Integer brokerId) {
        for (User user : users) {
            VmMobility nearest = findNearestVm(user, vms);


            for (CloudletMobility cloudlet : user.getCloudlets()) {
                cloudlet.setGuestId(nearest.getId());
                cloudlet.setUserId(brokerId);
            }
        }
    }

    private VmMobility findNearestVm(User user, List<VmMobility> vms) {

        VmMobility nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (VmMobility vm : vms) {

            double distance = Math.sqrt(
                    Math.pow(vm.getX() - user.getX(), 2) +
                    Math.pow(vm.getY() - user.getY(), 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearest = vm;
            }
        }

        return nearest;
    }
}