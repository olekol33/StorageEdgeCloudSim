package edu.boun.edgecloudsim.storage_advanced;

import edu.boun.edgecloudsim.core.SimSettings;

import java.io.*;
import java.util.*;

//changed

public class ParseStorageObject {
    private final int OBJECT_NAME = 0; // object[0]
    private final int OBJECT_SIZE = 1; // object[1]
    private final int OBJECT_LOCATION_VECTOR = 2; // object[2]
    private final int OBJECT_LOCATION_PROB_VECTOR = 3; // object[3]
    private final int OBJECT_CLASS = 4; // object[4]
    private Vector<StorageObject> objectsVector;

    /*
    public static void csvWrite(HashMap<String,String> h){
        try {
            if (h == null) throw new Exception("The HashMap you are trying to export is null!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try(PrintWriter writer = new PrintWriter(new File("scripts/sample_app6/Objects_Hash.csv"))){
            StringBuilder sbTitle = new StringBuilder();
            sbTitle.append("conventional name");
            sbTitle.append(",");
            sbTitle.append("original name");
            sbTitle.append("\n");
            writer.write(sbTitle.toString());
            for(Map.Entry m : h.entrySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(m.getKey());
                sb.append(",");
                sb.append(m.getValue());
                sb.append("\n");
                writer.write(sb.toString());
            }
            System.out.println("The objects have been exported to Objects_Hash.csv successfully!!!");
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }
    }
    */

    public HashMap<String,String> parser(HashMap<Integer,String> nodesHashVector, String file_path){
        String line;
        String splitLineBy = ",";
        String splitVectorBy = " ";
        int lineCounter = 1;

        //TODO: change to exception
        if(nodesHashVector.size() == 0){
            System.out.println("EMPTY!!!");
        }

        //create objects vector
        objectsVector = new Vector<>();

        //maps between the conventional name ant the original provided one
        HashMap<String,String> map = new HashMap<>();
        int mapIndex = 0;
        try{
            if(nodesHashVector.isEmpty()){
                throw new Exception("There are no nodes in the system (nodesHashVector is empty)");
            }
            BufferedReader br;
            if(file_path.equals("")) {
                br = new BufferedReader(new FileReader("scripts/sample_app6/input_files/Objects.csv"));
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
                    throw new Exception("The number of locations does not match the number of probs!! error in line " + lineCounter);
                }

                //checks if the current object's name is unique
                for(Map.Entry<String,String> m: map.entrySet()){
                    if(objects[OBJECT_NAME].equals(m.getValue())){
                        //System.out.println("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                        throw new Exception("The object name " + objects[OBJECT_NAME] + " is not unique!! error in line " + lineCounter);
                    }
                }

                //CHECKS THAT THE LOCATIONS IN THE location LIST MATCH A NODES IN THE NODES FILE!
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
                //System.out.println(objectNewName);
                map.put(objectNewName,objects[OBJECT_NAME]);
                mapIndex++;

                objects[OBJECT_NAME] = objectNewName; //The actually name changing of the object

                double[] lpb = new double[locProbVec.length];
                double sum = 0;

                //check if the locationProvVector is empty
                if(!objects[OBJECT_LOCATION_PROB_VECTOR].equals("")) {
                    for (int i = 0; i < lpb.length; i++) {
                        lpb[i] = Double.parseDouble(locProbVec[i]);
                        sum += lpb[i];
                    }
                }

                //convert the array in to a List
                List<Double> lpv = new ArrayList<>();
                for (double v : lpb) {
                    lpv.add(v);
                }

                //create new storage object and add it to the objects list
                List<String> locationsList = Arrays.asList(locations);
                StorageObject so = new StorageObject(objects[OBJECT_NAME],
                        objects[OBJECT_SIZE],locationsList,lpv,Integer.parseInt(objects[OBJECT_CLASS]));
                //objectsList.add(so);
                objectsVector.add(so);

                //System.out.println("Object Name: " + objects[0] + " Size: " + objects[1] + " Location Vector: "+ objects[2] + " locationProbVector: " + objects[3] + " Class: " + objects[4]);
                //checks if the sum of the probabilities is less then or equal to 1
                if(sum > 1){
                    //System.out.println("The prob sum is more then 1!");
                    throw new Exception("The prob sum is more then 1!! error in line " + lineCounter);
                }
                lineCounter++;
            }
            System.out.println("The objects' list successfully created!!!");
            /*
            System.out.println("Displaying HashMap:");
            for(Map.Entry m: map.entrySet()){
                System.out.println(m.getKey() +" "+m.getValue());
            }*/

            //write the HashMap to a csv file
            //csvWrite(map);
            CsvWrite.csvWriteSS(map,"scripts/sample_app6/hash_tables/Objects_Hash.csv" );
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
        return map;
    }

    public Vector<StorageObject> getObjectsVector() {
        return objectsVector;
    }
}
