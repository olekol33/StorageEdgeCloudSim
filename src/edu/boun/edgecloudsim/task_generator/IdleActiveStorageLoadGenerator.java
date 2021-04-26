package edu.boun.edgecloudsim.task_generator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.storage.MMPP;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
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
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        if(SimSettings.getInstance().isMMPP()) {
            intervalSize = 1;//sec
            numOfIntervals = (int)(SimSettings.getInstance().getSimulationTime() / intervalSize);
            MMPPObjectDemand = new int[SimSettings.getInstance().getNumOfDataObjects()][numOfIntervals];
//            createMMPPDemandArray();
        }
        if(SimSettings.getInstance().getObjectDistRead().equals("MULTIZIPF")){
//            numOfZipfPermutations = numberOfMobileDevices/SimSettings.getInstance().getNumOfEdgeHosts();
            numOfZipfPermutations = SimSettings.getInstance().getNumOfEdgeHosts()/2;
//            numOfZipfPermutations = 10;
            createZipfPermutations(numOfZipfPermutations);
        }
        if(SimSettings.getInstance().getObjectDistRead().equals("MULTIZIPFN")){
            numOfZipfPermutations = nZIPF*SimSettings.getInstance().getNumOfEdgeHosts()/2;
            createZipfPermutations(numOfZipfPermutations);
        }
        if(SimSettings.getInstance().isNsfExperiment())
            lambda = new double[2];

    }

    @Override
    public void initializeModel() {
        int ioTaskID=0;
        double sumPoisson = 0;
        double dataSizeMean = 0;
        Random random = new Random();
        random.setSeed(ObjectGenerator.getSeed());
        parityRandom = new Random();
//        DynamicZipfRandom = new Random();
        failRandom = new Random();
        failRandom.setSeed(ObjectGenerator.getSeed());
        parityRandom.setSeed(ObjectGenerator.getSeed());
//        DynamicZipfRandom.setSeed(ObjectGenerator.getSeed());
        taskList = new ArrayList<TaskProperty>();
        ObjectGenerator OG = new ObjectGenerator(objectPlacementPolicy);

        //exponential number generator for file input size, file output size and task length
        //TODO: copy
        ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][ACTIVE_PERIOD];

        //create random number generator for each place
        for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
            if(SimSettings.getInstance().getTaskLookUpTable()[i][USAGE_PERCENTAGE] ==0)
                continue;
            expRngList[i][LIST_DATA_UPLOAD] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_UPLOAD]);
            expRngList[i][LIST_DATA_DOWNLOAD] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD]);
            expRngList[i][LIST_TASK_LENGTH] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][TASK_LENGTH]);
            dataSizeMean+=SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD];
        }
        dataSizeMean /= SimSettings.getInstance().getTaskLookUpTable().length;

        //Each mobile device utilizes an app type (task type)
        taskTypeOfDevices = new int[numberOfMobileDevices];
        //Calculate lambdas for NSF experiment

        for(int i=0; i<numberOfMobileDevices; i++) {
            int randomTaskType = -1;

            //TODO: currently select random task, not by weight
            if(SimSettings.getInstance().isNsfExperiment()) {
                randomTaskType = i%2;
            }
            else
                randomTaskType = random.nextInt(SimSettings.getInstance().getTaskLookUpTable().length);
            taskTypeOfDevices[i] = randomTaskType;

            double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][POISSON_INTERARRIVAL];
            double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][ACTIVE_PERIOD];
            double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][IDLE_PERIOD];
