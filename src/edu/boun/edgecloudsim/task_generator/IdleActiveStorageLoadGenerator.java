package edu.boun.edgecloudsim.task_generator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdleActiveStorageLoadGenerator extends LoadGeneratorModel{
    int taskTypeOfDevices[];
    static private int numOfIOTasks=0;
    String orchestratorPolicy;
    String objectPlacementPolicy;
    Random random = new Random();
    RandomGenerator rand = new Well19937c(ObjectGenerator.seed);
    Map<Integer,Integer> activeCodedIOTasks;
    double[] lambda;
    double singleLambda;
    boolean[] parityReadStarted;
    boolean[] parityReadFinished;
    public IdleActiveStorageLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario, String _orchestratorPolicy,
                                          String _objectPlacementPolicy) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
        orchestratorPolicy = _orchestratorPolicy;
        objectPlacementPolicy = _objectPlacementPolicy;
        random.setSeed(ObjectGenerator.getSeed());
        activeCodedIOTasks = new HashMap<>();
        if(SimSettings.getInstance().isNsfExperiment())
            lambda = new double[2];

    }

    @Override
    public void initializeModel() {
        int ioTaskID=0;
        double sumPoisson = 0;
        double dataSizeMean = 0;
        taskList = new ArrayList<TaskProperty>();
        ObjectGenerator OG = new ObjectGenerator(objectPlacementPolicy);

        //exponential number generator for file input size, file output size and task length
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
                lambda[randomTaskType] = rate / mu;
                System.out.println("\nLambda " + i + ": " + lambda[randomTaskType] + " mu");
            }

        }

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
            while(virtualTime < simulationTime) {
                double interval = rng.sample();
                if(interval <= 0){
                    SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
                    continue;
                }
                //SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
                virtualTime += interval;

                if(virtualTime > activePeriodStartTime + activePeriod){
                    activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                    virtualTime = activePeriodStartTime;
                    continue;
                }
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
                else
                    objectID = OG.getDataObjectID();

                taskList.add(new TaskProperty(i,randomTaskType, virtualTime, objectID, ioTaskID, 0,expRngList));
                ioTaskID++;
            }
        }
        if(SimSettings.getInstance().isNsfExperiment()) {
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(4);
            SimLogger.getInstance().setFilePrefix("SIMRESULT_" + df.format(lambda[0]) + "_" + df.format(lambda[1]) + "_" +
                    simScenario + "_" + orchestratorPolicy +
                    "_" + objectPlacementPolicy + "_" + numberOfMobileDevices + "DEVICES");
        }
        if(SimSettings.getInstance().isOrbitMode()) {
            try {
                exportTaskList();
            } catch (Exception e) {
                SimLogger.printLine("Failed to generate task list");
                System.exit(0);
            }
        }

        if(SimSettings.getInstance().isParamScanMode()){
            double muTotal = 0;
            double poissonMean = sumPoisson/numberOfMobileDevices;
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
            muTotal = (muTotal*1000) / dataSizeMean;
            double meanRate = numberOfMobileDevices * (1/poissonMean);
            singleLambda = meanRate / muTotal;
            System.out.println("\nLambda: " + singleLambda + " mu");

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(4);
            SimLogger.getInstance().setFilePrefix("SIMRESULT_" + df.format(singleLambda) + "_" +
                    simScenario + "_" + orchestratorPolicy +
                    "_" + objectPlacementPolicy + "_" + numberOfMobileDevices + "DEVICES");


        }


        numOfIOTasks = ioTaskID;
        parityReadStarted = new boolean[numOfIOTasks];
        parityReadFinished = new boolean[numOfIOTasks];
    }

    public void exportTaskList() throws IOException {
        File taskListFile = null;
        FileWriter taskListFW = null;
        BufferedWriter taskListBW = null;

        taskListFile = new File(SimLogger.getInstance().getOutputFolder(), "TASK_LIST.txt");
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
            else if(objectLocations.size()>= 2) {
                taskList.add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                        task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength()));
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
            mdObjectIndex = random.nextInt(mdObjects.size());
            stripeID = RedisListHandler.getObjectID(mdObjects.get(mdObjectIndex));
            stripeObjects = RedisListHandler.getStripeObjects(stripeID);
            dataObjects = new ArrayList<String>(Arrays.asList(stripeObjects[0].split(" ")));
            parityObjects = new ArrayList<String>(Arrays.asList(stripeObjects[1].split(" ")));
        }
        else {
            boolean stripeFound=false;
            while (mdObjects.size() > 0) { //Check that stripe objects are available
                mdObjectIndex = random.nextInt(mdObjects.size());
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
        }
        return true;
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

}