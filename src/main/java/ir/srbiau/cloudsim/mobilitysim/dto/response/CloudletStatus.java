package ir.srbiau.cloudsim.mobilitysim.dto.response;

public record CloudletStatus(
        int cloudletId,
        String status,
        int vmId,
        String startTime,
        String finishTime
) {
}
