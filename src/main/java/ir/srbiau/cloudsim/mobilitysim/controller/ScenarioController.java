package ir.srbiau.cloudsim.mobilitysim.controller;

import ir.srbiau.cloudsim.mobilitysim.model.SimulationScenario;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cloudsim")
public class ScenarioController {

    @PostMapping
    public ResponseEntity<String> submitScenario(@RequestBody SimulationScenario scenario) {
        try {
//            CloudSimExample1.main();
            // Initialize CloudSim
            CloudSim.init(1, null, false);

            // Process Datacenters, Hosts, VMs
            DatacenterBroker broker = new DatacenterBroker("Broker");


            // Start the simulation
            CloudSim.startSimulation();

            // Stop the simulation
            CloudSim.stopSimulation();

            // Return the result
            return ResponseEntity.ok("Simulation finished successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during simulation");
        }
    }

}
