package ir.srbiau.cloudsim.mobilitysim.model;

import java.util.List;

public record SimulationScenario(
        List<DatacenterConfig> datacenters,
        List<HostConfig> hosts,
        List<VmConfig> vms
) {}
