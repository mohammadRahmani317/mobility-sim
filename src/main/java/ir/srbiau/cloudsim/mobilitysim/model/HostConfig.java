package ir.srbiau.cloudsim.mobilitysim.model;

public record HostConfig(
        int ram,
        long storage,
        long bw,
        int pes,
        int mipsPerPe
) {}
