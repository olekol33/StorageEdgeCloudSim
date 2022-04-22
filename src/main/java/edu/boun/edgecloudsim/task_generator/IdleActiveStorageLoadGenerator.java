package edu.boun.edgecloudsim.task_generator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.storage.MMPP;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.storage.StorageRequest;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.cloudbus.cloudsim.core.CloudSim;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//changed by Harel

public class IdleActiveStorageLoadGenerator extends LoadGeneratorModel{
    int taskTypeOfDevices[];
    static private int numOfIOTasks;
    static private int numOfAIOTasks,numOfBIOTasks; //NSF
    String orchestratorPolicy;
    String objectPlacementPolicy;


    RandomGenerator rand = new Well19937c(ObjectGenerator.seed);
    Map<Integer,Integer> activeCodedIOTasks;
    double[] lambda;
    double singleLambda;
    boolean[] parityReadStarted;
    boolean[] parityReadFinished;
    int[][] MMPPObjectDemand;
    int[][] zipfPermutations;
    int numOfZipfPermutations;


    Random parityRandom;
    Random failRandom;
//    Random DynamicZipfRandom;
    int intervalSize, numOfIntervals;
    int nZIPF;
    public IdleActiveStorageLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario, String _orchestratorPolicy,
                                          String _objectPlacementPolicy) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
        orchestratorPolicy = _orchestratorPolicy;
        objectPlacementPolicy = _objectPlacementPolicy;


        activeCodedIOTasks = new HashMap<>();
        numOfIOTasks=0;
        numOfAIOTasks = 0;
        numOfBIOTasks = 0;
        nZIPF=2;
        if(SimSettings.getInstance().isNsfExperiment())
            lambda = new double[2];
        else //permutation controlled by input seed
            createZipfPermutations(SimSettings.getInstance().getServiceRateIterations());
    }

    @Override
    public void initializeModel() {
        if(SimSettings.getInstance().isExternalRequests()){
            initializeModelWithRequestsFromInput();
            return;
        }
        int ioTaskID=0;
//        double sumPoisson = 0;
        double dataSizeMean = 0;
        Random random = new Random();
        random.setSeed(ObjectGenerator.getSeed());
        parityRandom = new Random();
        failRandom = new Random();
        failRandom.setSeed(ObjectGenerator.getSeed());
        parityRandom.setSeed(ObjectGenerator.getSeed());
        taskList = new ArrayList<TaskProperty>();

        ObjectGenerator OG = new ObjectGenerator(objectPlacementPolicy);

        //Count number of requests for each object after warm up period
        double[] objectRequests = new double[SimSettings.getInstance().getNumOfDataObjects()];
        Arrays.fill(objectRequests, 0);

        //exponential number generator for file input size, file output size and task length
        ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][ACTIVE_PERIOD];

        //create random number generator for each place
        for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
            if(SimSettings.getInstance().getTaskLookUpTable()[i][USAGE_PERCENTAGE] ==0)
                continue;
            expRngList[i][LIST_DATA_DOWNLOAD] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD]);
            dataSizeMean+=SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD];
        }
        dataSizeMean /= SimSettings.getInstance().getTaskLookUpTable().length;

        //Each mobile device utilizes an app type (task type)
        taskTypeOfDevices = new int[numberOfMobileDevices];

        OG.resetNewObjectRandomGenerator(SimSettings.getInstance().getCurrentServiceRateIteration());
        //Calculate lambdas for NSF experiment

        for(int i=0; i<numberOfMobileDevices; i++) {
            int randomTaskType = -1;


            if(SimSettings.getInstance().isNsfExperiment()) {
                randomTaskType = i%2;
            }
            else
                randomTaskType = random.nextInt(SimSettings.getInstance().getTaskLookUpTable().length);
            taskTypeOfDevices[i] = randomTaskType;

            double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][POISSON_INTERARRIVAL];
            double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][ACTIVE_PERIOD];
            double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][IDLE_PERIOD];
            //random start time with seed
            double activePeriodStartTime = SimSettings.CLIENT_ACTIVITY_START_TIME + (activePeriod) * random.nextDouble();
            double virtualTime = activePeriodStartTime;

