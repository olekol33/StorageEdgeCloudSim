package edu.boun.edgecloudsim.network;

import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.SampleMobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.task_generator.IdleActiveStorageLoadGenerator;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
//import java.io.IOException;

public class StorageNetworkModel extends SampleNetworkModel {

    protected double[] hostManPoissonMeanForDownload; //seconds
    protected double[] hostAvgManTaskOutputSize; //bytes
    protected double[] hostTotalManTaskOutputSize;
    protected double[] hostNumOfManTaskForDownload;
    protected int[] manHostClients;
    protected int[] hostOperativity;

    protected double[] previousHostManPoissonMeanForDownload; //seconds
    protected double[] previousHostAvgManTaskOutputSize; //bytes
    protected double[] previousHostTotalManTaskOutputSize;
    protected double[] previousHostNumOfManTaskForDownload;
    protected int[] previousManHostClients;

    public static int MAN_DELAY=-999;


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
    public void initialize() {
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        wanClients = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        wlanClients = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        manHostClients = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        hostOperativity = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter

        hostManPoissonMeanForDownload = new double[numOfEdgeDatacenters];
        hostAvgManTaskOutputSize = new double[numOfEdgeDatacenters];
        hostTotalManTaskOutputSize = new double[numOfEdgeDatacenters];
        hostNumOfManTaskForDownload = new double[numOfEdgeDatacenters];

        previousManHostClients = new int[numOfEdgeDatacenters];
        previousHostManPoissonMeanForDownload = new double[numOfEdgeDatacenters];
        previousHostAvgManTaskOutputSize = new double[numOfEdgeDatacenters];
        previousHostTotalManTaskOutputSize = new double[numOfEdgeDatacenters];
        previousHostNumOfManTaskForDownload = new double[numOfEdgeDatacenters];


        int numOfApp = SimSettings.getInstance().getTaskLookUpTable().length;
        SimSettings SS = SimSettings.getInstance();
        for(int taskIndex=0; taskIndex<numOfApp; taskIndex++) {
            if(SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.USAGE_PERCENTAGE] == 0) {
                SimLogger.printLine("Usage percantage of task " + taskIndex + " is 0! Terminating simulation...");
                System.exit(0);
            }
            else{
                double weight = SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.USAGE_PERCENTAGE]/(double)100;

                //assume half of the tasks use the MAN at the beginning
                //Oleg: why multiply by 4?
//                ManPoissonMeanForDownload += ((SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.POISSON_INTERARRIVAL])*weight) * 4;

                ManPoissonMeanForDownload += ((SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.POISSON_INTERARRIVAL])*weight);
                ManPoissonMeanForUpload = ManPoissonMeanForDownload;

                avgManTaskInputSize += SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.DATA_UPLOAD]*weight;
                avgManTaskOutputSize += SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.DATA_DOWNLOAD]*weight;

                }
            }
        for(int host=0; host<numOfEdgeDatacenters; host++) {
//            hostManPoissonMeanForDownload[host] = ManPoissonMeanForDownload;

            hostManPoissonMeanForDownload[host] = SampleMobileDeviceManager.getMm1QueueModelUpdateInteval(); //=MM1_QUEUE_MODEL_UPDATE_INTEVAL
            hostAvgManTaskOutputSize[host] = avgManTaskOutputSize;
            hostTotalManTaskOutputSize[host] = 0;
            hostNumOfManTaskForDownload[host] = 0;
            hostOperativity[host] = 1;
        }
        //Oleg: not sure why do average after weight calculation
