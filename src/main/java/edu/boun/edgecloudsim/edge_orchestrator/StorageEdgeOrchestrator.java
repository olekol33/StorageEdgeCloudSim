package edu.boun.edgecloudsim.edge_orchestrator;

import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.task_generator.IdleActiveStorageLoadGenerator;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StorageEdgeOrchestrator extends BasicEdgeOrchestrator {

    Random random = new Random();
    Vector<Double> parityProbVector;
    public StorageEdgeOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
        random.setSeed(ObjectGenerator.seed);
        createParityProbVector();
    }

    private void createParityProbVector(){
        parityProbVector= new Vector<>();
        int numberOfEdgeNodes = SimSettings.getInstance().getNumOfEdgeDatacenters();
        for(int i=0;i<numberOfEdgeNodes;i++){
            parityProbVector.add(i,(double)0);
        }
    }

    private void increaseParityProb(int hostID){
        double prevParityProb=parityProbVector.get(hostID);
        double parityProbStep = SimSettings.getInstance().getParityProbStep();
        if(prevParityProb<=1-parityProbStep) {
            prevParityProb+=parityProbStep;
            prevParityProb = BigDecimal.valueOf(prevParityProb).setScale(3, RoundingMode.HALF_UP).doubleValue();
            parityProbVector.set(hostID, prevParityProb);
        }
    }

    private void decreaseParityProb(int hostID){
        double prevParityProb=parityProbVector.get(hostID);
        double parityProbStep = SimSettings.getInstance().getParityProbStep();
        if(prevParityProb>=2*parityProbStep) {
            prevParityProb = prevParityProb - 2 * parityProbStep;
            prevParityProb = BigDecimal.valueOf(prevParityProb).setScale(3, RoundingMode.HALF_UP).doubleValue();
            parityProbVector.set(hostID, prevParityProb);
        }
    }

    //Randomly selects one location from list of location where object exists
    private int randomlySelectHostToRead(ArrayList<Integer> objectLocations) {
        return objectLocations.get(random.nextInt(objectLocations.size()));
    }

    private int selectNearestHostToRead(ArrayList<Integer> locations, Location deviceLocation) {
        StaticRangeMobility staticMobility = (StaticRangeMobility)SimManager.getInstance().getMobilityModel();
        return staticMobility.getNearestHost(locations, deviceLocation);
    }

    private boolean isNodeInList(ArrayList<Integer> locations, int node){
        for (int location : locations) {
            if (node == location)
                return true;
        }
        return false;
    }


    private int getHostWithShortestQueueForDevice(ArrayList<Integer> objectLocations) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        double minQueueSize = Double.MAX_VALUE;
        int minQueueHost=-1;
        if (objectLocations.size()==1)
            return objectLocations.get(0);
        for(Integer host : objectLocations){
            double queueSize = ((StorageNetworkModel) networkModel).getEdge2EdgeDelay(host);
            if (minQueueSize > queueSize) {
                minQueueSize = queueSize;
                minQueueHost = host;
            }
        }
        return minQueueHost;
    }

    //Returns node id with shortest queue (index 0) and queue size (index 1)
    private Object[] getHostWithShortestQueue(List<Integer> locations) {
        Object[] returnValues = new Object[2];
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        int minQueuesize = Integer.MAX_VALUE;
        int minQueueHost=-1;
        List<Integer> objectLocations = new ArrayList<>(locations);

        //only one location exists
        if (objectLocations.size()==1) {
            returnValues[0] = objectLocations.get(0);
            returnValues[1] = ((StorageNetworkModel) networkModel).getEdge2EdgeDelay(objectLocations.get(0));
            return returnValues;
        }
        for(int i=0; i<objectLocations.size();i++){
            int queueSize = (int)((StorageNetworkModel) networkModel).getEdge2EdgeDelay(objectLocations.get(i));
            if (minQueuesize > queueSize) {
                minQueuesize = queueSize;
                minQueueHost = i;
            }
        }
        returnValues[0] = minQueueHost;
        returnValues[1] = minQueuesize;
        return returnValues;
    }



    @Override
    public EdgeVM selectVmOnHost(Task task) {
        EdgeVM selectedVM = null;
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
        List<Integer> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();

        List<Integer> objectLocations = RedisListHandler.getObjectLocations(task.getObjectRead());

        for (int host : nonOperateHosts)
            objectLocations.remove(host);
        if (task.getIsParity()==1){
            //remove host of data if other options exist
            if (objectLocations.size()>1)
                objectLocations.remove((Integer) task.getSubmittedLocation().getServingWlanId());
        }
        ArrayList<Integer> operativeHosts = new ArrayList<>(objectLocations);
        if(policy.equalsIgnoreCase("IF_CONGESTED_READ_PARITY")){
            //if it's the parity, don't read from congested host
            if (task.getIsParity()==1){
                if (objectLocations.size()==0) {
                    SimLogger.getInstance().taskFailedDueToInaccessibility(task.getCloudletId(), CloudSim.clock(),
                            SimSettings.VM_TYPES.EDGE_VM.ordinal(),task);
                    return selectedVM;
                }
                int relatedHostId= getHostWithShortestQueueForDevice(operativeHosts);
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                selectedVM = vmArray.get(0);
            }
            else { //not parity
                //if data object can't be read - read parity
                if (objectLocations.size()==0) {
                    boolean parityGenerated = createParityTask(task, -1);
                    if (!parityGenerated) //if parity not generated, it's lost
                        SimLogger.getInstance().taskFailedDueToInaccessibility(task.getCloudletId(), CloudSim.clock(),
                                SimSettings.VM_TYPES.EDGE_VM.ordinal(),task);
                    return selectedVM;
                }
                int relatedHostId;
                if (isNodeInList(operativeHosts, deviceLocation.getServingWlanId()))
                    relatedHostId= selectNearestHostToRead(operativeHosts,deviceLocation);
                else
                    relatedHostId = -1;
                //if not nearest host contains object or host is congested, read from it - read on grid
//                if (relatedHostId!=deviceLocation.getServingWlanId() || queueSize >= SimSettings.getInstance().getManThreshold()){ //mind the threshold
                if (relatedHostId!=deviceLocation.getServingWlanId()){ //if object not in the nearest host
                    //select host with min queue from grid
                    relatedHostId = getHostWithShortestQueueForDevice(operativeHosts);
                    //always try to use parity if it's faster by a factor
                    task.setHostID(relatedHostId);
                    boolean parityGenerated = createParityTask(task, relatedHostId);
                    if (parityGenerated) {
                        //return null
                        SimLogger.getInstance().setHostId(task.getCloudletId(),relatedHostId);
                        SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock(),SimSettings.VM_TYPES.EDGE_VM.ordinal(), task);
                        return selectedVM;
                    }
                }
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                selectedVM = vmArray.get(0);
            }
        }
        //if access point doesn't contain the object, pick one randomly
        else if(policy.equalsIgnoreCase("NEAREST_HOST") || policy.equalsIgnoreCase("CLOUD_OR_NEAREST_IF_CONGESTED")){
            int relatedHostId= selectNearestHostToRead(operativeHosts,deviceLocation);
            if (relatedHostId != deviceLocation.getServingWlanId()){
                relatedHostId = randomlySelectHostToRead(operativeHosts);
            }
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }
        else
        {
            System.out.println("ERROR: Non familiar policy");
            System.exit(0);
        }
        return selectedVM;
    }

    public boolean createParityTask(Task task, int dataObjectLocation){
        double objectMinQueueSize=Double.MAX_VALUE;
        int objectMinQueueSizeHost=-1;
        IdleActiveStorageLoadGenerator loadGeneratorModel = (IdleActiveStorageLoadGenerator) SimManager.getInstance().getLoadGeneratorModel();
        int taskType = task.getTaskType();
        int isParity=1;
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        List<Integer> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
        boolean replication=false;
        boolean dataParity=false;
        boolean coding=false;
        int numOfDataInStripe = SimSettings.getInstance().getNumOfDataInStripe();
        int numOfParityInStripe = SimSettings.getInstance().getNumOfParityInStripe();
        if (SimManager.getInstance().getObjectPlacementPolicy().equalsIgnoreCase("REPLICATION_PLACE"))
            replication=true;
        else if (SimManager.getInstance().getObjectPlacementPolicy().equalsIgnoreCase("DATA_PARITY_PLACE"))
            dataParity=true;
        else
            coding=true;
        //If replication policy, read the same object, but mark it as parity
        if (replication) {
            List<Integer> objectLocations = RedisListHandler.getObjectLocations(task.getObjectRead());
            //if some hosts are unavailable
            if (nonOperateHosts.size()>0){
                //remove non-active locations
                for (int host : nonOperateHosts)
                    objectLocations.remove(host);
                //if replication and no available objects - false
                if(objectLocations.size()==0)
                    return false;
            }
            //if no replicas and REPLICATION_PLACE
            if (objectLocations.size()==1)
                return false;

            //if there are replicas of the object
            else if(objectLocations.size()>= 2) {
                TaskProperty newTask = new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                        task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getLength(),
                        task.getHostID());
                SimManager.getInstance().createNewTask(newTask);
                return true;
            }
        }
        else if (dataParity || coding){
//            String locations = RedisListHandler.getObjectLocations(task.getObjectRead());
            List<Integer> objectLocations = RedisListHandler.getObjectLocations(task.getObjectRead());
            Object[] result = getHostWithShortestQueue(objectLocations);
            objectMinQueueSizeHost = (int) result[0];
            objectMinQueueSize = (double) result[1];
        }
        else{
            System.out.println("No policy found");
            System.exit(1);
        }
        List<String[]> mdObjects = RedisListHandler.getObjects(task.getObjectRead());
        //no parities
        if (mdObjects==null)
            return false;
        int mdObjectIndex;
        String[] minStripeObjects = new String[0];
        List<String> dataObjects = null;
        List<String> parityObjects = null;
        double minStripeQueueSize = Double.MAX_VALUE;
        if (nonOperateHosts.size()==0){ //no failed hosts
            for (String[] stripe:mdObjects){ //for each stripe
                ArrayList<String> stripeObjectsList = new ArrayList<>(Arrays.asList(stripe));
                stripeObjectsList.remove(task.getObjectRead());
                double stripeQueueSize = getStripeMaxQueueSize(stripeObjectsList,dataObjectLocation);
                if(stripeQueueSize<minStripeQueueSize && stripeQueueSize!=-1){ //compare queue size for each stripe
                    minStripeQueueSize=stripeQueueSize;
                    minStripeObjects=stripe;
                }
            }
            if(minStripeQueueSize == Integer.MAX_VALUE) { //not found parity
                if(objectMinQueueSizeHost>=0){ //use replica
                    TaskProperty newTask = new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                            task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getLength(),
                            task.getHostID());
                    SimManager.getInstance().createNewTask(newTask);
                    return true;
                }
                return false; //no replica nor parity (error)
            }
            //used to reduce delay accuracy
            if(minStripeQueueSize>=objectMinQueueSize) {  //if object queue lower no need for parity
                decreaseParityProb(dataObjectLocation);
                TaskProperty newTask = new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                        task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getLength(),
                        task.getHostID());
                SimManager.getInstance().createNewTask(newTask);
                return true;
            }
            else { //create parity only if queue lower
                increaseParityProb(dataObjectLocation);
                double randomVal = random.nextDouble();
                //if uniformly not in parityProb - rerun same task again (mark as parity to avoid same node usage)
                if(randomVal>parityProbVector.get(dataObjectLocation)){
                    TaskProperty newTask = new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                            task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getLength(),
                            task.getHostID());
                    SimManager.getInstance().createNewTask(newTask);
                    return true;
                }
            }
            //get object from stripe with min queue
            dataObjects = new ArrayList<String>(Arrays.asList(minStripeObjects).subList(0, numOfDataInStripe));
            parityObjects = new ArrayList<String>(Arrays.asList(minStripeObjects).subList(numOfDataInStripe, minStripeObjects.length));
        }
        else { //There are inactive nodes
            boolean stripeFound=false;
            while (mdObjects.size() > 0) { //Check that stripe objects are available
                mdObjectIndex = loadGeneratorModel.getParityRandom().nextInt(mdObjects.size());
                String[] randomMdObject = mdObjects.get(mdObjectIndex);
                dataObjects = new ArrayList<String>(Arrays.asList(randomMdObject).subList(0, numOfDataInStripe));
                parityObjects = new ArrayList<String>(Arrays.asList(randomMdObject).subList(numOfDataInStripe, minStripeObjects.length));
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
            TaskProperty newTask = new TaskProperty(task.getMobileDeviceId(),taskType, CloudSim.clock(),
                    objectID, task.getIoTaskID(), isParity,0,task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getLength());
            SimManager.getInstance().createNewTask(newTask);
        }
        for (String objectID:parityObjects){
            i++;
            TaskProperty newTask = new TaskProperty(task.getMobileDeviceId(),taskType, CloudSim.clock(),
                    objectID, task.getIoTaskID(), isParity,paritiesToRead,task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getLength());
            SimManager.getInstance().createNewTask(newTask);
            //count just one read for queue
            paritiesToRead=0;
        }
        if (i!=(numOfDataInStripe+numOfParityInStripe))
            System.out.println("Not created tasks for all parities");
        loadGeneratorModel.updateActiveCodedIOTasks(task.getIoTaskID(),i-1);
        return true;
    }

    //Returns min queue size for stripe
    private double getStripeMaxQueueSize(ArrayList<String> stripeObjects, int dataObjectLocation){
        double stripeQueueSize=0;
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        for (String objectID:stripeObjects){ //For each stripe object
            List<Integer> objectLocations = RedisListHandler.getObjectLocations(objectID);
            double objectQueueSize=Double.MAX_VALUE;
            for (int location:objectLocations){  //for each object location
                double queue = ((StorageNetworkModel) networkModel).getEdge2EdgeDelay(location);
                //if min host so far and not dataObjectLocation
                if(queue<objectQueueSize && location != dataObjectLocation){
                    objectQueueSize=queue;
                }
            }
            if (objectQueueSize==Integer.MAX_VALUE) //if stripe must read from dataObjectLocation
                return -1;
            else{
                if(stripeQueueSize<objectQueueSize) //take max queue size between stripe objects
                    stripeQueueSize=objectQueueSize;
            }
        }
        return stripeQueueSize;
    }

    //Checks if all stripe parities are available
    private boolean checkStripeValidity(List<String>  stripeObjects){
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        List<Integer> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
        IdleActiveStorageLoadGenerator loadGeneratorModel = (IdleActiveStorageLoadGenerator) SimManager.getInstance().getLoadGeneratorModel();
        for (String objectID:stripeObjects){
            List<Integer> objectLocations = RedisListHandler.getObjectLocations(objectID);
            if (loadGeneratorModel.contains(nonOperateHosts,objectLocations))
                return false;
        }
        return true;
    }

    @Override
    //Oleg: Set type of device to offload. When single tier - it's edge host.
    //TODO: Need both edge and cloud
    public int getDeviceToOffload(Task task) {
        int result = SimSettings.GENERIC_EDGE_DEVICE_ID;
        if(!simScenario.equals("SINGLE_TIER")){
            if(policy.equalsIgnoreCase("CLOUD_OR_NEAREST_IF_CONGESTED")){
                Location deviceLocation = task.getSubmittedLocation();
                NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
                int wlanQueueSize = ((StorageNetworkModel)networkModel).getWlanQueueSize(deviceLocation.getServingWlanId());

                List<Integer> nonOperativeHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
                List<Integer> objectLocations = RedisListHandler.getObjectLocations(task.getObjectRead());
                for (int host : nonOperativeHosts)
                    objectLocations.remove(host);

               //if edge node non operative
                //TODO: check case when wanQueue is congested and task sent to it
                if (objectLocations.size()==0)
                    result = SimSettings.CLOUD_DATACENTER_ID;
                //if WLAN queue size larger than threshold
                else if (wlanQueueSize >= SimSettings.getInstance().getCongestedThreshold())
                    result = SimSettings.CLOUD_DATACENTER_ID;
            }
        }

        return result;
    }


    @Override
    public Vm getVmToOffload(Task task, int deviceId) {
        Vm selectedVM = null;

        if(deviceId == SimSettings.CLOUD_DATACENTER_ID){
            //Select VM on cloud devices via Least Loaded algorithm!
            double selectedVmCapacity = 0; //start with min value
            List<Host> list = SimManager.getInstance().getCloudServerManager().getDatacenter().getHostList();
            for (int hostIndex=0; hostIndex < list.size(); hostIndex++) {
                List<CloudVM> vmArray = SimManager.getInstance().getCloudServerManager().getVmList(hostIndex);
                for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                    double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                    double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                    if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
                        selectedVM = vmArray.get(vmIndex);
                        selectedVmCapacity = targetVmCapacity;
                    }
                }
            }
        }
        else
            selectedVM = selectVmOnHost(task);

        return selectedVM;
    }

    public String getPolicy() {
        return policy;
    }
}
