package edu.boun.edgecloudsim.storage_advanced;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ParseStorageDevices {

//changed



    public static void csvWrite(HashMap<Integer,String> h) throws Exception{
        try {
            if (h == null) throw new Exception("The HashMap you are trying to export is null!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try(PrintWriter writer = new PrintWriter(new File("C:\\Users\\h3k\\Desktop\\csvs\\Devices_Hash.csv"))){
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
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }
    }

    public HashMap<Integer,String> prepareDevicesVector() throws Exception{
        String line;
        String splitLineBy = ",";
        int lineCounter = 1;
        double firstTimeDeclared = -1;
        boolean ftdFlag = false, declared = false;
        int oldName = -1, newName;

        //create nodes vector
        Vector<StorageDevice> devicesVector = new Vector<StorageDevice>();

        //maps between the conventional name ant the original provided one
        HashMap<Integer,String> map = new HashMap<Integer,String>();
        int mapIndex = 0;
        try{
            BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\h3k\\Desktop\\csvs\\Devices.csv"));
            line = br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                String[] objects = line.split(splitLineBy);

                if(!ftdFlag){
                    firstTimeDeclared = Double.parseDouble(objects[3]);
                    ftdFlag = true;
                }

                //pointless check if device moved before its declaration time
                if(firstTimeDeclared > Double.parseDouble(objects[3])){
                    throw new Exception("The time of " + objects[0] +" cannot be below the " + firstTimeDeclared + " sec threshold!! error in line " + lineCounter);
                }

                declared = false;
                for(Map.Entry m: map.entrySet()){

                    //check if this is a declaration
                    if(firstTimeDeclared == Double.parseDouble(objects[3])){
                        //checks if the current object's name is unique
                        if(objects[0].equals(m.getValue())){
                            //System.out.println("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                            throw new Exception("The device name " + objects[0] + " is not unique!! error in line " + lineCounter);
                        }
                        //declared = true;
                    } else {//it is not a declaration
                        //checks if the current device exists
                        if(objects[0].equals(m.getValue())){
                            declared = true;
                            oldName = (int)m.getKey();
                            break;
                        }
                    }
                }
                //checks if declaration was found to the device
                if((!declared) && (firstTimeDeclared != Double.parseDouble(objects[3]))){
                    throw new Exception("The device name " + objects[0] + " was not declared!! error in line " + lineCounter);
                }

                if((!declared) && (firstTimeDeclared == Double.parseDouble(objects[3]))) {
                    //mapping the objects and renaming them to the convention.
                    map.put(mapIndex, objects[0]);
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
                StorageDevice sDevice = new StorageDevice(newName,Double.parseDouble(objects[1]),Double.parseDouble(objects[2]),Double.parseDouble(objects[3]));
                devicesVector.add(sDevice);
            }
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
        return map;
    }
}