/*        ManPoissonMeanForDownload = ManPoissonMeanForDownload/numOfApp;
        ManPoissonMeanForUpload = ManPoissonMeanForUpload/numOfApp;
        avgManTaskInputSize = avgManTaskInputSize/numOfApp;
        avgManTaskOutputSize = avgManTaskOutputSize/numOfApp;*/

        lastMM1QueeuUpdateTime = SimSettings.CLIENT_ACTIVITY_START_TIME;
        totalManTaskOutputSize = 0;
        numOfManTaskForDownload = 0;
        totalManTaskInputSize = 0;
        numOfManTaskForUpload = 0;
    }

    @Override
    //Download from edge (sourceDevice) to device (destDevice)
    public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        double delay = 0;

        //special case for man communication
        // When communication is between edge nodes -> On grid
        if(sourceDeviceId == destDeviceId && sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
//            return delay = getManDownloadDelay();
            return delay = getManDownloadDelay(task.getAssociatedHostId(),1);
        }
/*        else if(sourceDeviceId == destDeviceId && sourceDeviceId != SimSettings.CLOUD_DATACENTER_ID)
            delay = getManDownloadDelay(task.getAssociatedHostId(),0);*/
        //When device reads from node, it's MAN delay + WLAN delay
        else if(sourceDeviceId == destDeviceId && sourceDeviceId != SimSettings.GENERIC_EDGE_DEVICE_ID) {
            delay = getManDownloadDelay(task.getAssociatedHostId(), 0);
            if (delay < 0)
                return MAN_DELAY;
        }
        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId, CloudSim.clock());
        Location accessPointLocation = StaticRangeMobility.getDCLocation(deviceLocation.getServingWlanId());
