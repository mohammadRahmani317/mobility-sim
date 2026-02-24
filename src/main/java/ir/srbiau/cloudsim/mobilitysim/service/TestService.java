package ir.srbiau.cloudsim.mobilitysim.service;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

@Service
public class TestService {
    public static DatacenterBroker broker;
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public void runTest() {

        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            CloudSim.init(num_user, calendar, trace_flag);
            createDatacenter("Datacenter_0");
            broker = new DatacenterBroker("Broker");
            int brokerId = broker.getId();
            vmlist = new ArrayList();
            int mips = 1000;
            long size = 10000L;
            int ram = 512;
            long bw = 1000L;
            int pesNumber = 1;
            String vmm = "Xen";
            vmlist.add(new Vm(0, brokerId, (double)mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared()));
            vmlist.add(new Vm(1, brokerId, (double)mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared()));
            broker.submitGuestList(vmlist);
            cloudletList = new ArrayList();
            int id = 0;
            long fileSize = 300L;
            long outputSize = 300L;
            UtilizationModel utilizationModel = new UtilizationModelFull();
            Cloudlet cloudlet1 = new Cloudlet(id, 10000L, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet1.setUserId(brokerId);
            cloudlet1.setGuestId(0);
            cloudletList.add(cloudlet1);
            ++id;
            Cloudlet cloudlet2 = new Cloudlet(id ,100000L, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet2.setUserId(brokerId);
            cloudlet2.setGuestId(0);
            cloudletList.add(cloudlet2);
            ++id;
            Cloudlet cloudlet3 = new Cloudlet(id, 1000000L, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet3.setUserId(brokerId);
            cloudlet3.setGuestId(0);
            cloudletList.add(cloudlet3);
            ++id;
            Cloudlet cloudlet4 = new Cloudlet(id, 10000L, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet4.setUserId(brokerId);
            cloudlet4.setGuestId(1);
            cloudletList.add(cloudlet4);
            ++id;
            Cloudlet cloudlet5 = new Cloudlet(id, 100000L, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet5.setUserId(brokerId);
            cloudlet5.setGuestId(1);
            cloudletList.add(cloudlet5);
            ++id;
            Cloudlet cloudlet6 = new Cloudlet(id, 1000000L, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet6.setUserId(brokerId);
            cloudlet6.setGuestId(1);
            cloudletList.add(cloudlet6);
            broker.submitCloudletList(cloudletList);
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            newList.sort(Comparator.comparing(Cloudlet::getCloudletId));
            printCloudletList(newList);
            Log.println("CloudSimExample9 finished!");
        } catch (Exception var26) {
            Exception e = var26;
            e.printStackTrace();
            Log.println("Unwanted errors happen");
        }


    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList();
        List<Pe> peList = new ArrayList();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple((double)mips)));
        int ram = 2048;
        long storage = 1000000L;
        int bw = 10000;
        hostList.add(new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple((long)bw), storage, peList, new VmSchedulerTimeShared(peList)));
        hostList.add(new Host(1, new RamProvisionerSimple(ram), new BwProvisionerSimple((long)bw), storage, peList, new VmSchedulerTimeShared(peList)));
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList();
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
        Datacenter datacenter = null;

        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0.0);
        } catch (Exception var25) {
            Exception e = var25;
            e.printStackTrace();
        }

        return datacenter;
    }

    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        String indent = "    ";
        Log.println();
        Log.println("========== OUTPUT ==========");
        Log.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");
        DecimalFormat dft = new DecimalFormat("###.##");
        Iterator var5 = list.iterator();

        while(var5.hasNext()) {
            Cloudlet value = (Cloudlet)var5.next();
            Cloudlet cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.print("SUCCESS");
                Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getGuestId() + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + dft.format(cloudlet.getExecFinishTime()));
            }
        }

    }
}
