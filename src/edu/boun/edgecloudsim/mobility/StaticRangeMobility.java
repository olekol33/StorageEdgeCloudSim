package edu.boun.edgecloudsim.mobility;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.StorageDevice;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.core.CloudSim;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

//changed by Harel

public class StaticRangeMobility extends MobilityModel {
    private List<TreeMap<Double, Location>> treeMapArray;
    private HashMap<Integer,Location> DCLocationArray;
//    private static List<Location> dcLocations = new ArrayList<>();

    public StaticRangeMobility(int _numberOfMobileDevices, double _simulationTime) {
        super(_numberOfMobileDevices, _simulationTime);
    }

    @Override
    public void initialize() {
        treeMapArray = new ArrayList<TreeMap<Double, Location>>();
        DCLocationArray = new HashMap<Integer,Location>();

//        ExponentialDistribution[] expRngList = new ExponentialDistribution[SimSettings.getInstance().getNumOfEdgeDatacenters()];

        //create random number generator for each place
        Random random = new Random();
        random.setSeed(ObjectGenerator.seed);
        createDCLocationHash();

        //create list of DC locations
//        createDCLocationList();

        if(!SimSettings.getInstance().isExternalDevices()) {
            //initialize tree maps and position of mobile devices
            //places each mobile device at a location of a DC
            for (int i = 0; i < numberOfMobileDevices; i++) {
                treeMapArray.add(i, new TreeMap<Double, Location>());
                Location placedDevice;
                if (SimSettings.getInstance().isOrbitMode())
                    placedDevice = orbitPlaceDevice(random);
                else
                    placedDevice = randomPlaceDevice(random);
                try {
                    if (SimSettings.getInstance().isStorageLogEnabled())
                        logAccessLocation(i, placedDevice.getServingWlanId());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, placedDevice);
//            System.out.println(placedDevice.getServingWlanId());

            }
        }else{// we have external file for the devices list
            for(int i = 0; i < numberOfMobileDevices; i++){
                treeMapArray.add(i, new TreeMap<Double, Location>());
                Location placedDevice;
                placedDevice = realPlaceDevice(i);
                try {
                    if (SimSettings.getInstance().isStorageLogEnabled())
                        logAccessLocation(i, placedDevice.getServingWlanId());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, placedDevice);
//            System.out.println(placedDevice.getServingWlanId());
            }

        }

    }

    //Logs access locations
    public void logAccessLocation(int deviceID, int hostID) throws FileNotFoundException {
        String savestr = SimLogger.getInstance().getOutputFolder()+ "/" + SimLogger.getInstance().getFilePrefix() + "_DEVICE_ACCESS.log";
        File f = new File(savestr);

        PrintWriter out = null;
        if ( f.exists() && !f.isDirectory() ) {
            out = new PrintWriter(new FileOutputStream(new File(savestr), true));
        }
        else {
            out = new PrintWriter(savestr);
            out.append("Device;HostID");
            out.append("\n");
        }
        out.append(Integer.toString(deviceID)
                + SimSettings.DELIMITER + Integer.toString(hostID));
        out.append("\n");
        out.close();
    }

    @Override
    public Location getLocation(int deviceId, double time) {
        TreeMap<Double, Location> treeMap = treeMapArray.get(deviceId);

        Map.Entry<Double, Location> e = treeMap.floorEntry(time);

        if(e == null){
            SimLogger.printLine("impossible is occured! no location is found for the device '" + deviceId + "' at " + time);
            System.exit(0);
        }

        return e.getValue();
    }
    //In case location is updated (mostly for host update) - only static
    public void setLocation(int deviceId, Location deviceLocation) {
//        Map.Entry<Double, Location> e = treeMap.floorEntry(time);
        treeMapArray.get(deviceId).put(SimSettings.CLIENT_ACTIVITY_START_TIME, deviceLocation);

/*        if(e == null){
            SimLogger.printLine("impossible is occured! no location is found for the device '" + deviceId + "' at " + time);
            System.exit(0);
        }

        return e.getValue();*/
    }
//    public static Location getDCLocation(int DatacenterId) {
    public Location getDCLocation(int DatacenterId) {
/*        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");
        Node datacenterNode = datacenterList.item(DatacenterId);
        Element datacenterElement = (Element) datacenterNode;
        Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
        String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
        int placeTypeIndex = Integer.parseInt(attractiveness);
        int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
        int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
        int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());

        return new Location(placeTypeIndex, wlan_id, x_pos, y_pos);*/
        return DCLocationArray.get(DatacenterId);
    }

