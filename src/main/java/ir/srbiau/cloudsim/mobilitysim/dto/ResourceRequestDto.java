package ir.srbiau.cloudsim.mobilitysim.dto;

import java.util.List;

public record ResourceRequestDto(
        List<DatacenterConfig> datacenters,
        List<HostConfig> hosts,
        List<VmConfig> vms
) {
}