//            sumPoisson += poissonMean;

            //Oleg: random with seed
            ExponentialDistribution rng = new ExponentialDistribution(rand,
                    poissonMean, ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
//            int requests = 0;
            while(virtualTime < simulationTime) {
                double interval;
                interval = rng.sample();
                if(interval <= 0){
                    SimLogger.printLine("Impossible has occurred! interval is " + interval + " for device " + i + " time " + virtualTime);
                    continue;
                }
                //SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
                virtualTime += interval;
                //if activePeriod has passed, wait (add idlePeriod)
                if(virtualTime > activePeriodStartTime + activePeriod){
                    activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                    virtualTime = activePeriodStartTime;
                    continue;
                }
//                requests++;
                String objectID="";
//                String objectID = OG.getObjectID(SimSettings.getInstance().getNumOfDataObjects(),"objects");
                if(SimSettings.getInstance().isNsfExperiment()) {
                    //odd/even tasks will read only odd/even objects
                    while (1==1){
                        objectID = OG.getDataObjectID();
                        int objectNum = Integer.valueOf(objectID.replaceAll("[^\\d.]", ""));
                        if(objectNum%2 == randomTaskType)
                            break;
                    }
                }

                if(!SimSettings.getInstance().isNsfExperiment()) {
                    String dist = SimSettings.getInstance().getObjectDistRead();
                    int currentIteration = SimSettings.getInstance().getCurrentServiceRateIteration();
                    int objectNum = zipfPermutations[currentIteration][OG.getObjectID(SimSettings.getInstance().getNumOfDataObjects(), "objects", dist)];
                    objectID = "d" + String.valueOf(objectNum);

                    //count
                    if(virtualTime>=SimSettings.getInstance().getWarmUpPeriod()){
                        objectRequests[objectNum]++;
                    }
                }
                taskList.add(new TaskProperty(i,randomTaskType, virtualTime, objectID, ioTaskID, 0,expRngList));
                ioTaskID++;
            }
//            System.out.println("Device: " + i + " prdocued request: " + requests);
        }
        numOfIOTasks = ioTaskID;
        //sort since each user has a different start time
        taskList.sort(Comparator.comparing(TaskProperty::getStartTime));



        ////Analyze request rate on the fly
        //normalize to mu
        int servedReqsPerSec = SimSettings.getInstance().getServedReqsPerSec();
        double measuredDuration = SimSettings.getInstance().getSimulationTime() - SimSettings.getInstance().getWarmUpPeriod();
        int totalReqsPerSec = 0;
        for(int a = 0; a < objectRequests.length; a++)
        {
            totalReqsPerSec += objectRequests[a];
            objectRequests[a] = objectRequests[a]/(servedReqsPerSec*measuredDuration);
        }
        totalReqsPerSec /= measuredDuration;
        double objectRequestsSum = Arrays.stream(objectRequests).sum();
        double objectRequestsAvg = objectRequestsSum / objectRequests.length;
        double dataObjectsInNodes = (double)SimSettings.getInstance().getNumOfDataObjects() / SimSettings.getInstance().getNumOfEdgeDatacenters();
        System.out.println("Avg object request rate: " +String.valueOf(objectRequestsAvg) + "  mu for " +
                String.valueOf(dataObjectsInNodes) + " objects in node");
        double objectRequestsMax = Arrays.stream(objectRequests).max().getAsDouble();
        System.out.println("Max object request rate: " +String.valueOf(objectRequestsMax) + "  mu");
        SimSettings.getInstance().setObjectRequestRateArray(objectRequests);

        checkModeAfterInit(dataSizeMean, OG.getOverhead());
    }

    //TODO: created by Harel
    public void initializeModelWithRequestsFromInput() {
        //int ioTaskID=0;
        //double sumPoisson = 0;
        double dataSizeMean = 0;
        Random random = new Random();
        random.setSeed(ObjectGenerator.getSeed());
        //parityRandom = new Random();
//        DynamicZipfRandom = new Random();
        //failRandom = new Random();
        //failRandom.setSeed(ObjectGenerator.getSeed());
        //parityRandom.setSeed(ObjectGenerator.getSeed());
//        DynamicZipfRandom.setSeed(ObjectGenerator.getSeed());
        taskList = new ArrayList<TaskProperty>();
        ObjectGenerator OG = new ObjectGenerator(objectPlacementPolicy);
        int numOfExternalTasks = SimSettings.getInstance().getNumOfExternalTasks();

        //exponential number generator for file input size, file output size and task length
        //TODO: copy
        ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][ACTIVE_PERIOD];

        //create random number generator for each place
        for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
            if(SimSettings.getInstance().getTaskLookUpTable()[i][USAGE_PERCENTAGE] ==0)
                continue;
            expRngList[i][LIST_DATA_UPLOAD] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_UPLOAD]);
            expRngList[i][LIST_DATA_DOWNLOAD] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD]);
