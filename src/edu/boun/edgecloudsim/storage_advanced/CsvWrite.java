package edu.boun.edgecloudsim.storage_advanced;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class CsvWrite {

    public CsvWrite() {}

    public static void csvWriteIS(HashMap<Integer,String> map, String filePath){
        try {
            if (map == null) throw new Exception("The HashMap you are trying to export is null!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        assert (map != null);
        //try(PrintWriter writer = new PrintWriter(new File("scripts/sample_app6/Nodes_Hash.csv"))){
        try(PrintWriter writer = new PrintWriter(filePath)){
            String sbTitle = "number" +
                    "," +
                    "original name" +
                    "\n";
            writer.write(sbTitle);
            int mapIndex = 0;
            while(mapIndex < map.size()){
                String sb = mapIndex +
                        "," +
                        map.get(mapIndex) +
                        "\n";
                writer.write(sb);
                mapIndex++;
            }//TODO: change!
            System.out.println("The nodes have been exported to Nodes_Hash.csv successfully!!!");
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }
    }

    public static void csvWriteSS(HashMap<String,String> map, String filePath){
        try {
            if (map == null) throw new Exception("The HashMap you are trying to export is null!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        assert (map != null);
        //try(PrintWriter writer = new PrintWriter(new File("scripts/sample_app6/Objects_Hash.csv"))){
        try(PrintWriter writer = new PrintWriter(filePath)){
            String sbTitle = "new name" +
                    "," +
                    "original name" +
                    "\n";
            writer.write(sbTitle);
            for(Map.Entry m : map.entrySet()) {
                String sb = m.getKey() +
                        "," +
                        m.getValue() +
                        "\n";
                writer.write(sb);
            }
            System.out.println("The objects have been exported to Objects_Hash.csv successfully!!!");
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }
    }
}
