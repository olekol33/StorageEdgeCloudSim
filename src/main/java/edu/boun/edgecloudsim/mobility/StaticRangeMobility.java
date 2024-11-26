package edu.boun.edgecloudsim.mobility;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.storage.StorageDevice;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;


public class StaticRangeMobility extends MobilityModel {
    private List<TreeMap<Double, Location>> treeMapArray;
    private HashMap<Integer, Location> staticLocationHash;
    private HashMap<Integer,Location> DCLocationArray;

    public StaticRangeMobility(int _numberOfMobileDevices, double _simulationTime) {
        super(_numberOfMobileDevices, _simulationTime);
    }


    @Override
    public void initialize() {
        treeMapArray = new ArrayList<>();
        DCLocationArray = new HashMap<>();
        staticLocationHash = new HashMap<>();


        //create random number generator for each place
        Random random = new Random();
        random.setSeed(SimSettings.getInstance().getRandomSeed());
        createDCLocationHash();

        //create list of DC locations
//        createDCLocationList();

        if(SimSettings.getInstance().isExternalDevices()) { // we have external file for the devices list
            for(int i = 0; i < numberOfMobileDevices; i++){
                treeMapArray.add(i, new TreeMap<>());
                Location placedDevice;
                placedDevice = realPlaceDevice(i);
                try {
                    if (SimSettings.getInstance().isStorageLogEnabled())
                        logAccessLocation(i, placedDevice.getServingWlanId());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, placedDevice);
                if (SimSettings.getInstance().isUserInNodes())
                    staticLocationHash.put(i, placedDevice);
//            System.out.println(placedDevice.getServingWlanId());
            }

        }else{
            File deviceListFile = null;
            FileWriter deviceListFW = null;
            BufferedWriter deviceListBW = null;
            deviceListFile = new File(SimSettings.getInstance().getPathOfDevicesFile());
            try {
                if (SimSettings.getInstance().isExportRunFiles()) {
                    deviceListFW = new FileWriter(deviceListFile, false);
                    deviceListBW = new BufferedWriter(deviceListFW);
                    deviceListBW.write("deviceName,xPos,yPos,time");
                    deviceListBW.newLine();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
            //initialize tree maps and position of mobile devices
            //places each mobile device at a location of a DC
            for (int i = 0; i < numberOfMobileDevices; i++) {
                treeMapArray.add(i, new TreeMap<Double, Location>());
                Location placedDevice;
                if (SimSettings.getInstance().isOrbitMode() || SimSettings.getInstance().isUserInNodes()) {
                    int nextHost = i % SimSettings.getInstance().getNumOfEdgeDatacenters();
                    placedDevice = orbitPlaceDevice(nextHost);
                }
                else
                    placedDevice = randomPlaceDevice(random);
                try {
                    if (SimSettings.getInstance().isExportRunFiles()) {
                        DecimalFormat df = new DecimalFormat();
                        df.setMaximumFractionDigits(9);
                        //        List<deviceProperty> deviceList = getdeviceList();
                        deviceListBW.write(String.valueOf(i) + "," + String.valueOf(placedDevice.getXPos())
                                + "," + String.valueOf(placedDevice.getYPos()) + ",0");
                        deviceListBW.newLine();
                    }
                    if (SimSettings.getInstance().isStorageLogEnabled())
                        logAccessLocation(i, placedDevice.getServingWlanId());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, placedDevice);
                if (SimSettings.getInstance().isUserInNodes())
                    staticLocationHash.put(i, placedDevice);
            }
            if (SimSettings.getInstance().isExportRunFiles()) {
                try {
                    deviceListBW.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        if (SimSettings.getInstance().isUserInNodes())
            return getLocationStatic(deviceId);

        TreeMap<Double, Location> treeMap = treeMapArray.get(deviceId);
        Map.Entry<Double, Location> e = treeMap.floorEntry(time);
        if(e == null){
            SimLogger.printLine("impossible has occurred! no location is found for the device '" + deviceId + "' at " + time);
            System.exit(0);
        }

        return e.getValue();
    }

    private Location getLocationStatic(int deviceId){
        return staticLocationHash.get(deviceId);
    }

    //In case location is updated (mostly for host update) - only static
    public void setLocation(int deviceId, Location deviceLocation) {
        treeMapArray.get(deviceId).put(SimSettings.CLIENT_ACTIVITY_START_TIME, deviceLocation);
    }

//    public static Location getDCLocation(int DatacenterId) {
    //TODO: remove this wrapper
    public Location getDCLocation(int DatacenterId) {
        return DCLocationArray.get(DatacenterId);
    }

    public void createDCLocationHash() {
        for (int DatacenterId=0;DatacenterId<SimSettings.getInstance().getNumOfEdgeDatacenters();DatacenterId++){
            Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
            NodeList datacenterList = doc.getElementsByTagName("datacenter");
            Node datacenterNode = datacenterList.item(DatacenterId);
            Element datacenterElement = (Element) datacenterNode;
            Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
            int placeTypeIndex = 0;
            int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
            int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
            int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());

            DCLocationArray.put(DatacenterId,new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
        }

    }

    //If device in range of host - returns host location. If not, returns nearest access point.
    public Location getAccessPoint(Location deviceLocation, Location hostLocation){
        int hostRadius = SimSettings.getInstance().getHostRadius();
        double deviceXPos = deviceLocation.getXPos();
        double deviceYPos = deviceLocation.getXPos();
        double hostXPos = hostLocation.getXPos();
        double hostYPos = hostLocation.getYPos();
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
        double x_pos = deviceLocation.getXPos();
        double y_pos = deviceLocation.getYPos();
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
    public static double getGridDistance(Location srcLocation, Location destLocation){
        double xDist = Math.abs(srcLocation.getXPos()-destLocation.getXPos());
        double yDist = Math.abs(srcLocation.getYPos()-destLocation.getYPos());
        return xDist+yDist;
    }

    //Returns euclidean distance assuming slot size is 100m
    public static double getEuclideanDistance(Location srcLocation, Location destLocation){
        double xDist = Math.abs(srcLocation.getXPos()-destLocation.getXPos());
        double yDist = Math.abs(srcLocation.getYPos()-destLocation.getYPos());
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


    // Receives list of hosts in which device is in range, returns nearest host.
    public int getNearestHost(List<Integer> hosts, Location deviceLocation){
        double minDistance = Integer.MAX_VALUE;
        int minDCLocationID = -1;
        if(hosts.size() == 1)
            return hosts.get(0);
        for (int host : hosts) {
            double distance = getGridDistance(deviceLocation, getDCLocation(host));
            if (distance == 0)
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
        if(SimSettings.getInstance().isExternalNodes()) {
            xRange = (int)SimSettings.getInstance().getxRange();
            yRange = (int)SimSettings.getInstance().getyRange();

        }else{
            xRange = SimSettings.getInstance().getXRange();
            yRange = SimSettings.getInstance().getYRange();
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
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), xPos, yPos,hosts);
        }
        //When several hosts in range, take nearest
        else {
            Location host = getDCLocation(getNearestHost(hosts, new Location(0,0,xPos,yPos)));
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), xPos, yPos,hosts);
        }
    }

    //places device on grid within range of at least one host
    private Location realPlaceDevice(int index){
        StorageDevice s1 = SimSettings.getInstance().getDevicesVector().get(index);
        List<Integer> hosts = new ArrayList<Integer>();
        Location deviceLocation;
        //Initialize list of hosts in proximity of device
        deviceLocation = new Location(0, 0, (int)s1.getxPos(),  (int)s1.getyPos());
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
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), (int)s1.getxPos(), (int)s1.getyPos(),hosts);
        }
        //When several hosts in range, take nearest
        else {
            Location host = getDCLocation(getNearestHost(hosts, new Location(0,0,(int)s1.getxPos(),(int)s1.getyPos())));
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), (int)s1.getxPos(), (int)s1.getyPos(),hosts);
        }
    }

    /**Returns location of host ID
     *
     * @param host
     * @return
     */
    private Location orbitPlaceDevice(int host){
        return getDCLocation(host);
    }


}