//        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId,CloudSim.clock());


        //cloud server to mobile device
        //TODO: update for cloud
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID){
            delay += getWanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
        }
        //edge device (wifi access point) to mobile device
        else{
            //factor of #accesses
            double wlanDelay = getWlanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
            delay += wlanDelay;
            //In case something went wrong
            if (wlanDelay==0)
                System.out.println("delay=0");
            if (wlanDelay<0)
                System.out.println("delay<0");
//                return delay;
            //Add delay on network if access point not in range
            Location nearestAccessPoint = StaticRangeMobility.getAccessPoint(deviceLocation,accessPointLocation);
            if (nearestAccessPoint.getServingWlanId() != accessPointLocation.getServingWlanId())
                System.out.println("nearestAccessPoint.getServingWlanId() != accessPointLocation.getServingWlanId()");
            //divide by factor
/*            double distance = StaticRangeMobility.getDistance(deviceLocation,nearestAccessPoint);//Inverse-square law
            double distance2 = Math.sqrt(distance);
            System.out.println(delay*distance2);*/
            delay /= StaticRangeMobility.getDistanceDegradation(deviceLocation,nearestAccessPoint);
//            System.out.println(delay);
        }

        return delay;
    }


    @Override
    double getWlanDownloadDelay(Location accessPointLocation, double dataSize) {
        int numOfWlanUser = wlanClients[accessPointLocation.getServingWlanId()];
        double taskSizeInKb = dataSize * (double)8; //KB to Kb
        double result=0;
//        System.out.println("previously " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");
        if(numOfWlanUser < experimentalWlanDelay.length)
            result = taskSizeInKb /*Kb*/ / (experimentalWlanDelay[numOfWlanUser] * (double) 3 ) /*Kbps*/; //802.11ac is around 3 times faster than 802.11n
        else
            result = -1;
//            System.out.println("Insufficient delay data at experimentalWlanDelay for " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");

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
    @Override
    double getWanDownloadDelay(Location accessPointLocation, double dataSize) {
        int numOfWanUser = wanClients[accessPointLocation.getServingWlanId()];
        double taskSizeInKb = dataSize * (double)8; //KB to Kb
        double result=0;

        if(numOfWanUser < experimentalWanDelay.length)
            result = taskSizeInKb /*Kb*/ / (experimentalWanDelay[numOfWanUser]) /*Kbps*/;
/*        else
            System.out.println("Insufficient delay data at experimentalWanDelay for " + wanClients[accessPointLocation.getServingWlanId()]+ " tasks");*/

        //System.out.println("--> " + numOfWanUser + " user, " + taskSizeInKb + " KB, " +result + " sec");

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

    //Logs queue in all hosts in each interval
    public void logManQueue() throws FileNotFoundException {
        String savestr = SimLogger.getInstance().getOutputFolder()+ "/" + SimLogger.getInstance().getFilePrefix() + "_MAN_QUEUE.log";
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
                    + SimSettings.DELIMITER + Integer.toString(manHostClients[i]));
            out.append("\n");
        }
        out.close();
    }


    @Override
    public void updateMM1QueeuModel(){
        double lastInterval = CloudSim.clock() - lastMM1QueeuUpdateTime;
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        lastMM1QueeuUpdateTime = CloudSim.clock();

        //Log queue in edge hosts
        try {
            if (SimSettings.getInstance().isStorageLogEnabled()) {
                logHostQueue();
                logManQueue();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //update failed host if this is the scenario
        if (SimSettings.getInstance().isHostFailureScenario()) {
            if (CloudSim.clock() > SimSettings.getInstance().getHostFailureTime()) {
                if (SimSettings.getInstance().isDynamicFailure()){
                    LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
                    hostOperativity = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).dynamicFailureGenerator(hostOperativity);
                }
                else { //static failure
                    for (int host : SimSettings.getInstance().getHostFailureID()) {
                        if (hostOperativity[host] != 0) {
                            SimLogger.printLine("Failed host: " + host);
                            hostOperativity[host] = 0;
                        }
                    }
                }
            }
        }
//        System.out.println("Time: " + CloudSim.clock() + ", Tasks: " + numOfManTaskForDownload);
        if(numOfManTaskForDownload != 0){
            ManPoissonMeanForDownload = lastInterval / (numOfManTaskForDownload / (double)numberOfMobileDevices);
            avgManTaskOutputSize = totalManTaskOutputSize / numOfManTaskForDownload;

            for(int host=0; host<numOfEdgeDatacenters; host++) {


                hostManPoissonMeanForDownload[host] = lastInterval;
                if (hostNumOfManTaskForDownload[host]==0)
                    hostAvgManTaskOutputSize[host]=avgManTaskOutputSize;
                else
                    hostAvgManTaskOutputSize[host] = hostTotalManTaskOutputSize[host] / hostNumOfManTaskForDownload[host];

                previousHostTotalManTaskOutputSize[host] = hostTotalManTaskOutputSize[host];
                previousHostNumOfManTaskForDownload[host] = hostNumOfManTaskForDownload[host];
                previousManHostClients[host] = manHostClients[host];

                hostTotalManTaskOutputSize[host] = 0;
                hostNumOfManTaskForDownload[host] = 0;
                manHostClients[host] = 0;


            }
//			System.out.println("numOfManTaskForDownload: " + numOfManTaskForDownload + " avgManTaskOutputSize: "+ avgManTaskOutputSize); //TO remove
        }
/*        if(numOfManTaskForUpload != 0){
            ManPoissonMeanForUpload = lastInterval / (numOfManTaskForUpload / (double)numberOfMobileDevices);
            avgManTaskInputSize = totalManTaskInputSize / numOfManTaskForUpload;
        }*/

        totalManTaskOutputSize = 0;
        numOfManTaskForDownload = 0;
        totalManTaskInputSize = 0;
        numOfManTaskForUpload = 0;
    }
    //Use M/M/1 queue for each node on the grid
    double getManDownloadDelay(int hostIndex, int readOnGrid) {
        //calculateMM1(propogationDelay=0,bandwidth param, PoissonMean paran,avgTaskSize param,deviceCount i)
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        if (hostIndex>=numOfEdgeDatacenters)
            System.out.println("ERROR: Illegal host id " + hostIndex);
        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
        if (vmArray.size()>1)
            System.out.println("More than 1 VM");
        int bandwidth = vmArray.get(0).getReadRate()*1024*8;//kbps
        //These are identical and they deducted (kept for legacy)



        //count for interval
        manHostClients[hostIndex]++;
        hostNumOfManTaskForDownload[hostIndex] = manHostClients[hostIndex];
        hostTotalManTaskOutputSize[hostIndex] += hostAvgManTaskOutputSize[hostIndex];

        //initialization
        if (previousHostAvgManTaskOutputSize[hostIndex]==0)
            previousHostAvgManTaskOutputSize[hostIndex] = hostAvgManTaskOutputSize[hostIndex];
        if (previousManHostClients[hostIndex]==0)
            previousManHostClients[hostIndex] = manHostClients[hostIndex];


        //check for overflow on the fly
        double lambda = ((double)1/(double)hostManPoissonMeanForDownload[hostIndex]); //task per seconds
        //TODO: convert KB to Kb
        double mu = bandwidth /*Kbps*/ / (8*previousHostAvgManTaskOutputSize[hostIndex]) /*Kb*/; //task per seconds
        if (1 >mu - (lambda*(double)manHostClients[hostIndex])) {
//        if ((lambda*(double)manHostClients[hostIndex]>mu)) {
            manHostClients[hostIndex]--;
            hostTotalManTaskOutputSize[hostIndex] -= hostAvgManTaskOutputSize[hostIndex];
            return -1;
        }

        double result = calculateMM1(readOnGrid * SimSettings.getInstance().getInternalLanDelay(),
                bandwidth,
//                ManPoissonMeanForDownload,
                hostManPoissonMeanForDownload[hostIndex],
                previousHostAvgManTaskOutputSize[hostIndex],
                previousManHostClients[hostIndex]);

        totalManTaskOutputSize += avgManTaskOutputSize;
        numOfManTaskForDownload++;


        if (result < 0){
            manHostClients[hostIndex]--;
            hostTotalManTaskOutputSize[hostIndex] -= hostAvgManTaskOutputSize[hostIndex];
        }
//			System.out.println("totalManTaskOutputSize: " + totalManTaskOutputSize + " numOfManTaskForDownload: "+ numOfManTaskForDownload); //TO remove
        //System.out.println("--> " + SimManager.getInstance().getNumOfMobileDevice() + " user, " +result + " sec");
        return result;
    }

    public void downloadStarted(Location accessPointLocation, int sourceDeviceId, int hostIndex) {
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID)
            wanClients[accessPointLocation.getServingWlanId()]++;
        else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            wlanClients[accessPointLocation.getServingWlanId()]++;
        }
        else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID+1) {
            manClients++;
        }
        else {
            SimLogger.printLine("Error - unknown device id in downloadStarted(). Terminating simulation...");
            System.exit(0);
        }
