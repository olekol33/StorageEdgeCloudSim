# StorageEdgeCloudSim

StorageEdgeCloudSim (SECS) provides a simulation environment for a edge storage scenario of N nodes hosting K users which generate requests at every time stamp. SECS was built upon EdgeCloudSim (see [README](/README_ECS.md)). The main goal of SECS is to evaluate the performance of edge storage systems under different scenarios.
It was used as the main simulator in "Theory vs. practice in modeling edge storage systems", 2023 IEEE International Performance, Computing, and Communications Conference (IPCCC).

## Structure
### General Structure
See EdgeCloudSim  [README](/README_ECS.md)  and [SECS Report](/SECS_Report.pd) for the detailed structure.

SECS supports multiple scenarios, each defined as an application.
The configuration files are stored in `scripts/<application>/config` folder. Main applications:
- **service_rate_app:** The main scenario used for the paper to simulate requests inserted from an external source.
- **NSF_BSF_app1:** The code used to generate "simulated" toy scenario for the paper (Fig. 6).
- **param_scan_app:** Also code for toy scenario, probably older version of the code.

External input files are located in `scripts/<application>/input_files`. 

### Configuration Files
- **default_config.properties:** Simulation settings are managed in configuration file
- **applications.xml:** Application properties are stored in xml file
- **edge_devices.xml:** Edge devices (datacenters, hosts, VMs etc.) are defined in xml file

The latter two are not used when external input files are inserted.

### service_rate_app
#### Input Files
- **devices.csv:** Defines users (devices) in the system. A CSV with the format `deviceName,xPos,yPos,time`. `time` is not used in the simulator.
- **nodes.csv:** Defines edge storage nodes in the system. A CSV with the format `nodeName,xPos,yPos,serviceClass,capacity,serviceRate`.  Only `nodeName,xPos,yPos` are used in the simulator. SECS uses the coordinated to associate each device to the nearest node. `capacity,serviceRate` are overridden in runtime.
- **objects.csv:**: Defines the objects that can be requested by devices and stored in the nodes. A CSV with the format `objectName,size,locationVector,locationProbVector,class,popularityShare`. Only name and size are relevant.
- **requests.csv:** Maps requests from devices to objects, each with a time stamp and a unique ID. A CSV with the format `deviceName,time,object,ioTaskID,taskPriority,taskDeadline`.

Activate this mode in the configuration file:
```
external_nodes_input=true
external_devices_input=true
external_objects_input=true
external_requests_input=true
```

See explanation below on generating these files.

#### Results
Written to `sim_results`. `ite1` contains the run dump, usually used for debug.
`service_rate` contains service rate results for the main application by redundancy policy (coding/replication).
- **PLACEMENT.csv:** Contains the placement of objects in the nodes. Has the header `object1,object2,node`. When `object2` is indicated, this represents a coding object (`object1+object2`), otherwise `object1` indicates a data object.
- **DEMAND.csv:** Contains the service rate for each object. Header is `<object_enumeration>,type,reqsPerUserSec,readRate,iteration,interval,tasksPerInterval,failedPerInterval,simServiceCost,completed`. The main indicator is `completed` which shows the iterations that completed successfully (indicating load was managable).

### NSF_BSF_app1
Output written to `sim_results/ite1`. Parses generate file to determine for each lambda1, lambda2 tuple if run completed or not. For example: `SIMRESULT_<lam1>_<lam2_SINGLE_TIER_IF_CONGESTED_READ_PARITY_CODING_PLACE_6DEVICES_TASK_FAILED` indicates failed, while `SIMRESULT_<lam1>_<lam2_SINGLE_TIER_IF_CONGESTED_READ_PARITY_CODING_PLACE_6DEVICES_NODE_QUEUE` indicates completed.


## Building and Running
### Build
SECS uses Maven for build. `pom.xml` contains the dependencies and build instructions.
The compiled app is set under `mainClass` in `pom.xml`.

### Running

1. Download the code and (optionally) import it to your IDE
2. Install and use Java 11
3. Running the code:
- (From IDE) Run `MainApp` for the relevant application under `src.main.java.edu.boun.edgecloudsim.applications`
- (From terminal) Run `mvn clean install` to build the project. Copy `StorageEdgeCloudSim.jar` from `target/` to main dir, then run `java -jar StorageEdgeCloudSim.jar`

## Generating Input Files
TODO

