package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimSettings;

import java.io.*;
import java.util.*;

//changed

/**
 * This class parses the objects input file.
 */
public class ParseStorageObject {
    private final int OBJECT_NAME = 0; // object[0]
    private final int OBJECT_SIZE = 1; // object[1]
    private final int OBJECT_LOCATION_VECTOR = 2; // object[2]
    private final int OBJECT_LOCATION_PROB_VECTOR = 3; // object[3]
    private final int OBJECT_CLASS = 4; // object[4]
    private Vector<StorageObject> objectsVector;
    private HashMap<String, StorageObject> objectHash;


    /**
     * gets a path to a file and parse the objects from it into a vector.
     * in edition, creates hash map between the names of the objects in the vector, and the original names from the file.
     * @param nodesHashVector contains a hashMap of nodes that used to check the correctness of the data imported
     *                        from the objects input file.
     * @param file_path contains the path to the objects input file.
     * @return hash map contains a mapping between the new name (key) to the original name (value) from the input file.
     */
    public HashMap<String,String> prepareObjectsHashVector(HashMap<Integer,String> nodesHashVector, String file_path){
        String line;
        String splitLineBy = ",";
        String splitVectorBy = " ";
        int lineCounter = 1;
        int mapIndex = 0;

        objectsVector = new Vector<>();
        objectHash = new HashMap<>();

        //maps between the conventional name and the original provided one
        HashMap<String,String> objectMap = new HashMap<>();

        try{
            if(nodesHashVector.isEmpty()){
                throw new Exception("There are no nodes in the system (nodesHashVector is empty)");
            }
            BufferedReader br;
            if(file_path.equals("")) {
                br = new BufferedReader(new FileReader("scripts/" + SimSettings.getInstance().getRundir() +"/input_files/objects.csv"));
            }else{
                br = new BufferedReader(new FileReader(file_path));
            }
            br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                String[] objects = line.split(splitLineBy);
                String[] locations = objects[OBJECT_LOCATION_VECTOR].split(splitVectorBy);
                String[] locProbVec = objects[OBJECT_LOCATION_PROB_VECTOR].split(splitVectorBy);

                //check if the locationVector has the same amount of object as the locationProbVector
                if(locations.length != locProbVec.length ||
                        ((objects[OBJECT_LOCATION_PROB_VECTOR].equals("") &&
                                !objects[OBJECT_LOCATION_VECTOR].equals("")) ||
                                !objects[OBJECT_LOCATION_PROB_VECTOR].equals("") &&
                                        objects[OBJECT_LOCATION_VECTOR].equals(""))){
                    throw new Exception("The number of locations does not match the number of probabilities!! error in line " + lineCounter);
                }

                //checks that the locations in the location list match a nodes in the nodes list
                boolean checkIfNodeExists;
                for(int i = 0; i < locations.length && !objects[OBJECT_LOCATION_PROB_VECTOR].equals(""); i++){
                    checkIfNodeExists = false;
                    for(Map.Entry<Integer,String> m: nodesHashVector.entrySet()){
                        if (m.getValue().equals(locations[i])) {
                            checkIfNodeExists = true;
                            break;
                        }
                    }
                    if(!checkIfNodeExists){
                        throw new Exception("The object " + objects[OBJECT_NAME] +
                                " is trying to access a non-existing Node!! error in line " +
                                lineCounter + "\n" + "The node " + locations[i] + " does not exist!!");
                    }
                }


                //mapping the objects and renaming them to the convention.
                String objectPrefix = "d";
                String objectNewName = objectPrefix + mapIndex;
                objectMap.put(objectNewName,objects[OBJECT_NAME]);
                mapIndex++;

                objects[OBJECT_NAME] = objectNewName; //The actual name changing of the object

                double[] lpb = new double[locProbVec.length];
                double sum = 0;

                //check if the locationProvVector is empty
                if(!objects[OBJECT_LOCATION_PROB_VECTOR].equals("")) {
                    for (int i = 0; i < lpb.length; i++) {
                        lpb[i] = Double.parseDouble(locProbVec[i]);
                        sum += lpb[i];
                    }
                }

                //convert the array in to a List - may be redundant
                List<Double> lpv = new ArrayList<>();
                for (double v : lpb) {
                    lpv.add(v);
                }

                //create new storage object and add it to the objects list
                List<String> locationsList = Arrays.asList(locations);
                StorageObject so = new StorageObject(objects[OBJECT_NAME],
                        objects[OBJECT_SIZE],locationsList,lpv,Integer.parseInt(objects[OBJECT_CLASS]));
                objectsVector.add(so);
                objectHash.put(objects[OBJECT_NAME], so);

                //System.out.println("Object Name: " + objects[0] + " Size: " + objects[1] + " Location Vector: "+ objects[2] + " locationProbVector: " + objects[3] + " Class: " + objects[4]);

                //checks if the sum of the probabilities is less then or equal to 1
                if(sum > 1){
                    //System.out.println("The prob sum is more then 1!");
                    throw new Exception("The prob sum is more then 1!! error in line " + lineCounter);
                }
                lineCounter++;
            }
            //check duplicacies in objects
            // check size of both collections; if unequal, you have duplicates
            List<String>  valuesList = new ArrayList(objectMap.values());
            Set<String> valuesSet = new HashSet<String>(objectMap.values());
            if(valuesList.size() != valuesSet.size())
                throw new IllegalStateException("Duplicacies in objectMap");
            if (isInvalidNumOfObjects(objectsVector.size())) {
                addObjectsToResolve(objectMap);
            }


            System.out.println("The objects' list successfully created!!!");

            //write the HashMap to a csv file
            CsvWrite.csvWriteSS(objectMap,"scripts/" + SimSettings.getInstance().getRundir() +"/hash_tables/Objects_Hash.csv" );
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return objectMap;
    }//end of prepareObjectsHashVector

    private void addObjectsToResolve(HashMap<String,String> objectMap){
        int numOfObjectsToAdd = getNumOfObjectsToAdd();
        for (int i=0; i < numOfObjectsToAdd; i++) {
            String objectName = addFakeObject();
            objectMap.put(objectName, "FAKE_" + String.valueOf(i));
        }
    }

    private int getNumOfObjectsToAdd(){
        int originalNumOfObjects = objectsVector.size();
        int currentNumOfObjects = originalNumOfObjects;
        while(isInvalidNumOfObjects(currentNumOfObjects)){
            currentNumOfObjects += 1;
        }
        return currentNumOfObjects - originalNumOfObjects;
    }

    private String addFakeObject(){
        StorageObject newObject = objectsVector.lastElement().clone();
        int objectID = Integer.parseInt(newObject.getObjName().substring(1)) + 1;
        String objectName = "d" + String.valueOf(objectID);
        newObject.setObjName(objectName);
        objectsVector.add(newObject);
        objectHash.put(objectName, newObject);
        SimSettings.getInstance().setNumOfDataObjects(objectsVector.size());
        return objectName;
    }

    private boolean isInvalidNumOfObjects(double numOfDataObjects){
        double numOfObjects = numOfDataObjects * (SimSettings.getInstance().getOverhead() - 1);
        if (numOfObjects != Math.ceil(numOfObjects) || numOfObjects != Math.floor(numOfObjects))
            return true;
        else
            return false;
    }

    public Vector<StorageObject> getObjectsVector() {
        return objectsVector;
    }

    public HashMap<String, StorageObject> getObjectHash() {
        return objectHash;
    }


}//end of class ParseStorageObjects

//This section has been written by Harel
