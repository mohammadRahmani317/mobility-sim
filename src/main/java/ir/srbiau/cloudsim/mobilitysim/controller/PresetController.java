package ir.srbiau.cloudsim.mobilitysim.controller;

import ir.srbiau.cloudsim.mobilitysim.dto.preset.ResourcePreset;
import ir.srbiau.cloudsim.mobilitysim.service.PresetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
