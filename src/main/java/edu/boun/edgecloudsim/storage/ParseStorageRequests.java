package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;

import java.io.*;
import java.util.*;

/**
 * This class parses the requests input file.
 */
public class ParseStorageRequests {
    private final int[] reqsToHostPerSec = new int[SimSettings.getInstance().getNumOfEdgeDatacenters()];
    private final double utilization = SimSettings.getInstance().getAvoidSpikesUtilization();
    private final int REQUEST_DEVICE_NAME = 0; // object[0]
    private final int REQUEST_TIME = 1; // object[1]
    private final int REQUEST_OBJECT_ID = 2; // object[2]
    private final int REQUEST_IO_TASK_ID = 3; // object[3]
    private final int REQUEST_TASK_PRIORITY = 4; // object[4]
    private final int REQUEST_TASK_DEADLINE = 5; // object[5]

    private int lastSpikeTrimmerCheckpoint=0;
    private final int throwOverfilledSecToSimEnd=30;
    int queueFactor = 1000;
    private final int intervalSizeSec = 1;
    private final int readRate = (int) (SimSettings.getInstance().getServedReqsPerSec() * utilization * intervalSizeSec);

    private int maxSpikesQueueSize = (int)SimSettings.getInstance().getSimulationTime()*queueFactor;

    private int requestsDiscarded = 0;

    private int ioTaskIdCounter = 0;
    private final Queue<StorageRequest> queuedRequests = new LinkedList<>(); //FIFO

    private final Vector<StorageRequest> requestsVector = new Vector<>();

    Random rnd = new Random(SimSettings.getInstance().getRandomSeed());

    /**
     * gets a path to a file and parse the requests from it into a vector.
     * @param devicesHashVector contains a hashMap of devices used for checking the correctness of every device name
     *                          parameter imported from the requests input file.
     * @param objectsHashVector contains a hashMap of objects used for checking the correctness of every object ID
     *                          parameter imported from the requests input file.
     * @param file_path contains the path to the requests input file.
     * @return StorageRequests vector holding all the requests imported from the requests input file.
     */
    public Vector<StorageRequest> prepareRequestsVector(HashMap<Integer,String> devicesHashVector,
                                                        HashMap<String,String> objectsHashVector, String file_path){
        String line;
        String splitLineBy = ",";
        int lineCounter = 1;
        double prevRequestTime = 0;
        double firstReqTime = 0;
        int[] req2ObjectsCount = new int[objectsHashVector.size()];
        HashMap<Integer, ArrayList<String>> object2DevicesHash = initObject2DevicesHash(objectsHashVector.size());


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
                String objectName = reversed_objectsHashVector.get(objects[REQUEST_OBJECT_ID]);
                double reqTime = Double.parseDouble(objects[REQUEST_TIME]);
                //checks if the time is in ascending order
                if(reqTime < prevRequestTime)
                    throw new Exception("The requests' time must be in ascending order!! error in line " + lineCounter);

                lineCounter++;

                if (SimSettings.getInstance().isSmoothExternalRequests()) {
                    countObjectRequest(objectName, req2ObjectsCount, object2DevicesHash,
                            objects[REQUEST_DEVICE_NAME]);
                    if (Integer.parseInt(objects[REQUEST_IO_TASK_ID]) == 0)
                        firstReqTime = reqTime;
                }
                else if (SimSettings.getInstance().isAvoidSpikesInExternalRequests()){
                    StorageRequest sRequest = new StorageRequest(objects[REQUEST_DEVICE_NAME], reqTime,
                            objectName, Integer.parseInt(objects[REQUEST_IO_TASK_ID]));
                    handleSpikes(sRequest, reqTime, objects, objectName);
                }
                else {
                    StorageRequest sRequest;
                    sRequest = new StorageRequest(objects[REQUEST_DEVICE_NAME], reqTime,
                            objectName, Integer.parseInt(objects[REQUEST_IO_TASK_ID]));
                    requestsVector.add(sRequest);
                }
            }
            System.out.println("The requests' vector successfully created!!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        if (SimSettings.getInstance().isSmoothExternalRequests())
            return createSmoothRequestVector(object2DevicesHash, req2ObjectsCount, firstReqTime);
        if(SimSettings.getInstance().isAvoidSpikesInExternalRequests()){
            int numOfDiscarded = requestsDiscarded  + queuedRequests.size();
            System.out.println("Discarded requests = " + numOfDiscarded);
            writeNumOfDiscardedToFile(numOfDiscarded, requestsVector.size()-numOfDiscarded);
        }
        return requestsVector;
    }

