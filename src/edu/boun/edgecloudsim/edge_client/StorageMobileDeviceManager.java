package edu.boun.edgecloudsim.edge_client;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;

//TODO: extend samplemobiledevicemanager
public class StorageMobileDeviceManager extends DefaultMobileDeviceManager {
    private int taskIdCounter=0;
    public StorageMobileDeviceManager() throws Exception {
    }


    @Override
    public void submitTask(TaskProperty edgeTask) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();

        //create a task
        Task task = createTask(edgeTask);

        Location currentLocation = SimManager.getInstance().getMobilityModel().
                getLocation(task.getMobileDeviceId(), CloudSim.clock());

        //set location of the mobile device which generates this task
        task.setSubmittedLocation(currentLocation);

        //storage
        task.setObjectID(edgeTask.getObjectID());
        task.setStripeID(edgeTask.getStripeID());

        //add related task to log list
        //TODO: Check what log list does
        SimLogger.getInstance().addLog(task.getCloudletId(),
                task.getTaskType(),
                (int)task.getCloudletLength(),
                (int)task.getCloudletFileSize(),
                (int)task.getCloudletOutputSize(),
                task.getStripeID(),
                task.getObjectID());

        int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(task);



        //TODO: Currently shouldn't enter here
        if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){
            double WanDelay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);

            if(WanDelay>0){
                networkModel.uploadStarted(currentLocation, nextHopId);
                SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
                SimLogger.getInstance().setUploadDelay(task.getCloudletId(), WanDelay, SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
                schedule(getId(), WanDelay, REQUEST_RECEIVED_BY_CLOUD, task);
            }
            else
            {
                //SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
                SimLogger.getInstance().rejectedDueToBandwidth(
                        task.getCloudletId(),
                        CloudSim.clock(),
                        SimSettings.VM_TYPES.CLOUD_VM.ordinal(),
                        SimSettings.NETWORK_DELAY_TYPES.WAN_DELAY);
            }
        }
        else if(nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            double WlanDelay = networkModel.getUploadDelay(task.getMobileDeviceId(), nextHopId, task);

            if(WlanDelay > 0){
                //TODO: disable upload
                networkModel.uploadStarted(currentLocation, nextHopId);
                schedule(getId(), WlanDelay, REQUEST_RECIVED_BY_EDGE_DEVICE, task);
                SimLogger.getInstance().taskStarted(task.getCloudletId(), CloudSim.clock());
                SimLogger.getInstance().setUploadDelay(task.getCloudletId(), WlanDelay, SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
            else {
                SimLogger.getInstance().rejectedDueToBandwidth(
                        task.getCloudletId(),
                        CloudSim.clock(),
                        SimSettings.VM_TYPES.EDGE_VM.ordinal(),
                        SimSettings.NETWORK_DELAY_TYPES.WLAN_DELAY);
            }
        }
        else {
            SimLogger.printLine("Unknown nextHopId! Terminating simulation...");
            System.exit(0);
        }
    }
}
