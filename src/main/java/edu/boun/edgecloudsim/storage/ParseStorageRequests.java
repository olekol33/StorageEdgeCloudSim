package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimSettings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * This class parses the requests input file.
 */
public class ParseStorageRequests {
    private final int REQUEST_DEVICE_NAME = 0; // object[0]
    private final int REQUEST_TIME = 1; // object[1]
    private final int REQUEST_OBJECT_ID = 2; // object[2]
    private final int REQUEST_IO_TASK_ID = 3; // object[3]
    private final int REQUEST_TASK_PRIORITY = 4; // object[4]
    private final int REQUEST_TASK_DEADLINE = 5; // object[5]

    /**
     * gets a path to a file and parse the requests from it into a vector.
     * @param devicesHashVector contains a hashMap of devices used for checking the correctness of every device name
     *                          parameter imported from the requests input file.
     * @param objectsHashVector contains a hashMap of objects used for checking the correctness of every object ID
     *                          parameter imported from the requests input file.
     * @param file_path contains the path to the requests input file.
     * @return StorageRequests vector holding all the requests imported from the requests input file.
     */
    public Vector<StorageRequest> prepareRequestsVector(HashMap<Integer,String> devicesHashVector, HashMap<String,String> objectsHashVector, String file_path){
        String line;
        String splitLineBy = ",";
        int lineCounter = 1;
        double prevRequestTime = 0;

        Vector<StorageRequest> requestsVector = new Vector<>();

        Map<String, String> reversed_objectsHashVector = new HashMap<>();
        for(Map.Entry<String, String> entry : objectsHashVector.entrySet()){
            reversed_objectsHashVector.put(entry.getValue(), entry.getKey());
        }

        Map<String, Integer> reversed_devicesHashVector = new HashMap<>();
        for(Map.Entry<Integer, String> entry : devicesHashVector.entrySet()){
            reversed_devicesHashVector.put(entry.getValue(), entry.getKey());
        }

        //maps between the conventional name and the original provided one
        try{
            BufferedReader br;
            if(file_path.equals(""))
                br = new BufferedReader(new FileReader("scripts/" + SimSettings.getInstance().getRundir() +
                        "/input_files/requests.csv"));
            else
                br = new BufferedReader(new FileReader(file_path));
            br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                line = line.replace("\"",""); //remove quotes if there are any
                String[] objects = line.split(splitLineBy,-1);

                //check if the deviceName is in the Devices file
                if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
                    boolean checkIfDeviceExists = reversed_devicesHashVector.containsKey(objects[REQUEST_DEVICE_NAME]);
                    if (!checkIfDeviceExists) {
                        throw new Exception("The request is trying to access a non-existing device!! error in line " +
                                lineCounter + "\n" + "The node " + objects[REQUEST_DEVICE_NAME] + " does not exist!!");
                    }
                    boolean checkIfObjectExists = reversed_objectsHashVector.containsKey(objects[REQUEST_OBJECT_ID]);
                    if(!checkIfObjectExists){
                        throw new Exception("The request is trying to access a non-existing Device!! error in line " +
                                lineCounter + "\n" + "The object " + objects[REQUEST_OBJECT_ID] + " does not exist!!");
                    }
                }
                String new_object_name = reversed_objectsHashVector.get(objects[REQUEST_OBJECT_ID]);
                double reqTime = Double.parseDouble(objects[REQUEST_TIME]);
                //checks if the time is in ascending order
                if(reqTime < prevRequestTime)
                    throw new Exception("The requests' time must be in ascending order!! error in line " + lineCounter);

                lineCounter++;
                StorageRequest sRequest;
                //check if the priority or deadline fields are empty
                sRequest = new StorageRequest(objects[REQUEST_DEVICE_NAME],reqTime,
                        new_object_name,Integer.parseInt(objects[REQUEST_IO_TASK_ID]));
                //creat new StorageDevice and add it to the nodes vector
                requestsVector.add(sRequest);
            }
            System.out.println("The requests' vector successfully created!!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return requestsVector;
    }

}//end of class ParseStorageRequests

//This section has been written by Harel