//		System.out.println("manClients: " + manClients); //To remove
/*        if((100<CloudSim.clock()) && (CloudSim.clock()<105))
            System.out.println(hostIndex);*/
    }

    public void downloadFinished(Location accessPointLocation, int sourceDeviceId, int hostIndex) {
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID)
            wanClients[accessPointLocation.getServingWlanId()]--;
        else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            wlanClients[accessPointLocation.getServingWlanId()]--;
        }
        else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID+1) {
            manClients--;
//            manHostClients[hostIndex]--;
        }
        else {
            SimLogger.printLine("Error - unknown device id in downloadFinished(). Terminating simulation...");
            System.exit(0);
        }
/*        if((100<CloudSim.clock()) && (CloudSim.clock()<105))
            System.out.println(hostIndex);*/

//		System.out.println("Users in " + accessPointLocation.getServingWlanId() + ": " + wlanClients[accessPointLocation.getServingWlanId()]); //TO remove
    }

    public int getWlanQueueSize(int hostID){
        return wlanClients[hostID];
    }

    public int getWanQueueSize(int hostID) {
        return wanClients[hostID];
    }

    public int getManQueueSize(int hostID) {
        return manHostClients[hostID];
    }
    public List<String> getNonOperativeHosts() {
        List<String> nonOperativeHosts = new ArrayList<>();
        for(int i=0; i<SimSettings.getInstance().getNumOfEdgeDatacenters(); i++){
            if (hostOperativity[i]==0)
                nonOperativeHosts.add(Integer.toString(i));
        }
        return nonOperativeHosts;
    }
}
