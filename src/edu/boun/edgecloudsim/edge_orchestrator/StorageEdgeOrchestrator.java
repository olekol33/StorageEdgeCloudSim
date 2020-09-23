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
    private int selectShortestQueue(String locations) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
        int minQueuesize = 1000;
        int minQueueHost=-1;
        //shuffle
        Collections.shuffle(objectLocations,random);
        //get shortest
        for(String s : objectLocations) {
            int selectedHost = Integer.valueOf(s);
            if (minQueuesize>((StorageNetworkModel) networkModel).getWlanQueueSize(selectedHost)) {
                minQueuesize = ((StorageNetworkModel) networkModel).getWlanQueueSize(selectedHost);
                minQueueHost=selectedHost;
//                System.out.println("Host: "+selectedHost + ", queue size: " + ((StorageNetworkModel) networkModel).getWlanQueueSize(selectedHost));
                //shortest possible
                if(minQueuesize==0)
                    return minQueueHost;
            }
        }
        if(minQueueHost==-1)
            System.out.println("Host not selected");
        return minQueueHost;
    }

    private int selectNearestHostToRead(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        List<Integer> intObjectLocations = new ArrayList<>();
        for(String s : objectLocations) intObjectLocations.add(Integer.valueOf(s));
        return StaticRangeMobility.getNearestHost(intObjectLocations, deviceLocation);
    }

    private int selectNearestHostToReadWOQueue(String locations, Location deviceLocation) {
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
    }

    @Override
    public EdgeVM selectVmOnHost(Task task) {
        EdgeVM selectedVM = null;
        //TODO: check why this was a problem in LEAST_UTIL_IN_RANGE_WITH_PARITY
//        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
        //In static scenario
        Location deviceLocation = task.getSubmittedLocation();
        //in our scenasrio, serving wlan ID is equal to the host id
        //because there is only one host in one place
//        int relatedHostId=deviceLocation.getServingWlanId();
//        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);

        //Oleg: Get location of object according to policy
        //if not greedy shouldn't read parity at this stage
        //TODO: one more policy - read same object from several locations
/*        if(!policy.equalsIgnoreCase("NEAREST_WITH_PARITY")
                && !policy.equalsIgnoreCase("LEAST_UTIL_IN_RANGE_WITH_PARITY")
                && !policy.equalsIgnoreCase("CLOUD_OR_NEAREST_IF_CONGESTED") ){
            //if first char in object name is 'p' it's parity
            if(task.getIsParity() == 1) {
                SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock());
                return selectedVM;
            }
        }*/


        if(policy.equalsIgnoreCase("UNIFORM_HOST")){
            int relatedHostId= randomlySelectHostToRead(RedisListHandler.getObjectLocations(task.getObjectRead()));
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }
/*        else if(policy.equalsIgnoreCase("IF_CONGESTED_READ_PARITY")){
            NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
            int selectedHost = deviceLocation.getServingWlanId();
            int queueSize = ((StorageNetworkModel) networkModel).getWlanQueueSize(selectedHost);
            //if it's the parity, don't read from congested host
            if (task.getIsParity()==1){
                int relatedHostId= selectNearestHostToReadWOQueue(RedisListHandler.getObjectLocations(task.getObjectRead()),deviceLocation);
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                selectedVM = vmArray.get(0);
                return selectedVM;
            }
            //if queue size larger than threshold
            if (queueSize >= SimSettings.getInstance().getCongestedThreshold()) {
                LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
                ((IdleActiveStorageLoadGenerator) loadGeneratorModel).createParityTask(task);
            }
            int relatedHostId= selectNearestHostToRead(RedisListHandler.getObjectLocations(task.getObjectRead()),deviceLocation);
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }*/
/*        else if(policy.equalsIgnoreCase("SHORTEST_QUEUE")) {
            int relatedHostId= selectShortestQueue(RedisListHandler.getObjectLocations(task.getObjectRead()));
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }*/
/*        else if(policy.equalsIgnoreCase("IF_CONGESTED_READ_ONLY_PARITY") ||
                policy.equalsIgnoreCase("IF_CONGESTED_READ_PARITY")){
            NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
            int selectedHost = deviceLocation.getServingWlanId();
            int queueSize = ((StorageNetworkModel) networkModel).getWlanQueueSize(selectedHost);
            //if it's the parity, don't read from congested host
            if (task.getIsParity()==1){
                int relatedHostId= selectNearestHostToReadWOQueue(RedisListHandler.getObjectLocations(task.getObjectRead()),deviceLocation);
                if (relatedHostId>=SimSettings.getInstance().getNumOfEdgeDatacenters())
                    System.out.println("ERROR: Illegal host id " + relatedHostId);
                List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
                selectedVM = vmArray.get(0);
                return selectedVM;
            }
            //if queue size larger than threshold
            if (queueSize >= SimSettings.getInstance().getCongestedThreshold()) {
                LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
                boolean parityGenerated = ((IdleActiveStorageLoadGenerator) loadGeneratorModel).createParityTask(task);
                //don't read data, only parity
                if (parityGenerated==true && policy.equalsIgnoreCase("IF_CONGESTED_READ_ONLY_PARITY")) {
                    return selectedVM;
                }
            }
            int relatedHostId= selectNearestHostToRead(RedisListHandler.getObjectLocations(task.getObjectRead()),deviceLocation);
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }*/
        else if(policy.equalsIgnoreCase("NEAREST_HOST") ||
                policy.equalsIgnoreCase("CLOUD_OR_NEAREST_IF_CONGESTED") ){
            int relatedHostId= selectNearestHostToRead(RedisListHandler.getObjectLocations(task.getObjectRead()),deviceLocation);
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

               //if max requests to cloud
                if (wanQueueSize >= SimSettings.getInstance().getMaxCloudRequests())
                    result = SimSettings.GENERIC_EDGE_DEVICE_ID;
                //if queue size larger than threshold
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
