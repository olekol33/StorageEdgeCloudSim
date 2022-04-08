package edu.boun.edgecloudsim.storage;

import org.apache.commons.math3.util.Pair;

import java.io.*;
import java.util.*;

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
    private int xMin;
    private int yMin;
    private int xRange;
    private int yRange;


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
                writer.write("\t<datacenter arch=\"x86\" os=\"Linux\" vmm=\"Xen\">\n" +
                        "\t\t<costPerBw>0.1</costPerBw>\n" +
                        "\t\t<costPerSec>3.0</costPerSec>\n" +
                        "\t\t<costPerMem>0.05</costPerMem>\n" +
                        "\t\t<costPerStorage>0.1</costPerStorage>\n" +
                        "\t\t<location>\n" +
                        "\t\t\t<x_pos>" + (int)s.getxPos() + "</x_pos>\n" +
                        "\t\t\t<y_pos>" + (int)s.getyPos() + "</y_pos>\n" +
                        "\t\t\t<wlan_id>" + s.getNodeName() + "</wlan_id>\n" +
                        "\t\t\t<attractiveness>1</attractiveness>\n" +
                        "\t\t</location>\n" +
                        "\t\t<hosts>\n" +
                        "\t\t\t<host>\n"+
                        "\t\t\t\t<core>16</core>\n"+
                        "\t\t\t\t<mips>80000</mips>\n"+
                        "\t\t\t\t<ram>16000</ram>\n"+
                        "\t\t\t\t<storage>" + s.getCapacity() + "</storage>\n"+
                        "\t\t\t\t<readRate>" + s.getServiceRate() + "</readRate>\n"+
                        "\t\t\t\t<taskProcessingTimeUS>300</taskProcessingTimeUS>\n"+
                        "\t\t\t\t<VMs>\n"+
                        "\t\t\t\t\t<VM vmm=\"Xen\">\n"+
                        "\t\t\t\t\t\t<core>16</core>\n"+
                        "\t\t\t\t\t\t<mips>80000</mips>\n"+
                        "\t\t\t\t\t\t<ram>16000</ram>\n"+
                        "\t\t\t\t\t\t<storage>" + s.getCapacity() + "</storage>\n"+
                        "\t\t\t\t\t\t<readRate>" + s.getServiceRate() + "</readRate>\n"+
                        "\t\t\t\t\t\t<taskProcessingTimeUS>300</taskProcessingTimeUS>\n"+
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
    public List<Object> prepareNodesHashVector(String file_path){
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
            //collect device locations
            BufferedReader br;
            br = new BufferedReader(new FileReader("scripts/sample_app6/input_files/Devices.csv"));
            br.readLine();
            lineCounter++;
            ArrayList<Pair<Double,Double>> devicesXY = new ArrayList<>();
            int XPOS=1;
            int YPOS=2;
            while((line = br.readLine()) != null) {
                String[] objects = line.split(splitLineBy);
                devicesXY.add(new Pair<Double,Double>(Double.valueOf(objects[XPOS]),Double.valueOf(objects[YPOS])));
            }

//            BufferedReader br;
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
                StorageNode sNode = new StorageNode(mapIndex-1,Double.valueOf(objects[NODE_X_POSE]),
                        Double.valueOf(objects[NODE_Y_POSE]),Integer.parseInt(objects[NODE_SERVICE_CLASS]),
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
            for(int i=0;i<devicesXY.size();i++){
                double x = devicesXY.get(i).getFirst();
                double y = devicesXY.get(i).getSecond();
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
            }

            System.out.println("Nodes vector successfully created!!!");

            //cast the range of the nodes to the range: (0,0) - (maxX, maxY)
            for (StorageNode temp : nodesVector) {
                int[] coord = latlonToMeters(temp.getxPos(),temp.getyPos(),minX ,minY);
                temp.setxPos(coord[0]);
                temp.setyPos(coord[1]);
            }

            int[] coord = latlonToMeters(maxX,maxY, minX, minY);
            xMin = 0;
            yMin = 0;
            xRange = coord[0];
            yRange = coord[1];

            //write the HashMap to a csv file
            CsvWrite.csvWriteIS(map, "scripts/sample_app6/hash_tables/Nodes_Hash.csv");

            //write the edge_device.xml file
            xmlWrite(nodesVector);
            System.out.println("The edge_devices.xml file has been overwrite successfully!!!");
        }
        catch (Exception e){
            e.printStackTrace();
        }

//        return map;
        return Arrays.asList(map,minX,minY,maxX,maxY);

    }// end of prepareNodesHashVector

    /*Return distance in meters between two coordinates in longitude and latitude*/
    private static int distanceBetweenCoordinates(double lat1, double lon1, double lat2, double lon2){  // generally used geo measurement function
        var R = 6378.137; // Radius of earth in KM
        var dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
        var dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
        var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        var d = R * c;
        return (int)(d * 1000); // meters
    }

    /*Returns x,y coordinates in meters on grid*/
    public static int[] latlonToMeters(double lat, double lon, double xMin, double yMin){
        int[] coordinates = new int[2];
        coordinates[0] = distanceBetweenCoordinates(xMin, yMin,lat, yMin); //get x distance
        coordinates[1] = distanceBetweenCoordinates(xMin, yMin, xMin,lon); //get y distance
        return coordinates;
    }


    public int getxMin() {
        return xMin;
    }

    public int getyMin() {
        return yMin;
    }

    public int getxRange() {
        return xRange;
    }

    public int getyRange() {
        return yRange;
    }

}// end of class ParseStorageNodes

//This section has been written by Harel