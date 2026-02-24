package ir.srbiau.cloudsim.mobilitysim.dto.preset;

import ir.srbiau.cloudsim.mobilitysim.dto.request.DatacenterConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.request.HostConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.request.VmConfig;

import java.util.List;

public record ResourcePreset(
        PresetType type,
        List<DatacenterConfig> datacenters,
        List<HostConfig> hosts,
        List<VmConfig> vms
) {}
