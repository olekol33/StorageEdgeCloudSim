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
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorageMobileDeviceManager extends SampleMobileDeviceManager {
    private int taskIdCounter=0;
    private int failedDueToBW=0;
    public StorageMobileDeviceManager() throws Exception {
    }

    public void submitOrbitTask(TaskProperty edgeTask) {
        int vmType=0;
        int nextEvent=0;
        int nextDeviceForNetworkModel;
        SimSettings.NETWORK_DELAY_TYPES delayType;
        double delay=0;

//        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        //create a task
        Task task = createTask(edgeTask);

        Location currentLocation = SimManager.getInstance().getMobilityModel().
                getLocation(task.getMobileDeviceId(), CloudSim.clock());

        //Oleg:set location of the mobile access point of the device
//        task.setSubmittedLocation(currentLocation);
        task.setSubmittedLocation(StaticRangeMobility.getDCLocation(currentLocation.getServingWlanId()));


        //storage
        task.setObjectRead(edgeTask.getObjectRead());
        task.setStripeID(edgeTask.getStripeID());
        task.setParitiesToRead(edgeTask.getParitiesToRead());
        task.setIoTaskID(edgeTask.getIoTaskID());
        task.setIsParity(edgeTask.getIsParity());

        //add related task to log list
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize(),
                task.getStripeID(),
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

//        if(delay>0){
        if(delay>=0){
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
                if (delay!=0)
                    System.out.println("Upload delay >0");
//                System.out.println("2Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
//                System.out.println("\n2For ioTask " + task.getIoTaskID() + "object " + task.getObjectRead() +
//                        " submitted to " + task.getSubmittedLocation().getServingWlanId() + " and " + task.getAccessHostID() + "\n");
                schedule(getId(), delay, nextEvent, task);
            }
//            else if (selectedVM == -1)
            else{
//                SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock(),vmType);
            }
        }
        else
        {
            //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
//            SimLogger.getInstance().rejectedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), vmType, delayType);
//            SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());

        }
    }



    @Override
    //Submit task and upload it to host
    public void submitTask(TaskProperty edgeTask) {
        int vmType=0;
        int nextEvent=0;
        int nextDeviceForNetworkModel;
        SimSettings.NETWORK_DELAY_TYPES delayType;
        double delay=0;

//        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        //create a task
        Task task = createTask(edgeTask);

        Location currentLocation = SimManager.getInstance().getMobilityModel().
                getLocation(task.getMobileDeviceId(), CloudSim.clock());

        //Oleg:set location of the mobile access point of the device
//        task.setSubmittedLocation(currentLocation);
        task.setSubmittedLocation(StaticRangeMobility.getDCLocation(currentLocation.getServingWlanId()));


        //storage
        task.setObjectRead(edgeTask.getObjectRead());
        task.setStripeID(edgeTask.getStripeID());
        task.setParitiesToRead(edgeTask.getParitiesToRead());
        task.setIoTaskID(edgeTask.getIoTaskID());
        task.setIsParity(edgeTask.getIsParity());

        //add related task to log list
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize(),
                task.getStripeID(),
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

//        if(delay>0){
        if(delay>=0){
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
                if (delay!=0)
                    System.out.println("Upload delay >0");
//                System.out.println("2Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
//                System.out.println("\n2For ioTask " + task.getIoTaskID() + "object " + task.getObjectRead() +
//                        " submitted to " + task.getSubmittedLocation().getServingWlanId() + " and " + task.getAccessHostID() + "\n");
                schedule(getId(), delay, nextEvent, task);
            }
//            else if (selectedVM == -1)
            else{
//                SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock(),vmType);
            }
        }
        else
        {
            //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
//            SimLogger.getInstance().rejectedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), vmType, delayType);
//            SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());

        }
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
        int activeCodedRequests = 0;

        switch (ev.getTag()) {
            case UPDATE_MM1_QUEUE_MODEL:
            {
                ((SampleNetworkModel)networkModel).updateMM1QueeuModel();
                schedule(getId(), MM1_QUEUE_MODEL_UPDATE_INTEVAL, UPDATE_MM1_QUEUE_MODEL);

                break;
            }
            case REQUEST_RECEIVED_BY_CLOUD:
            {
                Task task = (Task) ev.getData();
//                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                submitTaskToVm(task, SimSettings.VM_TYPES.CLOUD_VM);
                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE:
            {
                Task task = (Task) ev.getData();
//                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);
                break;
            }
            case REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE:
            {
                Task task = (Task) ev.getData();
//                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID+1);
                submitTaskToVm(task, SimSettings.VM_TYPES.EDGE_VM);

                break;
            }
            case REQUEST_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_NEIGHBOR:
            {
                Task task = (Task) ev.getData();
//                networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);

//                double manDelay =  networkModel.getUploadDelay(SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
                double manDelay =  0;
                if(manDelay>=0){
//                    networkModel.uploadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID+1);
                    SimLogger.getInstance().setUploadDelay(task.getCloudletId(), manDelay, SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY);
//                    System.out.println("3Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
//                    System.out.println("\n3For ioTask " + task.getIoTaskID() + "object " + task.getObjectRead() +
//                            " submitted to " + task.getSubmittedLocation().getServingWlanId() + " and " + task.getAccessHostID() + "\n");
                    schedule(getId(), manDelay, REQUEST_RECEIVED_BY_REMOTE_EDGE_DEVICE, task);
                }
                else
                {
                    //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
                    SimLogger.getInstance().rejectedDueToBandwidth(
                            task.getCloudletId(),
                            CloudSim.clock(),
                            SimSettings.VM_TYPES.EDGE_VM.ordinal(),
                            SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY);
                }

                break;
            }
            //Oleg: Once data is at accessPoint, read from it
            case RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE:
            {
                Task task = (Task) ev.getData();
                //TODO: recheck, stop read from data host and then read from access point
                //TODO: GENERIC_EDGE_DEVICE_ID+1 is MAN, not affecting count at host
                if (!(task.getIsParity() == 1 && task.getParitiesToRead() == 0))
                    ((StorageNetworkModel) networkModel).downloadFinished(StaticRangeMobility.getDCLocation(task.getAssociatedHostId()),
                            SimSettings.GENERIC_EDGE_DEVICE_ID+1,task.getAssociatedHostId());

                //SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from edge");
                //get delay between access point and device
                //TODO: add delay for distance between them (Hops)
                double delay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);

                if(delay > 0)
                {
                    Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
                    if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
                    {
/*                        if(task.getIsParity()==1)
                            activeCodedRequests = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).updateActiveCodedIOTasks(task.getIoTaskID(),1);*/
                        if(((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadStarted(task.getIoTaskID())==false) {
                            networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                            ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadStarted(true,task.getIoTaskID());
                        }
                        SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
//                        System.out.println("4Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
                        schedule(getId(), delay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
                    }
                    else
                    {
                        SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                    }
                }
                else if (delay < 0)
                {
//                    System.out.println("delay < 0");
//                    SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());
                    SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
                    failedDueToBW++;
                }
                else
                    System.out.println("delay=0");

                break;
            }
            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
/*            {
                Task task = (Task) ev.getData();
                if(task.getIsParity()==1) {
                    activeCodedRequests = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).updateActiveCodedIOTasks(task.getIoTaskID(), 0);
                    //key was removed
                    if (activeCodedRequests == -1) {
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                        break;
                    }

                    //need to remove key
                    if (activeCodedRequests==0){
                        if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
                            networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                        else {
                            ((IdleActiveStorageLoadGenerator) loadGeneratorModel).updateActiveCodedIOTasks(task.getIoTaskID(), -1);
                            networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                        }
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                        break;
                    }
                    else if(activeCodedRequests>0){
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
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
                SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                break;
            }*/
            {
                Task task = (Task) ev.getData();
                if(task.getIsParity()==1) {
//                    activeCodedRequests = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).updateActiveCodedIOTasks(task.getIoTaskID(), 0);
                    //key was removed
                    if (((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadFinished(task.getIoTaskID()) == true) {
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                        break;
                    }

                    //need to remove key
                    if (((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadFinished(task.getIoTaskID()) == false){
                        if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
                            networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                        else {
//                            ((IdleActiveStorageLoadGenerator) loadGeneratorModel).updateActiveCodedIOTasks(task.getIoTaskID(), -1);
                            networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                        }
                        ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadFinished(true,task.getIoTaskID());
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                        break;
                    }
/*                    else if(activeCodedRequests>0){
                        SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                        break;
                    }*/
                }
                if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID) {
                    if (!(task.getIsParity() == 1 && task.getParitiesToRead() == 0))
                        networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                }
                else {
                    if(!(task.getIsParity()==1 && task.getParitiesToRead()==0))
                        networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                }
                SimLogger.getInstance().taskEnded(task.getCloudletId(), CloudSim.clock());
                break;
            }
            default:
                SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
                System.exit(0);
                break;
        }
    }
    //Process task return event
    @Override
    protected void processCloudletReturn(SimEvent ev) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
        int activeCodedRequests = 0;
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
/*                    if(!(task.getIsParity()==1 && task.getParitiesToRead()==0))
                        ((StorageNetworkModel) networkModel).downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID,
                                task.getAssociatedHostId());*/
                    if(((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadStarted(task.getIoTaskID())==false) {
                        ((StorageNetworkModel) networkModel).downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID,
                                task.getAssociatedHostId());
                        ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadStarted(true,task.getIoTaskID());
                    }
                    SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), WanDelay, SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
//                    System.out.println("5Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
                    schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
                }
                else
                {
                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                }
            }
            else
            {
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
                failedDueToBW++;
            }
        }
        else{
            int nextEvent = RESPONSE_RECEIVED_BY_MOBILE_DEVICE;
            int nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
            SimSettings.NETWORK_DELAY_TYPES delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
            double delay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);
            EdgeHost host = (EdgeHost)(SimManager.
                    getInstance().
                    getEdgeServerManager().
                    getDatacenterList().get(task.getAssociatedHostId()).
                    getHostList().get(0));

            //When source host is not at access point (read from distant edge) intiate download from host (MAN delay)
            //host - data location, task.getSubmittedLocation() - access point location
            if(host.getLocation().getServingWlanId() != task.getSubmittedLocation().getServingWlanId())
            {
                delay = networkModel.getDownloadDelay(SimSettings.GENERIC_EDGE_DEVICE_ID, SimSettings.GENERIC_EDGE_DEVICE_ID, task);
                if (delay<0) {
                    SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType);
                    failedDueToBW++;
                }
                nextEvent = RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE;
                nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID + 1;
                delayType = SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY;
            }
            else if ( delay==0)
                SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());
            if(delay > 0)
            {
//                Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
                //task.getSubmittedLocation() - access point location, currentLocation - device location
                //TODO: under static mobility no need to check if device has moved, otherwise check
/*                if((task.getSubmittedLocation().getXPos() == currentLocation.getXPos()) && task.getSubmittedLocation().getYPos() == currentLocation.getYPos())
                {*/
/*                if(task.getIsParity()==1 && nextDeviceForNetworkModel==SimSettings.GENERIC_EDGE_DEVICE_ID)
                    activeCodedRequests = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).updateActiveCodedIOTasks(task.getIoTaskID(),1);
                if(activeCodedRequests==0)
                    ((StorageNetworkModel) networkModel).downloadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel,
                            task.getAssociatedHostId());*/
                if (nextDeviceForNetworkModel==SimSettings.GENERIC_EDGE_DEVICE_ID+1)
                    ((StorageNetworkModel) networkModel).downloadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel,
                            task.getAssociatedHostId());
                else if(((IdleActiveStorageLoadGenerator) loadGeneratorModel).getParityReadStarted(task.getIoTaskID())==false ) {
                    ((StorageNetworkModel) networkModel).downloadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel,
                            task.getAssociatedHostId());
                    ((IdleActiveStorageLoadGenerator) loadGeneratorModel).setParityReadStarted(true,task.getIoTaskID());
                }
                    SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, delayType);
