package ir.srbiau.cloudsim.mobilitysim.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final int id;
    private double x;
    private double y;
    private UserLevel userLevel;
    private List<CloudletMobility> cloudlets;
    private String name;
    private final List<WorkflowEdge> workflowEdges = new ArrayList<>();
    private double deadline;
    private boolean schedulingFailed;

    public User(int id, UserLevel userLevel, String name) {
        this.id = id;
        this.userLevel = userLevel;
        this.x = Math.random() * 1000;
        this.y = Math.random() * 1000;
        this.cloudlets = new ArrayList<>();
        this.name = name;
    }


    public void addCloudlet(CloudletMobility cloudlet) {
        this.cloudlets.add(cloudlet);
    }


    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public UserLevel getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(UserLevel userLevel) {
        this.userLevel = userLevel;
    }

    public List<CloudletMobility> getCloudlets() {
        return cloudlets;
    }

    public String getName() {
        return name;
    }

    public List<WorkflowEdge> getWorkflowEdges() {
        return List.copyOf(workflowEdges);
    }

    public void addWorkflowEdge(CloudletMobility source, CloudletMobility target, double dataSize) {
        if (!cloudlets.contains(source) || !cloudlets.contains(target)) {
            throw new IllegalArgumentException("Workflow edges must connect tasks in the same workflow");
        }
        if (source == target || target.getParents().contains(source)) {
            if (source == target) {
                throw new IllegalArgumentException("A task cannot depend on itself");
            }
            return;
        }
        if (hasAncestor(source, target, new java.util.HashSet<>())) {
            throw new IllegalArgumentException("Workflow edge would create a cycle");
        }
        List<CloudletMobility> parents = new ArrayList<>(target.getParents());
        parents.add(source);
        target.setParents(parents);
        workflowEdges.add(new WorkflowEdge(source.getCloudletId(), target.getCloudletId(), dataSize));
    }

    private boolean hasAncestor(CloudletMobility task, CloudletMobility possibleAncestor,
                                java.util.Set<CloudletMobility> visited) {
        if (!visited.add(task)) {
            return false;
        }
        if (task.getParents().contains(possibleAncestor)) {
            return true;
        }
        return task.getParents().stream()
                .anyMatch(parent -> hasAncestor(parent, possibleAncestor, visited));
    }

    public double getEdgeDataSize(CloudletMobility source, CloudletMobility target) {
        return workflowEdges.stream()
                .filter(edge -> edge.sourceCloudletId() == source.getCloudletId()
                        && edge.targetCloudletId() == target.getCloudletId())
                .mapToDouble(WorkflowEdge::dataSize)
                .findFirst()
                .orElse(0.0);
    }

    public double getDeadline() {
        return deadline;
    }

    public void setDeadline(double deadline) {
        this.deadline = deadline;
    }

    public boolean isSchedulingFailed() {
        return schedulingFailed;
    }

    public void setSchedulingFailed(boolean schedulingFailed) {
        this.schedulingFailed = schedulingFailed;
    }
}
