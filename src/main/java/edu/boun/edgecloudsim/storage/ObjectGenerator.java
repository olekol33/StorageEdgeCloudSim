//Written by Oleg Kolosov
// Create stripes of object in Redis for the simulator
//TODO: Check location distribution
package edu.boun.edgecloudsim.storage;



import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//changed by Harel

public class ObjectGenerator {
    private int numOfDataObjects;
    private int numOfStripes;
    private int numOfDataInStripe;
    private int numOfParityInStripe;
    private RandomGenerator rand;
    private RandomGenerator newObjectRand;
    private RandomGenerator newObjectLoadRand;
    public static int seed;
//    private static RandomGenerator rand = null;
    //assuming same size for all objects
    private int objectSize;
    private HashMap<String,List<Map>> listOfStripes;
    private HashMap<Integer, HashMap<String, Object>> hostsContents;
    private HashMap<String,Map> dataObjects;
    private List<Map> parityObjects;
    private List<Map> metadataObjects;
    String objectPlacementPolicy;
    private int currHost;
    private Map<Integer,List<Map>> objectsInHosts;
    private List<Map> listOfObjects;
    private double overhead;
    private int locationDelta;
    private int numOfNodes;
    private static final double zipfExponent = SimSettings.getInstance().getZipfExponent();

    //TODO: check why 21 objects generated when capacity is 20 (2 nodes)
    Random ran;


    public ObjectGenerator(String _objectPlacementPolicy) {
        seed = SimSettings.getInstance().getRandomSeed();
        numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
        numOfStripes = SimSettings.getInstance().getNumOfStripes();
        numOfDataInStripe = SimSettings.getInstance().getNumOfDataInStripe();
        numOfParityInStripe = SimSettings.getInstance().getNumOfParityInStripe();
        if (SimSettings.getInstance().isOrbitMode()) {
            objectSize = (int) SimSettings.getInstance().getTaskLookUpTable()[0][LoadGeneratorModel.DATA_DOWNLOAD]; //bytes
            numOfNodes=SimSettings.getInstance().getNumOfEdgeDatacenters();
        }
        else {
            numOfNodes=SimSettings.getInstance().getNumOfEdgeDatacenters();
            objectSize = (int) SimSettings.getInstance().getTaskLookUpTable()[0][LoadGeneratorModel.DATA_DOWNLOAD];
        }
        hostsContents = new HashMap<Integer, HashMap<String, Object>>(numOfNodes);
        objectPlacementPolicy = _objectPlacementPolicy;
        ran = new Random();
        ran.setSeed(seed);
        listOfStripes = new HashMap<>();
//        newObjectRand = new Well19937c(seed);
        resetNewObjectRandomGenerator(seed);
        newObjectLoadRand = new Well19937c(seed);
        getRandomGenerator();
        currHost=0;
        locationDelta=1;
        for(int i=0; i<numOfNodes; i++) {
            HashMap<String, Object> host = new HashMap<String, Object>();
            host.put("capacity",0);
            host.put("objects","");
            hostsContents.put(i,host);
        }

        //Get host capacities
        setHostStorageCapacity();
        //Create data objects
        if(!SimSettings.getInstance().isExternalObjects()) {
            createDataObjects(numOfDataObjects, Integer.toString(this.objectSize));
        }else{//import objects from file
            importObjectsFromFile(numOfDataObjects);
        }
        //Initial data object placement
        if(SimSettings.getInstance().isNsfExperiment()) {
            //RAID 4 or 5
            if(SimSettings.getInstance().getRAID()==4)
                NSFBSFInitializeDataObjectPlacement();
            else             //RAID 5 NSF
                InitializeDataObjectPlacement();
        }
        else
            InitializeDataObjectPlacement();



        if (objectPlacementPolicy.equalsIgnoreCase("CODING_PLACE")){
            createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe);
            fillHostsWithCodingObjects();
        }
        else if (objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")){
            fillHostsWithDataObjects();
        }
        else if (objectPlacementPolicy.equalsIgnoreCase("DATA_PARITY_PLACE")){
            //fill in predefined ratio
            partiallyFillHostsWithDataObjects();
            createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe);
            fillHostsWithCodingObjects();
        }

        listOfObjects = getDataObjects();

        if (!objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")) {
            parityObjects = extractObjectsFromList(numOfDataInStripe);
            metadataObjects = extractObjectsFromList(numOfDataInStripe + numOfParityInStripe);
            addObjectLocationsToMetadata();
            completeMetadataForDataObjects();
            listOfObjects.addAll(parityObjects);
            listOfObjects.addAll(metadataObjects);
        }


        if(SimSettings.getInstance().isOrbitMode()) {
            try {
                exportObjectList();
                SimLogger.printLine("Object list generated");
            } catch (Exception e) {
                SimLogger.printLine("Failed to generate object list");
                System.exit(0);
            }

        Map<Integer, ArrayList<Double>> timeToReadStripe = new HashMap<Integer, ArrayList<Double>>();
        objectsInHosts = new HashMap<Integer, List<Map>>(numOfNodes);
        populateObjectsInHosts();
        }

