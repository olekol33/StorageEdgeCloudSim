package edu.boun.edgecloudsim.storage_advanced;

import edu.boun.edgecloudsim.core.SimSettings;

import java.util.HashMap;
import java.util.Vector;

/**
 * This is a unit-test class.
 * Its purpose is to check the modules responsible for parsing and processing the input files.
 * IT IS NOT part of the simulator.
 */
public class PrepareData {
    public static void main(String[] args){
        try {
            ParseStorageNodes p1 = new ParseStorageNodes();
            HashMap<Integer,String> nodesHashVector =
                    p1.prepareNodesHashVector(SimSettings.getInstance().getPathOfNodesFile());

            ParseStorageDevices d1 = new ParseStorageDevices();
            HashMap<Integer,String> devicesHashVector =
                    d1.prepareDevicesVector(SimSettings.getInstance().getPathOfDevicesFile());

            ParseStorageObject p2 = new ParseStorageObject();
            HashMap<String,String> objectsHashVector =
                    p2.prepareObjectsHashVector(nodesHashVector, SimSettings.getInstance().getPathOfObjectsFile());

            ParseStorageRequests r1 = new ParseStorageRequests();
            Vector<StorageRequest> storageRequests =
                r1.prepareRequestsVector(devicesHashVector, objectsHashVector, SimSettings.getInstance().getPathOfRequestsFile());
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