    public void createDCLocationHash() {
        for (int DatacenterId=0;DatacenterId<SimSettings.getInstance().getNumOfEdgeDatacenters();DatacenterId++){
            Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
            NodeList datacenterList = doc.getElementsByTagName("datacenter");
            Node datacenterNode = datacenterList.item(DatacenterId);
            Element datacenterElement = (Element) datacenterNode;
            Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
            String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
            int placeTypeIndex = Integer.parseInt(attractiveness);
            int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
            int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
            int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());

            DCLocationArray.put(DatacenterId,new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
        }

    }

/*    private void createDCLocationList(){
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");
        for (int i =0; i<SimSettings.getInstance().getNumOfEdgeDatacenters() ; i++){
            Node datacenterNode = datacenterList.item(i);
            Element datacenterElement = (Element) datacenterNode;
            Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
            String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
            int placeTypeIndex = Integer.parseInt(attractiveness);
            int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
            int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
            int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
            dcLocations.add(new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
        }
    }*/

    //If device in range of host - returns host location. If not, returns nearest access point.
    public Location getAccessPoint(Location deviceLocation, Location hostLocation){
        int hostRadius = SimSettings.getInstance().getHostRadius();
        int deviceXPos = deviceLocation.getXPos();
        int deviceYPos = deviceLocation.getXPos();
        int hostXPos = hostLocation.getXPos();
        int hostYPos = hostLocation.getYPos();
        //if device in radius of host
        if (hostXPos-hostRadius <= deviceXPos && deviceXPos <= hostXPos+hostRadius) {
            if (hostYPos - hostRadius <= deviceYPos && deviceYPos <= hostYPos + hostRadius)
                return hostLocation;
        }
        //get all hosts in range
        List<Integer> hostsInRange = checkLegalPlacement(deviceLocation);
        //return nearest
        return getDCLocation(getNearestHost(hostsInRange,hostLocation));
    }
    // Receives DC list and mobile device location. Check if device located within radius of the DCs
    // If yes, add it to a list
    public List<Integer> checkLegalPlacement(Location deviceLocation) {
        int x_pos = deviceLocation.getXPos();
        int y_pos = deviceLocation.getYPos();
        int hostRadius = SimSettings.getInstance().getHostRadius();

        List<Integer> hosts = new ArrayList<Integer>();
        for (int i = 0; i<SimSettings.getInstance().getNumOfEdgeDatacenters(); i++){
            Location DCLocation = getDCLocation(i);
            //Checks if x,y of device in range of host, for each host
            if (DCLocation.getXPos()-hostRadius <= x_pos && x_pos <= DCLocation.getXPos()+hostRadius){
                if (DCLocation.getYPos()-hostRadius <= y_pos && y_pos <= DCLocation.getYPos()+hostRadius)
                    hosts.add(i);
            }

        }
        return hosts;
    }
    //Returns number of slots on grid between two locations
    public static int getGridDistance(Location srcLocation, Location destLocation){
        int xDist = Math.abs(srcLocation.getXPos()-destLocation.getXPos());
        int yDist = Math.abs(srcLocation.getYPos()-destLocation.getYPos());
        return xDist+yDist;
    }

    //Returns euclidean distance assuming slot size is 100m
    public static double getEuclideanDistance(Location srcLocation, Location destLocation){
/*        //size of box in grid
        double boxSizeMeters = 100; //meters
        double boxSizeGrid = 6; //max box size in grid
        int xDist = Math.abs(srcLocation.getXPos()-destLocation.getXPos());
        int yDist = Math.abs(srcLocation.getYPos()-destLocation.getYPos());
        double xyDistance = Math.sqrt(Math.pow(xDist,2) + Math.pow(yDist,2));
        double gridDistanceUnit = boxSizeMeters / boxSizeGrid;
        return xyDistance*gridDistanceUnit;*/
        int xDist = Math.abs(srcLocation.getXPos()-destLocation.getXPos());
        int yDist = Math.abs(srcLocation.getYPos()-destLocation.getYPos());
        double xyDistance = Math.sqrt(Math.pow(xDist,2) + Math.pow(yDist,2));
        return xyDistance;
    }

