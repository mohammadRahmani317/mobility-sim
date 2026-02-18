package ir.srbiau.cloudsim.mobilitysim.dto;

public record HostConfig(
        int ram,
        long storage,
        long bw,
        int pes,
        int mipsPerPe
) {}
