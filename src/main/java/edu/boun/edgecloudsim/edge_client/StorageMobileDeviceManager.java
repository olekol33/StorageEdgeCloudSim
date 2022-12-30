package edu.boun.edgecloudsim.edge_client;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.SampleNetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.task_generator.IdleActiveStorageLoadGenerator;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageMobileDeviceManager extends SampleMobileDeviceManager {
//    private int taskIdCounter=0;
    private int failedDueToBW=0;
    private int validFailed=0;
    private int tasksInInterval=0;
    private int currentInterval=0;
    private int failedDueToInaccessibility=0;
    public StorageMobileDeviceManager() throws Exception {
    }

    public void submitOrbitTask(TaskProperty edgeTask) {
        int vmType=0;
        int nextEvent;
//        int nextDeviceForNetworkModel;
        SimSettings.NETWORK_DELAY_TYPES delayType;
        double delay;

//        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        //create a task
        Task task = createTask(edgeTask);

        Location currentLocation = SimManager.getInstance().getMobilityModel().
                getLocation(task.getMobileDeviceId(), CloudSim.clock());

        //Oleg:set location of the mobile access point of the device
//        task.setSubmittedLocation(currentLocation);
        StaticRangeMobility staticMobility = (StaticRangeMobility)SimManager.getInstance().getMobilityModel();
//        task.setSubmittedLocation(StaticRangeMobility.getDCLocation(currentLocation.getServingWlanId()));
        task.setSubmittedLocation(staticMobility.getDCLocation(currentLocation.getServingWlanId()));


        //storage
        task.setObjectRead(edgeTask.getObjectRead());
//        task.setStripeID(edgeTask.getStripeID());
        task.setParitiesToRead(edgeTask.getParitiesToRead());
        task.setIoTaskID(edgeTask.getIoTaskID());
        task.setIsParity(edgeTask.getIsParity());

        //added by - Harel
        task.setTaskPriority(edgeTask.getTaskPriority());
        task.setTaskDeadline(edgeTask.getTaskDeadline());
        task.setStart_time(edgeTask.getStartTime());

        //add related task to log list
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize(),
                task.getObjectRead(),
                task.getIoTaskID(),
                task.getIsParity(),
                task.getParitiesToRead(),
                task.getAccessHostID());


        int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);

        //Oleg: Only download in storage simulation. Upload delay is 0.
        if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){
//            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.CLOUD_DATACENTER_ID, task);
            delay = 0;
            vmType = SimSettings.VM_TYPES.CLOUD_VM.ordinal();
            nextEvent = REQUEST_RECEIVED_BY_CLOUD;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY;
//            nextDeviceForNetworkModel = SimSettings.CLOUD_DATACENTER_ID;
        }
        else {
            //no upload delay
//            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.GENERIC_EDGE_DEVICE_ID, task);
            delay = 0;
            vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
            nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
//            nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
        }

        Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);
        task.setAccessHostID(task.getSubmittedLocation().getServingWlanId());
        SimLogger.getInstance().setAccessHostID(task.getCloudletId(),task.getSubmittedLocation().getServingWlanId());
        edgeTask.setAccessHostID(task.getSubmittedLocation().getServingWlanId());


        SimLogger.getInstance().setObjectRead(task.getCloudletId(),task.getObjectRead());

        if(selectedVM != null){
            //
//                SimLogger.getInstance().setHostId(task.getCloudletId(),task.getHostID());
            SimLogger.getInstance().setHostId(task.getCloudletId(),selectedVM.getHost().getId());

            //set related host id
            task.setAssociatedDatacenterId(nextHopId);

            //set related host id
            task.setAssociatedHostId(selectedVM.getHost().getId());

            //set related vm id
            task.setAssociatedVmId(selectedVM.getId());

            //bind task to related VM
            getCloudletList().add(task);
            bindCloudletToVm(task.getCloudletId(), selectedVM.getId());

            if(selectedVM instanceof EdgeVM){
                EdgeHost host = (EdgeHost)(selectedVM.getHost());

                //if neighbor edge device is selected
                //Oleg: When access point is different from data source
                if(host.getLocation().getServingWlanId() != task.getSubmittedLocation().getServingWlanId()){
                    nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR;
                }
            }
//                networkModel.uploadStarted(currentLocation, nextDeviceForNetworkModel);

            SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
            SimLogger.getInstance().setUploadDelay(task.getCloudletId(), delay, delayType);
            schedule(getId(), delay, nextEvent, task);
        }
    }



    @Override
    //Submit task and upload it to host
    public void submitTask(TaskProperty edgeTask) {
        int nextEvent;
        int nextDeviceForNetworkModel;
        SimSettings.NETWORK_DELAY_TYPES delayType;
        double delay;

        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        //create a task
        Task task = createTask(edgeTask);

        StaticRangeMobility staticMobility = (StaticRangeMobility)SimManager.getInstance().getMobilityModel();
        Location currentLocation = staticMobility.getLocation(task.getMobileDeviceId(), CloudSim.clock());
        int taskServingWlanId = currentLocation.getServingWlanId();

        task.setSubmittedLocation(staticMobility.getDCLocation(taskServingWlanId));



        //storage
        task.setObjectRead(edgeTask.getObjectRead());
//        task.setStripeID(edgeTask.getStripeID());
        task.setParitiesToRead(edgeTask.getParitiesToRead());
        task.setIoTaskID(edgeTask.getIoTaskID());
        task.setIsParity(edgeTask.getIsParity());
        task.setHostID(edgeTask.getHostID());

        updateCurrentFailInterval(edgeTask.getIoTaskID(), task.getIsParity());

        //add related task to log list
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize(),
                task.getObjectRead(),
                task.getIoTaskID(),
                task.getIsParity(),
                task.getParitiesToRead(),
                task.getAccessHostID());


        int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);

        //Oleg: Only download in storage simulation. Upload delay is 0.
        if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){
//            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.CLOUD_DATACENTER_ID, task);
            delay = 0;
//            vmType = SimSettings.VM_TYPES.CLOUD_VM.ordinal();
            nextEvent = REQUEST_RECEIVED_BY_CLOUD;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY;
            nextDeviceForNetworkModel = SimSettings.CLOUD_DATACENTER_ID;
        }
        else {
            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), taskServingWlanId, SimSettings.DEVICE_TO_LOCAL_EDGE,task);
            if(delay < 0 ) {
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY, task);
                return;
            }
            nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
            nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
        }

            Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);
            task.setAccessHostID(taskServingWlanId);
            SimLogger.getInstance().setAccessHostID(task.getCloudletId(),taskServingWlanId);
            edgeTask.setAccessHostID(taskServingWlanId);

            SimLogger.getInstance().setObjectRead(task.getCloudletId(),task.getObjectRead());

            if(selectedVM != null){
            //
//                SimLogger.getInstance().setHostId(task.getCloudletId(),task.getHostID());
            SimLogger.getInstance().setHostId(task.getCloudletId(),selectedVM.getHost().getId());

            //set related host id
            task.setAssociatedDatacenterId(nextHopId);

            //set related host id
            task.setAssociatedHostId(selectedVM.getHost().getId());

            //set related vm id
            task.setAssociatedVmId(selectedVM.getId());

            //bind task to related VM
            getCloudletList().add(task);
            bindCloudletToVm(task.getCloudletId(), selectedVM.getId());

            if(selectedVM instanceof EdgeVM){
                EdgeHost host = (EdgeHost)(selectedVM.getHost());
                //if neighbor edge device is selected
                //Oleg: When access point is different from data source
                if(host.getLocation().getServingWlanId() != taskServingWlanId){
                    nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR;
                }
            }
            SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
            SimLogger.getInstance().setUploadDelay(task.getCloudletId(), delay, delayType);
            schedule(getId(), delay, nextEvent, task);
        }
    }

    //When trace is subdivided into intervals, reset counter when new interval begins
    private void updateCurrentFailInterval(int ioTaskID, int isParity){
        int realTimeInterval = getCurrentInterval();
        if (ioTaskID == SimSettings.getInstance().getNumOfExternalTasks() - 1)
            realTimeInterval++;
        if (currentInterval != realTimeInterval && realTimeInterval >= 0){
            double ratioTh = SimSettings.getInstance().getFailThreshold();
            double failedRatio= (double)  validFailed / tasksInInterval;
            if (failedRatio > ratioTh)
                SimSettings.getInstance().setSimulationFailed(currentInterval);
            SimSettings.getInstance().setTasksInInterval(currentInterval, tasksInInterval, validFailed);
            currentInterval = realTimeInterval;
            validFailed = 0;
            tasksInInterval = 0;
        }
        if (realTimeInterval>=0 && isParity==0)
            tasksInInterval++;
    }
    @Override
    //When need to process on another host
    //Oleg: removed all upload cases
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
            System.exit(0);
            return;
        }
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();

        switch (ev.getTag()) {
            case UPDATE_MM1_QUEUE_MODEL:
            {
                ((SampleNetworkModel)networkModel).updateMM1QueeuModel();
                schedule(getId(), MM1_QUEUE_MODEL_UPDATE_INTERVAL, UPDATE_MM1_QUEUE_MODEL);
                break;
            }
            case REQUEST_RECEIVED_BY_CLOUD:
            {
                Task task = (Task) ev.getData();
                submitTaskToVm(task, SimSettings.VM_TYPES.CLOUD_VM);
                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE:
            {
                Task task = (Task) ev.getData();
                submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
                break;
            }
            case REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE:
            {
                Task task = (Task) ev.getData();
                submitTaskToVm(task, SimSettings.LOCAL_EDGE_TO_REMOTE_EDGE);
                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR:
            {
                Task task = (Task) ev.getData();
                double manDelay =  networkModel.getUploadDelay(task.getSubmittedLocation().getServingWlanId(), task.getAssociatedHostId(),
                        SimSettings.EDGE_TO_EDGE, task);
                if(manDelay < 0 ) {
                    SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY, task);
                    return;
                }
                SimLogger.getInstance().setUploadDelay(task.getCloudletId(), manDelay, SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY);
                schedule(getId(), manDelay, REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE, task);
                break;
            }
            //Oleg: Once data is at accessPoint, read from it
            case RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE:
            {
                Task task = (Task) ev.getData();
                SimSettings.NETWORK_DELAY_TYPES delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
                //TODO: recheck, stop read from data host and then read from access point
                //TODO: GENERIC_EDGE_DEVICE_ID+1 is MAN, not affecting count at host
                if (!(task.getIsParity() == 1 && task.getParitiesToRead() == 0)) {
                    StaticRangeMobility staticMobility = (StaticRangeMobility)SimManager.getInstance().getMobilityModel();
                    ((StorageNetworkModel) networkModel).downloadFinished(staticMobility.getDCLocation(task.getAssociatedHostId()),
                            SimSettings.GENERIC_EDGE_DEVICE_ID + 1, task.getAssociatedHostId());
                }

                //get delay between access point and device
//                double delay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);
                //Insert tag instead of user location to calculate WLAN delay of remote node
                //TODO: why use upload size when read object is returned? should be object size (download)
                double delay = networkModel.getUploadDelay(SimSettings.GENERIC_EDGE_DEVICE_ID,
                        task.getSubmittedLocation().getServingWlanId(), SimSettings.REMOTE_EDGE_TO_LOCAL_EDGE, task);
                if (delay==((StorageNetworkModel) networkModel).MAN_DELAY)
                    delayType = SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY;

                if(delay > 0)
                {
                    Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
                    if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
                    {
                        if(!((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadStarted(task.getIoTaskID())) {
                            networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                            ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadStarted(true,task.getIoTaskID());
                        }
                        SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
                        schedule(getId(), delay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
                    }
                    else
                        SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                }
                else if (delay < 0)
                    SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType,task);
                else
                    System.out.println("delay=0");

                break;
            }
            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
            {
                Task task = (Task) ev.getData();
                double delayToUser = SimSettings.getInstance().getDelayToUser();
                double time = CloudSim.clock()+delayToUser;
                if(task.getIsParity()==1) {
                    //key was removed
                    if (((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadFinished(task.getIoTaskID())) {
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), time, task);
                        break;
                    }

                    //need to remove key
                    if (!((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadFinished(task.getIoTaskID())){
                        if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
                            networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                        else
                            networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                        ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadFinished(true,task.getIoTaskID());
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), time, task);
                        break;
                    }
                }
                if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID) {
                    if (!(task.getIsParity() == 1 && task.getParitiesToRead() == 0))
                        networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                }
                else {
                    if(!(task.getIsParity()==1 && task.getParitiesToRead()==0))
                        networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                }
                SimLogger.getInstance().taskEnded(task.getCloudletId(),time , task);
                break;
            }
            default:
                SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
                System.exit(0);
                break;
        }
    }
    @Override
    protected void submitTaskToVm(Task task, int uploadType) {
        double delay=0;
        if(uploadType==SimSettings.LOCAL_EDGE_TO_REMOTE_EDGE) {
            NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
            delay = networkModel.getUploadDelay(task.getSubmittedLocation().getServingWlanId(),task.getAssociatedHostId() , SimSettings.LOCAL_EDGE_TO_REMOTE_EDGE, task);
        }
        if(delay < 0 ) {
            SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY, task);
            return;
        }
        schedule(getVmsToDatacentersMap().get(task.getVmId()), delay, CloudSimTags.CLOUDLET_SUBMIT, task);

        SimSettings.VM_TYPES vmType = SimSettings.VM_TYPES.EDGE_VM;
        SimLogger.getInstance().taskAssigned(task.getCloudletId(),
                task.getAssociatedDatacenterId(),
                task.getAssociatedHostId(),
                task.getAssociatedVmId(),
                vmType.ordinal());
    }

    //Process task return event
    @Override
    protected void processCloudletReturn(SimEvent ev) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
        Task task = (Task) ev.getData();

        SimLogger.getInstance().taskExecuted(task.getCloudletId());

        if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
            //SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from cloud");
            double WanDelay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID, task.getMobileDeviceId(), task);
            if(WanDelay > 0)
            {
                Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+WanDelay);
                if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
                {
                    if(!((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadStarted(task.getIoTaskID())) {
                        networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                        ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadStarted(true,task.getIoTaskID());
                    }
                    SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WanDelay, SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
                    schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
                }
                else
                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
            }
            else
            {
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(),
                        SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY,task);
            }
        }
        else{
            int nextEvent = RESPONSE_RECEIVED_BY_MOBILE_DEVICE;
            int nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
            SimSettings.NETWORK_DELAY_TYPES delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
            double delay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);
            if (delay == ((StorageNetworkModel) networkModel).MAN_DELAY)
                delayType = SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY;
