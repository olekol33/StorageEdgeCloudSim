package edu.boun.edgecloudsim.mobility;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.geom.Point2D;
import java.util.*;

public class StaticRangeMobility extends MobilityModel {
    private List<TreeMap<Double, Location>> treeMapArray;

    public StaticRangeMobility(int _numberOfMobileDevices, double _simulationTime) {
        super(_numberOfMobileDevices, _simulationTime);
    }

    @Override
    public void initialize() {
        treeMapArray = new ArrayList<TreeMap<Double, Location>>();

//        ExponentialDistribution[] expRngList = new ExponentialDistribution[SimSettings.getInstance().getNumOfEdgeDatacenters()];

        //create random number generator for each place

        //initialize tree maps and position of mobile devices
        //places each mobile device at a location of a DC
        for(int i=0; i<numberOfMobileDevices; i++) {
            treeMapArray.add(i, new TreeMap<Double, Location>());
            //random edge id
//            int randDatacenterId = SimUtils.getRandomNumber(0, SimSettings.getInstance().getNumOfEdgeDatacenters()-1);
            // get elements for the datacenter from DC list
            /*Node datacenterNode = datacenterList.item(randDatacenterId);
            Element datacenterElement = (Element) datacenterNode;
            Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
            String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
            int placeTypeIndex = Integer.parseInt(attractiveness);
            int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
            int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
            int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());*/

            //start locating user shortly after the simulation started (e.g. 10 seconds)
//            treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
//            treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, getDCLocation(randDatacenterId));
            treeMapArray.get(i).put(SimSettings.CLIENT_ACTIVITY_START_TIME, placeDevice());

        }

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

    private Location getDCLocation(int DatacenterId) {
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

        return new Location(placeTypeIndex, wlan_id, x_pos, y_pos);
    }
    // Receives DC list and mobile device location. Check if device located within radius of the DCs
    // If yes, add it to a list
    private List<Integer> checkLegalPlacement(Location deviceLocation) {
        int x_pos = deviceLocation.getXPos();
        int y_pos = deviceLocation.getYPos();
        int hostRadius = SimSettings.getInstance().getHostRadius();
        List<Integer> hosts = new ArrayList<Integer>();
        for (int i = 0; i<SimSettings.getInstance().getNumOfEdgeDatacenters(); i++){
            Location DCLocation = getDCLocation(i);
            if (DCLocation.getXPos()-hostRadius <= x_pos && x_pos <= DCLocation.getXPos()+hostRadius){
                if (DCLocation.getYPos()-hostRadius <= y_pos && y_pos <= DCLocation.getYPos()+hostRadius)
                    hosts.add(i);
            }

        }
        return hosts;
    }
    // Receives list of hosts in whih device is in range, returns nearest host.
    private int getNearestHost(List<Integer> hosts, Location deviceLocation){
        Double minDistance = Double.MAX_VALUE;
        int minDCLocationID = -1;
        int deviceXPos = deviceLocation.getXPos();
        int deviceYPos = deviceLocation.getYPos();
        int hostXPos;
        int hostYPos;

        for (int i=0 ; i < hosts.size() ; i++){
            int host = hosts.get(i);
            hostXPos = getDCLocation(host).getXPos();
            hostYPos = getDCLocation(host).getYPos();
            //calculate euclidean distance and keep if it's lowest
            Double distance = Point2D.distance(deviceXPos, deviceYPos, hostXPos, hostYPos);
            if (distance < minDistance) {
                minDistance = distance;
                minDCLocationID = host;
            }
        }
        return minDCLocationID;
    }

    private Location placeDevice(){
        int xRange = SimSettings.getInstance().getXRange();
        int yRange = SimSettings.getInstance().getYRange();
        int xPos = 0;
        int yPos = 0;
        List<Integer> hosts = new ArrayList<Integer>();
        Location deviceLocation;

        while (hosts.size() == 0){
            xPos = SimUtils.getRandomNumber(1, xRange);
            yPos = SimUtils.getRandomNumber(1, yRange);
            deviceLocation = new Location(0,0,xPos,yPos);
            hosts = checkLegalPlacement(deviceLocation);
        }
        if (hosts.size()==1)
            return getDCLocation(hosts.get(0));
        else {
            Location host = getDCLocation(getNearestHost(hosts, new Location(0,0,xPos,yPos)));
            return new Location(host.getPlaceTypeIndex(), host.getServingWlanId(), xPos, yPos);
//            return host;
        }


    }


}
