package ir.srbiau.cloudsim.mobilitysim.dto;

import ir.srbiau.cloudsim.mobilitysim.scheduling.SchedulingAlgorithm;

import java.util.List;

public record ResourceRequestDto(
        List<DatacenterConfig> datacenters,
        List<HostConfig> hosts,
        List<VmConfig> vms,
        SchedulingAlgorithm schedulingAlgorithm
) {
    public SchedulingAlgorithm selectedSchedulingAlgorithm() {
        return schedulingAlgorithm == null ? SchedulingAlgorithm.ROUND_ROBIN : schedulingAlgorithm;
    }
}
