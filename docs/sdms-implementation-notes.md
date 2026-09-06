# SDMS scheduling adaptation

This project implements only the scheduling portion of Hajvali et al. (2023),
“Decentralized and scalable hybrid scheduling-clustering method for real-time
applications in volatile and dynamic Fog-Cloud Environments” (DOI:
10.1186/s13677-023-00428-4).

## Mapping and implemented flow

- A `User` is one workflow, `CloudletMobility` is a task, and `VmMobility` is an
  available computational resource.
- `WorkflowEdge` stores a directed dependency and its data size in MB. Generated
  edges always point forward, and model/scheduler checks reject cycles.
- SDMS computes a double PRT from communication data, deadline-aware scheduler
  pressure, immediate successor count, execution cost, and compatible-VM
  availability. Each factor is min-max normalized per workflow.
- The five PRT weights are constants set to 0.2 each. This is an implementation
  assumption because the paper defines weights but does not prescribe values that
  map directly to this simulator.
- A DFS with memoization selects the source-to-sink DAG path with the greatest sum
  of task PRT values. It is not the conventional longest-runtime path.
- Tasks are ordered topologically. Within each ready set, critical-path tasks rank
  first and then higher-PRT tasks.
- Bounded exhaustive backtracking assigns tasks to existing VMs. EST is the later
  of VM availability and every parent EFT plus cross-VM communication. EFT is EST
  plus `cloudletTotalLength / vmTotalMips`. Edge MB is converted to Mbit and VM
  bandwidth is interpreted as Mbit/s. Branches exceeding the workflow deadline are
  pruned. At most 3,000 valid candidates are retained per workflow as an engineering
  safety bound, so this is not unrestricted exhaustive search.
- Candidate utility is
  `ETMP_candidate / ETMP_bestResource + ETMS_candidate / ETMS_bestResource`.
  ETMP is candidate makespan; ETMS is the sum of candidate task durations. Baselines
  use the most powerful VM, with dependency-limited parallel time and serial time.
  Lower utility is better. This follows the paper's textual ratio interpretation
  despite the sorting-direction ambiguity in its pseudo-code.
- Candidates are tried in ascending utility order. Assignment validity and PE
  compatibility are checked before cloudlets are bound. If no candidate meets the
  deadline, that workflow is marked scheduling-failed and is not submitted.

## Deadline and DAG execution

Generated workflow deadline is
`estimated serial time at 1000 MIPS * 2.5`. Both the reference MIPS and deadline
factor are named constants. SDMS rejects predicted candidates beyond that deadline;
results also compare actual workflow finish time with it.

CloudSim 7.0.1 does not expose a cloudlet submission-delay API on its standard
broker. `WorkflowDatacenterBroker` therefore makes the smallest broker-side change:
it submits roots first, waits for all parent cloudlets to return, waits for any
remaining cross-VM edge-transfer delay, and only then submits the child. VM creation,
binding, scheduling, and execution remain standard CloudSim behavior. This dependency
enforcement applies to Round Robin and Nearest VM as well as SDMS.

## Intentionally excluded and adapted paper behavior

No fuzzy clustering, fuzzy membership functions/rules, SoCH, SoMob, cluster-head
election, dynamic clustering, cluster distance/velocity selection, Fog mobility,
energy model, or distributed reservation protocol is implemented. Existing VMs
replace Fog/Cloud cluster resources. Because this project has no resource-type
catalog or request counters, SRR is approximated by the fraction of VMs whose PE
capacity can execute a task. Reservation is an in-memory validity check followed by
CloudSim binding, with fallback to the next candidate.

Resource utilization is intentionally absent from the result DTO: the current model
does not retain enough host uptime/busy-time data to report it accurately.
