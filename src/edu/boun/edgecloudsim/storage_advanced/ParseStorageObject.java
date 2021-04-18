package edu.boun.edgecloudsim.storage_advanced;

import java.io.*;
import java.util.*;

//changed

public class ParseStorageObject {

    public static void csvWrite(HashMap<String,String> h) throws Exception{
        try {
            if (h == null) throw new Exception("The HashMag you are trying to export is null!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try(PrintWriter writer = new PrintWriter(new File("C:\\Users\\h3k\\Desktop\\csvs\\Objects_Hash.csv"))){
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

    public HashMap<String,String> parser(HashMap<Integer,String> nodesHashVector) throws Exception{
        String line;
        String splitLineBy = ",";
        String splitVectorBy = " ";
        int lineCounter = 1;

        //TODO: change to exception
        if(nodesHashVector.size() == 0){
            System.out.println("EMPTY!!!");
        }

        //create objects list
        List<StorageObject> objectsList = new ArrayList<StorageObject>();

        //maps between the conventional name ant the original provided one
        HashMap<String,String> map = new HashMap<String,String>();
        int mapIndex = 0;
        try{
            if(nodesHashVector == null){
                throw new Exception("The nodesHashVector is null!!!");
            }
            if(nodesHashVector.isEmpty()){
                throw new Exception("There are no nodes in the system (nodesHashVector is empty)");
            }
            BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\h3k\\Desktop\\csvs\\Objects.csv"));
            line = br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                String[] objects = line.split(splitLineBy);
                String[] locations = objects[2].split(splitVectorBy);
                String[] locProbVec = objects[3].split(splitVectorBy);

                //check if the locationVector has the same amount of object as the locationProbVector
                if(locations.length != locProbVec.length ||
                        ((objects[3].equals("") && !objects[2].equals("")) || !objects[3].equals("") && objects[2].equals(""))){
                    //System.out.println("The number of locations does not match the number of probs!! error in line " + lineCounter);
                    throw new Exception("The number of locations does not match the number of probs!! error in line " + lineCounter);
                }

                //checks if the current object's name is unique
                for(Map.Entry m: map.entrySet()){
                    if(objects[0].equals(m.getValue())){
                        //System.out.println("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                        throw new Exception("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                    }
                }

                //CHECKS THAT THE LOCATIONS IN THE location LIST MATCH A NODES IN THE NODES FILE!
                boolean checkIfNodeExists = false;
                for(int i = 0; i < locations.length && !objects[3].equals(""); i++){
                    checkIfNodeExists = false;
                    for(Map.Entry m: nodesHashVector.entrySet()){
                        if(m.getValue().equals(locations[i])){
                            checkIfNodeExists = true;
                        }
                    }
                    if(!checkIfNodeExists){
                        throw new Exception("The object " + objects[0] + " is trying to access a non-existing Node!! error in line " + lineCounter + "\n" + "The node " + locations[i] + " does not exist!!");
                    }
                }


                //mapping the objects and renaming them to the convention.
                String objectPrefix = "d";
                String objectNewName = objectPrefix + mapIndex;
                //System.out.println(objectNewName);
                map.put(objectNewName,objects[0]);
                mapIndex++;

                objects[0] = objectNewName; //The actually name changing of the object

                double[] lpb = new double[locProbVec.length];
                double sum = 0;

                //check if the locationProvVector is empty
                if(!objects[3].equals("")) {
                    for (int i = 0; i < lpb.length; i++) {
                        lpb[i] = Double.parseDouble(locProbVec[i]);
                        sum += lpb[i];
                    }
                }

                //convert the array in to a List
                List<Double> lpv = new ArrayList<Double>();
                for(int i = 0; i < lpb.length; i++){
                    lpv.add(lpb[i]);
                }

                //create new storage object and add it to the objects list
                List<String> locationsList = Arrays.asList(locations);
                StorageObject so = new StorageObject(objects[0],objects[1],locationsList,lpv,Integer.parseInt(objects[4]));
                objectsList.add(so);

                //System.out.println("Object Name: " + objects[0] + " Size: " + objects[1] + " Location Vector: "+ objects[2] + " locationProbVector: " + objects[3] + " Class: " + objects[4]);
                //checks if the sum of the probabilities is less then or equal to 1
                if(sum <= 1) {
                    //System.out.println("The sum is: " + sum);
                }else{
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
            csvWrite(map);
        }
        catch (IOException e){
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

}
