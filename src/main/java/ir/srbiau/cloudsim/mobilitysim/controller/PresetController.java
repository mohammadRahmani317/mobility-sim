package ir.srbiau.cloudsim.mobilitysim.controller;

import ir.srbiau.cloudsim.mobilitysim.preset.ResourcePreset;
import ir.srbiau.cloudsim.mobilitysim.service.PresetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mobility-sim/presets")
public class PresetController {

    private final PresetService presetService;

    public PresetController(PresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    public List<ResourcePreset> getPresets() {
        return presetService.getPresets();
    }
}