//            expRngList[i][LIST_TASK_LENGTH] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][TASK_LENGTH]);
            dataSizeMean+=SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD];
        }
        dataSizeMean /= SimSettings.getInstance().getTaskLookUpTable().length;

        //Each mobile device utilizes an app type (task type)
        //taskTypeOfDevices = new int[numberOfMobileDevices];
        //Calculate lambdas for NSF experiment

        for(int i = 0; i < numOfExternalTasks; i++) {
            StorageRequest sRequest = SimSettings.getInstance().getStorageRequests().elementAt(i);
            int device_ID = SimSettings.getInstance().getReversedHashDevicesVector().get(sRequest.getDeviceName());
            //taskList.add(new TaskProperty(i, 0, sRequest.getTime(), sRequest.getObjectID(), sRequest.getIoTaskID(), 0, expRngList)); //TODO: this line is important
            taskList.add(new TaskProperty(device_ID, 0, sRequest.getTime(), sRequest.getObjectID(), sRequest.getIoTaskID(), sRequest.getTaskPriority(), sRequest.getTaskDeadline(), 0)); //TODO: this line is important

        }

        //numOfIOTasks = ioTaskID;
        numOfIOTasks = SimSettings.getInstance().getNumOfExternalTasks();//TODO: check if make sense with oleg!!

        checkModeAfterInit(dataSizeMean, OG.getOverhead());
    }

    // Remaining part of init
    private void checkModeAfterInit(double dataSizeMean, double overhead){
        if(SimSettings.getInstance().isNsfExperiment()) {
            for(int i=0; i<2; i++) {
                int randomTaskType = i % 2;
                Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
                NodeList datacenterList = doc.getElementsByTagName("datacenter");
                Node datacenterNode = datacenterList.item(randomTaskType);
                Element datacenterElement = (Element) datacenterNode;
                NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
                Node hostNode = hostNodeList.item(0);
                Element hostElement = (Element) hostNode;
                NodeList vmNodeList = hostElement.getElementsByTagName("VM");
                Node vmNode = vmNodeList.item(0);
                Element vmElement = (Element) vmNode;

//                readRate = (readRate*(int)(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD]))/1000; //reqs to MB
                int bandwidth = Integer.parseInt(vmElement.getElementsByTagName("readRate").item(0).getTextContent());

                double mu = bandwidth * 1000 / SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][DATA_DOWNLOAD];
                //numberOfMobileDevices/2  times lambda is rate
                // 1/2 of devices are from group a and 1/2 are b
                double rate = numberOfMobileDevices / (SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][POISSON_INTERARRIVAL] * 2);
                double active = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][ACTIVE_PERIOD];
                double idle = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][IDLE_PERIOD];
                rate = rate * (active/(active+idle));
/*                double rate;
                if (i==0)
                    rate = numOfAIOTasks;
                else
                    rate = numOfBIOTasks;
                rate = rate/SimSettings.getInstance().getSimulationTime(); //Tasks per second*/
                lambda[randomTaskType] = rate / mu;
                System.out.println("\nLambda " + i + ": " + lambda[randomTaskType] + " mu");
                throw new IllegalStateException("Oleg:Fix readRate setting");
            }

