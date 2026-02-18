package ir.srbiau.cloudsim.mobilitysim.model;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class VmMobility extends Vm {

    private double x;
    private double y;

    public VmMobility(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
        this.x = Math.random() * 1000;
        this.y = Math.random() * 1000;
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
        return "VmMobility{" +
               "x=" + x +
               ", y=" + y +
               '}';
    }
}
