package ir.srbiau.cloudsim.mobilitysim.dto;

import ir.srbiau.cloudsim.mobilitysim.scheduling.SchedulingAlgorithm;

public record SimulationResultDto(
        SchedulingAlgorithm schedulingAlgorithm,
        int totalWorkflowCount,
        int totalTaskCount,
        int completedTaskCount,
        int failedTaskCount,
        int successfulWorkflowCount,
        int failedWorkflowCount,
        double deadlineSuccessRate,
        double makespan,
        double averageWaitingTime
) {}