/*            double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
                    SimSettings.CLIENT_ACTIVITY_START_TIME,
                    SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)*/
            //random start time with seed
            double activePeriodStartTime = SimSettings.CLIENT_ACTIVITY_START_TIME + (activePeriod) * random.nextDouble();
            double virtualTime = activePeriodStartTime;

            sumPoisson += poissonMean;

            //Oleg: random with seed
            ExponentialDistribution rng = new ExponentialDistribution(rand,
                    poissonMean, ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
//            int requests = 0;
            while(virtualTime < simulationTime) {
                double interval;
                interval = rng.sample();
                if(interval <= 0){
                    SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
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
                //for MMPP take object with max requests in interval
                else if(SimSettings.getInstance().isMMPP()){
                    int curInterval=(int)(virtualTime/intervalSize);
                    if (virtualTime>SimSettings.getInstance().getSimulationTime())
                        continue;
                    int maxRequests=50; //Above this threshold address as burst
                    List<Integer> maxID= new ArrayList<>();
                    for(int id=0;id<SimSettings.getInstance().getNumOfDataObjects();id++){
                        if (MMPPObjectDemand[id][curInterval]>maxRequests) {
                            //if object has more requests, empty previous list
//                            maxID.removeAll(maxID);
                            maxID.add(id); //add to list of high request objects
//                            maxRequests=MMPPObjectDemand[id][curInterval];
                        }
                        //list of objects with the same popularity
/*                        else if (MMPPObjectDemand[id][curInterval]==maxRequests)
                            maxID.add(id);*/
                    }
                    //randomly select object with top popularity
                    //TODO: double use of this value (for parity as well) - this is incorrect
                    double prob = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][PROB_CLOUD_SELECTION];
                    //Load distribution - to avoid same object requested in each interval
                    //with probability prob proceed with original distribution
                    //if no bursty behavior also process with original distribution
                    if (random.nextInt(100)<=prob || maxID.size()==0)
                        objectID = OG.getDataObjectID();
                    else{
                        int rnd = random.nextInt(maxID.size());
                        objectID = OG.getDataObjectID(maxID.get(rnd));
                    }

                }
                else if (SimSettings.getInstance().getObjectDistRead().equals("MULTIZIPF")){
                    int permutationIntervalSize = (int)SimSettings.getInstance().getSimulationTime()/numOfZipfPermutations;
                    //Get object rank
                    int intObjectID = OG.getObjectIDForLoad(SimSettings.getInstance().getNumOfDataObjects(),"MULTIZIPF");
//                    int permutation = i%numOfZipfPermutations;
                    int permutation = (int)virtualTime/permutationIntervalSize;

                    if (virtualTime>SimSettings.getInstance().getSimulationTime())
                        continue;
                    //use rank as index of permutation
                    intObjectID = zipfPermutations[permutation][intObjectID];
                    objectID = OG.getDataObjectID(intObjectID);

                }
                else if (SimSettings.getInstance().getObjectDistRead().equals("MULTIZIPFN")){ //divide load by 2
                    int permutationIntervalSize = nZIPF*((int)SimSettings.getInstance().getSimulationTime()/numOfZipfPermutations);
                    //Get object rank
                    int intObjectID = OG.getObjectIDForLoad(SimSettings.getInstance().getNumOfDataObjects(),"MULTIZIPF");
                    int permutation = (int)virtualTime/permutationIntervalSize;

                    if (virtualTime>SimSettings.getInstance().getSimulationTime())
                        continue;
                    int remainder = i%nZIPF;
                    //use rank as index of permutation
                    intObjectID = zipfPermutations[nZIPF*permutation+remainder][intObjectID];
                    objectID = OG.getDataObjectID(intObjectID);

                }
                else
                    objectID = OG.getDataObjectID();

                //TODO: randomTaskType = 0
                taskList.add(new TaskProperty(i,randomTaskType, virtualTime, objectID, ioTaskID, 0,expRngList)); //TODO: this line is important
                ioTaskID++;
/*                if(randomTaskType%2==0)
                    numOfAIOTasks++;
                else
                    numOfBIOTasks++;*/
            }
//            System.out.println("Device: " + i + " prdocued request: " + requests);
        }
        numOfIOTasks = ioTaskID;

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
            }

//            double muMean = muTotal/datacenterList.getLength();
            muTotal = (muTotal*1000) / dataSizeMean; //Per second
            //Calculation from poisson
//            double meanRate = numberOfMobileDevices * (1/poissonMean);
            //Calculation from actual number of tasks
            double meanRate = numOfIOTasks/SimSettings.getInstance().getSimulationTime(); //Tasks per second
            singleLambda = meanRate / muTotal;
            double overhead = OG.getOverhead();
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
        File taskListFile = null;
        FileWriter taskListFW = null;
        BufferedWriter taskListBW = null;

