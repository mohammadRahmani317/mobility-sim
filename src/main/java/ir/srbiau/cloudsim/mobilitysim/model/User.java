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
}