//                    System.out.println("1Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
                    schedule(getId(), delay, nextEvent, task);
/*                }
                else
                {
                    System.out.println("Failed due to mobility");
                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                }*/
            }
            else
            {
                //For % of all IO requests, if they've failed, stop run
                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType);
                failedDueToBW++;
//                if(SimSettings.getInstance().isNsfExperiment()) {
                List<TaskProperty> taskList = loadGeneratorModel.getTaskList();
//                    double ratio = (double)failedDueToBW/IdleActiveStorageLoadGenerator.getNumOfIOTasks();
                    double ratio = (double)failedDueToBW/taskList.size();
                    if(ratio>0.01) {
                        try {
                            if (SimSettings.getInstance().isParamScanMode()) {
                                int numOfDevices = SimManager.getInstance().getNumOfMobileDevice();
                                String simScenario = SimManager.getInstance().getSimulationScenario();
                                String orchestratorPolicy = SimManager.getInstance().getOrchestratorPolicy();
                                String objectPlacementPolicy = SimManager.getInstance().getObjectPlacementPolicy();
                                Pattern pattern = Pattern.compile("SIMRESULT_(.*)_SINGLE.*");
                                Matcher matcher = pattern.matcher(SimLogger.getInstance().getFilePrefix());
                                matcher.find();
                                SimManager.getInstance().setLambda0(Double.valueOf(matcher.group(1)));
                                String[] simParams = {Integer.toString(numOfDevices), simScenario, orchestratorPolicy, objectPlacementPolicy,
                                        matcher.group(1)};
                                SimUtils.cleanOutputFolderPerConfiguration(SimLogger.getInstance().getOutputFolder(), simParams);
                            }
                            File file = new File(SimLogger.getInstance().getOutputFolder(), SimLogger.getInstance().getFilePrefix() + "_TASK_FAILED.log");
                            if (!file.exists())
                                new FileOutputStream(file).close();
                            System.out.println("Failed for: " + SimLogger.getInstance().getFilePrefix());
                            SimManager.getInstance().shutdownEntity();
                            SimLogger.printLine("100");
                            CloudSim.terminateSimulation();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(0);
                        }
                    }
//                }
            }
        }
    }
}