    //according to "Experimental analysis of multipoint-to-point UAV communications with IEEE 802.11 n and 802.11 ac." Hayat et al
    //Calculate by how many % the throughput has decreased as function of distance
    //Worst is 5%
    //Mobile phone operation may use power of n for 1/(r)^n where 3.5<n<5
    //https://www.electronics-notes.com/articles/antennas-propagation/propagation-overview/radio-signal-path-loss.php
    //Free-space path loss formula 1/(4*pi*r)^2, r>>1. WE use 1/(r)^n.
    public static double getSignalAttenuation(Location srcLocation, Location destLocation, double steadySignalRange, double n){
        double distance = getEuclideanDistance(srcLocation,destLocation);
        if(distance<steadySignalRange)
            return 1; //not attenuation
        else{
            double r = distance /steadySignalRange;
            return 1/Math.pow(r,n);
        }
    }
/*    public static double getDistance(Location srcLocation, Location destLocation){
        double x0 = 0;
        double x1 = 100;
        //devided y by 2 compared to paper
        double y0 = 100;
        double y1 = 5;
        double m = (y0-y1) / (x0-x1);
        return getEuclideanDistance(srcLocation,destLocation);
    }*/
    public static void logDistanceDegradation(Location srcLocation, Location destLocation, double distance, double degradation) throws FileNotFoundException {
        String savestr = SimLogger.getInstance().getOutputFolder()+ "/" + SimLogger.getInstance().getFilePrefix() + "_DISTANCE_DEGRADATION.log";
        File f = new File(savestr);

        PrintWriter out = null;
        if ( f.exists() && !f.isDirectory() ) {
            out = new PrintWriter(new FileOutputStream(new File(savestr), true));
        }
        else {
            out = new PrintWriter(savestr);
            out.append("srcX;srcY;dstX;dstY;distance;degradation");
            out.append("\n");
        }

        out.append(srcLocation.getXPos()+ SimSettings.DELIMITER +srcLocation.getYPos()+ SimSettings.DELIMITER +destLocation.getXPos()+ SimSettings.DELIMITER +
                destLocation.getYPos()+ SimSettings.DELIMITER +distance+ SimSettings.DELIMITER +degradation);
        out.append("\n");


        out.close();
    }


    // Receives list of hosts in which device is in range, returns nearest host.
    public int getNearestHost(List<Integer> hosts, Location deviceLocation){
        int minDistance = Integer.MAX_VALUE;
        int minDCLocationID = -1;

        for (int i=0 ; i < hosts.size() ; i++){
            int host = hosts.get(i);
            int distance = getGridDistance(deviceLocation,getDCLocation(host));
            //best possible
            if (distance==0)
                return host;
            if (distance < minDistance) {
                minDistance = distance;
                minDCLocationID = host;
            }
        }
        return minDCLocationID;
    }

