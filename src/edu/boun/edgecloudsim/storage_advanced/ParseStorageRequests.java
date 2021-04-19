package edu.boun.edgecloudsim.storage_advanced;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ParseStorageRequests {
    private final int REQUEST_DEVICE_NAME = 0; // object[0]
    private final int REQUEST_TIME = 1; // object[1]
    private final int REQUEST_OBJECT_ID = 2; // object[2]
    private final int REQUEST_IO_TASK_ID = 3; // object[3]
    private final int REQUEST_TASK_PRIORITY = 4; // object[4]
    private final int REQUEST_TASK_DEADLINE = 5; // object[5]

    public Vector<StorageRequest> prepareRequests(HashMap<Integer,String> nodesHashVector, HashMap<String,String> objectsHashVector) throws Exception{
        String line;
        String splitLineBy = ",";
        int lineCounter = 1;
        double prevRequestTime = 0;
        //boolean ftdFlag = false, declared = false;
        //int oldName = -1, newName;

        //create nodes vector
        Vector<StorageRequest> requestsVector = new Vector<StorageRequest>();

        //maps between the conventional name ant the original provided one
        //HashMap<Integer,String> map = new HashMap<Integer,String>();
        //int mapIndex = 0;
        try{
            BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\h3k\\Desktop\\csvs\\Requests.csv"));
            line = br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                String[] objects = line.split(splitLineBy,-1);

                //check if the deviceName is in the Devices file
                boolean checkIfDeviceExists = false;
                checkIfDeviceExists = false;
                for(Map.Entry m: nodesHashVector.entrySet()){
                    if(m.getValue().equals(objects[REQUEST_DEVICE_NAME])){
                        checkIfDeviceExists = true;
                        break;
                    }
                }
                if(!checkIfDeviceExists){
                    throw new Exception("The request is trying to access a non-existing Node!! error in line " + lineCounter + "\n" + "The node " + objects[REQUEST_DEVICE_NAME] + " does not exist!!");
                }


                //TODO: check if the objectID is in the Objects file
                boolean checkIfObjectExists = false;
                checkIfObjectExists = false;
                for(Map.Entry m: objectsHashVector.entrySet()){
                    if(m.getValue().equals(objects[REQUEST_OBJECT_ID])){
                        checkIfObjectExists = true;
                        break;
                    }
                }
                if(!checkIfObjectExists){
                    throw new Exception("The request is trying to access a non-existing Node!! error in line " + lineCounter + "\n" + "The object " + objects[REQUEST_OBJECT_ID] + " does not exist!!");
                }



                //checks if the time is in ascending order
                if(Double.parseDouble(objects[REQUEST_TIME]) < prevRequestTime){
                    throw new Exception("The requests' time must be in ascending order!! error in line " + lineCounter);
                }

                //System.out.println("The length of the objects array is: " + objects.length);

                //System.out.println("Object Name: " + objects[0] + " Size: " + objects[1] + " Location Vector: "+ objects[2] + " locationProbVector: " + objects[3] + " Class: " + objects[4]);
                lineCounter++;


                StorageRequest sRequest;
                //check if the priority or deadline fields are empty
                if(!objects[REQUEST_TASK_PRIORITY].equals("") && !objects[REQUEST_TASK_DEADLINE].equals("")){//both are not empty
                    sRequest = new StorageRequest(objects[REQUEST_DEVICE_NAME],Double.parseDouble(objects[REQUEST_TIME]),objects[REQUEST_OBJECT_ID],Integer.parseInt(objects[REQUEST_IO_TASK_ID]),Integer.parseInt(objects[REQUEST_TASK_PRIORITY]),Double.parseDouble(objects[REQUEST_TASK_DEADLINE]));
                } else if(objects[REQUEST_TASK_PRIORITY].equals("") && objects[REQUEST_TASK_DEADLINE].equals("")){//both are empty
                    sRequest = new StorageRequest(objects[REQUEST_DEVICE_NAME],Double.parseDouble(objects[REQUEST_TIME]),objects[REQUEST_OBJECT_ID],Integer.parseInt(objects[REQUEST_IO_TASK_ID]));
                } else if(objects[REQUEST_TASK_PRIORITY].equals("")){
                    sRequest = new StorageRequest(objects[REQUEST_DEVICE_NAME],Double.parseDouble(objects[REQUEST_TIME]),objects[REQUEST_OBJECT_ID],Integer.parseInt(objects[REQUEST_IO_TASK_ID]),Double.parseDouble(objects[REQUEST_TASK_DEADLINE]));
                } else{
                    sRequest = new StorageRequest(objects[REQUEST_DEVICE_NAME],Double.parseDouble(objects[REQUEST_TIME]),objects[REQUEST_OBJECT_ID],Integer.parseInt(objects[REQUEST_IO_TASK_ID]),Integer.parseInt(objects[REQUEST_TASK_PRIORITY]));
                }

                //creat new StorageDevice and add it to the nodes vector
                //StorageRequest sRequest = new StorageRequest(objects[0],Double.parseDouble(objects[1]),objects[2],Integer.parseInt(objects[3]),Integer.parseInt(objects[4]),Double.parseDouble(objects[5]));
                requestsVector.add(sRequest);
            }
            System.out.println("The requests' vector successfully created!!!");
        /*
        System.out.println("Displaying HashMap:");
        for(Map.Entry m: map.entrySet()){
            System.out.println(m.getKey() +" "+m.getValue());
        }*/

        }
        catch (IOException e){
            e.printStackTrace();
        }
        return requestsVector;
    }
}
