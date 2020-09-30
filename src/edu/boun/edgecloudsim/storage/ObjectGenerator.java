//Written by Oleg Kolosov
// Create stripes of object in Redis for the simulator
//TODO: Check location distribution
package edu.boun.edgecloudsim.storage;


import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectGenerator {
    private int numOfDataObjects;
    private int numOfStripes;
    private int numOfDataInStripe;
    private int numOfParityInStripe;
    private RandomGenerator rand = null;
    static public int seed;
    //assuming same size for all objects
    private int objectSize;
    private List<List<Map>> listOfStripes;
    private HashMap<Integer, HashMap<String, Object>> objectsInHosts;
    private List<Map> dataObjects;
    private List<Map> parityObjects;
    private List<Map> metadataObjects;
    String objectPlacementPolicy;
    private int currHost;


    public ObjectGenerator(String _objectPlacementPolicy) {
        numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
        numOfStripes = SimSettings.getInstance().getNumOfStripes();
        numOfDataInStripe = SimSettings.getInstance().getNumOfDataInStripe();
        numOfParityInStripe = SimSettings.getInstance().getNumOfParityInStripe();
//        this.hostStorageCapacity = new int[SimSettings.getInstance().getNumOfEdgeDatacenters()];
        objectSize = (int)SimSettings.getInstance().getTaskLookUpTable()[0][LoadGeneratorModel.DATA_DOWNLOAD]; //bytes
        objectsInHosts = new HashMap<Integer, HashMap<String, Object>>(SimSettings.getInstance().getNumOfEdgeDatacenters());
        objectPlacementPolicy = _objectPlacementPolicy;
        seed = SimSettings.getInstance().getRandomSeed();
        ran.setSeed(seed);
        currHost=0;
        for(int i=0; i<SimSettings.getInstance().getNumOfEdgeDatacenters(); i++) {
            HashMap<String, Object> host = new HashMap<String, Object>();
            host.put("capacity",0);
            host.put("objects","");
            objectsInHosts.put(i,host);
        }

        //Get host capacities
        setHostStorageCapacity();
        //Create data objects
        dataObjects = createDataObjects(numOfDataObjects, Integer.toString(this.objectSize));
        //Initial data object placement
        InitializeDataObjectPlacement();



        if (objectPlacementPolicy.equalsIgnoreCase("CODING_PLACE")){
            listOfStripes = createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe,numOfDataObjects);
            fillHostsWithCodingObjects();
        }
        else if (objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")){
            fillHostsWithDataObjects();
            listOfStripes = createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe,numOfDataObjects);
        }
        else if (objectPlacementPolicy.equalsIgnoreCase("DATA_PARITY_PLACE")){
            listOfStripes = createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe,numOfDataObjects);
        }
        parityObjects = extractObjectsFromList(numOfDataInStripe);
        metadataObjects = extractObjectsFromList(numOfDataInStripe+numOfParityInStripe);


    }




    //TODO: fix
    private static int getNumOfDataInStripeStatic = 2;
    Random ran = new Random();

    public List<List<Map>> getListOfStripes() {
        return listOfStripes;
    }

    //Extract objects at location objectIndex from listOfStripes
    private List<Map> extractObjectsFromList(int objectIndex){
        List<Map> listOfObjects = new ArrayList(listOfStripes.size());
        for (List<Map> stripe : listOfStripes){
            //if parity was placed
            //todo:only one parity for now
            if(stripe.get(numOfDataInStripe).get("locations")!=null)
                listOfObjects.add(stripe.get(objectIndex));
        }
        return listOfObjects;
    }

    //create list of storage capacities of edge hosts
    private void setHostStorageCapacity(){
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");
        for (int i=0;i<SimSettings.getInstance().getNumOfEdgeDatacenters();i++){
            Node datacenterNode = datacenterList.item(i);
            Element datacenterElement = (Element) datacenterNode;
            int storageCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
//            hostStorageCapacity[i] = storageCapacity;
            objectsInHosts.get(i).put("capacity",storageCapacity);
        }
    }

    //Initialize random generator by seed
    private void initRan(int seed) {
//        rand = new Well19937c(System.currentTimeMillis() + System.identityHashCode(this));
        rand = new Well19937c(seed);
    }

    private RandomGenerator getRandomGenerator() {
        if (rand == null) {
            initRan(this.seed);
        }
        return rand;
    }
    //Returns location between 1-numberOfElements