    private void writeNumOfDiscardedToFile(int numOfDiscarded, int totalInputRequests){
        try {
            File serviceRatePath = SimSettings.getInstance().getServiceratePath();
            String filename = "discarded_requests.csv";
            File serviceRateFile = new File(serviceRatePath, filename);
            FileWriter serviceRateFileFW = new FileWriter(serviceRateFile, true);
            BufferedWriter serviceRateFileBW = new BufferedWriter(serviceRateFileFW);
            SimLogger.appendToFile(serviceRateFileBW, String.valueOf(numOfDiscarded) + "," + String.valueOf(totalInputRequests));
            serviceRateFileBW.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void handleSpikes(StorageRequest sRequest, double reqTime, String[] objects, String objectName){
        if (Integer.parseInt(objects[REQUEST_IO_TASK_ID]) == 0)
            lastSpikeTrimmerCheckpoint = (int) reqTime;
        if(reqTime >= lastSpikeTrimmerCheckpoint + intervalSizeSec){
            lastSpikeTrimmerCheckpoint += intervalSizeSec;
            Arrays.fill(reqsToHostPerSec, 0);
        }
        int objectID = Integer.parseInt(objectName.substring(1));
        int objectLocation = SimManager.getInstance().getObjectGenerator().getOriginalDataObjLocation(objectID);
        if(reqsToHostPerSec[objectLocation] >= readRate) {
            queuedRequests.add(sRequest);
            return;
        }
        else {
            reqsToHostPerSec[objectLocation]++;
            sRequest.setIoTaskID(ioTaskIdCounter);
            sRequest.setTime(reqTime);
            requestsVector.add(sRequest);
            ioTaskIdCounter++;
        }

//        while(attemptToDequeueRequest(reqTime)){
//        }
    }

    private boolean attemptToDequeueRequest(double reqTime){
        if(queuedRequests.size() == 0)
            return false;
        StorageRequest topRequest = queuedRequests.peek();
        int objectID = Integer.parseInt(topRequest.getObjectID().substring(1));
        int objectLocation = SimManager.getInstance().getObjectGenerator().getOriginalDataObjLocation(objectID);
        boolean fullQandNotFromThisInterval = (queuedRequests.size() > maxSpikesQueueSize) &&
                (Math.floor(queuedRequests.peek().getTime()) + intervalSizeSec -1 < Math.floor(reqTime));
        boolean isTimeToDiscardRequests = reqTime > (SimSettings.getInstance().getSimulationTime() -
                throwOverfilledSecToSimEnd);
        if (reqsToHostPerSec[objectLocation] < readRate) {
            reqsToHostPerSec[objectLocation]++;
            StorageRequest sRequest = queuedRequests.remove();
            sRequest.setTime(requestsVector.lastElement().getTime() + 0.00001);
            sRequest.setIoTaskID(ioTaskIdCounter);
            requestsVector.add(sRequest);
            ioTaskIdCounter++;
            return true;
        }
        else if (isTimeToDiscardRequests){
            moveToBackOfQueue(); //in a request was inserted in this iteration
            queuedRequests.remove();
            requestsDiscarded++;
            return false;
        }
        else {
            moveToBackOfQueue();
            return false;
        }
    }
    private void moveToBackOfQueue(){
        StorageRequest stuckRequest = queuedRequests.remove();
        queuedRequests.add(stuckRequest);
    }



    private HashMap<Integer, ArrayList<String>> initObject2DevicesHash(int numOfObjects){
        HashMap<Integer, ArrayList<String>> object2DevicesHash = new HashMap<>();
        if (!SimSettings.getInstance().isSmoothExternalRequests())
            return object2DevicesHash;
        for (int i=0; i<numOfObjects; i++)
            object2DevicesHash.put(i, new ArrayList<String>());
        return object2DevicesHash;
    }

    private void countObjectRequest(String objectName, int[] req2ObjectsCount,
                                    HashMap<Integer, ArrayList<String>> object2DevicesHash, String deviceName){
        int objectID = Integer.parseInt(objectName.substring(1));
        req2ObjectsCount[objectID]++;
        ArrayList<String> devices =  object2DevicesHash.get(objectID);
        devices.add(deviceName);
    }

    private double[] getMeanTimeBetweenObjectReqs(int[] req2ObjectsCount, double firstReqTime){
        double[] objectReqTimes = Arrays.stream(req2ObjectsCount).asDoubleStream().toArray();
        double runTime = SimSettings.getInstance().getSimulationTime() - firstReqTime;
        Arrays.setAll(objectReqTimes, i -> runTime / objectReqTimes[i]);
        return objectReqTimes;
    }

    private Vector<StorageRequest> createSmoothRequestVector(HashMap<Integer, ArrayList<String>> object2DevicesHash,
                                                             int[] req2ObjectsCount, double firstReqTime){
        SimLogger.printLine("Smoothing external requests vector");
        Vector<StorageRequest> requestsVector = new Vector<>();
        ArrayList<Map.Entry<Integer, Double>> objectRequests = createRequestedObjectsList(req2ObjectsCount, firstReqTime);
        prepareDevicesLists(object2DevicesHash);
        int ioTaskID = 0;
        for (Map.Entry<Integer, Double> objReq : objectRequests){
            int objectID = objReq.getKey();
            String objectName = "d" + String.valueOf(objectID);
            double t = objReq.getValue();
            String device = pop(object2DevicesHash.get(objectID));
            StorageRequest sRequest;
            sRequest = new StorageRequest(device, t,objectName , ioTaskID);
            requestsVector.add(sRequest);
            ioTaskID++;
        }
        return requestsVector;
    }

    private String pop(ArrayList<String> deviceList){
        return deviceList.remove(deviceList.size() - 1);
    }

    private void prepareDevicesLists(HashMap<Integer, ArrayList<String>> object2DevicesHash){
        double additionalListSize = 0.01;
        for (Map.Entry<Integer, ArrayList<String>> entry : object2DevicesHash.entrySet() ){
            ArrayList<String> devices = entry.getValue();
            Collections.shuffle(devices, rnd);
            int listSize = devices.size();
            int additionalDevices = (int) (listSize * additionalListSize);
            for (int i=0; i<additionalDevices; i++)
                devices.add(devices.get(rnd.nextInt(listSize)));
        }
    }

    private ArrayList<Map.Entry<Integer, Double>> createRequestedObjectsList(int[] req2ObjectsCount, double firstReqTime){
        ArrayList<Map.Entry<Integer, Double>> objectRequests = new ArrayList<>();
        double[] objectReqT = getMeanTimeBetweenObjectReqs(req2ObjectsCount, firstReqTime);
        for (int i=0; i<objectReqT.length; i++){
            double t = firstReqTime;
            while (t + objectReqT[i] < SimSettings.getInstance().getSimulationTime()){
                double randomTimeOffset = rnd.nextDouble() * 0.002 -0.001; //random double between -0.001 and 0.001
                t += objectReqT[i] + randomTimeOffset;
                Map.Entry<Integer, Double> newReq = new AbstractMap.SimpleImmutableEntry<>(i, t);
                objectRequests.add(newReq);
            }
        }
        sortListOfObjects(objectRequests);
        return objectRequests;
    }

    private void sortListOfObjects(ArrayList<Map.Entry<Integer, Double>> objectRequests){
        Collections.sort(objectRequests, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
    }

//    private void smoothExternalRequests()

}//end of class ParseStorageRequests

//This section has been written by Harel
