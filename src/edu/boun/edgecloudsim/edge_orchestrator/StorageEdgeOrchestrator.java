package edu.boun.edgecloudsim.edge_orchestrator;

import edu.boun.edgecloudsim.cloud_server.CloudVM;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.task_generator.IdleActiveStorageLoadGenerator;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.*;

public class StorageEdgeOrchestrator extends BasicEdgeOrchestrator {

    Random random = new Random();
    public StorageEdgeOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
        random.setSeed(ObjectGenerator.seed);
    }

    //Randomly selects one location from list of location where object exists
    private int randomlySelectHostToRead(String locations) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        String randomLocation = objectLocations.get(random.nextInt(objectLocations.size()));
        return Integer.parseInt(randomLocation);
    }
    //Get host with shortest queue
/*    private int getHostWithShortestQueueInRange(Location deviceLocation) {
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        List<Integer> hostsInRange = deviceLocation.getHostsInRange();
        int queueThreshold = SimSettings.getInstance().getCongestedThreshold();
        int minQueuesize = 1000;
        int minQueueHost=-1;
        //get shortest
        for(int selectedHost : hostsInRange) {
            int queueSize = ((StorageNetworkModel) networkModel).getWlanQueueSize(selectedHost);
            if (minQueuesize > queueSize) {
                minQueuesize = queueSize;
                minQueueHost = selectedHost;
            }
        }
        if(minQueueHost==-1)
            System.out.println("Host not selected");
        //if selected access point is different and has shorter queue with thershold return it
        if (minQueueHost!=deviceLocation.getServingWlanId()){
            if (((StorageNetworkModel) networkModel).getWlanQueueSize(minQueueHost) + queueThreshold<
                    ((StorageNetworkModel) networkModel).getWlanQueueSize(deviceLocation.getServingWlanId()))
                return minQueueHost;
        }
        //else keep same access point
        return deviceLocation.getServingWlanId();
    }*/

    private int selectNearestHostToRead(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        List<Integer> intObjectLocations = new ArrayList<>();
        for(String s : objectLocations) intObjectLocations.add(Integer.valueOf(s));
        return StaticRangeMobility.getNearestHost(intObjectLocations, deviceLocation);
    }

/*    private int selectNearestHostToReadWOQueue(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        List<Integer> intObjectLocations = new ArrayList<>();
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        //only one location exists
        if (objectLocations.size()==1)
            return selectNearestHostToRead(locations,deviceLocation);
        for(String s : objectLocations){
            int selectedHost = Integer.valueOf(s);
            //if large queue, skip
            if (((StorageNetworkModel) networkModel).getWlanQueueSize(selectedHost)>SimSettings.getInstance().getCongestedThreshold())
                continue;
            else
                intObjectLocations.add(Integer.valueOf(s));
        }
        //all are with queue, return nearest
        if (intObjectLocations.size()==0)
            return selectNearestHostToRead(locations,deviceLocation);
        else //if some not with queue
            return StaticRangeMobility.getNearestHost(intObjectLocations, deviceLocation);
    }*/

    private int getHostWithShortestManQueue(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
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
/*            //if large queue, skip
            if (((StorageNetworkModel) networkModel).getManQueueSize(selectedHost)>SimSettings.getInstance().getCongestedThreshold())
                continue;
            else
                intObjectLocations.add(Integer.valueOf(s));*/
        }
/*        //all are with queue, return nearest
        if (intObjectLocations.size()==0)
            return selectNearestHostToRead(locations,deviceLocation);
        else //if some not with queue
            return StaticRangeMobility.getNearestHost(intObjectLocations, deviceLocation);
        if (minQueueHost==-1)
            System.out.println("ERROR: No host found");*/
        return minQueueHost;
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

        //for all non parity policies check if object can be read
