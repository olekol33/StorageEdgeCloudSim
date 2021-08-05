package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimSettings;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * This class parses the nodes input file.
 */
public class ParseStorageNodes {
    private final int NODE_NAME = 0; // object[0]
    private final int NODE_X_POSE = 1; // object[1]
    private final int NODE_Y_POSE = 2; // object[2]
    private final int NODE_SERVICE_CLASS = 3; // object[3]
    private final int NODE_CAPACITY = 4; // object[4]
    private final int NODE_SERVICE_RATE = 5; // object[5]
    private double xMin;
    private double yMin;
    private double xRange;
    private double yRange;


    /**
     * exports the nodes from the nodes vector to a file (edge_devices.xml).
     * creates new file or rewrites an existing one.
     *
     * @param nodesVector contains the nodes. each Node in the vector is from type StorageNode.
     */
    public static void xmlWrite(Vector<StorageNode> nodesVector){
        try{
            FileWriter writer = new FileWriter("scripts/sample_app6/config/edge_devices.xml");
            writer.write("<?xml version=\"1.0\"?>\n");
            writer.write("<edge_devices>\n");
            int vecIndex = 0;
            while(vecIndex < nodesVector.size()) {
                StorageNode s = nodesVector.get(vecIndex++);
                if(!SimSettings.getInstance().isItIntTest()) {
                    writer.write("\t<datacenter arch=\"x86\" os=\"Linux\" vmm=\"Xen\">\n" +
                            "\t\t<costPerBw>0.1</costPerBw>\n" +
                            "\t\t<costPerSec>3.0</costPerSec>\n" +
                            "\t\t<costPerMem>0.05</costPerMem>\n" +
                            "\t\t<costPerStorage>0.1</costPerStorage>\n" +
                            "\t\t<location>\n" +
                            "\t\t\t<x_pos>" + s.getxPos() + "</x_pos>\n" +
                            "\t\t\t<y_pos>" + s.getyPos() + "</y_pos>\n" +
                            "\t\t\t<wlan_id>" + s.getNodeName() + "</wlan_id>\n" +
                            "\t\t\t<attractiveness>1</attractiveness>\n" +
                            "\t\t</location>\n" +
                            "\t\t<hosts>\n" +
                            "\t\t\t<host>\n");
                }else{//TODO: delete this section - for testing purposes only
                    writer.write("\t<datacenter arch=\"x86\" os=\"Linux\" vmm=\"Xen\">\n" +
                            "\t\t<costPerBw>0.1</costPerBw>\n" +
                            "\t\t<costPerSec>3.0</costPerSec>\n" +
                            "\t\t<costPerMem>0.05</costPerMem>\n" +
                            "\t\t<costPerStorage>0.1</costPerStorage>\n" +
                            "\t\t<location>\n" +
                            "\t\t\t<x_pos>" + (int) s.getxPos() + "</x_pos>\n" +
                            "\t\t\t<y_pos>" + (int) s.getyPos() + "</y_pos>\n" +
                            "\t\t\t<wlan_id>" + s.getNodeName() + "</wlan_id>\n" +
                            "\t\t\t<attractiveness>1</attractiveness>\n" +
                            "\t\t</location>\n" +
                            "\t\t<hosts>\n" +
                            "\t\t\t<host>\n");
                }
                writer.write("\t\t\t\t<core>16</core>\n"+
                                "\t\t\t\t<mips>80000</mips>\n"+
                                "\t\t\t\t<ram>16000</ram>\n"+
                                "\t\t\t\t<storage>" + s.getCapacity() + "</storage>\n"+
                                "\t\t\t\t<readRate>" + s.getServiceRate() + "</readRate>\n"+
                                "\t\t\t\t<taskProcessingTimeUS>300</taskProcessingTimeUS>"+
                                "\t\t\t\t<VMs>\n"+
                                "\t\t\t\t\t<VM vmm=\"Xen\">\n"+
                                "\t\t\t\t\t\t<core>16</core>\n"+
                                "\t\t\t\t\t\t<mips>80000</mips>\n"+
                                "\t\t\t\t\t\t<ram>16000</ram>\n"+
                                "\t\t\t\t\t\t<storage>" + s.getCapacity() + "</storage>\n"+
                                "\t\t\t\t\t\t<readRate>" + s.getServiceRate() + "</readRate>\n"+
                                "\t\t\t\t\t\t<taskProcessingTimeUS>300</taskProcessingTimeUS>"+
                                "\t\t\t\t\t</VM>\n"+
                                "\t\t\t\t</VMs>\n"+
                                "\t\t\t</host>\n"+
                                "\t\t</hosts>\n"+
                                "\t</datacenter>\n");
            }
            writer.write("</edge_devices>\n");
            writer.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * gets a path to a file and parse the nodes from it into a vector.
     * in edition, creates hash map between the names of the nodes in the vector, and the original names of the nodes.
     *
     * @param file_path contains the path to the nodes input file.
     * @return hash map contains a mapping between the new name (key) to the original name (value) from the input file.
     */
    public HashMap<Integer,String> prepareNodesHashVector(String file_path){
        String line;
        String splitLineBy = ",";
        int lineCounter = 1;
        double maxX = 0, maxY = 0, minX = 0, minY = 0;
        boolean checkFirst = true;

        //create nodes vector
        Vector<StorageNode> nodesVector = new Vector<>();

        //maps between the conventional name and the original provided one
        HashMap<Integer,String> map = new HashMap<>();
        int mapIndex = 0;
        try{
            BufferedReader br;
            if(file_path.equals("")) {
                br = new BufferedReader(new FileReader("scripts/sample_app6/input_files/Nodes.csv"));
            }else{
                br = new BufferedReader(new FileReader(file_path));
            }
            br.readLine();
            lineCounter++;
            while((line = br.readLine()) != null){
                String[] objects = line.split(splitLineBy);

                //checks if the current object's name is unique
                for(Map.Entry<Integer,String> m: map.entrySet()){
                    if(objects[NODE_NAME].equals(m.getValue())){
                        //System.out.println("The object name " + objects[0] + " is not unique!! error in line " + lineCounter);
                        throw new Exception("The node name " + objects[NODE_NAME] + " is not unique!! error in line " + lineCounter);
                    }
                }

                //mapping the objects and renaming them to the convention.
                map.put(mapIndex,objects[NODE_NAME]);
                mapIndex++;
                lineCounter++;

                //create new storageNode and add it to the nodes vector
                StorageNode sNode = new StorageNode(mapIndex-1,Double.parseDouble(objects[NODE_X_POSE]),
                        Double.parseDouble(objects[NODE_Y_POSE]),Integer.parseInt(objects[NODE_SERVICE_CLASS]),
                        Integer.parseInt(objects[NODE_CAPACITY]),Integer.parseInt(objects[NODE_SERVICE_RATE]));
                nodesVector.add(sNode);

                double x = Double.parseDouble(objects[NODE_X_POSE]);
                double y = Double.parseDouble(objects[NODE_Y_POSE]);


                //find the max (x,y) and min (x,y)
                if(checkFirst){
                    minX = x;
                    minY = y;
                    maxX = minX;
                    maxY = minY;
                    checkFirst = false;
                }
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
            }
            System.out.println("The nodes' vector successfully created!!!");

            /*
            System.out.println("Displaying HashMap:");
            for(Map.Entry m: map.entrySet()){
                System.out.println(m.getKey() +" "+m.getValue());
            }*/

            //expand the limits of the grid by host radius (for devices with negative location on the aligned grid).
            minX -= SimSettings.getInstance().getHostRadius();
            minY -= SimSettings.getInstance().getHostRadius();


            //cast the range of the nodes to the range: (0,0) - (maxX, maxY)
            if(SimSettings.getInstance().isGpsConversionRequired()) { //check the GPS conversion flag
                for (StorageNode temp : nodesVector) {
                    //TODO: delete this section - for testing purposes only
                    if (SimSettings.getInstance().isItIntTest()) {
                        //temp.setxPos((int) ((temp.getxPos() - minX) * 100));
                        //temp.setyPos((int) ((temp.getyPos() - minY) * 100));
                    } else {
                        temp.setxPos(temp.getxPos() - minX);
                        temp.setyPos(temp.getyPos() - minY);
                    }
                }
                //maxX = maxX - minX + 2 * SimSettings.getInstance().getHostRadius();
                //maxY = maxY - minY + 2 * SimSettings.getInstance().getHostRadius();
                if(SimSettings.getInstance().isExternalNodes()){
                    for(StorageNode temp : nodesVector){
                        maxX = Math.max(maxX, temp.getxPos());
                        maxY = Math.max(maxY, temp.getyPos());
                    }
                }
            }

            xMin = minX;
            yMin = minY;
            xRange = maxX;
            yRange = maxY;

            //write the HashMap to a csv file
            CsvWrite.csvWriteIS(map, "scripts/sample_app6/hash_tables/Nodes_Hash.csv");

            //write the edge_device.xml file
            xmlWrite(nodesVector);
            System.out.println("The edge_devices.xml file has been overwrite successfully!!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return map;

    }// end of prepareNodesHashVector

    public double getxMin() {
        return xMin;
    }

    public double getyMin() {
        return yMin;
    }

    public double getxRange() {
        return xRange;
    }

    public double getyRange() {
        return yRange;
    }

}// end of class ParseStorageNodes

//This section has been written by Harel