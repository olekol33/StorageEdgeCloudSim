package edu.boun.edgecloudsim.network;

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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//import java.io.IOException;

public class StorageNetworkModel extends SampleNetworkModel {

    EdgeQueue wlanQueue;
    EdgeQueue manQueue;

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

    protected double[] hostAvgWlanTaskOutputSize; //bytes
    protected double[] hostTotalWlanTaskOutputSize;
    protected double[] hostNumOfWlanTaskForDownload;
    protected double[] previousHostAvgWlanTaskOutputSize; //bytes
    protected double[] previousHostTotalWlanTaskOutputSize;
    protected double[] previousHostNumOfWlanTaskForDownload;
    protected int[] previousWlanHostClients;

    protected int[] wlanHostClients;
    protected double totalWlanTaskInputSize;
    protected double totalWlanTaskOutputSize;
    protected double numOfWlanTaskForDownload;
    protected double numOfWlanTaskForUpload;
    protected double wlanPoissonMeanForDownload; //seconds
    protected double avgWlanTaskOutputSize; //bytes
    protected double[] wlanLatestDelay; //for log
    protected double[] manLatestDelay; //for log


    public static int MAN_DELAY=-999;


    public StorageNetworkModel(int _numberOfMobileDevices, String _simScenario) {
        super(_numberOfMobileDevices, _simScenario);
    }

    @Override
    public void initialize() {
        //assuming all sizes in vms are equal

        double manMu = (SimSettings.getInstance().getTaskProcessingMbps()/8) * 1000 *(long)1000 /
                SimSettings.getInstance().getObjectSize();
        wlanQueue = new EdgeQueue(SimSettings.getInstance().getServedReqsPerSec());
        manQueue = new EdgeQueue(manMu);
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        wanClients = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        wlanClients = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        manHostClients = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        hostOperativity = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        

        //Man
        hostManPoissonMeanForDownload = new double[numOfEdgeDatacenters];
        hostAvgManTaskOutputSize = new double[numOfEdgeDatacenters];
        hostTotalManTaskOutputSize = new double[numOfEdgeDatacenters];
        hostNumOfManTaskForDownload = new double[numOfEdgeDatacenters];
        previousManHostClients = new int[numOfEdgeDatacenters];
        previousHostManPoissonMeanForDownload = new double[numOfEdgeDatacenters];
        previousHostAvgManTaskOutputSize = new double[numOfEdgeDatacenters];
        previousHostTotalManTaskOutputSize = new double[numOfEdgeDatacenters];
        previousHostNumOfManTaskForDownload = new double[numOfEdgeDatacenters];
        manLatestDelay = new double[numOfEdgeDatacenters];


        //Wlan
        wlanHostClients = new int[numOfEdgeDatacenters];  //we have one access point for each datacenter
        hostAvgWlanTaskOutputSize = new double[numOfEdgeDatacenters];
        wlanLatestDelay = new double[numOfEdgeDatacenters];
        hostTotalWlanTaskOutputSize = new double[numOfEdgeDatacenters];
        hostNumOfWlanTaskForDownload = new double[numOfEdgeDatacenters];

        previousWlanHostClients = new int[numOfEdgeDatacenters];
        previousHostAvgWlanTaskOutputSize = new double[numOfEdgeDatacenters];
        previousHostTotalWlanTaskOutputSize = new double[numOfEdgeDatacenters];
        previousHostNumOfWlanTaskForDownload = new double[numOfEdgeDatacenters];


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

            //Get interval to update MM1 model
            hostManPoissonMeanForDownload[host] = SampleMobileDeviceManager.getMm1QueueModelUpdateInterval(); //=MM1_QUEUE_MODEL_UPDATE_INTEVAL
            //Average task (object) size
            hostAvgManTaskOutputSize[host] = avgManTaskOutputSize;
            hostTotalManTaskOutputSize[host] = 0;
            hostNumOfManTaskForDownload[host] = 0;
            hostOperativity[host] = 1;

            hostAvgWlanTaskOutputSize[host] = avgManTaskOutputSize;
            hostTotalWlanTaskOutputSize[host] = 0;
            hostNumOfWlanTaskForDownload[host] = 0;
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

        totalWlanTaskOutputSize = 0;
        numOfWlanTaskForDownload = 0;
        totalWlanTaskInputSize = 0;
        numOfWlanTaskForUpload = 0;
    }