//            EdgeHost host = (EdgeHost)(SimManager.getInstance().getEdgeServerManager().
//                    getDatacenterList().get(task.getAssociatedHostId()).getHostList().get(0));

            if(delay < 0 && delayType != SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY) { //access point is full in this case, no point to continue
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType, task);
                return;
            }
            //When source host is not at access point (read from distant edge) initiate download from host (MAN delay)
            //host - data location, task.getSubmittedLocation() - access point location
            if(task.getHostID() != task.getSubmittedLocation().getServingWlanId())
            {
                //WLAN local node + MAN delay
                delay += networkModel.getDownloadDelay(SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
                nextEvent = RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE;
                nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID + 1;
                delayType = SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY;
            }
            else if ( delay==0)
                SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());
            if(delay > 0)
            {
                if (nextDeviceForNetworkModel==SimSettings.GENERIC_EDGE_DEVICE_ID+1)
                    networkModel.downloadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel);
                else if(!((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadStarted(task.getIoTaskID())) {
                    networkModel.downloadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel);
                    ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadStarted(true,task.getIoTaskID());
                }
                    SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, delayType);
                    schedule(getId(), delay, nextEvent, task);
            }
            else
                //For % of all IO requests, if they've failed, stop run
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType,task);
        }
    }

    public void terminateFailedRun(){
        LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
        int numOfIOTasks = IdleActiveStorageLoadGenerator.getNumOfValidIOTasks();
        double ratio;
        double ratioTh = SimSettings.getInstance().getFailThreshold();
        if(CloudSim.clock() > SimSettings.getInstance().getWarmUpPeriod())
            validFailed++;
        else
            return;
        // if trace divided into intervals, just count validFailed
        if (SimSettings.getInstance().getTraceIntervalDuration() > 0){
            return;
        }
        ratio=(double)  validFailed / numOfIOTasks;
        if(ratio>ratioTh && SimSettings.getInstance().isTerminateFailedRun()) {
            try {
                if (SimSettings.getInstance().isParamScanMode()) {
                    int numOfDevices = SimManager.getInstance().getNumOfMobileDevice();
                    String simScenario = SimManager.getInstance().getSimulationScenario();
                    String orchestratorPolicy = SimManager.getInstance().getOrchestratorPolicy();
                    String objectPlacementPolicy = SimManager.getInstance().getObjectPlacementPolicy();
                    String distribution = SimSettings.getInstance().getObjectDistPlace();
                    String fail = "";
                    if (SimSettings.getInstance().isHostFailureScenario())
                        fail="WITHFAIL";
                    else
                        fail="NOFAIL";
                    Pattern pattern;
                    if (SimSettings.getInstance().isOverheadScan())
                        pattern = Pattern.compile("SIMRESULT_(.*)_OH.*");
                    else
                        pattern = Pattern.compile("SIMRESULT_(.*)_SINGLE.*");
                    Matcher matcher = pattern.matcher(SimLogger.getInstance().getFilePrefix());
                    matcher.find();
                    SimManager.getInstance().setLambda0(Double.valueOf(matcher.group(1)));
                    String[] simParams = {Integer.toString(numOfDevices), simScenario, orchestratorPolicy, objectPlacementPolicy,
                            matcher.group(1),distribution,fail};
//                            SimUtils.cleanOutputFolderPerConfiguration(SimLogger.getInstance().getOutputFolder(), simParams);
                    SimUtils.cleanOutputFolderPerConfiguration(SimLogger.getInstance().getOutputFolder(), new String[]{SimLogger.getInstance().getFilePrefix()});
                }
                File file = new File(SimLogger.getInstance().getOutputFolder() + "/" + SimLogger.getInstance().getFilePrefix()  + "_TASK_FAILED.log");
                PrintStream out = new PrintStream(new FileOutputStream(file));
                out.print("failedDueToBW,failedDueToInaccessibility\n" + failedDueToBW+","+failedDueToInaccessibility);
                out.print("\nfailedDueToBW,failedDueToInaccessibility\n" + (double)(failedDueToBW)/numOfIOTasks+","+(double)(failedDueToInaccessibility)/numOfIOTasks);
                out.close();
                SimLogger.getInstance().calculateServiceCost();
                System.out.println("Failed above threshold for: " + SimLogger.getInstance().getFilePrefix());
                SimManager.getInstance().shutdownEntity();
                SimLogger.printLine("100");
                SimSettings.getInstance().setSimulationFailed(0);
                SimSettings.getInstance().setTasksInInterval(0, tasksInInterval, validFailed);
                CloudSim.terminateSimulation();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }

    private int getCurrentInterval(){
        int intervalDuration = SimSettings.getInstance().getTraceIntervalDuration();
        if (CloudSim.clock() < SimSettings.getInstance().getWarmUpPeriod())
            return -1;
        if (intervalDuration==0)
            return 0;
        double measuredTime = CloudSim.clock() - SimSettings.getInstance().getWarmUpPeriod();
        return (int) (measuredTime / intervalDuration);
    }
    public void incrementFailedDueToInaccessibility() {
        failedDueToInaccessibility++;
    }

    public void incrementFailedDueToBW() {
        failedDueToBW++;
    }



}
