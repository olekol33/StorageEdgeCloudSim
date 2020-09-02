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
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

    private int selectNearestHostToRead(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        List<Integer> intObjectLocations = new ArrayList<>();
        for(String s : objectLocations) intObjectLocations.add(Integer.valueOf(s));
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
        if(!policy.equalsIgnoreCase("NEAREST_WITH_PARITY")
                && !policy.equalsIgnoreCase("LEAST_UTIL_IN_RANGE_WITH_PARITY")
                && !policy.equalsIgnoreCase("CLOUD_OR_NEAREST_IF_CONGESTED") ){
            //if first char in object name is 'p' it's parity
            if(task.getIsParity() == 1) {
                SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock());
                return selectedVM;
            }
        }
        if(policy.equalsIgnoreCase("RANDOM_HOST")){
            int relatedHostId= randomlySelectHostToRead(RedisListHandler.getObjectLocations(task.getObjectToRead()));
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }
        else if(policy.equalsIgnoreCase("NEAREST_HOST") || policy.equalsIgnoreCase("NEAREST_WITH_PARITY") ||
                policy.equalsIgnoreCase("CLOUD_OR_NEAREST_IF_CONGESTED") ){
            int relatedHostId= selectNearestHostToRead(RedisListHandler.getObjectLocations(task.getObjectToRead()),deviceLocation);
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }
        if(policy.equalsIgnoreCase("LEAST_UTIL_IN_RANGE_WITH_PARITY")){
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
        }
/*        else if(policy.equalsIgnoreCase("NEAREST_OR_PARITY")){
            String dataLocations = RedisListHandler.getObjectLocations(task.getObjectToRead());
            int relatedHostId = selectNearestHostToRead(dataLocations, deviceLocation);
            //TODO: incorrect parities to read update
            if (task.getParitiesToRead()>0){
                //Get parity objects
                String[] stripeObjects = RedisListHandler.getStripeObjects(task.getStripeID());
                List<String> parityObjects = new ArrayList<String>(Arrays.asList(stripeObjects[1].split(" ")));
                //TODO: for multiple parities mark it if it was read
                //for each parity
                for (String parityObject:parityObjects){
                    String parityHostID = RedisListHandler.getObjectLocations(parityObject);
                    //Locations of parity+data
                    String allObjectLocations = parityHostID + " " + dataLocations;
                    relatedHostId = selectNearestHostToRead(allObjectLocations,deviceLocation);
                    //Check if parity is closer than data
                    if (relatedHostId==Integer.parseInt(parityHostID)){
                        task.setParitiesToRead(task.getParitiesToRead()-1);
                        task.setObjectRead(parityObject);
                        task.setHostID(relatedHostId);

                    }
                }
            }
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }*/
        return selectedVM;
    }

    @Override
    //Oleg: Set type of device to offload. When single tier - it's edge host.
    //TODO: Need both edge and cloud
    public int getDeviceToOffload(Task task) {
        int result = SimSettings.GENERIC_EDGE_DEVICE_ID;
        if(!simScenario.equals("SINGLE_TIER")){
/*            //decide to use cloud or Edge VM
            int CloudVmPicker = SimUtils.getRandomNumber(0, 100);

            if(CloudVmPicker <= SimSettings.getInstance().getTaskLookUpTable()[task.getTaskType()][LoadGeneratorModel.PROB_CLOUD_SELECTION])
                result = SimSettings.CLOUD_DATACENTER_ID;
            else
                result = SimSettings.GENERIC_EDGE_DEVICE_ID;*/
            if(policy.equalsIgnoreCase("CLOUD_OR_NEAREST_IF_CONGESTED")){
                Location deviceLocation = task.getSubmittedLocation();
                NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
                int queueSize = ((StorageNetworkModel)networkModel).getQueueSize(deviceLocation.getServingWlanId());
                int selectedHost = deviceLocation.getServingWlanId();
                //if queue size larger than threshold
                if (queueSize >= SimSettings.getInstance().getCongestedThreshold())
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
}
