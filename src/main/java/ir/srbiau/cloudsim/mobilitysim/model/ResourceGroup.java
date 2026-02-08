package ir.srbiau.cloudsim.mobilitysim.model;

public record ResourceGroup<T>(
        T config,
        int count
) {}
