package ir.srbiau.cloudsim.mobilitysim.preset;

import ir.srbiau.cloudsim.mobilitysim.model.*;

public record ResourcePreset(
        PresetType type,
        DatacenterConfig datacenter,
        HostConfig host,
        VmConfig vm
) {}
