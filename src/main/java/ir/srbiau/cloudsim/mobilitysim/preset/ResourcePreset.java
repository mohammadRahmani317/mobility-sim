package ir.srbiau.cloudsim.mobilitysim.preset;

import ir.srbiau.cloudsim.mobilitysim.dto.DatacenterConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.HostConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.VmConfig;

public record ResourcePreset(
        PresetType type,
        DatacenterConfig datacenter,
        HostConfig host,
        VmConfig vm
) {}
