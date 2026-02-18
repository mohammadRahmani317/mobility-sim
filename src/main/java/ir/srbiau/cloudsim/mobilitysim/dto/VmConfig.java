package ir.srbiau.cloudsim.mobilitysim.dto;

public record VmConfig(
        int mips,
        int pes,
        int ram,
        long bw,
        long size,
        String vmm
) {}
