package edu.boun.edgecloudsim.storage_advanced;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

//changed

public class ParseStorageNodes {
    private final int NODE_NAME = 0; // object[0]
    private final int NODE_X_POSE = 1; // object[1]
    private final int NODE_Y_POSE = 2; // object[2]
    private final int NODE_SERVICE_CLASS = 3; // object[3]
    private final int NODE_CAPACITY = 4; // object[4]
    private final int NODE_SERVICE_RATE = 5; // object[5]

    public static void csvWrite(HashMap<Integer,String> h) throws Exception{
        try {
            if (h == null) throw new Exception("The HashMag you are trying to export is null!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try(PrintWriter writer = new PrintWriter(new File("C:\\Users\\h3k\\Desktop\\csvs\\Nodes_Hash.csv"))){
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
            System.out.println("The nodes have been exported to Nodes_Hash.csv successfully!!!");
        }
        catch(FileNotFoundException e){
            System.out.println(e.getMessage());
        }
    }

    public static void xmlWrite(Vector<StorageNode> nodesVector){
        try{
            FileWriter writer = new FileWriter("C:\\Users\\h3k\\Desktop\\csvs\\edge_devices.xml");
            writer.write("<?xml version=\"1.0\"?>\n");
            writer.write("<edge_devices>\n");
            int vecIndex = 0;
            while(vecIndex < nodesVector.size()) {
                StorageNode s = nodesVector.get(vecIndex++);

                /*
                writer.write("\t<datacenter arch=\"x86\" os=\"Linux\" vmm=\"Xen\">\n");
                writer.write("\t\t<costPerBw>0.1</costPerBw>\n");
                writer.write("\t\t<costPerSec>3.0</costPerSec>\n");
                writer.write("\t\t<costPerMem>0.05</costPerMem>\n");
                writer.write("\t\t<costPerStorage>0.1</costPerStorage>\n");
                writer.write("\t\t<location>\n");
                writer.write("\t\t\t<x_pos>" +String.valueOf(s.getxPos())+ "</x_pos>\n");
                writer.write("\t\t\t<y_pos>" +String.valueOf(s.getyPos())+ "</y_pos>\n");
                writer.write("\t\t\t<wlan_id>" +String.valueOf(s.getNodeName())+ "</wlan_id>\n");
                writer.write("\t\t\t<attractiveness>1</attractiveness>\n");
                writer.write("\t\t</location>\n");
                writer.write("\t\t<hosts>\n");
                writer.write("\t\t\t<host>\n");
                writer.write("\t\t\t\t<core>16</core>\n");
                writer.write("\t\t\t\t<mips>80000</mips>\n");
                writer.write("\t\t\t\t<ram>16000</ram>\n");
                writer.write("\t\t\t\t<storage>" + String.valueOf(s.getCapacity()) + "</storage>\n");
                writer.write("\t\t\t\t<readRate>" + String.valueOf(s.getServiceRate()) + "</readRate>\n");
                writer.write("\t\t\t\t<VMs>\n");
                writer.write("\t\t\t\t\t<VM vmm=\"Xen\">\n");
                writer.write("\t\t\t\t\t\t<core>16</core>\n");
                writer.write("\t\t\t\t\t\t<mips>80000</mips>\n");
                writer.write("\t\t\t\t\t\t<ram>16000</ram>\n");
                writer.write("\t\t\t\t\t\t<storage>" + String.valueOf(s.getCapacity()) + "</storage>\n");
                writer.write("\t\t\t\t\t\t<readRate>" + String.valueOf(s.getServiceRate()) + "</readRate>\n");
                writer.write("\t\t\t\t\t</VM>\n");
                writer.write("\t\t\t\t</VMs>\n");
                writer.write("\t\t\t</host>\n");
                writer.write("\t\t</hosts>\n");
                writer.write("\t</datacenter>\n");
                */



                writer.write("\t<datacenter arch=\"x86\" os=\"Linux\" vmm=\"Xen\">\n"+
                                "\t\t<costPerBw>0.1</costPerBw>\n"+
                                "\t\t<costPerSec>3.0</costPerSec>\n"+
                                "\t\t<costPerMem>0.05</costPerMem>\n"+
                                "\t\t<costPerStorage>0.1</costPerStorage>\n"+
                                "\t\t<location>\n"+
                                "\t\t\t<x_pos>" +String.valueOf(s.getxPos())+ "</x_pos>\n"+
                                "\t\t\t<y_pos>" +String.valueOf(s.getyPos())+ "</y_pos>\n"+
                                "\t\t\t<wlan_id>" +String.valueOf(s.getNodeName())+ "</wlan_id>\n"+
                                "\t\t\t<attractiveness>1</attractiveness>\n"+
                                "\t\t</location>\n"+
                                "\t\t<hosts>\n"+
                                "\t\t\t<host>\n");
                writer.write("\t\t\t\t<core>16</core>\n"+
                                "\t\t\t\t<mips>80000</mips>\n"+
                                "\t\t\t\t<ram>16000</ram>\n"+
                                "\t\t\t\t<storage>" + String.valueOf(s.getCapacity()) + "</storage>\n"+
                                "\t\t\t\t<readRate>" + String.valueOf(s.getServiceRate()) + "</readRate>\n"+
                                "\t\t\t\t<VMs>\n"+
                                "\t\t\t\t\t<VM vmm=\"Xen\">\n"+
                                "\t\t\t\t\t\t<core>16</core>\n"+
                                "\t\t\t\t\t\t<mips>80000</mips>\n"+
                                "\t\t\t\t\t\t<ram>16000</ram>\n"+
                                "\t\t\t\t\t\t<storage>" + String.valueOf(s.getCapacity()) + "</storage>\n"+
                                "\t\t\t\t\t\t<readRate>" + String.valueOf(s.getServiceRate()) + "</readRate>\n"+
                                "\t\t\t\t\t</VM>\n"+
                                "\t\t\t\t</VMs>\n"+
                                "\t\t\t</host>\n"+
                                "\t\t</hosts>\n"+
                                "\t</datacenter>\n");
            }
            writer.write("</edge_devices>\n");
            writer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public HashMap<Integer,String> prepareNodesHashVector() throws Exception{
        String line;
        String splitLineBy = ",";
        int lineCounter = 1;

        //create nodes vector
        Vector<StorageNode> nodesVector = new Vector<StorageNode>();

        //maps between the conventional name ant the original provided one
        HashMap<Integer,String> map = new HashMap<Integer,String>();
        int mapIndex = 0;
        try{
            BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\h3k\\Desktop\\csvs\\Nodes.csv"));
            line = br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                String[] objects = line.split(splitLineBy);

                //checks if the current object's name is unique
                for(Map.Entry m: map.entrySet()){
                    if(objects[NODE_NAME].equals(m.getValue())){
                        //System.out.println("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                        throw new Exception("The node name " + objects[NODE_NAME] + " is not unique!! error in line " + lineCounter);
                    }
                }

                //mapping the objects and renaming them to the convention.
                map.put(mapIndex,objects[NODE_NAME]);
                mapIndex++;


                //System.out.println("Object Name: " + objects[0] + " Size: " + objects[1] + " Location Vector: "+ objects[2] + " locationProbVector: " + objects[3] + " Class: " + objects[4]);
                lineCounter++;

                //creat new storageNode and add it to the nodes vector
                StorageNode sNode = new StorageNode(mapIndex-1,Double.parseDouble(objects[NODE_X_POSE]),
                        Double.parseDouble(objects[NODE_Y_POSE]),Integer.parseInt(objects[NODE_SERVICE_CLASS]),
                        Integer.parseInt(objects[NODE_CAPACITY]),Integer.parseInt(objects[NODE_SERVICE_RATE]));
                nodesVector.add(sNode);
            }
            System.out.println("The nodes' vector successfully created!!!");
            /*
            System.out.println("Displaying HashMap:");
            for(Map.Entry m: map.entrySet()){
                System.out.println(m.getKey() +" "+m.getValue());
            }*/

            //write the HashMap to a csv file
            csvWrite(map);

            //write the edge_device.xml file
            xmlWrite(nodesVector);
            System.out.println("The edge_devices.xml file has been overwrite successfully!!!");
        }
        catch (IOException e){
            e.printStackTrace();
        }
        return map;
    }

}
