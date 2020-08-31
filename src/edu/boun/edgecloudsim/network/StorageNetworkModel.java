package edu.boun.edgecloudsim.network;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.*;
//import java.io.IOException;

public class StorageNetworkModel extends SampleNetworkModel {
    public StorageNetworkModel(int _numberOfMobileDevices, String _simScenario) {
        super(_numberOfMobileDevices, _simScenario);
    }

    //Adds delay as function of #slots in grid
/*    //TODO: adjust to number of users on link
    private double gridDistanceDelay(Location srcLocation, Location destLocation, double taskSize){
        double taskSizeInKb = taskSize * (double)8; //KB to Kb
        int gridDistance = StaticRangeMobility.getGridDistance(srcLocation,destLocation);
        //TODO: temporary. Need mechanism to penalize for distant read.
        double result = taskSizeInKb *//*Kb*//* / (experimentalWlanDelay[gridDistance]);
        return result;
    }*/


    @Override
    //Download from edge (sourceDevice) to device (destDevice)
    public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        double delay = 0;

        //special case for man communication
        // When edge downloads from itself
        //TODO: check, avoid entering in
        if(sourceDeviceId == destDeviceId && sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
            return delay = getManDownloadDelay();
        }
        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId, CloudSim.clock());
        Location accessPointLocation = StaticRangeMobility.getDCLocation(deviceLocation.getServingWlanId());
//        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId,CloudSim.clock());


        //cloud server to mobile device
        //TODO: update for cloud
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID){
            delay = getWanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
        }
        //edge device (wifi access point) to mobile device
        else{
            //factor of #accesses
            delay = getWlanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
            //In case something went wrong
            if (delay==0)
                return delay;
            //Add delay on network if access point not in range
            //TODO: need to update access point to be nearest from destination
            //TODO: verify it's correct and matching orchestrator
            Location nearestAccessPoint = StaticRangeMobility.getAccessPoint(deviceLocation,accessPointLocation);
            //divide by factor
            delay /= StaticRangeMobility.getDistanceDegradation(deviceLocation,nearestAccessPoint);
        }

        return delay;
    }


    @Override
    double getWlanDownloadDelay(Location accessPointLocation, double dataSize) {
        int numOfWlanUser = wlanClients[accessPointLocation.getServingWlanId()];
        double taskSizeInKb = dataSize * (double)8; //KB to Kb
        double result=0;
//        System.out.println("Currently " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");
        if(numOfWlanUser < experimentalWlanDelay.length)
            result = taskSizeInKb /*Kb*/ / (experimentalWlanDelay[numOfWlanUser] * (double) 3 ) /*Kbps*/; //802.11ac is around 3 times faster than 802.11n
/*        else
            System.out.println("Insufficient delay data at experimentalWlanDelay for " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");*/

/*        if(numOfWlanUser >80)
            System.out.println("Insufficient delay data at experimentalWlanDelay for " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");
        else if (numOfWlanUser >60)
            System.out.println("Insufficient delay data at experimentalWlanDelay for " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");
        else if (numOfWlanUser >40)
            System.out.println("Insufficient delay data at experimentalWlanDelay for " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");
        else if (numOfWlanUser >20)
            System.out.println("Insufficient delay data at experimentalWlanDelay for " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");*/
        //System.out.println("--> " + numOfWlanUser + " user, " + taskSizeInKb + " KB, " +result + " sec");
        return result;
    }

    //Logs queue in all hosts in each interval
    public void logHostQueue() throws FileNotFoundException {
        String savestr = SimLogger.getInstance().getOutputFolder()+ "/" + SimLogger.getInstance().getFilePrefix() + "_HOST_QUEUE.log";
        File f = new File(savestr);

        PrintWriter out = null;
        if ( f.exists() && !f.isDirectory() ) {
            out = new PrintWriter(new FileOutputStream(new File(savestr), true));
        }
        else {
            out = new PrintWriter(savestr);
            out.append("Time;HostID;Requests");
            out.append("\n");
        }

        for (int i=0;i<SimSettings.getInstance().getNumOfEdgeHosts();i++){
            out.append(CloudSim.clock() + SimSettings.DELIMITER + Integer.toString(i)
                    + SimSettings.DELIMITER + Integer.toString(wlanClients[i]));
            out.append("\n");
        }
        out.close();
    }
    @Override
    public void updateMM1QueeuModel(){
        double lastInterval = CloudSim.clock() - lastMM1QueeuUpdateTime;
        lastMM1QueeuUpdateTime = CloudSim.clock();

        //Log queue in edge hosts
        try {
            logHostQueue();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if(numOfManTaskForDownload != 0){
            ManPoissonMeanForDownload = lastInterval / (numOfManTaskForDownload / (double)numberOfMobileDevices);
            avgManTaskOutputSize = totalManTaskOutputSize / numOfManTaskForDownload;
//			System.out.println("numOfManTaskForDownload: " + numOfManTaskForDownload + " avgManTaskOutputSize: "+ avgManTaskOutputSize); //TO remove
        }
        if(numOfManTaskForUpload != 0){
            ManPoissonMeanForUpload = lastInterval / (numOfManTaskForUpload / (double)numberOfMobileDevices);
            avgManTaskInputSize = totalManTaskInputSize / numOfManTaskForUpload;
        }

        totalManTaskOutputSize = 0;
        numOfManTaskForDownload = 0;
        totalManTaskInputSize = 0;
        numOfManTaskForUpload = 0;
    }

    public int getQueueSize(int hostID){
        return wlanClients[hostID];
    }
}
