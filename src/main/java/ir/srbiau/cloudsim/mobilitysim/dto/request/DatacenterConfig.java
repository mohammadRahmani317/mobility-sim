package ir.srbiau.cloudsim.mobilitysim.dto.request;

public record DatacenterConfig(
        String architecture,
        String os,
        String vmm,
        double timeZone,
        double costPerSec,
        double costPerMem,
        double costPerStorage,
        double costPerBw
) {}