//        }
//
//        if(SimSettings.getInstance().isNsfExperiment()) {
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(4);
            SimLogger.getInstance().setFilePrefix("SIMRESULT_" + df.format(lambda[0]) + "_" + df.format(lambda[1]) + "_" +
                    simScenario + "_" + orchestratorPolicy +
                    "_" + objectPlacementPolicy + "_" + numberOfMobileDevices + "DEVICES");
        }

        if(SimSettings.getInstance().isOrbitMode()) {
            try {
                exportTaskList();
                SimLogger.printLine("Task list generated");
                System.exit(0);
            } catch (Exception e) {
                SimLogger.printLine("Failed to generate task list");
                System.exit(0);
            }
        }

        if(SimSettings.getInstance().isParamScanMode()){
//        if(1==1){
            double muTotal = 0;
            Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
            NodeList datacenterList = doc.getElementsByTagName("datacenter");
            for (int i = 0; i < datacenterList.getLength(); i++) {
                Node datacenterNode = datacenterList.item(i);
                Element datacenterElement = (Element) datacenterNode;
                NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
                Node hostNode = hostNodeList.item(0);
                Element hostElement = (Element) hostNode;
                NodeList vmNodeList = hostElement.getElementsByTagName("VM");
                Node vmNode = vmNodeList.item(0);
                Element vmElement = (Element) vmNode;
                muTotal += Integer.parseInt(vmElement.getElementsByTagName("readRate").item(0).getTextContent());
                throw new IllegalStateException("Oleg:Fix readRate setting");
            }

//            double muMean = muTotal/datacenterList.getLength();
            muTotal = (muTotal*1000) / dataSizeMean; //Per second
            //Calculation from poisson
//            double meanRate = numberOfMobileDevices * (1/poissonMean);
            //Calculation from actual number of tasks
            double meanRate = numOfIOTasks/SimSettings.getInstance().getSimulationTime(); //Tasks per second
            singleLambda = meanRate / muTotal;
            //double overhead = OG.getOverhead();
            String dist = "";
            if(SimSettings.getInstance().isMMPP())
                dist = "MMPP";
            else
                dist = SimSettings.getInstance().getObjectDistRead();
            String fail = "";
            if (SimSettings.getInstance().isHostFailureScenario())
                fail="WITHFAIL";
            else
                fail="NOFAIL";

            DecimalFormat df = new DecimalFormat();
            DecimalFormat df2 = new DecimalFormat();
            df.setMaximumFractionDigits(4);
            df2.setMaximumFractionDigits(2);
            if (SimSettings.getInstance().isOverheadScan()) {
                System.out.println("\nLambda: " + df.format(singleLambda) + " mu, Overhead: " + df2.format(overhead));
                SimLogger.getInstance().setFilePrefix("SIMRESULT_" + df.format(singleLambda) + "_OH" + df2.format(overhead) + "_" +
                        "SEED" + SimSettings.getInstance().getRandomSeed() + "_" + simScenario + "_" + orchestratorPolicy +
                        "_" + objectPlacementPolicy + "_" + dist + "_" + fail + "_" + numberOfMobileDevices + "DEVICES");
            }
            else
            {
                System.out.println("\nLambda: " + df.format(singleLambda));
                SimLogger.getInstance().setFilePrefix("SIMRESULT_" + df.format(singleLambda) + "_" +
                        simScenario + "_" + orchestratorPolicy +
                        "_" + objectPlacementPolicy + "_" + dist + "_" + fail + "_" + numberOfMobileDevices + "DEVICES");
            }


        }
        parityReadStarted = new boolean[numOfIOTasks];
        parityReadFinished = new boolean[numOfIOTasks];
    }

    public void exportTaskList() throws IOException {
        String tmpFolder = "";
        if(SystemUtils.IS_OS_WINDOWS)
            tmpFolder = SimSettings.getInstance().getOutputFolder();
        else
            tmpFolder = "/tmp/";
        File taskListFile = null;
        FileWriter taskListFW = null;
        BufferedWriter taskListBW = null;
        int currentDevice = SimSettings.getInstance().getThisMobileDevice();
        taskListFile = new File(tmpFolder,"TASK_LIST"+Integer.toString(currentDevice)+".txt");
        taskListFW = new FileWriter(taskListFile, false);
        taskListBW = new BufferedWriter(taskListFW);
        taskListBW.write("startTime,outputFileSize,mobileDeviceId,objectRead,ioTaskID");
        taskListBW.newLine();
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(9);
        for(TaskProperty task:getTaskList()){
            if(task.getMobileDeviceId()==currentDevice) {
                taskListBW.write(df.format(task.getStartTime()) + "," + task.getOutputFileSize() +
                        "," + task.getMobileDeviceId() + "," + task.getObjectRead() + "," + task.getIoTaskID());
                taskListBW.newLine();
            }
        }
        taskListBW.close();
        System.out.println("Task list generated for client "+ Integer.toString(currentDevice));
        if(SimSettings.getInstance().isServiceRateScan()){
            File serviceRateFile = new File(tmpFolder, "service_rate.txt");
            FileWriter serviceRateFileFW = new FileWriter(serviceRateFile, false);
            BufferedWriter serviceRateFileBW = new BufferedWriter(serviceRateFileFW);
            String policy = "";
            if(objectPlacementPolicy.equals("CODING_PLACE"))
                policy="coding";
            else
                policy="replication";
            double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[0][POISSON_INTERARRIVAL];
            poissonMean = 1/ poissonMean;
            SimLogger.appendToFile(serviceRateFileBW, SimSettings.getInstance().getObjectRequestRateArray() + "," + policy + "," +
                    String.valueOf((int)poissonMean) + "," + SimSettings.getInstance().getServedReqsPerSec());
            System.out.println("Service rate output generated for client "+ Integer.toString(currentDevice));
            serviceRateFileBW.close();
        }
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        return taskTypeOfDevices[deviceId];
    }