    @Override
    public double getUploadDelay(int sourceDeviceId, int destDeviceId, int uploadType, Task task) {
        long dataSize = task.getCloudletFileSize();
        double delay = 0;
        int srcID=-1,dstID=destDeviceId;
        String operationType = "";

        //special case for man communication
        if(uploadType == SimSettings.EDGE_TO_EDGE){
			delay = getManUploadDelay(sourceDeviceId,dataSize, task.getSubmittedLocation().getServingWlanId());
            srcID = task.getSubmittedLocation().getServingWlanId();
            operationType="UPLOAD_MAN_EDGE_TO_EDGE";
        }
        else if(uploadType == SimSettings.DEVICE_TO_LOCAL_EDGE){
            delay = getWlanUploadDelay(destDeviceId, dataSize, task.getSubmittedLocation().getServingWlanId());
            delay += SimSettings.getInstance().getDelayToUser(); //user to node delay
            operationType="UPLOAD_WLAN_DEVICE_TO_EDGE";
        }
        else if(uploadType == SimSettings.LOCAL_EDGE_TO_REMOTE_EDGE){
            delay = getWlanUploadDelay(destDeviceId,dataSize, task.getSubmittedLocation().getServingWlanId());
            srcID = task.getSubmittedLocation().getServingWlanId();
            operationType="UPLOAD_WLAN_EDGE_TO_EDGE";
        }
        else if(uploadType == SimSettings.REMOTE_EDGE_TO_LOCAL_EDGE){
            delay = getWlanUploadDelay(destDeviceId,dataSize, task.getSubmittedLocation().getServingWlanId());
            srcID = task.getAssociatedHostId();
            dstID = task.getSubmittedLocation().getServingWlanId();
            operationType="DOWNLOAD_WLAN_EDGE_TO_DEVICE";
        }
        else{
            throw new IllegalStateException("No matching upload type");
        }

        logRequestPathInQueues(task, operationType, srcID, dstID, dataSize, delay);

        return delay;

/*        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(sourceDeviceId,CloudSim.clock());

        //mobile device to cloud server
        if(destDeviceId == SimSettings.CLOUD_DATACENTER_ID){
//            delay = getWanUploadDelay(accessPointLocation, task.getCloudletFileSize());
        }
        //mobile device to edge device (wifi access point)
        else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
            delay = getWlanUploadDelay(sourceDeviceId, task.getCloudletFileSize());
        }

        return delay;*/
    }

    //wlan upload and download delay is symmetric in this model
    private double getWlanUploadDelay(int sourceDeviceId, double dataSize, int userLocation) {
        return wlanQueue.getQueueDownloadDelay(sourceDeviceId, dataSize,0, userLocation);
    }

    //upload and download delay is symmetric in this model
    private double getManUploadDelay(int sourceDeviceId, double dataSize, int userLocation) {
        return manQueue.getQueueDownloadDelay(sourceDeviceId, dataSize,1, userLocation);
    }

    @Override
    //Download from edge (sourceDevice) to device (destDevice)
    public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        long dataSize = task.getCloudletOutputSize();
        double delay = 0;
        //special case for man communication
        // When communication is between edge nodes(on grid)
        if(destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID && sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
            delay = manQueue.getQueueDownloadDelay(task.getAssociatedHostId(),dataSize,1, task.getSubmittedLocation().getServingWlanId());
            logRequestPathInQueues(task, "DOWNLOAD_MAN_EDGE_TO_EDGE", task.getAssociatedHostId(),
                    task.getSubmittedLocation().getServingWlanId(), dataSize, delay);
            return delay;
        }
        StaticRangeMobility staticMobility = (StaticRangeMobility)SimManager.getInstance().getMobilityModel();

//        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
        Location deviceLocation = staticMobility.getLocation(task.getMobileDeviceId(), CloudSim.clock());
        Location accessPointLocation = staticMobility.getDCLocation(deviceLocation.getServingWlanId());;


