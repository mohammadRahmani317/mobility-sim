package ir.srbiau.cloudsim.mobilitysim.dto.response;

import java.util.List;

public record SimulationResultDto (
        List<CloudletStatus> cloudletStatuses
){
}
