package ir.srbiau.cloudsim.mobilitysim.dto.request;

import java.util.List;

public record ResourceRequestDto(
        List<DatacenterConfig> datacenters,
        List<HostConfig> hosts,
        List<VmConfig> vms
) {
}
