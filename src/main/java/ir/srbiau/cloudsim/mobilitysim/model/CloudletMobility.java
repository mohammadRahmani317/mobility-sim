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
    private User user;
    private double sdmsPriority;
    private double predictedEst;
    private double predictedEft;


    public CloudletMobility(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                            UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, User user) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.parents = new ArrayList<>();
        this.x = Math.random() * 1000;
        this.y = Math.random() * 1000;
        this.user = user;
    }


    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setParents(List<CloudletMobility> parents) {
        this.parents = new ArrayList<>(parents);
    }

    public List<CloudletMobility> getParents() {
        return List.copyOf(parents);
    }

    public double getSdmsPriority() {
        return sdmsPriority;
    }

    public void setSdmsPriority(double sdmsPriority) {
        this.sdmsPriority = sdmsPriority;
    }

    public double getPredictedEst() {
        return predictedEst;
    }

    public void setPredictedEst(double predictedEst) {
        this.predictedEst = predictedEst;
    }

    public double getPredictedEft() {
        return predictedEft;
    }

    public void setPredictedEft(double predictedEft) {
        this.predictedEft = predictedEft;
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

    public User getUser() {
        return user;
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
