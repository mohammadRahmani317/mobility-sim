package ir.srbiau.cloudsim.mobilitysim.model;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import java.util.ArrayList;
import java.util.List;

public class CloudletMobility extends Cloudlet {
    private int priority;
    private List<CloudletMobility> parents;
    private double x;
    private double y;


    public CloudletMobility(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                            UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.parents = new ArrayList<>();
        this.x = Math.random() * 1000;
        this.y = Math.random() * 1000;
    }


    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setParents(List<CloudletMobility> parents) {
        this.parents = parents;
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


    @Override
    public String toString() {
        return "CloudletMobility{" +
               "priority=" + priority +
               ", x=" + x +
               ", y=" + y +
               ", length=" + getCloudletLength() +
               ", pes=" + getNumberOfPes() +
               '}';
    }
}

