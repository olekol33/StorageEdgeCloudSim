package edu.boun.edgecloudsim.storage_advanced;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ParseStorageDevices {
    private final int DEVICE_NAME = 0; // object[0]
    private final int DEVICE_X_POSE = 1; // object[1]
    private final int DEVICE_Y_POSE = 2; // object[2]
    private final int DEVICE_TIME = 3; // object[3]
    private Vector<StorageDevice> devicesVector;
//TODO: check the device in range of nodes (for oleg)

//changed

    /*
    public static void csvWrite(HashMap<Integer,String> h){
        try {
            if (h == null) throw new Exception("The HashMap you are trying to export is null!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try(PrintWriter writer = new PrintWriter(new File("scripts/sample_app6/Devices_Hash.csv"))){
            StringBuilder sbTitle = new StringBuilder();
            sbTitle.append("number");
            sbTitle.append(",");
            sbTitle.append("original name");
            sbTitle.append("\n");
            writer.write(sbTitle.toString());
            int mapIndex = 0;
            while(mapIndex < h.size()){
                StringBuilder sb = new StringBuilder();
                sb.append(mapIndex);
                sb.append(",");
                sb.append(h.get(mapIndex));
                sb.append("\n");
                writer.write(sb.toString());
                mapIndex++;
            }
            System.out.println("The devices have been exported to Devices_Hash.csv successfully!!!");
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }
    }
    */

    public HashMap<Integer,String> prepareDevicesVector(String filePath){
        String line;
        String splitLineBy = ",";
        int lineCounter = 1;
        double firstTimeDeclared = -1;
        boolean ftdFlag = false, declared;
        int oldName = -1, newName;

        //create nodes vector
        devicesVector = new Vector<>();

        //maps between the conventional name ant the original provided one
        HashMap<Integer,String> map = new HashMap<>();
        int mapIndex = 0;
        try{
            BufferedReader br;
            if(filePath.equals("")) {
                br = new BufferedReader(new FileReader("scripts/sample_app6/input_files/Devices.csv"));
            }else{
                br = new BufferedReader(new FileReader(filePath));
            }
            br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                String[] objects = line.split(splitLineBy);

                if(!ftdFlag){
                    firstTimeDeclared = Double.parseDouble(objects[DEVICE_TIME]);
                    ftdFlag = true;
                }

                //pointless check if device moved before its declaration time
                if(firstTimeDeclared > Double.parseDouble(objects[DEVICE_TIME])){
                    throw new Exception("The time of " + objects[DEVICE_NAME] +" cannot be below the " +
                            firstTimeDeclared + " sec threshold!! error in line " + lineCounter);
                }

                declared = false;
                for(Map.Entry<Integer,String> m: map.entrySet()){

                    //check if this is a declaration
                    if(firstTimeDeclared == Double.parseDouble(objects[DEVICE_TIME])){
                        //checks if the current object's name is unique
                        if(objects[DEVICE_NAME].equals(m.getValue())){
                            //System.out.println("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                            throw new Exception("The device name " + objects[DEVICE_NAME] + " is not unique!! error in line " + lineCounter);
                        }
                        //declared = true;
                    } else {//it is not a declaration
                        //checks if the current device exists
                        if(objects[DEVICE_NAME].equals(m.getValue())){
                            declared = true;
                            oldName = m.getKey();
                            break;
                        }
                    }
                }
                //checks if declaration was found to the device
                if((!declared) && (firstTimeDeclared != Double.parseDouble(objects[DEVICE_TIME]))){
                    throw new Exception("The device name " + objects[DEVICE_NAME] + " was not declared!! error in line " + lineCounter);
                }

                if((!declared) && (firstTimeDeclared == Double.parseDouble(objects[DEVICE_TIME]))) {
                    //mapping the objects and renaming them to the convention.
                    map.put(mapIndex, objects[DEVICE_NAME]);
                    mapIndex++;
                    oldName = -1;
                }

                //System.out.println("Object Name: " + objects[0] + " Size: " + objects[1] + " Location Vector: "+ objects[2] + " locationProbVector: " + objects[3] + " Class: " + objects[4]);
                lineCounter++;


                if(oldName == -1){
                    newName = mapIndex-1;
                } else {
                    newName = oldName;
                }

                //creat new StorageDevice and add it to the nodes vector
                StorageDevice sDevice = new StorageDevice(newName,Double.parseDouble(objects[DEVICE_X_POSE]),
                        Double.parseDouble(objects[DEVICE_Y_POSE]),Double.parseDouble(objects[DEVICE_TIME]));
                devicesVector.add(sDevice);
            }
            System.out.println("The devices' vector successfully created!!!");
        /*
        System.out.println("Displaying HashMap:");
        for(Map.Entry m: map.entrySet()){
            System.out.println(m.getKey() +" "+m.getValue());
        }*/

            //write the HashMap to a csv file
            //csvWrite(map);
            CsvWrite.csvWriteIS(map, "scripts/sample_app6/hash_tables/Devices_Hash.csv");

        }
        catch (Exception e){
            e.printStackTrace();
        }
        return map;
    }

    public Vector<StorageDevice> getDevicesVector() {
        return devicesVector;
    }
}
