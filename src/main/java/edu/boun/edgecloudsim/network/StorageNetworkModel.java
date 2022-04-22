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
    //Download from edge (sourceDevice) to device (destDevice)
    public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
        double delay = 0;
        //special case for man communication
        // When communication is between edge nodes(on grid)
        if(destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID && sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
//            return delay = getManDownloadDelay();
            return delay = getManDownloadDelay(task.getAssociatedHostId(),1);
        }
/*        else if(sourceDeviceId == destDeviceId && sourceDeviceId != SimSettings.GENERIC_EDGE_DEVICE_ID) {
            delay = getManDownloadDelay(task.getAssociatedHostId(), 0);
            if (delay < 0) //if MAN already full, stop now (pass very negative number)
                return MAN_DELAY;
        }*/
        StaticRangeMobility staticMobility = (StaticRangeMobility)SimManager.getInstance().getMobilityModel();

        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getSubmittedLocation().getServingWlanId(), CloudSim.clock());
//        Location accessPointLocation = (StaticRangeMobility)SimManager.getInstance().getMobilityModel().getDCLocation(deviceLocation.getServingWlanId());
        Location accessPointLocation = staticMobility.getDCLocation(deviceLocation.getServingWlanId());;
//        Location accessPointLocation = StaticRangeMobility.getDCLocation(deviceLocation.getServingWlanId());
//        Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId,CloudSim.clock());


        //cloud server to mobile device
        //TODO: update for cloud
        if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID){
//            delay += getWanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
        }
        //edge device (wifi access point) to mobile device
        else{
            //factor of #accesses
            //For access node user location = destDeviceId
            //For remote read destDeviceId is object location
//            double wlanDelay = getWlanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
            int accessLocation = deviceLocation.getServingWlanId();
            if(destDeviceId == 100007) //RESPONSE_RECEIVED_BY_EDGE_DEVICE_TO_RELAY_MOBILE_DEVICE
                accessLocation = sourceDeviceId;
            double wlanDelay = getWlanDownloadDelay(accessLocation);
            delay += wlanDelay;
            //In case something went wrong
            if (wlanDelay==0)
                System.out.println("delay=0");
/*            if (wlanDelay<0)
                System.out.println("delay<0");*/
//                return delay;
            //Add delay on network if access point not in range

//            Location nearestAccessPoint = StaticRangeMobility.getAccessPoint(deviceLocation,accessPointLocation);
            Location nearestAccessPoint = staticMobility.getAccessPoint(deviceLocation,accessPointLocation);
            if (nearestAccessPoint.getServingWlanId() != accessPointLocation.getServingWlanId())
                System.out.println("nearestAccessPoint.getServingWlanId() != accessPointLocation.getServingWlanId()");
            //additional delay
            if(SimSettings.getInstance().isApplySignalAttenuation())
                delay /= StaticRangeMobility.getSignalAttenuation(deviceLocation,nearestAccessPoint,100,2);

        }

        return delay;
    }


/*    @Override
    double getWlanDownloadDelay(Location accessPointLocation, double dataSize) {
        int numOfWlanUser = wlanClients[accessPointLocation.getServingWlanId()];
        double taskSizeInKb = dataSize * (double)8; //KB to Kb
        double result=0;
//        System.out.println("previously " + wlanClients[accessPointLocation.getServingWlanId()]+ " tasks");
        if(numOfWlanUser < experimentalWlanDelay.length)
            result = taskSizeInKb *//*Kb*//* / (experimentalWlanDelay[numOfWlanUser] * (double) 3 ) *//*Kbps*//*; //802.11ac is around 3 times faster than 802.11n
        else
            result = -1;
        return result;
    }*/
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
        //calculateMM1(propogationDelay=0,bandwidth param, PoissonMean paran,avgTaskSize param,deviceCount i)
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

    double getWlanDownloadDelay(int hostIndex) {
        int numOfEdgeDatacenters = SimSettings.getInstance().getNumOfEdgeDatacenters();
        if (hostIndex>=numOfEdgeDatacenters)
            System.out.println("ERROR: Illegal host id " + hostIndex);
        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(hostIndex);
        if (vmArray.size()>1)
            System.out.println("More than 1 VM");
        double intervalsInSec=SampleMobileDeviceManager.getMm1QueueModelUpdateInterval();
        //getMm1QueueModelUpdateInterval to get relative share of 1 sec
        //need small sized Mm1QueueModelUpdateInterval to update frequently
//        double mu = procMbps2NumOfTasks(intervalsInSec*vmArray.get(0).getReadRate()*8); //tasks
        double mu = SimSettings.getInstance().getServedReqsPerSec()*intervalsInSec*SimSettings.getInstance().getSim2orbitReadrateRatio();
//        double mu = vmArray.get(0).getReadRate()*1000*SimSettings.getInstance().getMm1QueueModelUpdateInterval();//tasks

        //count for interval
        wlanHostClients[hostIndex]++;
        hostNumOfWlanTaskForDownload[hostIndex] = wlanHostClients[hostIndex];
        hostTotalWlanTaskOutputSize[hostIndex] += hostAvgWlanTaskOutputSize[hostIndex];

        //initialization
        if (previousHostAvgWlanTaskOutputSize[hostIndex]==0)
            previousHostAvgWlanTaskOutputSize[hostIndex] = hostAvgWlanTaskOutputSize[hostIndex];
        if (previousWlanHostClients[hostIndex]==0)
            previousWlanHostClients[hostIndex] = wlanHostClients[hostIndex];


        //check for overflow on the fly
        if (wlanHostClients[hostIndex] >mu ) {
            wlanHostClients[hostIndex]--;
            hostTotalWlanTaskOutputSize[hostIndex] -= hostAvgWlanTaskOutputSize[hostIndex];
            return -1;
        }
        //Use existing function. arrival rate is previousWlanHostClients, service rate is 1/taskProcessingTimeS
        double result = calculateMM1(0,
                mu,
                previousWlanHostClients[hostIndex]);
        wlanLatestDelay[hostIndex]=result;
        totalWlanTaskOutputSize += avgWlanTaskOutputSize;
        numOfWlanTaskForDownload++;


        if (result < 0){
            wlanHostClients[hostIndex]--;
            hostTotalWlanTaskOutputSize[hostIndex] -= hostAvgWlanTaskOutputSize[hostIndex];
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