        //cloud server to mobile device
        //TODO: update for cloud
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID){
            assert true; //do nothing
        }
        //edge device (wifi access point) to mobile device
        else{
            //factor of #accesses
            //For access node user location = destDeviceId
            //For remote read destDeviceId is object location
            int accessLocation = deviceLocation.getServingWlanId();
            if(destDeviceId == 100007) //RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE
                accessLocation = sourceDeviceId;
            double wlanDelay = wlanQueue.getQueueDownloadDelay(sourceDeviceId,dataSize,0, task.getSubmittedLocation().getServingWlanId());
            logRequestPathInQueues(task, "DOWNLOAD_WLAN_EDGE_NODE", task.getAssociatedHostId(),
                    task.getSubmittedLocation().getServingWlanId(), dataSize, wlanDelay);
            delay += wlanDelay;
            //In case something went wrong
            if (wlanDelay==0)
                System.out.println("delay=0");
            //Add delay on network if access point not in range

            Location nearestAccessPoint = staticMobility.getAccessPoint(deviceLocation,accessPointLocation);
            if (nearestAccessPoint.getServingWlanId() != accessPointLocation.getServingWlanId())
                System.out.println("nearestAccessPoint.getServingWlanId() != accessPointLocation.getServingWlanId()");
            //additional delay
            if(SimSettings.getInstance().isApplySignalAttenuation())
                delay /= StaticRangeMobility.getSignalAttenuation(deviceLocation,nearestAccessPoint,100,2);
        }
        return delay;
    }
    @Override
    double getWanDownloadDelay(Location accessPointLocation, double dataSize) {
        int numOfWanUser = wanClients[accessPointLocation.getServingWlanId()];
        double taskSizeInKb = dataSize * (double)8; //KB to Kb
        double result=0;

        if(numOfWanUser < experimentalWanDelay.length)
            result = taskSizeInKb /*Kb*/ / (experimentalWanDelay[numOfWanUser]) /*Kbps*/;
        return result;
    }

    private void logRequestPathInQueues(Task task, String operationType, int srcID, int dstID, long dataSize, double delay){
        if(!SimSettings.getInstance().getDeepFileLoggingEnabled())
            return;
        String filepath = SimLogger.getInstance().getOutputFolder()+ "/" + SimLogger.getInstance().getFilePrefix() + "_REQS_PATH.log";
        File f = new File(filepath);
        PrintWriter out = null;
        try {
            if (f.exists() && !f.isDirectory()) {
                out = new PrintWriter(new FileOutputStream(f, true));
            } else {
                out = new PrintWriter(filepath);
                out.append("Time;ioTaskID;delay;srcID;dstID;dataSize;type;object");
                out.append("\n");
            }
        }
        catch (Exception e){
            System.out.println("Failed logRequestPathInQueues");
            System.exit(1);
        }
        DecimalFormat df6 = new DecimalFormat();
        df6.setMaximumFractionDigits(6);
        DecimalFormat df3 = new DecimalFormat();
        df3.setMaximumFractionDigits(3);
        out.append(df6.format(CloudSim.clock()) + SimSettings.DELIMITER + Integer.toString(task.getIoTaskID()) + SimSettings.DELIMITER +
                df6.format(delay)   + SimSettings.DELIMITER + Integer.toString(srcID) + SimSettings.DELIMITER +
                Integer.toString(dstID)+ SimSettings.DELIMITER + Long.toString(dataSize) + SimSettings.DELIMITER + operationType
                + SimSettings.DELIMITER + task.getObjectRead());
        out.append("\n");

        out.close();
    }

    //Logs queue in all hosts in each interval
    public void logNodeQueue() throws FileNotFoundException {
        String savestr = SimLogger.getInstance().getOutputFolder()+ "/" + SimLogger.getInstance().getFilePrefix() + "_NODE_QUEUE.log";
        File f = new File(savestr);

        PrintWriter out = null;
        if ( f.exists() && !f.isDirectory() ) {
            out = new PrintWriter(new FileOutputStream(new File(savestr), true));
        }
        else {
            out = new PrintWriter(savestr);
            out.append("Time;HostID;Host Requests;Host Delay;MAN Requests;MAN Delay");
            out.append("\n");
        }
        DecimalFormat df6 = new DecimalFormat();
        df6.setMaximumFractionDigits(6);
        DecimalFormat df1 = new DecimalFormat();
        df1.setMaximumFractionDigits(1);
        for (int i=0;i<SimSettings.getInstance().getNumOfEdgeHosts();i++){
            out.append(df1.format(CloudSim.clock()) + SimSettings.DELIMITER + Integer.toString(i)
                    + SimSettings.DELIMITER + Integer.toString(wlanHostClients[i])+ SimSettings.DELIMITER +
                    df6.format(wlanLatestDelay[i])+SimSettings.DELIMITER+Integer.toString(manHostClients[i])+
                    SimSettings.DELIMITER+df6.format(manLatestDelay[i]));
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
        DecimalFormat df1 = new DecimalFormat();
        df1.setMaximumFractionDigits(1);
        for (int i=0;i<SimSettings.getInstance().getNumOfEdgeHosts();i++){
            out.append(df1.format(CloudSim.clock()) + SimSettings.DELIMITER + Integer.toString(i)
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
                logNodeQueue();
//                logManQueue();
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
        
        wlanQueue.updateMM1QueueModel();
        manQueue.updateMM1QueueModel();
        
        
//        System.out.println("Time: " + CloudSim.clock() + ", Tasks: " + numOfManTaskForDownload);
        //Update for this interval for numOfManTaskForDownload tasks
        if(numOfManTaskForDownload != 0){
            ManPoissonMeanForDownload = lastInterval / (numOfManTaskForDownload / (double)numberOfMobileDevices);
            avgManTaskOutputSize = totalManTaskOutputSize / numOfManTaskForDownload;

            for(int host=0; host<numOfEdgeDatacenters; host++) {
                hostManPoissonMeanForDownload[host] = lastInterval;
                if (hostNumOfManTaskForDownload[host]==0 || hostTotalManTaskOutputSize[host]==0)
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

        totalManTaskOutputSize = 0;
        numOfManTaskForDownload = 0;
        totalManTaskInputSize = 0;
        numOfManTaskForUpload = 0;

        //TODO: use a function for Man and wlan
        if(numOfWlanTaskForDownload != 0){
            wlanPoissonMeanForDownload = lastInterval / (numOfWlanTaskForDownload / (double)numberOfMobileDevices);
            avgWlanTaskOutputSize = totalWlanTaskOutputSize / numOfWlanTaskForDownload;

            for(int host=0; host<numOfEdgeDatacenters; host++) {
                if (hostNumOfWlanTaskForDownload[host]==0)
                    hostAvgWlanTaskOutputSize[host]=avgWlanTaskOutputSize;
                else
                    hostAvgWlanTaskOutputSize[host] = hostTotalWlanTaskOutputSize[host] / hostNumOfWlanTaskForDownload[host];

                previousHostTotalWlanTaskOutputSize[host] = hostTotalWlanTaskOutputSize[host];
                previousHostNumOfWlanTaskForDownload[host] = hostNumOfWlanTaskForDownload[host];
                previousWlanHostClients[host] = wlanHostClients[host];

                hostTotalWlanTaskOutputSize[host] = 0;
                hostNumOfWlanTaskForDownload[host] = 0;
                wlanHostClients[host] = 0;


            }
//			System.out.println("numOfWlanTaskForDownload: " + numOfWlanTaskForDownload + " avgWlanTaskOutputSize: "+ avgWlanTaskOutputSize); //TO remove
        }

        totalWlanTaskOutputSize = 0;
        numOfWlanTaskForDownload = 0;
        totalWlanTaskInputSize = 0;
        numOfWlanTaskForUpload = 0;
    }

    double procMbit2NumOfTasks(double taskProcCapacity){
        //byte to Mbit
        double objectSizeMbit = (SimSettings.getInstance().getTaskLookUpTable()[0][LoadGeneratorModel.DATA_DOWNLOAD] * 8) / (1000*1000) ;
        return taskProcCapacity / objectSizeMbit;
    }



    //Use M/M/1 queue for each node on the grid
    double getManDownloadDelay(int hostIndex, int readOnGrid) {
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        if (hostIndex>=numOfEdgeDatacenters)
            System.out.println("ERROR: Illegal host id " + hostIndex);
        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
        if (vmArray.size()>1)
            System.out.println("More than 1 VM");
        double intervalsInSec=SampleMobileDeviceManager.getMm1QueueModelUpdateInterval();
        //Assuming same service time
        //tasks per sec
        double mu = procMbit2NumOfTasks(intervalsInSec*vmArray.get(0).getTaskProcessingMbps());

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
        if (manHostClients[hostIndex] >mu) {
            manHostClients[hostIndex]--;
            hostTotalManTaskOutputSize[hostIndex] -= hostAvgManTaskOutputSize[hostIndex];
            return -1;
        }

        double result = calculateMM1(2*readOnGrid * SimSettings.getInstance().getInternalLanDelay(),
                mu,previousManHostClients[hostIndex] );
        manLatestDelay[hostIndex]=result;
        totalManTaskOutputSize += avgManTaskOutputSize;
        numOfManTaskForDownload++;


        if (result < 0){
            manHostClients[hostIndex]--;
            hostTotalManTaskOutputSize[hostIndex] -= hostAvgManTaskOutputSize[hostIndex];
        }

        return result;
    }

    @Override
    public void downloadStarted(Location accessPointLocation, int sourceDeviceId) {
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID)
            wanClients[accessPointLocation.getServingWlanId()]++;
        else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID)
            wlanClients[accessPointLocation.getServingWlanId()]++;
        else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID+1)
            manClients++;
        else {
            SimLogger.printLine("Error - unknown device id in downloadStarted(). Terminating simulation...");
            System.exit(0);
        }
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

    /** Get queue size in input to remote edge node
     * No need to check MAN delay because it's on node output -> identical to all sent requests
     * Calculate by current queue size (and not previous) to get more updated result
     */
    public double getEdge2EdgeDelay(int srcNode, int dstNode){
        if (SimSettings.getInstance().isQueueOracle())
            return wlanQueue.getHostTotalTaskSize(dstNode);
        else
            return wlanQueue.getLatestDelay(srcNode, dstNode) + manQueue.getLatestDelay(srcNode, dstNode);
    }
    public List<Integer> getNonOperativeHosts() {
        List<Integer> nonOperativeHosts = new ArrayList<>();
        if(!SimSettings.getInstance().isHostFailureScenario())
            return nonOperativeHosts;
        for(int i=0; i<SimSettings.getInstance().getNumOfEdgeDatacenters(); i++){
            if (hostOperativity[i]==0)
                nonOperativeHosts.add(i);
        }
        return nonOperativeHosts;
    }
}

class EdgeQueue{
    protected double[] hostTotalTaskSize;
    protected int[] failedFromRemote;
    protected int[] failedFromLocal;
    protected int[] hostOperativity;

    protected double[] previousHostTotalTaskSize;
    protected double[][] latestDelay;

    protected double numOfTasks;
    double mu;

    private int numOfFails; //debug

    public EdgeQueue(double readRate) {
        initialize(readRate);
        
    }
    
    private void initialize(double readRate){
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        double intervalsInSec=SampleMobileDeviceManager.getMm1QueueModelUpdateInterval();
        previousHostTotalTaskSize = new double[numOfEdgeDatacenters];
        latestDelay = new double[numOfEdgeDatacenters][numOfEdgeDatacenters]; //src -> dst
        hostTotalTaskSize = new double[numOfEdgeDatacenters];
        failedFromRemote = new int[numOfEdgeDatacenters];
        failedFromLocal = new int[numOfEdgeDatacenters];

        mu = readRate*intervalsInSec;

        for(int host=0; host<numOfEdgeDatacenters; host++) {
            hostTotalTaskSize[host] = 0;
        }
        numOfTasks = 0;
        numOfFails = 0; //debug
    }

    public double getHostTotalTaskSize(int hostID) {
        return hostTotalTaskSize[hostID];
    }
    public double getLatestDelay(int srdNode, int dstNode) {
        return latestDelay[srdNode][dstNode];
    }

    public double getMu() {
        return mu;
    }

    double getQueueDownloadDelay(int objectLocation, double dataSize, int readOnGrid, int userLocation) {
        DecimalFormat df5 = new DecimalFormat();
        df5.setMaximumFractionDigits(5);
        double propagationDelay = readOnGrid * SimSettings.getInstance().getInternalLanDelay();
        dataSize /= SimSettings.getInstance().getObjectSize();
        hostTotalTaskSize[objectLocation] += dataSize;
//        double dataSizeTasks = hostTotalTaskSize[hostIndex] / SimSettings.getInstance().getObjectSize();
        //check for overflow on the fly
        if (hostTotalTaskSize[objectLocation] > mu ) {
            numOfFails++;
            setLatestDelay(userLocation, objectLocation, 0, true);
            hostTotalTaskSize[objectLocation] -= dataSize;
            if (objectLocation != userLocation)
                failedFromRemote[objectLocation]++;
            else
                failedFromLocal[objectLocation]++;
            return -1;
        }
        double averageHostTotalTaskSize = (hostTotalTaskSize[objectLocation] + previousHostTotalTaskSize[objectLocation]) / 2;
        double result = calculateMM1(propagationDelay, mu, averageHostTotalTaskSize);
        numOfTasks++;

        setLatestDelay(userLocation, objectLocation, result, false);
        if (result > SimSettings.getInstance().getQueueTimeout()) {
            numOfFails++;
            hostTotalTaskSize[objectLocation] -= dataSize;
            if (objectLocation != userLocation)
                failedFromRemote[objectLocation]++;
            else
                failedFromLocal[objectLocation]++;
            result = -1;
        }
        return result;
    }

    private void setLatestDelay(int srcNode, int dstNode, double result, boolean isQueueFull){
        double backoffValue = 100;
        if (isQueueFull)
            latestDelay[srcNode][dstNode] = backoffValue;
        else {
            if (latestDelay[srcNode][dstNode] == backoffValue) //set legal value
                latestDelay[srcNode][dstNode] = SimSettings.getInstance().getQueueTimeout();
            latestDelay[srcNode][dstNode] = (latestDelay[srcNode][dstNode] + result) / 2;
        }
    }

    //scaled version of little's law - use num of objects for queue (scale by object size)
    public static double calculateMM1(double propagationDelay, double mu , double lambda){
        //Oleg: Little's law: total time a customer spends in the system
        double result = 1 / (mu-lambda);
        result += propagationDelay;
        return result;
    }

    public void updateMM1QueueModel() {
        //assuming same service rate for all queues
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        if (numOfTasks != 0) {
            for (int host = 0; host < numOfEdgeDatacenters; host++) {
                previousHostTotalTaskSize[host] = hostTotalTaskSize[host];
                hostTotalTaskSize[host] = 0;
            }
        }
        numOfTasks = 0;
/*        if(CloudSim.clock()>=60 && CloudSim.clock()<61){
            System.out.println("Failed from remote " + Arrays.stream(failedFromRemote).sum() +
                    ", failed from local " + Arrays.stream(failedFromLocal).sum());
        }*/
//        Arrays.fill(failedFromRemote, 0);
//        Arrays.fill(failedFromLocal, 0);
    }

}
