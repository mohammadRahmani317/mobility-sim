package ir.srbiau.cloudsim.mobilitysim.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final int id;
    private double x;
    private double y;
    private final List<CloudletMobility> cloudlets;

    public User(int id) {
        this.id = id;
        this.x = Math.random() * 1000;
        this.y = Math.random() * 1000;
        this.cloudlets = new ArrayList<>();
    }


    public void addCloudlet(CloudletMobility cloudlet) {
        this.cloudlets.add(cloudlet);
    }


    public int getId() {
        return id;
    }

    public List<CloudletMobility> getCloudlets() {
        return cloudlets;
    }
}

