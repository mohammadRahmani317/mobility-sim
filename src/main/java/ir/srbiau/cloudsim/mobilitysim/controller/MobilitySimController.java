package ir.srbiau.cloudsim.mobilitysim.controller;

import ir.srbiau.cloudsim.mobilitysim.dto.request.ResourceRequestDto;
import ir.srbiau.cloudsim.mobilitysim.dto.response.SimulationResultDto;
import ir.srbiau.cloudsim.mobilitysim.service.MobilitySimService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobility-sim")
public class MobilitySimController {

    private MobilitySimService mobilitySimService;

    public MobilitySimController(MobilitySimService mobilitySimService) {
        this.mobilitySimService = mobilitySimService;
    }

    @PostMapping
    public ResponseEntity<SimulationResultDto> run(@RequestBody ResourceRequestDto dto) {
        return mobilitySimService.run(dto);
    }

}
