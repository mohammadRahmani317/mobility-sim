package ir.srbiau.cloudsim.mobilitysim.service;

import ir.srbiau.cloudsim.mobilitysim.dto.ResourceRequestDto;
import ir.srbiau.cloudsim.mobilitysim.dto.SimulationResultDto;
import ir.srbiau.cloudsim.mobilitysim.dto.VmConfig;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;
import ir.srbiau.cloudsim.mobilitysim.workload.WorkloadGenerator;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.CloudSimExample3;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

@Service
public class MobilitySimService {
    @Autowired
    private WorkloadGenerator workloadGenerator;

    public ResponseEntity<SimulationResultDto> run(ResourceRequestDto dto) {
        try {
            CloudSimExample3.main(new String[]{});
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            CloudSim.init(num_user, calendar, trace_flag);
            createDatacenter("Datacenter_0");
            DatacenterBroker broker = new DatacenterBroker("Broker");
            List<VmMobility> vms = createVm(dto.vms(), broker.getId());
            broker.submitGuestList(vms);
            List<User> users = workloadGenerator.generateUsers();

            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //.....
        return null;
    }

    private List<VmMobility> createVm(List<VmConfig> vmConfigs, int brokerId) {

        final int[] vmid = {0};
        List<VmMobility> vmList = new ArrayList<>();
        vmConfigs.forEach(vmConfig -> {
            vmList.add(new VmMobility(
                    vmid[0],
                    brokerId,
                    vmConfig.mips(),
                    vmConfig.pes(),
                    vmConfig.ram(),
                    vmConfig.bw(),
                    vmConfig.size(),
                    vmConfig.vmm(),
                    new CloudletSchedulerSpaceShared()
            ));
            vmid[0] = vmid[0] + 1;
        });
        return vmList;
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList();
        List<Pe> peList = new ArrayList();
        int mips = 1000;
        peList.add(new Pe(0, new PeProvisionerSimple((double) mips)));
        int ram = 2048;
        long storage = 1000000L;
        int bw = 10000;
        hostList.add(new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple((long) bw), storage, peList, new VmSchedulerTimeShared(peList)));
        hostList.add(new Host(1, new RamProvisionerSimple(ram), new BwProvisionerSimple((long) bw), storage, peList, new VmSchedulerTimeShared(peList)));
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


    private void createDatacenter(ResourceRequestDto dto) {
        final int[] peId = {0};
        final int[] hostId = {0};
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();

        dto.hosts().forEach(hs -> {
            peList.add(new Pe(peId[0], new PeProvisionerSimple(hs.mipsPerPe())));
            hostList.add(new Host(hostId[0], new RamProvisionerSimple(hs.ram()), new BwProvisionerSimple(hs.bw()), hs.storage(), peList, new VmSchedulerSpaceShared(peList)));
            peId[0] = peId[0] + 1;
            hostId[0] = hostId[0] + 1;
        });

        int dcCounter = 1;
        dto.datacenters().forEach(dc -> {
            String dcName = "Datacenter-" + dcCounter;
            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                    dc.architecture(),
                    dc.os(),
                    dc.vmm(),
                    hostList,
                    dc.timeZone(),
                    dc.costPerSec(),
                    dc.costPerMem(),
                    dc.costPerStorage(),
                    dc.costPerBw()
            );

            try {
                new Datacenter(dcName, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0.0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }


}
