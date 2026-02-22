package ir.srbiau.cloudsim.mobilitysim.service;

import ir.srbiau.cloudsim.mobilitysim.dto.ResourceRequestDto;
import ir.srbiau.cloudsim.mobilitysim.dto.SimulationResultDto;
import ir.srbiau.cloudsim.mobilitysim.dto.VmConfig;
import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;
import ir.srbiau.cloudsim.mobilitysim.scheduling.NearestVmStrategy;
import ir.srbiau.cloudsim.mobilitysim.scheduling.RoundRobin;
import ir.srbiau.cloudsim.mobilitysim.scheduling.VmSchedulingStrategy;
import ir.srbiau.cloudsim.mobilitysim.workload.WorkloadGenerator;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

@Service
public class MobilitySimService {
    @Autowired
    private WorkloadGenerator workloadGenerator;

    public ResponseEntity<SimulationResultDto> run(ResourceRequestDto dto) {
        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;
            CloudSim.init(num_user, calendar, trace_flag);
            createDatacenter(dto);
            DatacenterBroker broker = new DatacenterBroker("Broker");
            List<VmMobility> vms = createVm(dto.vms(), broker.getId());
            broker.submitGuestList(vms);
            List<User> users = workloadGenerator.generateUsers();
            VmSchedulingStrategy vmSchedulingStrategy = new RoundRobin();
            vmSchedulingStrategy.assignCloudletsToVms(
                    users,
                    vms,
                    broker.getId()
            );

            users.forEach(user -> broker.submitCloudletList(user.getCloudlets()));
            CloudSim.startSimulation();
            List<CloudletMobility> cloudletFinished = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();
            printCloudletList(cloudletFinished);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //.....
        return null;
    }

    private List<Datacenter> createDatacenter(ResourceRequestDto dto) {
        final int[] peId = {0};
        final int[] hostId = {0};
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();

        dto.hosts().forEach(hs -> {
            peList.add(new Pe(peId[0], new PeProvisionerSimple(hs.mipsPerPe())));
            hostList.add(new Host(hostId[0], new RamProvisionerSimple(hs.ram()), new BwProvisionerSimple(hs.bw()), hs.storage(), peList, new VmSchedulerTimeShared(peList)));
            peId[0] = peId[0] + 1;
            hostId[0] = hostId[0] + 1;
        });

        int dcCounter = 1;
        List<Datacenter> datacenters = new ArrayList<>();
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
            Datacenter datacenter;
            try {
                datacenter = new Datacenter(dcName, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0.0);
                datacenters.add(datacenter);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return datacenters;
    }

    private List<VmMobility> createVm(List<VmConfig> vmConfigs, int brokerId) {

        final int[] vmid = {0};
        List<VmMobility> vmList = new ArrayList<>();
        vmConfigs.forEach(vmConfig -> {
            vmList.add(new VmMobility(vmid[0],
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

    private static void printCloudletList(List<CloudletMobility> list) {
        int size = list.size();
        String indent = "    ";
        Log.println();
        Log.println("========== OUTPUT ==========");
        Log.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time" + indent + "User Name");
        DecimalFormat dft = new DecimalFormat("###.##");
        Iterator var5 = list.iterator();

        while (var5.hasNext()) {
            CloudletMobility value = (CloudletMobility) var5.next();
            CloudletMobility cloudlet = value;
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                Log.print("SUCCESS");
                Log.println(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getGuestId() + indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + dft.format(cloudlet.getExecFinishTime()) + indent + indent + indent + cloudlet.getUser().getName());
            }
        }

    }

}