/*
    //TODO: changed by Harel
    public boolean createParityTask(Task task){
        int taskType = task.getTaskType();
        int isParity=1;
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        List<String> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
        boolean replication=false;
        boolean dataParity=false;
        if (SimManager.getInstance().getObjectPlacementPolicy().equalsIgnoreCase("REPLICATION_PLACE"))
            replication=true;
        else if (SimManager.getInstance().getObjectPlacementPolicy().equalsIgnoreCase("DATA_PARITY_PLACE"))
            dataParity=true;
        //If replication policy, read the same object, but mark it as parity
        //if DATA_PARITY_PLACE - if replica exists, read it
        if (replication || dataParity) {
            String locations = RedisListHandler.getObjectLocations(task.getObjectRead());
            List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
            //if some hosts are unavailable
            if (nonOperateHosts.size()>0){
                //remove non active locations
                for (String host : nonOperateHosts)
                    objectLocations.remove(host);
                //if replication and no available objects - false
                if(objectLocations.size()==0 && replication)
                    return false;
            }
            //if no replicas and REPLICATION_PLACE
            if (objectLocations.size()==1 && replication)
                return false;

            //if object not found

            //if there are replicas of the object
            //If dataParity, check that replica queue is less than threshold
            //TODO: add my line here
            else if(objectLocations.size()>= 2 && ((checkReplicaQueue(task.getObjectRead()) && dataParity)||replication)) {
                if(!SimSettings.getInstance().isExternalRequests()) {
                    taskList.add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                            task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength(),
                            task.getHostID()));
                }else{
                    taskList.add(new TaskProperty(task.getMobileDeviceId(), 0, task.getStart_time(), task.getObjectRead(), task.getIoTaskID(), task.getTaskPriority(), task.getTaskDeadline(), 0)); //TODO: this line is important
                }
                SimManager.getInstance().createNewTask();
                return true;
            }
        }
        List<String> mdObjects = RedisListHandler.getObjectsFromRedis("object:md*_"+task.getObjectRead()+"_*");
        //no parities
        if (mdObjects.size()==0)
            return false;
//        String stripeID = task.getStripeID();
        //TODO: currently selects random stripe
        int mdObjectIndex;
        String stripeID;
        String[] stripeObjects;
        List<String> dataObjects = null;
        List<String> parityObjects = null;
        if (nonOperateHosts.size()==0){ //no failed hosts
            mdObjectIndex = parityRandom.nextInt(mdObjects.size());
            stripeID = RedisListHandler.getObjectID(mdObjects.get(mdObjectIndex));
            stripeObjects = RedisListHandler.getStripeObjects(stripeID);
            dataObjects = new ArrayList<String>(Arrays.asList(stripeObjects[0].split(" ")));
            parityObjects = new ArrayList<String>(Arrays.asList(stripeObjects[1].split(" ")));
        }
        else {
            boolean stripeFound=false;
            while (mdObjects.size() > 0) { //Check that stripe objects are available
                mdObjectIndex = parityRandom.nextInt(mdObjects.size());
                stripeID = RedisListHandler.getObjectID(mdObjects.get(mdObjectIndex));
                stripeObjects = RedisListHandler.getStripeObjects(stripeID);
                dataObjects = new ArrayList<String>(Arrays.asList(stripeObjects[0].split(" ")));
                parityObjects = new ArrayList<String>(Arrays.asList(stripeObjects[1].split(" ")));
                List<String> stripeParities = Stream.concat(dataObjects.stream(), parityObjects.stream())
                        .collect(Collectors.toList());
                stripeParities.remove(task.getObjectRead());
                if (checkStripeValidity(stripeParities)) {
                    stripeFound=true;
                    break;
                }
                else
                    mdObjects.remove(mdObjectIndex);
            }
            if (!stripeFound)
                return false;
        }
        int i=0;
        int paritiesToRead=1;

        for (String objectID:dataObjects){
            i++;
            //if data object, skip
            if (objectID.equals(task.getObjectRead()))
                continue;
            //if not data, than data of parity
            //TODO: add delay for queue query
            //TODO: add my line here
            if(!SimSettings.getInstance().isExternalRequests()) {
                taskList.add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(),
                        objectID, task.getIoTaskID(), isParity, 0, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength()));
            }else{
                taskList.add(new TaskProperty(task.getMobileDeviceId(), 0, task.getStart_time(), task.getObjectRead(), task.getIoTaskID(), task.getTaskPriority(), task.getTaskDeadline(), 0)); //TODO: this line is important
            }
            SimManager.getInstance().createNewTask();
        }
        for (String objectID:parityObjects){
            i++;
            if(!SimSettings.getInstance().isExternalRequests()) {
                taskList.add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(),
                        objectID, task.getIoTaskID(), isParity, paritiesToRead, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength()));
            }else{
                taskList.add(new TaskProperty(task.getMobileDeviceId(), 0, task.getStart_time(), task.getObjectRead(), task.getIoTaskID(), task.getTaskPriority(), task.getTaskDeadline(), 0)); //TODO: this line is important

            }
            SimManager.getInstance().createNewTask();
            //count just one read for queue
            paritiesToRead=0;
        }
        if (i!=(SimSettings.getInstance().getNumOfDataInStripe()+SimSettings.getInstance().getNumOfParityInStripe()))
            System.out.println("Not created tasks for all parities");
        activeCodedIOTasks.put(task.getIoTaskID(),i-1);
        return true;
    }
*/
    public static int getNumOfIOTasks() {
        return numOfIOTasks;
    }

    //check if one list is subset of another
    public boolean contains(List<?> list, List<?> sublist) {
        return Collections.indexOfSubList(list, sublist) != -1;
    }

    //counts parities which haven't been read yet
    public int updateActiveCodedIOTasks(int ioTaskID, int operation){ //operation: 1 - reduce by 1, 0 - check, -1-remove key
        //if key doesn't exist
        if (!activeCodedIOTasks.containsKey(ioTaskID))
            return -1;
        if (operation==1) {
            activeCodedIOTasks.put(ioTaskID, activeCodedIOTasks.get(ioTaskID) - 1);
            return activeCodedIOTasks.get(ioTaskID);
        }
        if (operation==0){
            return activeCodedIOTasks.get(ioTaskID);
        }
        else if(operation==-1){
            activeCodedIOTasks.remove(ioTaskID);
            return -1;
        }
        return -2;
    }


    //Output demand vector for each object
    private void logMMPPDemand() throws FileNotFoundException {
        int numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
        for(int i=0; i<numOfDataObjects;i++){
            String file = SimLogger.getInstance().getOutputFolder()+ "/MMPP";
            File f = new File(file);
            f.mkdir();
            file = SimLogger.getInstance().getOutputFolder()+ "/MMPP/" + SimLogger.getInstance().getFilePrefix() +
                    "_D" + i + "_DEMAND.log";
            f = new File(file);
            PrintWriter out = null;
            out = new PrintWriter(file);
            out.append("Interval;Requests");
            out.append("\n");

            for (int j=0;j<numOfIntervals;j++){
                out.append(j + SimSettings.DELIMITER + MMPPObjectDemand[i][j]);
                out.append("\n");
            }
            out.close();
        }
    }

    //Creates n permutations of size numOfDataObjects
    private void createZipfPermutations(int n){
        if(n==0)
            n++;
        Random DynamicZipfRandom = new Random();
        DynamicZipfRandom.setSeed(SimSettings.getInstance().getRandomSeed());
        int numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
        zipfPermutations = new int[n][numOfDataObjects];

        for (int i=0;i<n;i++){
            List<Integer> numbers = new ArrayList<>();
            //List of size numOfDataObjects
            for (int j=0;j<numOfDataObjects;j++)
                numbers.add(j);
            //Shuffle list for permutations
            Collections.shuffle(numbers, DynamicZipfRandom);
            int index = 0;
            for( Integer in : numbers )
                zipfPermutations[i][index++] = in;
        }
    }

    public int[] dynamicFailureGenerator(int[] hostOperativity){
        int flipProb = 1;
        int[] hostOperativityNew = hostOperativity.clone();
        List<Integer> intArray = new ArrayList<>(hostOperativity.length);
        for (int i=0;i<hostOperativityNew.length;i++){
            intArray.add(i);
        }
        int hostOperativitySum;
        do {
            hostOperativitySum = IntStream.of(hostOperativityNew).sum();
            int amountOfFailedNodes = hostOperativityNew.length - hostOperativitySum;
            Collections.shuffle(intArray,failRandom);
            for (int host : intArray) {
                int rand = failRandom.nextInt(10000);
                //flip status with probability flipProb
                //odds to recover are more than odds to fail
                if (hostOperativityNew[host] == 1 && amountOfFailedNodes==0) {
                    if (rand<=10*flipProb)
                        hostOperativityNew[host] = 0;
                }
                //Less chance for another failure (for testing)
                else if (hostOperativityNew[host] == 1 && amountOfFailedNodes==1) {
                    if (rand<=1*flipProb)
                        hostOperativityNew[host] = 0;
                }
                else{ //was already failed
                    if (rand<=100*flipProb)
                        hostOperativityNew[host] = 1;
                }
            }
            hostOperativitySum = IntStream.of(hostOperativityNew).sum();
        }
        //no more than 2 failed nodes at a time
        while (hostOperativityNew.length - hostOperativitySum >2);
        for (int i=0;i<hostOperativityNew.length;i++){
            if (hostOperativity[i]!=hostOperativityNew[i]){
                if(hostOperativityNew[i]==1)
                    System.out.println("Host " + i + " recovered\n");
                else if(hostOperativityNew[i]==0)
                    System.out.println("Host " + i + " failed\n");
            }
        }
        return hostOperativityNew;
    }


    public boolean getParityReadStarted(int ioTaskID) {
        return parityReadStarted[ioTaskID];
    }


    public void setParityReadStarted(boolean value, int ioTaskID) {
        this.parityReadStarted[ioTaskID] = value;
    }

    public boolean getParityReadFinished(int ioTaskID) {
        return parityReadFinished[ioTaskID];
    }


    public void setParityReadFinished(boolean value, int ioTaskID) {
        this.parityReadFinished[ioTaskID] = value;
    }


    public int[][] getZipfPermutations() {
        return zipfPermutations;
    }

    public int getNumOfZipfPermutations() {
        return numOfZipfPermutations;
    }


    public Random getParityRandom() {
        return parityRandom;
    }

}