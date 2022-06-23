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
                    //System.out.println("The number of locations does not match the number of probs!! error in line " + lineCounter);
                    throw new Exception("The number of locations does not match the number of probabilities!! error in line " + lineCounter);
                }

                //checks if the current object's name is unique
/*                for(Map.Entry<String,String> m: objectMap.entrySet()){
                    if(objects[OBJECT_NAME].equals(m.getValue())){
                        //System.out.println("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                        throw new Exception("The object name " + objects[OBJECT_NAME] + " is not unique!! error in line " + lineCounter);
                    }
                }*/

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



            System.out.println("The objects' list successfully created!!!");
            /*
            System.out.println("Displaying HashMap:");
            for(Map.Entry m: map.entrySet()){
                System.out.println(m.getKey() +" "+m.getValue());
            }*/

            //write the HashMap to a csv file
            CsvWrite.csvWriteSS(objectMap,"scripts/" + SimSettings.getInstance().getRundir() +"/hash_tables/Objects_Hash.csv" );
        }
        catch (Exception e){
            e.printStackTrace();
        }
/*
        //Display the objects list
        Iterator<storageObject> iter = objectsList.iterator();
        while (iter.hasNext()){
            storageObject s = iter.next();
            System.out.println("Name: " + s.getObjName() + " Size: " + s.getObjSize());
            System.out.println("Printing locations list");
            s.getObjLocations().forEach(System.out::println);
            System.out.println("Printing prob list");
            s.getObjLocationsProb().forEach(System.out::println);
        }
*/
        return objectMap;
    }//end of prepareObjectsHashVector

    public Vector<StorageObject> getObjectsVector() {
        return objectsVector;
    }

}//end of class ParseStorageObjects

//This section has been written by Harel
