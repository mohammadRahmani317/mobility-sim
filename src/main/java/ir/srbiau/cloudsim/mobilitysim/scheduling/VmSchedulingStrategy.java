package ir.srbiau.cloudsim.mobilitysim.scheduling;

import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;

import java.util.List;

public interface VmSchedulingStrategy {
    void assignCloudletsToVms(List<User> users, List<VmMobility> vms,Integer brokerId);
}