//        //reset to be used in object selection for workload
//        newObjectRand = new Well19937c(seed);

    }

    public void resetNewObjectRandomGenerator(int seed){
        newObjectRand = new Well19937c(seed);
    }

    private void exportObjectList() throws IOException {
        File objectListFile = null;
        FileWriter objectListFW = null;
        BufferedWriter objectListBW = null;
        String tmpFolder = "";
        if(SystemUtils.IS_OS_WINDOWS)
            tmpFolder = SimSettings.getInstance().getOutputFolder();
        else
            tmpFolder = "/tmp/";
        objectListFile = new File(tmpFolder,"object_list.csv");
        objectListFW = new FileWriter(objectListFile, false);
        objectListBW = new BufferedWriter(objectListFW);
        objectListBW.write("objectID,objectLocations");
        objectListBW.newLine();
        for(Map<String,String>  object:dataObjects.values()){
            objectListBW.write(object.get("id")+","+object.get("locations"));
            objectListBW.newLine();
        }
        if (!objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")) {
            for (Map<String, String> object : dataObjects.values()) {
                objectListBW.write(object.get("id") + "," + object.get("locations"));
                objectListBW.newLine();
            }
        }
        objectListBW.close();
        //metadata list
        if (!objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")){
        objectListFile = new File(tmpFolder, "stripe_list.csv");
        objectListFW = new FileWriter(objectListFile, false);
        objectListBW = new BufferedWriter(objectListFW);
        objectListBW.write("dataObjects,parityObjects");
        objectListBW.newLine();



            for (Map<String, String> object : metadataObjects) {
                objectListBW.write(object.get("data") + "," + object.get("parity"));
                objectListBW.newLine();
            }

        }
        objectListBW.close();
    }


    private void populateObjectsInHosts () {
        for (Map<String, String> object : listOfObjects) {
            //if data or parity place only in specific locations
            if (!object.get("type").equals("metadata")) {
                String locations = object.get("locations");
                if (locations != null) {
                    StringTokenizer st = new StringTokenizer(locations, " "); // Space as delimiter
                    Set<String> locationsSet = new HashSet<String>();
                    while (st.hasMoreTokens())
                        locationsSet.add(st.nextToken());
                    for (String location : locationsSet) {
                        int host = Integer.valueOf(location);
                        List<Map> currentObjects = objectsInHosts.get(host);
                        if (currentObjects == null) {
                            currentObjects = new ArrayList<>();
                        }
                        currentObjects.add(object);
                        objectsInHosts.put(host, currentObjects);
                    }
                }
            }
            else { //if metadata place in client
                for(int i=0; i<numOfNodes; i++) {
                    List<Map> currentObjects = objectsInHosts.get(i);
                    if (currentObjects == null) {
                        currentObjects = new ArrayList<>();
                    }
                    currentObjects.add(object);
                    objectsInHosts.put(i, currentObjects);
                }
            }
        }
    }


    public static Set<String> tokenizeList (String list){
        StringTokenizer st= new StringTokenizer(list, " "); // Space as delimiter
        Set<String> set = new HashSet<String>();
        while (st.hasMoreTokens())
            set.add(st.nextToken());
        return set;
    }

    private void addObjectLocationsToMetadata(){
        for (Map<String,String> KV : metadataObjects) {
            Set<String> dataObjects = tokenizeList(KV.get("data"));
            Set<String> parityObjects = tokenizeList(KV.get("parity"));
            for (String object : dataObjects){
                for (Map<String,String> entry : this.dataObjects.values()){
                    if(entry.get("id").equals(object))
                        KV.put(object,entry.get("locations"));
                }
            }
            for (String object : parityObjects){
                for (Map<String,String> entry : this.parityObjects){
                    if(entry.get("id").equals(object))
                        KV.put(object,entry.get("locations"));
                }
            }

        }
    }

    public HashMap<String,List<Map>> getListOfStripes() {
        return listOfStripes;
    }

    //Extract objects at location objectIndex from listOfStripes
    private List<Map> extractObjectsFromList(int objectIndex){
        List<Map> listOfObjects = new ArrayList(listOfStripes.size());
        for (List<Map> stripe : listOfStripes.values()){
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
        int totalStorageCapacity = 0;
        for (int i=0;i<numOfNodes;i++){
            Node datacenterNode = datacenterList.item(i);
            Element datacenterElement = (Element) datacenterNode;
            int storageCapacity=0;
            try {
                storageCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                totalStorageCapacity += storageCapacity;
            }
            catch (Exception e){
                System.out.println("Failed reading node "+ String.valueOf(i));
                System.out.println(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                e.printStackTrace();
                System.exit(0);
            }
//            hostStorageCapacity[i] = storageCapacity;
            hostsContents.get(i).put("capacity",storageCapacity);
        }
        int objectCapacity = totalStorageCapacity / this.objectSize;
        System.out.println("System object capacity: " + String.valueOf(this.numOfDataObjects)+ " data objects out of " +
                String.valueOf(objectCapacity) + " objects in " + String.valueOf(numOfNodes)+
                " nodes");
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

/*    public static String getObject(){
        String dist=SimSettings.getInstance().getObjectDistRead();
        List<String> listOfDataObjects = RedisListHandler.getObjectsFromRedis("object:d*");
        int objectNum = -1;
        if (dist.equals("UNIFORM"))
        {
            objectNum =  rand.nextInt(SimSettings.getInstance().getNumOfDataObjects());
        }
        else if (dist.equals("ZIPF"))
        {
            objectNum = new ZipfDistribution(rand, SimSettings.getInstance().getNumOfDataObjects(), zipfExponent).sample();
            //need to reduce by 1
            objectNum--;
        }
//        return RedisListHandler.getObjectID(dataObjects.get(objectNum));
        return "1";
    }*/

    public int getObjectID(int numberOfElements, String type, String dist)  throws NotStrictlyPositiveException {
        int objectNum = -1;
        if (dist.equals("UNIFORM"))
        {
            objectNum =  newObjectRand.nextInt(numberOfElements);
        }
        else if (dist.equals("ZIPF") || dist.equals("MULTIZIPF"))
        {
            objectNum = new ZipfDistribution(newObjectRand, numberOfElements, SimSettings.getInstance().getZipfExponent()).sample();
            //need to reduce by 1
            objectNum--;
        }
        return objectNum;

    }

    public String getDataObjectID(){
        String dist = SimSettings.getInstance().getObjectDistRead();
        int objectID = getObjectID(numOfDataObjects,"objects",dist);
        return (String)dataObjects.get("d"+String.valueOf(objectID)).get("id");
    }
    public String getDataObjectID(int objectID){
        return (String)dataObjects.get(objectID).get("id");
    }


    private String getBinaryString(int n) {
        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to AlphaNumericString variable length
            sb.append(Math.abs(rand.nextInt())%2);
        }
        return sb.toString();
    }



    //Creates list of data objects with the naming convention: "object:ID"
    private void createDataObjects(int numOfDataObjects, String objectSize){
//        List<Map> listOfDataObjects = new ArrayList(numOfDataObjects);
        dataObjects = new HashMap<String,Map>();
        for (int i=0; i<numOfDataObjects; i++){
            HashMap<String, String> map = new HashMap<String,String>();
            map.put("id", "d" + Integer.toString(i));
            map.put("size", objectSize);
            map.put("type", "data");
            if (SimSettings.getInstance().isOrbitMode()){
                map.put("data", getBinaryString(Integer.valueOf(objectSize)));
            }
//            map.put("location", "");
            dataObjects.put("d" + Integer.toString(i),map);
        }
    }

    //TODO: created by Harel
    //Creates list of data objects that was given as external file
    private void importObjectsFromFile(int numOfDataObjects){
        //List<Map> listOfDataObjects = new ArrayList(numOfDataObjects);
        dataObjects = new HashMap<String,Map>();
        for (int i=0; i<numOfDataObjects; i++){
            HashMap<String, String> map = new HashMap<String,String>();
            StorageObject sObject = SimSettings.getInstance().getObjectsVector().get(i);
            //Map<String, String> map = new HashMap<String,String>();
            map.put("id", sObject.getObjName());
            map.put("size", sObject.getObjSize());
            map.put("type", sObject.getType());
            if (SimSettings.getInstance().isOrbitMode()){
                map.put("data", getBinaryString(Integer.valueOf(objectSize)));
            }
//            map.put("location", "");
            //listOfDataObjects.add(map);
            dataObjects.put(sObject.getObjName(), map);
        }
        //return listOfDataObjects;
    }

    //Creates list of parity objects with the naming convention: "object:ID0_ID1_..._<0...numOfParityInStripe>"
    private List<Map> createParityObjects(int numOfParityInStripe, List<Map> listOfDataObjects){
        String parityName = "p";
        List<Map> listOfParityObjects = new ArrayList(numOfParityInStripe);
        int objectSize = 0;
        BigInteger biginteger=null;

        for (Map<String,String> KV : listOfDataObjects){
                // Name is collection of data IDs
                String id = (KV.get("id")).replaceAll("[^\\d.]", "");
            if (SimSettings.getInstance().isOrbitMode()){
                if (biginteger==null)
                     biginteger = new BigInteger(KV.get("data"),2);
                else {
                    BigInteger val = new BigInteger(KV.get("data"),2);
                    biginteger = biginteger.xor(val);
                }
            }
                parityName += id + "-";
                int dataObjectSize = Integer.parseInt(KV.get("size"));
                // Size is of the largest data object
                if (dataObjectSize > objectSize)
                    objectSize = dataObjectSize;
            }

        for (int i=0; i<numOfParityInStripe; i++){
            Map<String, String> map = new HashMap<String,String>();
            map.put("id", parityName + Integer.toString(i));
            map.put("size", Integer.toString(objectSize));
            map.put("type", "parity");
            if (SimSettings.getInstance().isOrbitMode()){
                map.put("data", biginteger.toString(2));
            }
//            map.put("locations", "");
            listOfParityObjects.add(map);
        }
        return listOfParityObjects;
    }
    //Randomly selects a host and places all objects sequentially from it
    private List<Integer> sequentialRandomPlacement(int numOfHosts, int numofObjectsInStripe){
        int visitedHosts=1;
        List<Integer> hosts = new ArrayList<>(numofObjectsInStripe);
        int hostID = currHost;
        currHost = (currHost+1)%numOfHosts;
        //find vacant host
        while ((int) hostsContents.get(hostID).get("capacity") < objectSize){
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
            while ((int) hostsContents.get(hostID).get("capacity") < objectSize){
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
        int numOfHosts = numOfNodes;
        List<Integer> listOfPlacements = sequentialRandomPlacement(numOfHosts, numofObjectsInStripe);
        if(listOfPlacements.size()<numofObjectsInStripe) {
            return Collections.emptyList();
        }
        int i=0;
        for (Map<String,String> KV : stripe){
            String locations = KV.get("locations");
            String objectID = KV.get("id");
            int currentHost = listOfPlacements.get(i);
/*            String currentHostObjects = (String) hostsContents.get(currentHost).get("objects");
            StringTokenizer st= new StringTokenizer(currentHostObjects, " "); // Space as delimiter
            Set<String> objectsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                objectsSet.add(st.nextToken());*/
            Set<String> objectsSet = stringTokenizer((String) hostsContents.get(currentHost).get("objects"));

            //if object is not in current set
            if(!objectsSet.contains(objectID)) {
                int capacity = (int) hostsContents.get(currentHost).get("capacity");
                hostsContents.get(currentHost).put("capacity",capacity-objectSize);
                hostsContents.get(currentHost).put("objects", addLocation(objectID, (String) hostsContents.get(currentHost).get("objects")));
            }


            //if locations  not empty, add to unique set
            if (locations != null){
/*                st= new StringTokenizer(locations, " "); // Space as delimiter
                Set<String> locationsSet = new HashSet<String>();
                while (st.hasMoreTokens())
                    locationsSet.add(st.nextToken());*/
                Set<String> locationsSet = stringTokenizer(locations);
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

    //Provides locations of data objects not located on the client
    private void completeMetadataForDataObjects(){
        List<Map> dataMetadataObjects = new ArrayList(numOfDataObjects);

        for (Map<String,String> KV : dataObjects.values()){
            boolean noMetadata=true;
            String objectID = KV.get("id");
            for (int i=0; i<metadataObjects.size();i++){
                if (metadataObjects.get(i).containsKey(objectID)) {
                    noMetadata=false;
                    break;
                }
            }
            if(noMetadata) {
                Map<String, String> metadataObject = new HashMap<String, String>();
                metadataObject.put("id", "md_" + objectID);
                metadataObject.put("data", objectID);
                metadataObject.put("type", "metadata");
                metadataObject.put(objectID, KV.get("locations"));
                dataMetadataObjects.add(metadataObject);

            }
        }
        metadataObjects.addAll(dataMetadataObjects);
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

    private int getRemainingStorageCapacity(){
        int totalCapacity=0;
        for (int i=0;i<hostsContents.size();i++)
            totalCapacity += (int)hostsContents.get(i).get("capacity");
        return totalCapacity;
    }
    //uniformly allocate all objects in hosts
    private void InitializeDataObjectPlacement(){
        int numOfHosts = numOfNodes;
        int host = currHost;
        currHost = (currHost+1)% numOfHosts;
        int initialCapacity=0;
        int finalCapacity=0;
/*        for (int i=0;i<hostsContents.size();i++)
            initialCapacity += (int)hostsContents.get(i).get("capacity");*/
        initialCapacity = getRemainingStorageCapacity();
        //go over each object
        for (Map<String,String> KV : dataObjects.values()){
            KV.put("locations", Integer.toString(host));
            int actualObjectSize;
            if(SimSettings.getInstance().isExternalObjects())
                actualObjectSize = Integer.valueOf(KV.get("size")) ;
            else
                actualObjectSize = objectSize;
            hostsContents.get(host).put("capacity", (int) hostsContents.get(host).get("capacity")-actualObjectSize);
            hostsContents.get(host).put("objects", addLocation(KV.get("id"), (String) hostsContents.get(host).get("objects")));
            if ((int) hostsContents.get(host).get("capacity")<0)
                System.out.println("hostStorageCapacity[host]<0");
            //next host in list
            host = currHost;
            currHost = (currHost+1)% numOfHosts;
        }
/*        for (int i=0;i<hostsContents.size();i++)
            finalCapacity += (int)hostsContents.get(i).get("capacity");*/
        finalCapacity = getRemainingStorageCapacity();
        if(finalCapacity<0){
            System.out.println("Negative capacity");
            System.exit(1);
        }
        overhead = (double) initialCapacity / (initialCapacity-finalCapacity);
    }

    //Object from each group are on a separate host
    private void NSFBSFInitializeDataObjectPlacement(){
        int numOfDataHosts = 2;
        int host = currHost;
        currHost = (currHost+1)% numOfDataHosts;
        //go over each object
        for (Map<String,String> KV : dataObjects.values()){
            KV.put("locations", Integer.toString(host));
            hostsContents.get(host).put("capacity", (int) hostsContents.get(host).get("capacity")-objectSize);
            hostsContents.get(host).put("objects", addLocation(KV.get("id"), (String) hostsContents.get(host).get("objects")));
            if ((int) hostsContents.get(host).get("capacity")<0)
                System.out.println("hostStorageCapacity[host]<0");
            //next host in list
            host = currHost;
            currHost = (currHost+1)% numOfDataHosts;
        }
    }

    private int nextVacantHost(int realObjectSize, int currentHost){
        int i=0;
        while ((int) hostsContents.get(currentHost).get("capacity") < realObjectSize) {
            if(i==numOfNodes)
                return -1;
            currentHost = (currentHost + 1) % numOfNodes;
            i++;
        }
        return currentHost;
    }

    //place coding objects in hosts by policy
    private void fillHostsWithCodingObjects(){
        int i=1;
        int currentHost=0;
        int deadlockCount=0;
        String objectName="";
        List<String> listOfStripeIDs = null;

        //monitor parity occurrences
        HashMap<Integer,List<String>> objectsPlaced = new HashMap<>();
        List<String> objectList = new ArrayList<String>();
        for(List<Map> object:listOfStripes.values()) //create list of object names
            objectList.add((String)object.get(numOfDataInStripe).get("id"));
        objectsPlaced.put(0,objectList);
        objectsPlaced.put(1,new ArrayList<String>());
        int lowestNumOfOccurrences=0;

        //select stripe according to popularity policy
        if(SimSettings.getInstance().isNsfExperiment()) {
            //keep all coding objects in the list
            listOfStripeIDs = new ArrayList<>(listOfStripes.keySet());
            objectName = (String)listOfStripes.keySet().toArray()[0];
            listOfStripeIDs.remove(objectName);
        }
        else {
            List<String> lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
            objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
//            stripeID = getObjectID(numOfStripes, "stripes",dist);
        }
//        String objectName;
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");

        int realObjectSize = Integer.valueOf((String)listOfStripes.get(objectName).get(numOfDataInStripe).get("size"));
        //Get next vacant host
        currentHost = nextVacantHost(realObjectSize,currentHost);
        if(currentHost==-1)
            return;
//        int realObjectSize = Integer.valueOf((String)listOfStripes.get(objectName).get(numOfDataInStripe).get("size"));
//        while ((int) hostsContents.get(currentHost).get("capacity") < realObjectSize)
//            currentHost = (currentHost+1)%numOfNodes;

        int placementattempts=0;
        String attemptedObject = "";
        int attemptedHost = -1;
        while(1==1) {
            if(SimSettings.getInstance().getRAID()==4 && SimSettings.getInstance().isNsfExperiment())
                    currentHost=2;
            //get name of object by its ID
            if(SimSettings.getInstance().isNsfExperiment())
                objectName = (String)listOfStripes.get(objectName).get(numOfDataInStripe).get("id");
            //change host if enough tried
            if(placementattempts>numOfNodes){
                //if previously attempted this object
                if((attemptedObject==objectName)){
                    if(attemptedHost==currentHost) { //if tried all vacant hosts
                        objectsPlaced.get(lowestNumOfOccurrences).remove(objectName); //remove it
                        if (objectsPlaced.get(lowestNumOfOccurrences).isEmpty()) {
                            objectsPlaced.remove(lowestNumOfOccurrences);
                            lowestNumOfOccurrences++;
                            //1 above current might not exist
                            if(!objectsPlaced.containsKey(lowestNumOfOccurrences+1))
                                objectsPlaced.put(lowestNumOfOccurrences+1,new ArrayList<String>());
                        }
                        List<String> lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
                        objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
                        continue;
                    }
                }
                else {
                    attemptedObject = objectName;
                    attemptedHost=currentHost;
                }
                currentHost = (currentHost+1)%numOfNodes;
                currentHost = nextVacantHost(realObjectSize,currentHost);
                placementattempts=0;

                if(currentHost==-1)
                    return;
                continue;
            }
            //start with host 0, get objects it contains
            String currentHostObjects = (String) hostsContents.get(currentHost).get("objects");
            Set<String> objectsSet = stringTokenizer((String) hostsContents.get(currentHost).get("objects"));

            //if object is already in node
            if(objectsSet.contains(objectName)) {
                //select another
                if (SimSettings.getInstance().getDeepFileLoggingEnabled())
                    System.out.println(objectName + " is already in " + String.valueOf(currentHost));
                List<String> lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
                objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
                placementattempts++;
                continue;
            }
            placementattempts=0;
            realObjectSize = Integer.valueOf((String)listOfStripes.get(objectName).get(numOfDataInStripe).get("size"));
            //check if host can contain object
            while ((int) hostsContents.get(currentHost).get("capacity") < realObjectSize)
                currentHost = (currentHost+1)%numOfNodes;
            //flag to avoid having parity and data of same stripe in the same host
            boolean parityDataInSameHost=false;


            for (int id=0; id<numOfDataInStripe;id++){
                //if host contains data objects of stripe - select new stripe and break
                if(objectsSet.contains((String)listOfStripes.get(objectName).get(id).get("id"))){
                    if(SimSettings.getInstance().isNsfExperiment()) {
                        //return previous id to list and select next
                        listOfStripeIDs.add(objectName);
//                        stripeID = listOfStripeIDs.get(0);
                        objectName = (String)listOfStripes.keySet().toArray()[0];
                        listOfStripeIDs.remove(0);
                    }
                    else { //select new stripe
                        if (SimSettings.getInstance().getDeepFileLoggingEnabled())
                            System.out.println("Data of " + objectName + " is already in " + String.valueOf(currentHost));
                        List<String> lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
                        objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
                        if(deadlockCount>numOfNodes*10){
                            //it couldn't be placed anyway
/*                            objectsPlaced.remove(lowOccurrenceObjects);
                            lowestNumOfOccurrences++;
                            //1 above current might not exist
                            if(!objectsPlaced.containsKey(lowestNumOfOccurrences+1))
                                objectsPlaced.put(lowestNumOfOccurrences+1,new ArrayList<String>());
                            lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);*/
//                            objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
                            //change host if enough tried
                            currentHost = (currentHost+1)%numOfNodes;
                            currentHost = nextVacantHost(realObjectSize,currentHost);
                            if(currentHost==-1)
                                return;
                            deadlockCount=0;
                        }
                        deadlockCount++;
                    }
                    parityDataInSameHost=true;
                    break;
                }
            }
            if(parityDataInSameHost==true)
                continue;

            objectsPlaced.get(lowestNumOfOccurrences).remove(objectName);
            objectsPlaced.get(lowestNumOfOccurrences+1).add(objectName);
            //need at least two for coding
            if (objectsPlaced.get(lowestNumOfOccurrences).isEmpty()){
                objectsPlaced.remove(lowestNumOfOccurrences);
                lowestNumOfOccurrences++;
                //1 above current might not exist
                if(!objectsPlaced.containsKey(lowestNumOfOccurrences+1))
                    objectsPlaced.put(lowestNumOfOccurrences+1,new ArrayList<String>());
            }

            deadlockCount=0;
            //list of object locations
            String locations = (String)listOfStripes.get(objectName).get(numOfDataInStripe).get("locations");
            //add new location to coding object (location index = numOfDataInStripe)
            listOfStripes.get(objectName).get(numOfDataInStripe).put("locations",addLocation(Integer.toString(currentHost),locations));
            hostsContents.get(currentHost).put("capacity", (int) hostsContents.get(currentHost).get("capacity")-realObjectSize);
            hostsContents.get(currentHost).put("objects", addLocation(objectName, currentHostObjects));

            currentHost = (currentHost+1)%numOfNodes;
            //run until vacant host is found or return
            while ((int) hostsContents.get(currentHost).get("capacity") < realObjectSize)
            {
                if(i==numOfNodes) {
                    //check if hosts are full
                    for(int j=0;j<numOfNodes ; j++) {
                        Node datacenterNode = datacenterList.item(j);
                        Element datacenterElement = (Element) datacenterNode;
                        int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                        objectsSet = stringTokenizer((String) hostsContents.get(j).get("objects"));
                        if ((objectsSet.size() * realObjectSize) < hostCapacity)
                            System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                    }
                    return; //all are full
                }
                currentHost = (currentHost+1)%numOfNodes;
                i++;
            }
            i=1;
            if(SimSettings.getInstance().isNsfExperiment()) {
//                stripeID = listOfStripeIDs.get(0);
                objectName = (String)listOfStripes.keySet().toArray()[0];
                listOfStripeIDs.remove(0);
            }
            else {
//                stripeID = getObjectID(numOfStripes, "stripes", dist);
                List<String> lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
                objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
            }
        }

    }

    /*Used for DATA_PARITY_PLACE to fill remaining capacity up to predefined share of data objects*/
    private void partiallyFillHostsWithDataObjects(){
        double redundancyShare = SimSettings.getInstance().getRedundancyShare();
        int initialCapacity = getRemainingStorageCapacity();
        int remainingCapacity=initialCapacity;
        int host=0;
        int numOfHosts = numOfNodes;
        if(SimSettings.getInstance().getObjectDistPlace().equals("ZIPF")){
            System.out.println("partiallyFillHostsWithDataObjects() - Assumed UNIFORM");
            System.exit(1);
        }

        while(1==1) { //continue until reached redundancy share
            for (Map<String, String> KV : dataObjects.values()) {
                if ((double) remainingCapacity / initialCapacity <= redundancyShare) //target reached
                    return;
                Set<String> hostObjects = stringTokenizer((String) hostsContents.get(host).get("objects"));
                if (hostObjects.contains(KV.get("id"))) //avoid placing same object twice
                    continue;
                String locations = KV.get("locations");
                KV.put("locations", locations + " " + Integer.toString(host));
                hostsContents.get(host).put("capacity", (int) hostsContents.get(host).get("capacity") - objectSize);
                hostsContents.get(host).put("objects", addLocation(KV.get("id"), (String) hostsContents.get(host).get("objects")));
                if ((int) hostsContents.get(host).get("capacity") < 0)
                    System.out.println("hostStorageCapacity[host]<0");
                //next host in list
                host = (host + 1) % numOfHosts;
                remainingCapacity -= objectSize;
            }
        }

    }

    //Fill with replicas
    private void fillHostsWithDataObjects(){
        int nodesTried=1;
        int currentHost=0;
        int objectID=0;
        int deadlockCount=0;

        //monitor parity occurrences
        HashMap<Integer,List<String>> objectsPlaced = new HashMap<>();
        List<String> objectList = new ArrayList<String>();
        for(Map object:dataObjects.values()) //create list of object names
            objectList.add((String)object.get("id"));
        objectsPlaced.put(0,objectList);
        objectsPlaced.put(1,new ArrayList<String>());
        int lowestNumOfOccurrences=0;

        String dist = SimSettings.getInstance().getObjectDistPlace();
//        objectID = getObjectID(numOfDataObjects,"objects",dist);
//        String objectName;
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");


        while(1==1) {
            if(SimSettings.getInstance().isNsfExperiment()) {
                if (hostsContents.size()==3) { //if 3 hosts, one will contain replicas
                    currentHost = 2;
                    if (objectID % 2 != 0) {
                        objectID = getObjectID(numOfDataObjects, "objects", dist);
                        continue;
                    }
                }
                else if (hostsContents.size()==4) { //2 will contain replicas
                    if (objectID % 2 == 0) {
                        currentHost = 2; //if even, place on host 3
                    }
                    else { //else please on host 3
                        currentHost = 3;
                    }
                }
            }
            int objectDenied=0;
            //get object name
            List<String> lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
            String objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));


            //get objects on selected host
            Set<String> objectsSet = stringTokenizer((String) hostsContents.get(currentHost).get("objects"));

            //run until vacant host is found or return
            while ((int) hostsContents.get(currentHost).get("capacity") < objectSize)
            {
                if(nodesTried==numOfNodes) {
                    boolean notAllFull = false;
                    //check if hosts are full
                    for(int j=0;j<numOfNodes ; j++) {
                        Node datacenterNode = datacenterList.item(j);
                        Element datacenterElement = (Element) datacenterNode;
                        int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                        objectsSet = stringTokenizer((String) hostsContents.get(j).get("objects"));

                        if ((objectsSet.size() * objectSize) != hostCapacity) {
                            notAllFull=true;
                            currentHost=j;
                            objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
                            break;
//                            System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                        }
                    }

                    if(!notAllFull)
                        return; //all are full
                    else
                        break;
                }
                currentHost = (currentHost+1)%numOfNodes;
                nodesTried++;
            }

            //if object is already in node select another object
            if(objectsSet.contains(objectName)) {
                objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
//                objectID = getObjectID(numOfDataObjects,"objects",dist);
                deadlockCount++;
                if(deadlockCount>20){ //if tried enough replace host
                    currentHost = (currentHost+1)%numOfNodes;
                    deadlockCount=0;
                }
                continue;
            }
/*            Set<String> locationsSet = stringTokenizer((String)dataObjects.get(objectName).get("locations"));
            //load balancing - avoid object with locationDelta or more placements than min
            if (locationsSet.size()+1>getLowestNumberOfLocationsPerDataObject()+locationDelta){
                objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
//                objectID = getObjectID(numOfDataObjects,"objects",dist);
                deadlockCount++;
                if (deadlockCount < 20) {
                    objectDenied = 1;
                }
            }*/

            if (objectDenied!=1) {
                deadlockCount=0;
                String locations = (String)dataObjects.get(objectName).get("locations");
                //add new location
                dataObjects.get(objectName).put("locations",addLocation(Integer.toString(currentHost),locations));
                hostsContents.get(currentHost).put("capacity", (int) hostsContents.get(currentHost).get("capacity")-objectSize);
                String currentHostObjects = (String) hostsContents.get(currentHost).get("objects");
                hostsContents.get(currentHost).put("objects", addLocation(objectName, currentHostObjects));

                objectsPlaced.get(lowestNumOfOccurrences).remove(objectName);
                objectsPlaced.get(lowestNumOfOccurrences+1).add(objectName);
                if (objectsPlaced.get(lowestNumOfOccurrences).isEmpty()){
                    objectsPlaced.remove(lowestNumOfOccurrences);
                    lowestNumOfOccurrences++;
                    lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
                    //1 above current might not exist
                    if(!objectsPlaced.containsKey(lowestNumOfOccurrences+1))
                        objectsPlaced.put(lowestNumOfOccurrences+1,new ArrayList<String>());
                }

                currentHost = (currentHost+1)%numOfNodes;
                //run until vacant host is found or return
                while ((int) hostsContents.get(currentHost).get("capacity") < objectSize)
                {
                    if(nodesTried==numOfNodes) {
                        boolean notAllFull = false;
                        //check if hosts are full
                        for(int j=0;j<numOfNodes ; j++) {
                            Node datacenterNode = datacenterList.item(j);
                            Element datacenterElement = (Element) datacenterNode;
                            int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                            objectsSet = stringTokenizer((String) hostsContents.get(j).get("objects"));
                            if ((objectsSet.size() * objectSize) != hostCapacity) {
                                currentHost=j;
//                                System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                                notAllFull=true;
                                break;
                            }
                        }
                        if(!notAllFull)
                            return; //all are full
                        else
                            break;
                    }
                    currentHost = (currentHost+1)%numOfNodes;
                    nodesTried++;
                }
                nodesTried=1;
//                objectName = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
//                objectID = getObjectID(numOfDataObjects,"objects",dist);
                //TODO: test to check placement is not skewed
            }
        }
    }

    private String addLocation(String toAdd, String locations){
        if (locations != null) {
            //get list of locations of object
            Set<String> locationsSet = stringTokenizer(locations);
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
/*Creates list of integers between start and end inputs*/
    private List<Integer> getNumbersInRange(int start, int end) {
        List<Integer> result = new ArrayList<>();
        for (int i = start; i < end; i++) {
            result.add(i);
        }
        return result;
    }

    /*Creates hash map with keys as number of appearances and values objectID*/
    private HashMap<Integer,List<String>> countObjectsPlaced(Collection<Map> objectList){
        HashMap<Integer,List<String>> objectsPlaced = new HashMap<>();
        for(Map<String,String> object:objectList){
            int appearances = stringTokenizer(object.get("locations")).size();
            List<String> objects = objectsPlaced.get(appearances);
            if(objects!=null)
                objects.add(object.get("id"));
            else {
                objects = new ArrayList<String>();
                objects.add(object.get("id"));
            }
            objectsPlaced.put(appearances,objects);
        }
        return objectsPlaced;
    }
    /*Generates coding objects for data objects with lowest occurrence*/
    private void createStripes(int numOfStripes, int numOfDataInStripe, int numOfParityInStripe){
        HashMap<Integer,List<String>> objectsPlaced = countObjectsPlaced(dataObjects.values());
        int lowestNumOfOccurrences=1;
        for (int i=1;i<=5;i++){
            if(objectsPlaced.containsKey(i)){
                lowestNumOfOccurrences=i;
                break;
            }
        }
        //Basically for CODING_PLACE (since all data has single occurrence
        if(!objectsPlaced.containsKey(lowestNumOfOccurrences+1))
            objectsPlaced.put(lowestNumOfOccurrences+1,new ArrayList<String>());

        List<Integer> listOfDataObjectIDs = null;
        int objectDenied=0;
        if(SimSettings.getInstance().isNsfExperiment()){
            listOfDataObjectIDs = getNumbersInRange(0,SimSettings.getInstance().getNumOfDataObjects());
        }
        if(SimSettings.getInstance().getObjectDistPlace().equals("ZIPF")){
            System.out.println("ERROR: partiallyFillHostsWithDataObjects() - Assumed UNIFORM - GOT ZIPF");
            System.exit(1);
        }

        int notGeneratedStripes=0;
        // For each stripe
        for (int i = 0; i< numOfStripes; i++){
            boolean stripeNotCreated=false;
            int deadlockCount=0;
            List<Map> dataList = new ArrayList();
            Set<String> listOfObjects = new HashSet<String>();
            // Collect data objects with selected distribution
            for (int j = 0; j<numOfDataInStripe; j++) {
                if(deadlockCount>20) {
                    notGeneratedStripes++;
                    stripeNotCreated=true;
                    break;
                }
                String objectID="";
                if(SimSettings.getInstance().isNsfExperiment()){
                    objectID="d"+String.valueOf(listOfDataObjectIDs.get(0));
                    listOfDataObjectIDs.remove(0);
                }
                else {
                    List<String> lowOccurrenceObjects =objectsPlaced.get(lowestNumOfOccurrences);
                    objectID = lowOccurrenceObjects.get(newObjectRand.nextInt(lowOccurrenceObjects.size()));
                }
                //Check if same object not used twice. If yes, repeat.
                if (listOfObjects.contains(objectID)){
                    j--;
                }
                else {
                    if (!dataList.isEmpty()){ //if list is not empty //TODO: recheck
                        for (Map<String, String> dataObject:dataList){ //check if new object is in same host as previous
                            if (dataObject.get("locations").equals((String)dataObjects.get(objectID).get("locations"))) {
                                objectDenied=1;
                                deadlockCount++;
                            }
                        }
                    }
                    if (objectDenied==0) { //if object is approved
                        dataList.add(dataObjects.get(objectID));
                        listOfObjects.add(objectID);
                    }
                    else { //try again
                        objectDenied = 0;
                        j--;
                        continue; //expected to cause deadlock - solution is to retry whole function
                    }
                    //objectID will be used for coding -> +1 in occurrences
                    objectsPlaced.get(lowestNumOfOccurrences).remove(objectID);
                    objectsPlaced.get(lowestNumOfOccurrences+1).add(objectID);
                    //need at least two for coding
                    if (objectsPlaced.get(lowestNumOfOccurrences).isEmpty()){
                        objectsPlaced.remove(lowestNumOfOccurrences);
                        lowestNumOfOccurrences++;
                        //1 above current might not exist
                        if(!objectsPlaced.containsKey(lowestNumOfOccurrences+1))
                            objectsPlaced.put(lowestNumOfOccurrences+1,new ArrayList<String>());
                    }

                }
//                //from this point parities will be selected, and we want uniform selection
//                dist = "UNIFORM";
            }
            if(stripeNotCreated)
                continue;
            List<Map> parityList = createParityObjects(numOfParityInStripe, dataList);
            // Concatenate data and parity
            List<Map> stripe = Stream.concat(dataList.stream(), parityList.stream())
                    .collect(Collectors.toList());

            Map<String, String>  metadataObject = createMetadataObject(numOfDataInStripe+numOfParityInStripe,
                    stripe);
            stripe.add(metadataObject);
            this.listOfStripes.put((String)stripe.get(numOfDataInStripe).get("id"),stripe);
        }
        testCodingObjectOccurrence(); //TODO: convert to unit test
        if(notGeneratedStripes>0)
            System.out.println("Not generated stripes: " + String.valueOf(notGeneratedStripes));
        return;
    }


    //Tests difference between most and least common data objects in parities.
    //Expect diff of 1
    private void testCodingObjectOccurrence(){
        HashMap<String,Integer> objOccurrences = new HashMap<>();
        for (Map.Entry<String,Map> entry : dataObjects.entrySet()) {
            String object = entry.getKey();
            objOccurrences.put(object,0);
        }
        for (Map.Entry<String,List<Map>> entry : listOfStripes.entrySet()) {
            List<Map> object = entry.getValue();
            String data = (String)object.get(3).get("data");
            String[] dataObjects = data.split(" ");
            for(String obj:dataObjects) {
                objOccurrences.put(obj,objOccurrences.get(obj)+1);
            }
        }
        int minOcc = Integer.MAX_VALUE;
        int maxOcc = 0;
        String minOccObj = "";
        String maxOccObj = "";
        for (Map.Entry<String,Integer> entry : objOccurrences.entrySet()) {
            int occ = entry.getValue();
            if (occ<minOcc){
                minOcc=occ;
                minOccObj=entry.getKey();
            }
            if (occ>maxOcc){
                maxOcc=occ;
                maxOccObj=entry.getKey();
            }
        }
        if(maxOcc-minOcc>1)
            throw new IllegalStateException("Too many occurrences of " + maxOccObj + " in parity");

    }

    /*Break a single string into a set of strings*/
    public Set<String> stringTokenizer(String str){
        Set<String> hashSet = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(str, " ");
        while (st.hasMoreTokens())
            hashSet.add(st.nextToken());

        return hashSet;
    }

    private int getLowestNumberOfLocationsPerDataObject(){
        int minNumOfLocations=Integer.MAX_VALUE;
        for (int i=0;i<numOfDataObjects;i++){
            String locations = (String)dataObjects.get("d"+String.valueOf(i)).get("locations");
            StringTokenizer st = new StringTokenizer(locations, " "); // Space as delimiter
            Set<String> locationsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            if (locationsSet.size()<minNumOfLocations)
                minNumOfLocations=locationsSet.size();
        }
        return minNumOfLocations;
    }

    private int getLowestNumberOfLocationsPerParityObject(){
        int minNumOfLocations=Integer.MAX_VALUE;
        for (int i=0;i<numOfStripes;i++){
            String locations = (String)listOfStripes.get(i).get(numOfDataInStripe).get("locations");
            if (locations==null)
                continue;
            StringTokenizer st = new StringTokenizer(locations, " "); // Space as delimiter
            Set<String> locationsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            if (locationsSet.size()<minNumOfLocations)
                minNumOfLocations=locationsSet.size();
        }
        return minNumOfLocations;
    }

/*    public static int getNumOfDataInStripe() {
        return getNumOfDataInStripeStatic;
    }*/
    public int getNumOfObjectsInStripe(){
        return numOfDataInStripe+numOfParityInStripe;
    }

    public List<Map> getDataObjects() {
        Collection<Map> values = dataObjects.values();
        return new ArrayList<>(values);
    }

    public List<Map> getParityObjects() {
        if(parityObjects!=null)
            return parityObjects;
        return null;
    }

    public List<Map> getMetadataObjects() {
        return metadataObjects;
    }

    public HashMap<Integer, HashMap<String, Object>> getHostsContents() {
        return hostsContents;
    }
    public List<Map> getListOfObjects() {
        return listOfObjects;
    }

    public Map<Integer, List<Map>> getObjectsInHosts() {
        return objectsInHosts;
    }

    public int getNumOfDataObjects() {
        return numOfDataObjects;
    }

    public static int getSeed() {
        return seed;
    }


    public double getOverhead() {
        return overhead;
    }
}
