package ir.srbiau.cloudsim.mobilitysim.model;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

public class VmMobility extends Vm {

    private final double x;
    private final double y;

    public VmMobility(int id, int userId, double mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
        this.x = Math.random() * 1000;
        this.y = Math.random() * 1000;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "VmMobility{" +
               "x=" + x +
               ", y=" + y +
               '}';
    }
}
