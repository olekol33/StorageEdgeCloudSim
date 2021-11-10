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
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

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
        if(prevParityProb<=1-parityProbStep)
            parityProbVector.set(hostID,prevParityProb+parityProbStep);
    }

    private void decreaseParityProb(int hostID){
        double prevParityProb=parityProbVector.get(hostID);
        double parityProbStep = SimSettings.getInstance().getParityProbStep();
        if(prevParityProb>=2*parityProbStep)
            parityProbVector.set(hostID,prevParityProb-2*parityProbStep);
    }

    //Randomly selects one location from list of location where object exists
    private int randomlySelectHostToRead(String locations) {
        List<String> objectLocations = new ArrayList<>(Arrays.asList(locations.split(" ")));
        String randomLocation = objectLocations.get(random.nextInt(objectLocations.size()));
        return Integer.parseInt(randomLocation);
    }

    private int selectNearestHostToRead(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<>(Arrays.asList(locations.split(" ")));
        List<Integer> intObjectLocations = new ArrayList<>();
        for(String s : objectLocations) intObjectLocations.add(Integer.valueOf(s));
        StaticRangeMobility staticMobility = (StaticRangeMobility)SimManager.getInstance().getMobilityModel();
        return staticMobility.getNearestHost(intObjectLocations, deviceLocation);
    }

    private int getHostWithShortestManQueueForDevice(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<>(Arrays.asList(locations.split(" ")));
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        int minQueuesize = Integer.MAX_VALUE;
        int minQueueHost=-1;
        //only one location exists
        if (objectLocations.size()==1)
            return selectNearestHostToRead(locations,deviceLocation);
        for(String s : objectLocations){
            int selectedHost = Integer.valueOf(s);
            int queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(selectedHost);
            if (minQueuesize > queueSize) {
                minQueuesize = queueSize;
                minQueueHost = selectedHost;
            }
        }
        return minQueueHost;
    }

    //Returns node id with shortest queue (index 0) and queue size (index 1)
    private int[] getHostWithShortestManQueue(String locations) {
        int[] objectLocations = Stream.of(locations.split(" ")).mapToInt(Integer::parseInt).toArray();

        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        int minQueuesize = Integer.MAX_VALUE;
        int minQueueHost=-1;
        //only one location exists
        if (objectLocations.length==1)
            return new int[] {objectLocations[0],((StorageNetworkModel) networkModel).getManQueueSize(objectLocations[0])};
        for(int i=0; i<objectLocations.length;i++){
            int queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(i);
            if (minQueuesize > queueSize) {
                minQueuesize = queueSize;
                minQueueHost = i;
            }
        }
        return new int[] {minQueueHost, minQueuesize};
    }



    @Override
    public EdgeVM selectVmOnHost(Task task) {
        EdgeVM selectedVM = null;
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
        List<String> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
        String locations = RedisListHandler.getObjectLocations(task.getObjectRead());

        String operativeHosts = "";
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        for (String host : nonOperateHosts){
            objectLocations.remove(host);
        }
        if (task.getIsParity()==1){
            //remove host of data if other options exist
            if (objectLocations.size()>1)
                objectLocations.remove(String.valueOf(task.getHostID()));
        }
        for (String s : objectLocations)
        {
            operativeHosts += s + " ";
        }
        if(policy.equalsIgnoreCase("NEAREST_HOST")){
            if (objectLocations.size()==0) {
                SimLogger.getInstance().taskFailedDueToInaccessibility(task.getCloudletId(), CloudSim.clock(),
                        SimSettings.VM_TYPES.EDGE_VM.ordinal(), task);
                return selectedVM;
            }
        }
        if(policy.equalsIgnoreCase("IF_CONGESTED_READ_PARITY")){
            //if it's the parity, don't read from congested host
            if (task.getIsParity()==1){
                if (objectLocations.size()==0) {
                    SimLogger.getInstance().taskFailedDueToInaccessibility(task.getCloudletId(), CloudSim.clock(),
                            SimSettings.VM_TYPES.EDGE_VM.ordinal(),task);
                    return selectedVM;
                }
                int relatedHostId= getHostWithShortestManQueueForDevice(operativeHosts,deviceLocation);
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                selectedVM = vmArray.get(0);
            }
            else { //not parity
                //if data object can't be read - read parity
                if (objectLocations.size()==0) {
                    boolean parityGenerated = createParityTask(task, String.valueOf(-1));
                    if (!parityGenerated) //if parity not generated, it's lost
                        SimLogger.getInstance().taskFailedDueToInaccessibility(task.getCloudletId(), CloudSim.clock(),
                                SimSettings.VM_TYPES.EDGE_VM.ordinal(),task);
                    return selectedVM;
                }
                int relatedHostId= selectNearestHostToRead(operativeHosts,deviceLocation);
//                int queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(relatedHostId);
                //if not nearest host contains object or host is congested, read from it - read on grid
//                if (relatedHostId!=deviceLocation.getServingWlanId() || queueSize >= SimSettings.getInstance().getManThreshold()){ //mind the threshold
                if (relatedHostId!=deviceLocation.getServingWlanId()){ //if object not in the nearest host
                    //select host with min queue from grid
                    relatedHostId = getHostWithShortestManQueueForDevice(operativeHosts,deviceLocation);
//                    queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(relatedHostId);
                    //always try to use parity if it's faster by a factor
                    LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
                    task.setHostID(relatedHostId);
                    boolean parityGenerated = createParityTask(task, String.valueOf(relatedHostId) );
                    if (parityGenerated) {
                        //return null
                        //TODO: SimSettings.VM_TYPES.EDGE_VM.ordinal() - what about cloud?
                        SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock(),SimSettings.VM_TYPES.EDGE_VM.ordinal());
                        SimLogger.getInstance().setHostId(task.getCloudletId(),relatedHostId);


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

    public boolean createParityTask(Task task, String dataObjectLocation){
        int objectMinQueueSize=Integer.MAX_VALUE;
        int objectMinQueueSizeHost=-1;
        IdleActiveStorageLoadGenerator loadGeneratorModel = (IdleActiveStorageLoadGenerator) SimManager.getInstance().getLoadGeneratorModel();
        int taskType = task.getTaskType();
        int isParity=1;
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        List<String> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
        boolean replication=false;
        boolean dataParity=false;
        boolean coding=false;
        if (SimManager.getInstance().getObjectPlacementPolicy().equalsIgnoreCase("REPLICATION_PLACE"))
            replication=true;
        else if (SimManager.getInstance().getObjectPlacementPolicy().equalsIgnoreCase("DATA_PARITY_PLACE"))
            dataParity=true;
        else
            coding=true;
        //If replication policy, read the same object, but mark it as parity
        if (replication) {
            String locations = RedisListHandler.getObjectLocations(task.getObjectRead());
            List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
            //if some hosts are unavailable
            if (nonOperateHosts.size()>0){
                //remove non active locations
                for (String host : nonOperateHosts)
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
                loadGeneratorModel.getTaskList().add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                        task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength(),
                        task.getHostID()));
                SimManager.getInstance().createNewTask();
                return true;
            }
        }
        else if (dataParity || coding){
            String locations = RedisListHandler.getObjectLocations(task.getObjectRead());
            int result[] = getHostWithShortestManQueue(locations);
            objectMinQueueSizeHost = result[0];
            objectMinQueueSize = result[1];
        }
        else{
            System.out.println("No policy found");
            System.exit(1);
        }
        List<String> mdObjects = RedisListHandler.getObjectsFromRedis("object:md*_"+task.getObjectRead()+"_*");
        //no parities
        if (mdObjects.size()==0)
            return false;
        int mdObjectIndex;
        String stripeID;
//        String[] minStripeObjects = new String[0];
        String[] stripeObjects;
        List<String> dataObjects = null;
        List<String> parityObjects = null;
        int minStripeQueueSize = Integer.MAX_VALUE;
        if (nonOperateHosts.size()==0){ //no failed hosts
            for (String stripe:mdObjects){ //for each stripe
                stripeObjects = RedisListHandler.getStripeObjects(stripe); //get as array of data and parity
                ArrayList<String> stripeObjectsList = new ArrayList<>(Arrays.asList(stripeObjects[0].split(" "))); //convert to list
                stripeObjectsList.add(stripeObjects[1]);
                stripeObjectsList.remove(task.getObjectRead());
                int stripeQueueSize = getStripeMaxQueueSize(stripeObjectsList,dataObjectLocation);
                if(stripeQueueSize<minStripeQueueSize && stripeQueueSize!=-1){ //compare queue size for each stripe
                    minStripeQueueSize=stripeQueueSize;
//                    minStripeObjects=stripeObjects;
                }
            }
            if(minStripeQueueSize == Integer.MAX_VALUE) { //not found parity
                if(objectMinQueueSizeHost>=0){ //use replica
                    loadGeneratorModel.getTaskList().add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                            task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength(),
                            task.getHostID()));
                    return true;
                }
                return false; //no replica nor parity (error)
            }
            if(minStripeQueueSize>=objectMinQueueSize) { //create parity only if queue lower
                decreaseParityProb(Integer.valueOf(dataObjectLocation));
                loadGeneratorModel.getTaskList().add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                        task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength(),
                        task.getHostID()));//use replica
                return true;
            }
            else {
                increaseParityProb(Integer.valueOf(dataObjectLocation));
                if(random.nextDouble()>parityProbVector.get(Integer.valueOf(dataObjectLocation))){ //if uniformly not in parityProb
                    loadGeneratorModel.getTaskList().add(new TaskProperty(task.getMobileDeviceId(), taskType, CloudSim.clock(), task.getObjectRead(),
                            task.getIoTaskID(), isParity, 1, task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength(),
                            task.getHostID()));//use replica
                    return true;
                }
            }
        }
        else { //There are inactive nodes
            boolean stripeFound=false;
            while (mdObjects.size() > 0) { //Check that stripe objects are available
                mdObjectIndex = loadGeneratorModel.getParityRandom().nextInt(mdObjects.size()); //TODO: currently selects random, use above logic
                stripeID = RedisListHandler.getObjectID(mdObjects.get(mdObjectIndex));
                stripeObjects = RedisListHandler.getStripeObjects(stripeID);
                dataObjects = new ArrayList<>(Arrays.asList(stripeObjects[0].split(" ")));
                parityObjects = new ArrayList<>(Arrays.asList(stripeObjects[1].split(" ")));
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
            loadGeneratorModel.getTaskList().add(new TaskProperty(task.getMobileDeviceId(),taskType, CloudSim.clock(),
                    objectID, task.getIoTaskID(), isParity,0,task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength()));
            SimManager.getInstance().createNewTask();
        }
        for (String objectID:parityObjects){
            i++;
            loadGeneratorModel.getTaskList().add(new TaskProperty(task.getMobileDeviceId(),taskType, CloudSim.clock(),
                    objectID, task.getIoTaskID(), isParity,paritiesToRead,task.getCloudletFileSize(), task.getCloudletOutputSize(), task.getCloudletLength()));
            SimManager.getInstance().createNewTask();
            //count just one read for queue
            paritiesToRead=0;
        }
        if (i!=(SimSettings.getInstance().getNumOfDataInStripe()+SimSettings.getInstance().getNumOfParityInStripe()))
            System.out.println("Not created tasks for all parities");
        loadGeneratorModel.updateActiveCodedIOTasks(task.getIoTaskID(),i-1);
        return true;
    }

    //Returns min queue size for stripe
    private int getStripeMaxQueueSize(ArrayList<String> stripeObjects, String dataObjectLocation){
        int stripeQueueSize=0;
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        for (String objectID:stripeObjects){ //For each stripe object
            String locations = RedisListHandler.getObjectLocations(objectID);
            List<String> objectLocations = new ArrayList<>(Arrays.asList(locations.split(" ")));
            int objectQueueSize=Integer.MAX_VALUE;
            for (String location:objectLocations){  //for each object location
                int manQueue = ((StorageNetworkModel) networkModel).getManQueueSize(Integer.valueOf(location));
                //if min host so far and not dataObjectLocation
                if(manQueue<objectQueueSize && !location.equals(dataObjectLocation)){
                    objectQueueSize=manQueue;
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
        List<String> nonOperateHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
        IdleActiveStorageLoadGenerator loadGeneratorModel = (IdleActiveStorageLoadGenerator) SimManager.getInstance().getLoadGeneratorModel();


        for (String objectID:stripeObjects){
            String locations = RedisListHandler.getObjectLocations(objectID);
            List<String> objectLocations = new ArrayList<>(Arrays.asList(locations.split(" ")));
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
//                int wanQueueSize = ((StorageNetworkModel)networkModel).getWanQueueSize(deviceLocation.getServingWlanId());
//                int selectedHost = deviceLocation.getServingWlanId();

                List<String> nonOperativeHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
                String locations = RedisListHandler.getObjectLocations(task.getObjectRead());
//                String operativeHosts = "";
                List<String> objectLocations = new ArrayList<>(Arrays.asList(locations.split(" ")));
                for (String host : nonOperativeHosts){
                    objectLocations.remove(host);
                }

               //if edge node non operative
                //TODO: check case when wanQueue is congested and task sent to it
                if (objectLocations.size()==0)
                    result = SimSettings.CLOUD_DATACENTER_ID;
                //if wan queue is above threshold
/*                else if (wanQueueSize >= SimSettings.getInstance().getMaxCloudRequests())
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;*/
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