//        taskListFile = new File(SimLogger.getInstance().getOutputFolder(), "TASK_LIST.txt");
        taskListFile = new File("/tmp/TASK_LIST.txt");
        taskListFW = new FileWriter(taskListFile, false);
        taskListBW = new BufferedWriter(taskListFW);
        taskListBW.write("startTime,length,inputFileSize,outputFileSize,taskType,pesNumber,mobileDeviceId,objectRead,ioTaskID");
        taskListBW.newLine();
        for(TaskProperty task:getTaskList()){
            taskListBW.write(task.getStartTime()+","+task.getLength()+","+task.getInputFileSize()+","+task.getOutputFileSize()+","+
                    task.getTaskType()+","+task.getPesNumber()+","+task.getMobileDeviceId()+","+task.getObjectRead()+","+task.getIoTaskID());
            taskListBW.newLine();
        }
        taskListBW.close();
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        // TODO Auto-generated method stub
        return taskTypeOfDevices[deviceId];
    }

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
/*            else if(objectLocations.size()==0) {
                System.out.println("ERROR: No such object");
                System.exit(0);
            }*/
            //if there are replicas of the object
            //If dataParity, check that replica queue is less than threshold
            else if(objectLocations.size()>= 2 && ((checkReplicaQueue(task.getObjectRead()) && dataParity)||replication)) {
                taskList.add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                        task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength(),
                        task.getHostID()));
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
/*        if (SimManager.getInstance().getOrchestratorPolicy().equalsIgnoreCase("IF_CONGESTED_READ_PARITY"))
            paritiesToRead=1;*/
        for (String objectID:dataObjects){
            i++;
            //if data object, skip
            if (objectID.equals(task.getObjectRead()))
                continue;
            //if not data, than data of parity
            //TODO: add delay for queue query
            taskList.add(new TaskProperty(task.getMobileDeviceId(),taskType, CloudSim.clock(),
                    objectID, task.getIoTaskID(), isParity,0,task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength()));
            SimManager.getInstance().createNewTask();
        }
        for (String objectID:parityObjects){
            i++;
            taskList.add(new TaskProperty(task.getMobileDeviceId(),taskType, CloudSim.clock(),
                objectID, task.getIoTaskID(), isParity,paritiesToRead,task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength()));
            SimManager.getInstance().createNewTask();
            //count just one read for queue
            paritiesToRead=0;
        }
        if (i!=(SimSettings.getInstance().getNumOfDataInStripe()+SimSettings.getInstance().getNumOfParityInStripe()))
            System.out.println("Not created tasks for all parities");
        activeCodedIOTasks.put(task.getIoTaskID(),i-1);
        return true;
    }

    public static int getNumOfIOTasks() {
        return numOfIOTasks;
    }

    public double[] getLambda() {
        return lambda;
    }

    //Checks if all stripe parities are available
    private boolean checkStripeValidity(List<String>  stripeObjects){
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        List<String> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();

        for (String objectID:stripeObjects){
            String locations = RedisListHandler.getObjectLocations(objectID);
            List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
            if (contains(nonOperateHosts,objectLocations))
                return false;
            //Check if there is a host below queue threshold. If not, don't use parity.
/*            boolean availableHost=false;
            for (String host : objectLocations){
                int queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(Integer.valueOf(host));
                if (queueSize < SimSettings.getInstance().getManThreshold())
                    availableHost=true;
            }
            if (availableHost==false)
                return false;*/
        }
        return true;
    }
    //Check if replicas are below threshold
    private boolean checkReplicaQueue(String objectID){
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        List<String> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();

        String locations = RedisListHandler.getObjectLocations(objectID);
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        for (String host : nonOperateHosts)
            objectLocations.remove(host);
        //Check if there is a host below queue threshold. If not, don't use parity.
        for (String host : objectLocations){
            int queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(Integer.valueOf(host));
            if (queueSize < SimSettings.getInstance().getManThreshold())
                return true;
        }
        return false;
    }

    //check if one list is subset of another
    private boolean contains(List<?> list, List<?> sublist) {
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

    //For each data object create array with number of requests in each interval
/*    private void createMMPPDemandArray(){
        int numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
        MMPP mmpp = new MMPP(random); //TODO: change random variable
        for(int i=0; i<numOfDataObjects;i++){
            int curInterval=1;
            double arrivalTime = mmpp.gen_interval();
            int arrivalsInInterval=0;
            while(curInterval< numOfIntervals){
                while(arrivalTime<(curInterval+1)* intervalSize){
                    arrivalsInInterval++;
                    arrivalTime += mmpp.gen_interval();
                }
                MMPPObjectDemand[i][curInterval] = arrivalsInInterval;
                curInterval++;
                arrivalsInInterval=0;
            }
        }
        try {
            logMMPPDemand();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }*/

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
        Random DynamicZipfRandom = new Random();
        DynamicZipfRandom.setSeed(ObjectGenerator.getSeed());
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

}