package ir.srbiau.cloudsim.mobilitysim.service;

import ir.srbiau.cloudsim.mobilitysim.preset.*;
import ir.srbiau.cloudsim.mobilitysim.model.*;
import org.cloudbus.cloudsim.examples.CloudSimExample1;
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
                new DatacenterConfig(
                        "x64",
                        "Linux",
                        "Xen",
                        10.0,
                        0.01,
                        0.005,
                        0.001,
                        0.0
                ),
                new HostConfig(
                        16384,
                        1_000_000,
                        10_000,
                        8,
                        1000
                ),
                new VmConfig(
                        1000,
                        2,
                        2048,
                        1000,
                        10_000,
                        "Xen"
                )
        );
    }

    private ResourcePreset windowsPreset() {
        return new ResourcePreset(
                PresetType.WINDOWS,
                new DatacenterConfig(
                        "x86",
                        "Windows",
                        "Hyper-V",
                        12.0,
                        0.02,
                        0.01,
                        0.002,
                        0.0
                ),
                new HostConfig(
                        32768,
                        2_000_000,
                        20_000,
                        16,
                        2000
                ),
                new VmConfig(
                        2000,
                        4,
                        4096,
                        2000,
                        20_000,
                        "Hyper-V"
                )
        );
    }
}
