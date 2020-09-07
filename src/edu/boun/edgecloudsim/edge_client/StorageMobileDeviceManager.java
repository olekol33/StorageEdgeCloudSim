package edu.boun.edgecloudsim.edge_client;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.SampleNetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class StorageMobileDeviceManager extends SampleMobileDeviceManager {
    private int taskIdCounter=0;
    public StorageMobileDeviceManager() throws Exception {
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
        task.setObjectToRead(edgeTask.getObjectToRead());
        task.setObjectRead(edgeTask.getObjectRead());
        task.setStripeID(edgeTask.getStripeID());
        task.setParitiesToRead(edgeTask.getParitiesToRead());
        task.setIoTaskID(edgeTask.getIoTaskID());
        task.setIsParity(edgeTask.getIsParity());

        //todo: update access host
        //add related task to log list
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize(),
                task.getStripeID(),
                task.getObjectToRead(),
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
            nextDeviceForNetworkModel = SimSettings.CLOUD_DATACENTER_ID;
        }
        else {
            //no upload delay
//            delay = networkModel.getUploadDelay(task.getMobileDeviceId(), SimSettings.GENERIC_EDGE_DEVICE_ID, task);
            delay = 0;
            vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
            nextEvent = REQUEST_RECEIVED_BY_EDGE_DEVICE;
            delayType = SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY;
            //TODO: check why not used
            nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID;
        }

//        if(delay>0){
        if(delay>=0){
            Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, nextHopId);
            //TODO: perhaps can remove
            task.setAccessHostID(task.getSubmittedLocation().getServingWlanId());
            SimLogger.getInstance().setAccessHostID(task.getCloudletId(),task.getSubmittedLocation().getServingWlanId());
            //TODO: probably can remove
            edgeTask.setAccessHostID(task.getSubmittedLocation().getServingWlanId());




            //TODO: perhaps can remove
            SimLogger.getInstance().setObjectRead(task.getCloudletId(),task.getObjectRead());

            if(selectedVM != null){
                //TODO: perhaps hostid is redundant if hostindex already used
                //
//                SimLogger.getInstance().setHostId(task.getCloudletId(),task.getHostID());
                SimLogger.getInstance().setHostId(task.getCloudletId(),selectedVM.getHost().getId());

                //set related host id
                task.setAssociatedDatacenterId(nextHopId);

                //set related host id
                //TODO: perhaps can remove
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
            else{
                //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
//                SimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType);
            }
        }
        else
        {
            //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
            SimLogger.getInstance().rejectedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), vmType, delayType);
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
//                networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID+1);
                //TODO: recheck, stop read from data host and then read from access point
                //TODO: GENERIC_EDGE_DEVICE_ID+1 is MAN, not affecting count at host
                networkModel.downloadFinished(StaticRangeMobility.getDCLocation(task.getAssociatedHostId()), SimSettings.GENERIC_EDGE_DEVICE_ID+1);

                //SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from edge");
                //get delay between access point and device
                //TODO: add delay for distance between them (Hops)
                double delay = networkModel.getDownloadDelay(task.getAssociatedHostId(), task.getMobileDeviceId(), task);

                if(delay > 0)
                {
                    Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
                    if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
                    {
//                        networkModel.downloadStarted(currentLocation, SimSettings.GENERIC_EDGE_DEVICE_ID);
                        networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
                        SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
//                        System.out.println("4Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
                        schedule(getId(), delay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, task);
                    }
                    else
                    {
                        SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                    }
                }
                else
                {
                    System.out.println("delay < 0");
                    SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());
//                    SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
                }

                break;
            }
            case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
            {
                Task task = (Task) ev.getData();

                if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
                    networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
                else
                    networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);

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
                    networkModel.downloadStarted(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
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
                if (delay==0)
                    SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType);
                nextEvent = RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE;
                nextDeviceForNetworkModel = SimSettings.GENERIC_EDGE_DEVICE_ID + 1;
                delayType = SimSettings.NETWORK_DELAY_TYPES.MAN_DELAY;
            }
            else if ( delay==0)
                SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());
            if(delay > 0)
            {
                Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(),CloudSim.clock()+delay);
                //task.getSubmittedLocation() - access point location, currentLocation - device location
                //currently should always be true since no mobility
                if(task.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
                {
//                    networkModel.downloadStarted(currentLocation, nextDeviceForNetworkModel);

                    //TODO: recheck, download from host
                    networkModel.downloadStarted(task.getSubmittedLocation(), nextDeviceForNetworkModel);
//                    networkModel.downloadStarted(host.getLocation(), nextDeviceForNetworkModel);

                    SimLogger.getInstance().setDownloadDelay(task.getCloudletId(), delay, delayType);
//                    System.out.println("1Submitting IoTask " + task.getIoTaskID() + " object " + task.getObjectRead() + "\n");
                    schedule(getId(), delay, nextEvent, task);
                }
                else
                {
                    System.out.println("Failed due to mobility");
                    SimLogger.getInstance().failedDueToMobility(task.getCloudletId(), CloudSim.clock());
                }
            }
/*            else
            {
//                SimLogger.getInstance().taskRejectedDueToQueue(task.getCloudletId(), CloudSim.clock());
//                SimLogger.getInstance().failedDueToBandwidth(task.getCloudletId(), CloudSim.clock(), delayType);
            }*/
        }
    }
}