//        if(policy.equalsIgnoreCase("UNIFORM_HOST") || policy.equalsIgnoreCase("SHORTEST_QUEUE_IN_RANGE") ||
//                policy.equalsIgnoreCase("NEAREST_HOST")){
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
 /*               //randomly select host, try to avoid reading from same location as before (filtered in operativeHosts)
                int relatedHostId= selectNearestHostToRead(operativeHosts,deviceLocation);
                if (relatedHostId != deviceLocation.getServingWlanId()){
                    relatedHostId = getHostWithShortestManQueue(operativeHosts,deviceLocation);
                }*/
                int relatedHostId= getHostWithShortestManQueue(operativeHosts,deviceLocation);
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                selectedVM = vmArray.get(0);
            }
            else { //not parity

                //if data object can't be read - read parity
                if (objectLocations.size()==0) {
                    LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
                    boolean parityGenerated = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).createParityTask(task);
                    if (!parityGenerated) //if parity not generated, it's lost
                        SimLogger.getInstance().taskFailedDueToInaccessibility(task.getCloudletId(), CloudSim.clock(),
                                SimSettings.VM_TYPES.EDGE_VM.ordinal(),task);
                    return selectedVM;
                }
                int relatedHostId= selectNearestHostToRead(operativeHosts,deviceLocation);
                int queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(relatedHostId);
                //if not nearest host contains object or host is congested, read from it - read on grid
                if (relatedHostId!=deviceLocation.getServingWlanId() || queueSize >= SimSettings.getInstance().getManThreshold()){
                    //uniformly select host on grid
                    relatedHostId = randomlySelectHostToRead(operativeHosts);
                    queueSize = ((StorageNetworkModel) networkModel).getManQueueSize(relatedHostId);
                    //if MAN queue too large
                    if (queueSize >= SimSettings.getInstance().getManThreshold()) {
                        double probContinueRead = SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][LoadGeneratorModel.PROB_CLOUD_SELECTION];
                        //with probability probContinueRead proceed with original request
                        if (random.nextInt(100)<=probContinueRead) {
                            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                            selectedVM = vmArray.get(0);
                            return selectedVM;
                        }
                        LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
                        task.setHostID(relatedHostId);
                        boolean parityGenerated = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).createParityTask(task);
                        //don't read data, only parity
//                        if (parityGenerated==true && policy.equalsIgnoreCase("IF_CONGESTED_READ_ONLY_PARITY")) {
                        if (parityGenerated==true) {
                            //return null
                            //TODO: SimSettings.VM_TYPES.EDGE_VM.ordinal() - what about cloud?
                            SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock(),SimSettings.VM_TYPES.EDGE_VM.ordinal());
                            SimLogger.getInstance().setHostId(task.getCloudletId(),relatedHostId);


                            return selectedVM;
                        }
                    }
                }
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                selectedVM = vmArray.get(0);
            }

//            int relatedHostId= selectNearestHostToRead(RedisListHandler.getObjectLocations(task.getObjectRead()),deviceLocation);
//            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
//            selectedVM = vmArray.get(0);
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
/*        else if(policy.equalsIgnoreCase("LEAST_UTIL_IN_RANGE_WITH_PARITY")){
            NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
            int queueSize = ((StorageNetworkModel)networkModel).getQueueSize(deviceLocation.getServingWlanId());
            int selectedHost = deviceLocation.getServingWlanId();
            //if queue size larger than threshold and more than 1 host is range
            if (queueSize > SimSettings.getInstance().getCongestedThreshold() && deviceLocation.getHostsInRange().size() > 1){

                for (int host : deviceLocation.getHostsInRange()){
                    //if same device
                    if (host == deviceLocation.getServingWlanId())
                        continue;
                    //If shorter queue, read from here
                    if (((StorageNetworkModel)networkModel).getQueueSize(host) < queueSize){
                        queueSize = ((StorageNetworkModel)networkModel).getQueueSize(host);
                        selectedHost = host;
                    }
                }
                if (deviceLocation.getServingWlanId() != selectedHost) {
                    System.out.println("\nFor ioTask " + task.getIoTaskID() + "object " + task.getObjectRead() +
                            " replaced " + deviceLocation.getServingWlanId() + " to " + selectedHost + "\n");
                    deviceLocation.setServingWlanId(selectedHost);
                    task.setSubmittedLocation(deviceLocation);
                    MobilityModel mobilityModel = SimManager.getInstance().getMobilityModel();
                    ((StaticRangeMobility)mobilityModel).setLocation(task.getMobileDeviceId(),deviceLocation);


                }
            }
            int relatedHostId= selectNearestHostToRead(RedisListHandler.getObjectLocations(task.getObjectToRead()),deviceLocation);
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }*/
//        System.out.println("Selected host: " + selectedVM.getId());
        return selectedVM;
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
                int wanQueueSize = ((StorageNetworkModel)networkModel).getWanQueueSize(deviceLocation.getServingWlanId());
                int selectedHost = deviceLocation.getServingWlanId();

                List<String> nonOperativeHosts = ((StorageNetworkModel) networkModel).getNonOperativeHosts();
                String locations = RedisListHandler.getObjectLocations(task.getObjectRead());
                String operativeHosts = "";
                List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
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
/*        else if(simScenario.equals("TWO_TIER_WITH_EO"))
            selectedVM = selectVmOnLoadBalancer(task);*/
        else
            selectedVM = selectVmOnHost(task);

        return selectedVM;
    }

    public String getPolicy() {
        return policy;
    }
}
