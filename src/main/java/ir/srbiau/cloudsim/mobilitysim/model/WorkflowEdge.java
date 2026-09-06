package ir.srbiau.cloudsim.mobilitysim.model;

/** dataSize is expressed in megabytes. */
public record WorkflowEdge(int sourceCloudletId, int targetCloudletId, double dataSize) {
    public WorkflowEdge {
        if (dataSize < 0) {
            throw new IllegalArgumentException("Workflow edge data size cannot be negative");
        }
    }
}
