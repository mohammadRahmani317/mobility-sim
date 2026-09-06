package ir.srbiau.cloudsim.mobilitysim.service;

import ir.srbiau.cloudsim.mobilitysim.dto.DatacenterConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.HostConfig;
import ir.srbiau.cloudsim.mobilitysim.dto.ResourceRequestDto;
import ir.srbiau.cloudsim.mobilitysim.dto.SimulationResultDto;
import ir.srbiau.cloudsim.mobilitysim.dto.VmConfig;
import ir.srbiau.cloudsim.mobilitysim.scheduling.SchedulingAlgorithm;
import ir.srbiau.cloudsim.mobilitysim.workload.WorkloadGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobilitySimServiceTest {

    @ParameterizedTest
    @EnumSource(SchedulingAlgorithm.class)
    void allSchedulingAlgorithmsCompleteWithoutRuntimeErrors(SchedulingAlgorithm algorithm) {
        ResourceRequestDto request = new ResourceRequestDto(
                List.of(new DatacenterConfig("x64", "Linux", "Xen", 0, 0.01, 0, 0, 0)),
                List.of(new HostConfig(8192, 100_000, 10_000, 2, 2000)),
                List.of(
                        new VmConfig(1000, 1, 2048, 1000, 10_000, "Xen"),
                        new VmConfig(2000, 1, 2048, 2000, 10_000, "Xen")
                ), algorithm);

        SimulationResultDto result = new MobilitySimService(new WorkloadGenerator(42L))
                .run(request).getBody();

        assertEquals(algorithm, result.schedulingAlgorithm());
        assertEquals(result.totalTaskCount(), result.completedTaskCount());
        assertEquals(0, result.failedTaskCount());
        assertTrue(result.makespan() > 0);
    }
}