    //Randomly places device on grid within range of at least one host
    private Location randomPlaceDevice(Random random){
        //get grid size
        int xRange, yRange;
        if(!SimSettings.getInstance().isExternalNodes()) {
            xRange = SimSettings.getInstance().getXRange();
            yRange = SimSettings.getInstance().getYRange();
        }else{
            xRange = (int) SimSettings.getInstance().getxRange();
            yRange = (int) SimSettings.getInstance().getyRange();
        }
        int xPos = 0;
        int yPos = 0;
        List<Integer> hosts = new ArrayList<Integer>();
        Location deviceLocation;
        //Initialize list of hosts in proximity of device
        while (hosts.size() == 0){
            xPos = random.nextInt(xRange)+1;
            yPos = random.nextInt(yRange)+1;
            deviceLocation = new Location(0,0,xPos,yPos);
            //Returns list of hosts for which device is in range
            hosts = checkLegalPlacement(deviceLocation);//TODO:review
        }
        //When one host in range it's the only element in the list
        if (hosts.size()==1) {
            Location host = getDCLocation(hosts.get(0));
//            Location host = dcLocations.get(hosts.get(0));
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), xPos, yPos,hosts);
        }
        //When several hosts in range, take nearest
        else {
            Location host = getDCLocation(getNearestHost(hosts, new Location(0,0,xPos,yPos)));
//            Location host = dcLocations.get(getNearestHost(hosts, new Location(0,0,xPos,yPos)));
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), xPos, yPos,hosts);
//            return host;
        }
    }

    //TODO: created by Harel
    //places device on grid within range of at least one host
    private Location realPlaceDevice(int index){//TODO:edit
        StorageDevice s1 = SimSettings.getInstance().getDevicesVector().get(index);
        List<Integer> hosts = new ArrayList<Integer>();
        Location deviceLocation;
        //Initialize list of hosts in proximity of device
        //TODO: delete this section - for testing purposes only
        if(SimSettings.getInstance().isItIntTest() && SimSettings.getInstance().isExternalDevices()){
//            deviceLocation = new Location(0,0,(int)((s1.getxPos() - SimSettings.getInstance().getMinXpos())*100)
//                    ,(int)((s1.getyPos() - SimSettings.getInstance().getMinYpos())*100)); //TODO: remove casting
            deviceLocation = new Location(0,0,(int)((s1.getxPos() - SimSettings.getInstance().getMinXpos()))
                    ,(int)((s1.getyPos() - SimSettings.getInstance().getMinYpos())));
        }else {
            deviceLocation = new Location(0, 0, (int) s1.getxPos(), (int) s1.getyPos()); //TODO: remove casting
        }
        hosts = checkLegalPlacement(deviceLocation);
        try {
            if (hosts.size() == 0) {
                throw new Exception("Error! device number " + index + " is not in range of any node");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        //When one host in range it's the only element in the list
        if (hosts.size()==1) {
            Location host = getDCLocation(hosts.get(0));
//            Location host = dcLocations.get(hosts.get(0));
            //TODO: delete this section - for testing purposes only
            if(SimSettings.getInstance().isItIntTest() && SimSettings.getInstance().isExternalDevices()){
//                return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(),
//                        (int)((s1.getxPos() - SimSettings.getInstance().getMinXpos())*100)
//                        ,(int)((s1.getyPos() - SimSettings.getInstance().getMinYpos())*100),hosts); //TODO: remove casting
                return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(),
                        (int)((s1.getxPos() - SimSettings.getInstance().getMinXpos()))
                        ,(int)((s1.getyPos() - SimSettings.getInstance().getMinYpos())),hosts);
            }
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), (int)s1.getxPos(), (int)s1.getyPos(),hosts); //TODO: remove casting
        }
        //When several hosts in range, take nearest
        else {
            Location host = getDCLocation(getNearestHost(hosts, new Location(0,0,(int)s1.getxPos(),(int)s1.getyPos()))); //TODO: remove casting
//            Location host = dcLocations.get(getNearestHost(hosts, new Location(0,0,xPos,yPos)));
            //TODO: delete this section - for testing purposes only
            if(SimSettings.getInstance().isItIntTest() && SimSettings.getInstance().isExternalDevices()){
//                return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(),
//                        (int)((s1.getxPos() - SimSettings.getInstance().getMinXpos())*100)
//                        ,(int)((s1.getyPos() - SimSettings.getInstance().getMinYpos())*100),hosts); //TODO: remove casting
                return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(),
                        (int)((s1.getxPos() - SimSettings.getInstance().getMinXpos()))
                        ,(int)((s1.getyPos() - SimSettings.getInstance().getMinYpos())),hosts);
            }
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), (int)s1.getxPos(), (int)s1.getyPos(),hosts); //TODO: remove casting
//            return host;
        }
    }

    private Location orbitPlaceDevice(Random random){
        //get grid size
        int xRange = SimSettings.getInstance().getXRange();
        int yRange = SimSettings.getInstance().getYRange();
        int xPos = 0;
        int yPos = 0;
        List<Integer> hosts = new ArrayList<Integer>();
//        Location deviceLocation;
        int host = random.nextInt(SimSettings.getInstance().getNumOfEdgeDatacenters());
        hosts.add(host);
        Location deviceLocation = getDCLocation(host);
        return new Location(deviceLocation.getPlaceTypeIndex(), deviceLocation.getServingWlanId(), xPos, yPos,hosts);


        //Initialize list of hosts in proximity of device
/*        while (hosts.size() == 0){
            xPos = random.nextInt(xRange)+1;
            yPos = random.nextInt(yRange)+1;
            deviceLocation = new Location(0,0,xPos,yPos);
            //Returns list of hosts for which device is in range
            hosts = checkLegalPlacement(deviceLocation);
        }
        //When one host in range it's the only element in the list
        if (hosts.size()==1) {
            Location host = getDCLocation(hosts.get(0));
//            Location host = dcLocations.get(hosts.get(0));
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), xPos, yPos,hosts);
        }
        //When several hosts in range, take nearest
        else {
            Location host = getDCLocation(getNearestHost(hosts, new Location(0,0,xPos,yPos)));
//            Location host = dcLocations.get(getNearestHost(hosts, new Location(0,0,xPos,yPos)));
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), xPos, yPos,hosts);
//            return host;
        }*/
    }


}
