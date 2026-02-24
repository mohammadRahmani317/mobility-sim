package ir.srbiau.cloudsim.mobilitysim.service;

import ir.srbiau.cloudsim.mobilitysim.dto.request.DatacenterConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.request.HostConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.request.VmConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.preset.PresetType;
import ir.srbiau.cloudsim.mobilitysim.dto.preset.ResourcePreset;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PresetService {

    public List<ResourcePreset> getPresets() {
        return List.of(
                linuxPreset(),
                windowsPreset()
        );

    }

    private ResourcePreset linuxPreset() {
        return new ResourcePreset(
                PresetType.LINUX,
                List.of(
                        new DatacenterConfig("x64", "Linux", "Xen", 10.0, 0.01, 0.005, 0.001, 2.0)
                ),
                List.of(
                        new HostConfig(1024, 1_000_000, 10_000, 1, 1000),
                        new HostConfig(1024, 1_000_000, 10_000, 1, 1000)
                ),
                List.of(new VmConfig(1000, 1, 1024, 1000, 10_000, "Xen")));
    }

    private ResourcePreset windowsPreset() {
        return new ResourcePreset(
                PresetType.WINDOWS,
                List.of(new DatacenterConfig("x86", "Windows", "Hyper-V", 12.0, 0.02, 0.01, 0.002, 0.0)),
                List.of(
                        new HostConfig(2048, 2_000_000, 20_000, 1, 2000),
                        new HostConfig(2048, 2_000_000, 20_000, 1, 2000)
                ),
                List.of(new VmConfig(2000, 1, 1024, 2000, 20_000, "Hyper-V")));
    }

}
