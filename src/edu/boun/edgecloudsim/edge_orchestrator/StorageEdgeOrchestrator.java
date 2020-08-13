package edu.boun.edgecloudsim.edge_orchestrator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_client.CpuUtilizationModel_Custom;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StorageEdgeOrchestrator extends BasicEdgeOrchestrator {
    public StorageEdgeOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
    }
    //Randomly selects one location from list of location where object exists
    private int randomlySelectHostToOffload(String locations) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        Random random = new Random();
        random.setSeed(ObjectGenerator.seed);
        String randomLocation = objectLocations.get(random.nextInt(objectLocations.size()));
        return Integer.parseInt(randomLocation);
    }

    @Override
    public EdgeVM selectVmOnHost(Task task) {
        EdgeVM selectedVM = null;

        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
        //in our scenasrio, serving wlan ID is equal to the host id
        //because there is only one host in one place
//        int relatedHostId=deviceLocation.getServingWlanId();
        //Oleg: Get location of object according to policy
        int relatedHostId= randomlySelectHostToOffload(RedisListHandler.getObjectLocations(task.getObjectID()));
        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);

        if(policy.equalsIgnoreCase("RANDOM_FIT")){
            int randomIndex = SimUtils.getRandomNumber(0, vmArray.size()-1);
            double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(randomIndex).getVmType());
            double targetVmCapacity = (double)100 - vmArray.get(randomIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
            if(requiredCapacity <= targetVmCapacity)
                selectedVM = vmArray.get(randomIndex);
        }
        else if(policy.equalsIgnoreCase("WORST_FIT")){
            double selectedVmCapacity = 0; //start with min value
            for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                if(requiredCapacity <= targetVmCapacity && targetVmCapacity > selectedVmCapacity){
                    selectedVM = vmArray.get(vmIndex);
                    selectedVmCapacity = targetVmCapacity;
                }
            }
        }
        else if(policy.equalsIgnoreCase("BEST_FIT")){
            double selectedVmCapacity = 101; //start with max value
            for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                if(requiredCapacity <= targetVmCapacity && targetVmCapacity < selectedVmCapacity){
                    selectedVM = vmArray.get(vmIndex);
                    selectedVmCapacity = targetVmCapacity;
                }
            }
        }
        else if(policy.equalsIgnoreCase("FIRST_FIT")){
            for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
                double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(vmIndex).getVmType());
                double targetVmCapacity = (double)100 - vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                if(requiredCapacity <= targetVmCapacity){
                    selectedVM = vmArray.get(vmIndex);
                    break;
                }
            }
        }
        else if(policy.equalsIgnoreCase("NEXT_FIT")){
            int tries = 0;
            while(tries < vmArray.size()){
                lastSelectedVmIndexes[relatedHostId] = (lastSelectedVmIndexes[relatedHostId]+1) % vmArray.size();
                //Oleg: vm_utilization_on_edge
                double requiredCapacity = ((CpuUtilizationModel_Custom)task.getUtilizationModelCpu()).predictUtilization(vmArray.get(lastSelectedVmIndexes[relatedHostId]).getVmType());
                double targetVmCapacity = (double)100 - vmArray.get(lastSelectedVmIndexes[relatedHostId]).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
                if(requiredCapacity <= targetVmCapacity){
                    selectedVM = vmArray.get(lastSelectedVmIndexes[relatedHostId]);
                    break;
                }
                tries++;
            }
        }

        return selectedVM;
    }
}