/*    private int getZipf(int numberOfElements)  throws NotStrictlyPositiveException {
        return new ZipfDistribution(getRandomGenerator(), numberOfElements, SimSettings.getInstance().getZipfExponent()).sample();
    }*/

    private int getObject(int numberOfElements, String type)  throws NotStrictlyPositiveException {
        String dist="";
        if (type.equalsIgnoreCase("objects"))
            dist = SimSettings.getInstance().getObjectDistPlace();
        else if (type.equalsIgnoreCase("stripes"))
            dist = SimSettings.getInstance().getStripeDistPlace();
        else
            System.out.println("Type not recognized");

        int objectNum = -1;
        if (dist.equals("UNIFORM"))
        {
            objectNum =  ran.nextInt(numberOfElements);
        }
        else if (dist.equals("ZIPF"))
        {
            objectNum = new ZipfDistribution(getRandomGenerator(), numberOfElements, SimSettings.getInstance().getZipfExponent()).sample();
            //need to reduce by 1
            objectNum--;
        }
        return objectNum;

    }
    //Creates list of data objects with the naming convention: "object:ID"
    private List<Map> createDataObjects(int numOfDataObjects, String objectSize){
        List<Map> listOfDataObjects = new ArrayList(numOfDataObjects);
        for (int i=0; i<numOfDataObjects; i++){
            Map<String, String> map = new HashMap<String,String>();
            map.put("id", "d" + Integer.toString(i));
            map.put("size", objectSize);
            map.put("type", "data");
//            map.put("location", "");
            listOfDataObjects.add(map);
        }
        return listOfDataObjects;
    }
    //Creates list of parity objects with the naming convention: "object:ID0_ID1_..._<0...numOfParityInStripe>"
    private List<Map> createParityObjects(int numOfParityInStripe, List<Map> listOfDataObjects){
        String parityName = "p";
        List<Map> listOfParityObjects = new ArrayList(numOfParityInStripe);
        int objectSize = 0;

        for (Map<String,String> KV : listOfDataObjects){
                // Name is collection of data IDs
                String id = (KV.get("id")).replaceAll("[^\\d.]", "");
                parityName += id + "-";
                int dataObjectSize = Integer.parseInt(KV.get("size"));
                // Size is as of largest data object
                if (dataObjectSize > objectSize)
                    objectSize = dataObjectSize;
            }

        for (int i=0; i<numOfParityInStripe; i++){
            Map<String, String> map = new HashMap<String,String>();
            map.put("id", parityName + Integer.toString(i));
            map.put("size", Integer.toString(objectSize));
            map.put("type", "parity");
//            map.put("locations", "");
            listOfParityObjects.add(map);
        }
        return listOfParityObjects;
    }
    //Randomly selects a host and places all objects sequentially from it
    private List<Integer> sequentialRandomPlacement(int numOfHosts, int numofObjectsInStripe){
        int visitedHosts=1;
        List<Integer> hosts = new ArrayList<>(numofObjectsInStripe);
//        int hostID = Math.abs(getRandomGenerator().nextInt(numOfHosts));
        int hostID = currHost;
        currHost = (currHost+1)%numOfHosts;
        //find vacant host
        while ((int) objectsInHosts.get(hostID).get("capacity") < objectSize){
            if (visitedHosts==numOfHosts)
                return hosts;
            hostID = (hostID+1)%numOfHosts;
            visitedHosts++;
        }
        visitedHosts=1;
        //collect numofObjectsInStripe locations
        for (int i=0; i<numofObjectsInStripe;i++){
            hosts.add((hostID)%numOfHosts);
            hostID = (hostID+1)%numOfHosts;
            visitedHosts++;
            //find vacant host
            while ((int) objectsInHosts.get(hostID).get("capacity") < objectSize){
                if (visitedHosts==numOfHosts)
                    return hosts;
                hostID = (hostID+1)%numOfHosts;
//                if (hosts.contains(hostID))
                visitedHosts++;
            }

        }
        return hosts;
    }
    //Add host ID to each object according to placement policy
    private List<Map> placeObjects(int numofObjectsInStripe, List<Map> stripe){
        int numOfHosts = SimSettings.getInstance().getNumOfEdgeDatacenters();
        List<Integer> listOfPlacements = sequentialRandomPlacement(numOfHosts, numofObjectsInStripe);
        if(listOfPlacements.size()<numofObjectsInStripe) {
            return Collections.emptyList();
        }
        int i=0;
        for (Map<String,String> KV : stripe){
            String locations = KV.get("locations");
            String objectID = KV.get("id");
            int currentHost = listOfPlacements.get(i);
//            String currentHostObjects = (String)objectsInHosts.get(currentHost).get("objects");
            //if not in host, add it
/*            if(!currentHostObjects.contains(" " + objectID + " ")) {
                int capacity = (int)objectsInHosts.get(currentHost).get("capacity");
                objectsInHosts.get(currentHost).put("capacity",capacity-objectSize);
                objectsInHosts.get(currentHost).put("objects", addLocation(objectID, currentHostObjects));
            }*/

//            String objects = (String)objectsInHosts.get(currentHost).get("objects");
            String currentHostObjects = (String)objectsInHosts.get(currentHost).get("objects");
            StringTokenizer st= new StringTokenizer(currentHostObjects, " "); // Space as delimiter
            Set<String> objectsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                objectsSet.add(st.nextToken());

            //if object is already in node select another
//            if(currentHostObjects.contains(objectID + " ")) {
            if(!objectsSet.contains(objectID)) {
                int capacity = (int)objectsInHosts.get(currentHost).get("capacity");
                objectsInHosts.get(currentHost).put("capacity",capacity-objectSize);
                objectsInHosts.get(currentHost).put("objects", addLocation(objectID, currentHostObjects));
            }


            //if not empty, add to unique set
            if (locations != null){
            st= new StringTokenizer(locations, " "); // Space as delimiter
            Set<String> locationsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            locationsSet.add(Integer.toString(currentHost));
            locations = "";
            for (String loc:locationsSet)
                locations += loc + " ";
            KV.put("locations", locations);
            }
            //else just one location
            else {
                KV.put("locations", Integer.toString(currentHost));
            }
            i++;
        }
        return stripe;
    }

    private Map<String, String> createMetadataObject(int numofObjectsInStripe, List<Map> stripe){
//        List<Map> objects = new ArrayList<>(numofObjectsInStripe);
        Map<String, String> metadataObject = new HashMap<String,String>();
        String name = "";
//        Map<String, String> parityMap = new HashMap<String,String>();

//        int i=0;
        for (Map<String,String> KV : stripe){
            if (KV.get("type") == "data"){
                String id = KV.get("id");
                name += "_" + id;
                if (metadataObject.get("data") == null)
                    metadataObject.put("data", id);
                else
                metadataObject.put("data", metadataObject.get("data") + " " + id);
            }
            else{
                String id = KV.get("id");
                name += "_" + id;
                if (metadataObject.get("parity") == null)
                    metadataObject.put("parity", id);
                else
                    metadataObject.put("parity", metadataObject.get("data") + " " + id);
            }
        }
        metadataObject.put("id", "md" + name);
        metadataObject.put("type", "metadata");
        return metadataObject;
    }
    //uniformly allocate all objects in hosts
    private void InitializeDataObjectPlacement(){
        int numOfHosts = SimSettings.getInstance().getNumOfEdgeDatacenters();
        int host = currHost;
        currHost = (currHost+1)% numOfHosts;
        //go over each object
        for (Map<String,String> KV : dataObjects){
            KV.put("locations", Integer.toString(host));
            objectsInHosts.get(host).put("capacity", (int) objectsInHosts.get(host).get("capacity")-objectSize);
            objectsInHosts.get(host).put("objects", addLocation(KV.get("id"), (String) objectsInHosts.get(host).get("objects")));
            //TODO: write test
            if ((int) objectsInHosts.get(host).get("capacity")<0)
                System.out.println("hostStorageCapacity[host]<0");
            //next host in list
            host = currHost;
            currHost = (currHost+1)% numOfHosts;
        }
    }

    //place coding objects in hosts by policy
    private void fillHostsWithCodingObjects(){
        String stripeDist = SimSettings.getInstance().getStripeDistPlace();
        int numOfHosts = SimSettings.getInstance().getNumOfEdgeDatacenters();
        int i=1;
        int currentHost=0;
        int stripeID;
        stripeID = getObject(numOfStripes,"stripes");
        String objectName;
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");
        while(1==1) {
            //TODO: currently support one parity
            objectName = (String)listOfStripes.get(stripeID).get(numOfDataInStripe).get("id");
            String currentHostObjects = (String) objectsInHosts.get(currentHost).get("objects");

            String objects = (String)objectsInHosts.get(currentHost).get("objects");
            StringTokenizer st= new StringTokenizer(objects, " "); // Space as delimiter
            Set<String> objectsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                objectsSet.add(st.nextToken());

            //if object is already in node select another
//            if(currentHostObjects.contains(objectID + " ")) {
            if(objectsSet.contains(objectName)) {
                stripeID = getObject(numOfStripes,"stripes");
                continue;
            }
            else {
                String locations = (String)listOfStripes.get(stripeID).get(numOfDataInStripe).get("locations");
                //add new location
                listOfStripes.get(stripeID).get(numOfDataInStripe).put("locations",addLocation(Integer.toString(currentHost),locations));
                objectsInHosts.get(currentHost).put("capacity", (int) objectsInHosts.get(currentHost).get("capacity")-objectSize);
                objectsInHosts.get(currentHost).put("objects", addLocation(objectName, currentHostObjects));

/*                Node datacenterNode = datacenterList.item(currentHost);
                Element datacenterElement = (Element) datacenterNode;
                int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                objects = (String)objectsInHosts.get(currentHost).get("objects");
                st= new StringTokenizer(objects, " "); // Space as delimiter
                objectsSet = new HashSet<String>();
                while (st.hasMoreTokens())
                    objectsSet.add(st.nextToken());
                if ((objectsSet.size() * objectSize) < hostCapacity - (int) objectsInHosts.get(currentHost).get("capacity"))
                    System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(currentHost));*/

                currentHost = (currentHost+1)%numOfHosts;
                //run until vacant host is found or return
                while ((int) objectsInHosts.get(currentHost).get("capacity") < objectSize)
                {
                    if(i==numOfHosts) {
                        //check if hosts are full
                        for(int j=0;j<numOfHosts ; j++) {
                            Node datacenterNode = datacenterList.item(j);
                            Element datacenterElement = (Element) datacenterNode;
                            int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                            objects = (String)objectsInHosts.get(j).get("objects");
                            st= new StringTokenizer(objects, " "); // Space as delimiter
                            objectsSet = new HashSet<String>();
                            while (st.hasMoreTokens())
                                objectsSet.add(st.nextToken());
                            if ((objectsSet.size() * objectSize) < hostCapacity)
                                System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                        }
                        return; //all are full
                    }
                    currentHost = (currentHost+1)%numOfHosts;
                    i++;
                }
                i=1;
                stripeID = getObject(numOfStripes,"stripes");
            }
        }
    }

    //place coding objects in hosts by policy
    private void fillHostsWithDataObjects(){
        String objectDist = SimSettings.getInstance().getObjectDistPlace();
        int numOfHosts = SimSettings.getInstance().getNumOfEdgeDatacenters();
        int i=1;
        int currentHost=0;
        int objectID=0;
        objectID = getObject(numOfDataObjects,"objects");
        String objectName;
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");
        while(1==1) {
            objectName = (String)dataObjects.get(objectID).get("id");
            String currentHostObjects = (String) objectsInHosts.get(currentHost).get("objects");

            String objects = (String)objectsInHosts.get(currentHost).get("objects");
            StringTokenizer st= new StringTokenizer(objects, " "); // Space as delimiter
            Set<String> objectsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                objectsSet.add(st.nextToken());

            //if object is already in node select another
//            if(currentHostObjects.contains(objectID + " ")) {
            if(objectsSet.contains(objectName)) {
                objectID = getObject(numOfDataObjects,"objects");
                continue;
            }
            else {
                String locations = (String)dataObjects.get(objectID).get("locations");
                //add new location
                dataObjects.get(objectID).put("locations",addLocation(Integer.toString(currentHost),locations));
                objectsInHosts.get(currentHost).put("capacity", (int) objectsInHosts.get(currentHost).get("capacity")-objectSize);
                objectsInHosts.get(currentHost).put("objects", addLocation(objectName, currentHostObjects));

                currentHost = (currentHost+1)%numOfHosts;
                //run until vacant host is found or return
                while ((int) objectsInHosts.get(currentHost).get("capacity") < objectSize)
                {
                    if(i==numOfHosts) {
                        //check if hosts are full
                        for(int j=0;j<numOfHosts ; j++) {
                            Node datacenterNode = datacenterList.item(j);
                            Element datacenterElement = (Element) datacenterNode;
                            int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                            objects = (String)objectsInHosts.get(j).get("objects");
                            st= new StringTokenizer(objects, " "); // Space as delimiter
                            objectsSet = new HashSet<String>();
                            while (st.hasMoreTokens())
                                objectsSet.add(st.nextToken());
                            if ((objectsSet.size() * objectSize) != hostCapacity)
                                System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                        }

                        return; //all are full
                    }
                    currentHost = (currentHost+1)%numOfHosts;
                    i++;
                }
                i=1;
                objectID = getObject(numOfDataObjects,"objects");
            }
        }
    }

    private String addLocation(String toAdd, String locations){
        if (locations != null) {
            //get list of locations of object
            StringTokenizer st = new StringTokenizer(locations, " "); // Space as delimiter
            Set<String> locationsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            //add location
            locationsSet.add(toAdd);
            String newLocations = "";
            //convert back to list
            for (String loc : locationsSet)
                newLocations += loc + " ";
            return newLocations;
        }
        else {
            return toAdd;
        }
    }

    private List<List<Map>> createStripes(int numOfStripes, int numOfDataInStripe, int numOfParityInStripe, int numOfDataObjects){

        List<List<Map>> listOfStripes = new ArrayList();
        List<Set<Integer>> existingStripes = new ArrayList();
        // For each stripe
        for (int i = 0; i< numOfStripes; i++){
            List<Map> dataList = new ArrayList();
            Set<Integer> listOfIndices = new HashSet<Integer>();
            // Collect data objects with selected distribution
            for (int j = 0; j<numOfDataInStripe; j++) {
                int objectID = getObject(numOfDataObjects,"objects");
                //Check if same object not used twice. If yes, repeat.
                if (listOfIndices.contains(objectID)){
                    j--;
                }
                else {
                    listOfIndices.add(objectID);
                    dataList.add(dataObjects.get(objectID));
                }
            }
            //Check for duplicacy in stripes
            if (existingStripes.contains(listOfIndices)){
                i--;
                continue;
            }
            else{
                existingStripes.add(listOfIndices);
            }
            // Calculate parity for the data objects
            List<Map> parityList = createParityObjects(numOfParityInStripe, dataList);
            // Concatenate data and parity
            List<Map> stripe = Stream.concat(dataList.stream(), parityList.stream())
                    .collect(Collectors.toList());
            //place objects
            if (objectPlacementPolicy.equalsIgnoreCase("DATA_PARITY_PLACE")) {
                stripe = placeObjects(numOfDataInStripe + numOfParityInStripe, stripe);
                if (stripe.size() == 0) { //can't fill a whole stripe
                    List<Map> oneObjectStripe;

                    do {
                        int objectID = getObject(numOfDataObjects,"objects");
                        dataList = new ArrayList();
                        dataList.add(dataObjects.get(objectID));
                        oneObjectStripe = placeObjects(1, dataList);
                    }
                    while (oneObjectStripe.size()!=0);
                    return listOfStripes;
                }
            }
            Map<String, String>  metadataObject = createMetadataObject(numOfDataInStripe+numOfParityInStripe,
                    stripe);
            stripe.add(metadataObject);
            listOfStripes.add(stripe);
        }
        return listOfStripes;
    }

    public static int getNumOfDataInStripe() {
        return getNumOfDataInStripeStatic;
    }
    public int getNumOfObjectsInStripe(){
        return numOfDataInStripe+numOfParityInStripe;
    }

    public List<Map> getDataObjects() {
        return dataObjects;
    }

    public List<Map> getParityObjects() {
        return parityObjects;
    }

    public List<Map> getMetadataObjects() {
        return metadataObjects;
    }

    public HashMap<Integer, HashMap<String, Object>> getObjectsInHosts() {
        return objectsInHosts;
    }

}
