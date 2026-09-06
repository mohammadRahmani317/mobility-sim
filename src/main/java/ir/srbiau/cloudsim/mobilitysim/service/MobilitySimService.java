package ir.srbiau.cloudsim.mobilitysim.service;

import ir.srbiau.cloudsim.mobilitysim.dto.DatacenterConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.HostConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.ResourceRequestDto;
import ir.srbiau.cloudsim.mobilitysim.dto.SimulationResultDto;
import ir.srbiau.cloudsim.mobilitysim.dto.VmConfig;
import ir.srbiau.cloudsim.mobilitysim.model.CloudletMobility;
import ir.srbiau.cloudsim.mobilitysim.model.User;
import ir.srbiau.cloudsim.mobilitysim.model.VmMobility;
import ir.srbiau.cloudsim.mobilitysim.scheduling.NearestVmStrategy;
import ir.srbiau.cloudsim.mobilitysim.scheduling.RoundRobin;
import ir.srbiau.cloudsim.mobilitysim.scheduling.SchedulingAlgorithm;
import ir.srbiau.cloudsim.mobilitysim.scheduling.SdmsSchedulingStrategy;
import ir.srbiau.cloudsim.mobilitysim.scheduling.VmSchedulingStrategy;
import ir.srbiau.cloudsim.mobilitysim.workload.WorkloadGenerator;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MobilitySimService {
    private final WorkloadGenerator workloadGenerator;

    public MobilitySimService(WorkloadGenerator workloadGenerator) {
        this.workloadGenerator = workloadGenerator;
    }

    /** CloudSim uses global static simulation state, so concurrent HTTP runs must be serialized. */
    public synchronized ResponseEntity<SimulationResultDto> run(ResourceRequestDto dto) {
        validate(dto);
        try {
            CloudSim.init(1, Calendar.getInstance(), false);
            createDatacenters(dto.datacenters(), dto.hosts());
            WorkflowDatacenterBroker broker = new WorkflowDatacenterBroker("Broker");
            List<VmMobility> vms = createVms(dto.vms(), broker.getId());
            broker.submitGuestList(vms);

            List<User> users = workloadGenerator.generateUsers();
            SchedulingAlgorithm algorithm = dto.selectedSchedulingAlgorithm();
            VmSchedulingStrategy strategy = createStrategy(algorithm);
            strategy.assignCloudletsToVms(users, vms, broker.getId());

            List<CloudletMobility> scheduledTasks = users.stream()
                    .filter(user -> !user.isSchedulingFailed())
                    .flatMap(user -> user.getCloudlets().stream())
                    .toList();
            broker.setExpectedCloudlets(scheduledTasks.size());
            if (!scheduledTasks.isEmpty()) {
                broker.submitCloudletList(scheduledTasks);
                CloudSim.startSimulation();
                CloudSim.stopSimulation();
            }

            List<CloudletMobility> finished = broker.getCloudletReceivedList();
            return ResponseEntity.ok(createResult(algorithm, users, finished));
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("CloudSim execution failed", exception);
        }
    }

    private VmSchedulingStrategy createStrategy(SchedulingAlgorithm algorithm) {
        return switch (algorithm) {
            case ROUND_ROBIN -> new RoundRobin();
            case NEAREST_VM -> new NearestVmStrategy();
            case SDMS -> new SdmsSchedulingStrategy();
        };
    }

    private List<Datacenter> createDatacenters(List<DatacenterConfig> datacenterConfigs,
                                               List<HostConfig> hostConfigs) throws Exception {
        List<Datacenter> result = new ArrayList<>();
        int nextHostId = 0;
        for (int dcIndex = 0; dcIndex < datacenterConfigs.size(); dcIndex++) {
            DatacenterConfig config = datacenterConfigs.get(dcIndex);
            List<Host> hosts = new ArrayList<>();
            for (HostConfig hostConfig : hostConfigs) {
                List<Pe> pes = new ArrayList<>();
                for (int peId = 0; peId < hostConfig.pes(); peId++) {
                    pes.add(new Pe(peId, new PeProvisionerSimple(hostConfig.mipsPerPe())));
                }
                hosts.add(new Host(nextHostId++, new RamProvisionerSimple(hostConfig.ram()),
                        new BwProvisionerSimple(hostConfig.bw()), hostConfig.storage(), pes,
                        new VmSchedulerTimeShared(pes)));
            }
            DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                    config.architecture(), config.os(), config.vmm(), hosts, config.timeZone(),
                    config.costPerSec(), config.costPerMem(), config.costPerStorage(), config.costPerBw());
            result.add(new Datacenter("Datacenter-" + (dcIndex + 1), characteristics,
                    new VmAllocationPolicySimple(hosts), new LinkedList<Storage>(), 0.0));
        }
        return result;
    }

    private List<VmMobility> createVms(List<VmConfig> configs, int brokerId) {
        List<VmMobility> result = new ArrayList<>();
        for (int id = 0; id < configs.size(); id++) {
            VmConfig config = configs.get(id);
            result.add(new VmMobility(id, brokerId, config.mips(), config.pes(), config.ram(),
                    config.bw(), config.size(), config.vmm(), new CloudletSchedulerSpaceShared()));
        }
        return result;
    }

    private SimulationResultDto createResult(SchedulingAlgorithm algorithm, List<User> users,
                                             List<CloudletMobility> finished) {
        int totalTasks = users.stream().mapToInt(user -> user.getCloudlets().size()).sum();
        List<CloudletMobility> completed = finished.stream()
                .filter(task -> task.getStatus() == Cloudlet.CloudletStatus.SUCCESS).toList();
        Map<User, List<CloudletMobility>> completedByUser = completed.stream()
                .collect(Collectors.groupingBy(CloudletMobility::getUser));

        int successfulWorkflows = 0;
        for (User user : users) {
            List<CloudletMobility> workflowCompleted = completedByUser.getOrDefault(user, List.of());
            double workflowFinish = workflowCompleted.stream()
                    .mapToDouble(CloudletMobility::getExecFinishTime).max().orElse(Double.POSITIVE_INFINITY);
            if (!user.isSchedulingFailed()
                    && workflowCompleted.size() == user.getCloudlets().size()
                    && workflowFinish <= user.getDeadline()) {
                successfulWorkflows++;
            }
        }
        int failedWorkflows = users.size() - successfulWorkflows;
        double successRate = users.isEmpty() ? 0.0 : successfulWorkflows * 100.0 / users.size();
        double makespan = completed.stream().mapToDouble(CloudletMobility::getExecFinishTime)
                .max().orElse(0.0);
        double averageWaiting = completed.stream().mapToDouble(CloudletMobility::getWaitingTime)
                .average().orElse(0.0);

        return new SimulationResultDto(algorithm, users.size(), totalTasks, completed.size(),
                totalTasks - completed.size(), successfulWorkflows, failedWorkflows,
                successRate, makespan, averageWaiting);
    }

    private void validate(ResourceRequestDto dto) {
        if (dto == null || dto.datacenters() == null || dto.datacenters().isEmpty()
                || dto.hosts() == null || dto.hosts().isEmpty()
                || dto.vms() == null || dto.vms().isEmpty()) {
            throw new IllegalArgumentException("At least one datacenter, host, and VM are required");
        }
        if (dto.hosts().stream().anyMatch(host -> host.ram() <= 0 || host.storage() <= 0
                || host.bw() <= 0 || host.pes() <= 0 || host.mipsPerPe() <= 0)) {
            throw new IllegalArgumentException("Host capacity values must be positive");
        }
        if (dto.vms().stream().anyMatch(vm -> vm.mips() <= 0 || vm.pes() <= 0 || vm.ram() <= 0
                || vm.bw() <= 0 || vm.size() <= 0 || vm.vmm() == null || vm.vmm().isBlank())) {
            throw new IllegalArgumentException("VM capacity values and VMM are required and must be positive");
        }
        if (dto.datacenters().stream().anyMatch(dc -> dc.architecture() == null || dc.architecture().isBlank()
                || dc.os() == null || dc.os().isBlank() || dc.vmm() == null || dc.vmm().isBlank()
                || dc.costPerSec() < 0 || dc.costPerMem() < 0 || dc.costPerStorage() < 0 || dc.costPerBw() < 0)) {
            throw new IllegalArgumentException("Datacenter text fields are required and costs cannot be negative");
        }
        boolean everyVmFits = dto.vms().stream().allMatch(vm -> dto.hosts().stream().anyMatch(host ->
                vm.pes() <= host.pes() && vm.mips() <= host.mipsPerPe()
                        && vm.ram() <= host.ram() && vm.bw() <= host.bw() && vm.size() <= host.storage()));
        if (!everyVmFits) {
            throw new IllegalArgumentException("Every VM must fit at least one configured host");
        }
        long datacenterCount = dto.datacenters().size();
        long hostPes = dto.hosts().stream().mapToLong(HostConfig::pes).sum() * datacenterCount;
        long hostRam = dto.hosts().stream().mapToLong(HostConfig::ram).sum() * datacenterCount;
        long hostBw = dto.hosts().stream().mapToLong(HostConfig::bw).sum() * datacenterCount;
        long hostStorage = dto.hosts().stream().mapToLong(HostConfig::storage).sum() * datacenterCount;
        long vmPes = dto.vms().stream().mapToLong(VmConfig::pes).sum();
        long vmRam = dto.vms().stream().mapToLong(VmConfig::ram).sum();
        long vmBw = dto.vms().stream().mapToLong(VmConfig::bw).sum();
        long vmStorage = dto.vms().stream().mapToLong(VmConfig::size).sum();
        if (vmPes > hostPes || vmRam > hostRam || vmBw > hostBw || vmStorage > hostStorage) {
            throw new IllegalArgumentException("Total VM demand exceeds configured host capacity");
        }
    }
}
