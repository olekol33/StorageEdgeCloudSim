//Written by Oleg Kolosov
// Create stripes of object in Redis for the simulator
//TODO: Check location distribution
package edu.boun.edgecloudsim.storage;



import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.task_generator.IdleActiveStorageLoadGenerator;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
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
    private List<List<Map>> listOfStripes;
    private HashMap<Integer, HashMap<String, Object>> hostsContents;
    private List<Map> dataObjects;
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

    //TODO: fix
//    private static int getNumOfDataInStripeStatic = 2;
    Random ran;


    public ObjectGenerator(String _objectPlacementPolicy) {
        seed = SimSettings.getInstance().getRandomSeed();
        numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
        numOfStripes = SimSettings.getInstance().getNumOfStripes();
        numOfDataInStripe = SimSettings.getInstance().getNumOfDataInStripe();
        numOfParityInStripe = SimSettings.getInstance().getNumOfParityInStripe();
        if (SimSettings.getInstance().isOrbitMode()) {
            objectSize = (int) SimSettings.getInstance().getTaskLookUpTable()[0][LoadGeneratorModel.DATA_DOWNLOAD]; //bytes
            numOfNodes=SimSettings.getInstance().getNumberOfEdgeNodes();
        }
        else {
            numOfNodes=SimSettings.getInstance().getNumOfEdgeDatacenters();
            objectSize = 1000;
        }
        hostsContents = new HashMap<Integer, HashMap<String, Object>>(numOfNodes);
        objectPlacementPolicy = _objectPlacementPolicy;
        ran = new Random();
        ran.setSeed(seed);
        newObjectRand = new Well19937c(seed);
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
        dataObjects = createDataObjects(numOfDataObjects, Integer.toString(this.objectSize));
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
            listOfStripes = createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe,numOfDataObjects);
            fillHostsWithCodingObjects();
        }
        else if (objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")){
            fillHostsWithDataObjects();
        }
        else if (objectPlacementPolicy.equalsIgnoreCase("DATA_PARITY_PLACE")){
            listOfStripes = createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe,numOfDataObjects);
        }

        listOfObjects = new ArrayList<Map>(dataObjects);

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
                System.exit(0);
            } catch (Exception e) {
                SimLogger.printLine("Failed to generate object list");
                System.exit(0);
            }

        Map<Integer, ArrayList<Double>> timeToReadStripe = new HashMap<Integer, ArrayList<Double>>();
        objectsInHosts = new HashMap<Integer, List<Map>>(numOfNodes);
        populateObjectsInHosts();
        }



    }

    private void exportObjectList() throws IOException {
        File objectListFile = null;
        FileWriter objectListFW = null;
        BufferedWriter objectListBW = null;

        objectListFile = new File("/tmp/object_list.csv");
        objectListFW = new FileWriter(objectListFile, false);
        objectListBW = new BufferedWriter(objectListFW);
        objectListBW.write("objectID,objectLocations");
        objectListBW.newLine();
        for(Map<String,String>  object:dataObjects){
            objectListBW.write(object.get("id")+","+object.get("locations"));
            objectListBW.newLine();
        }
        if (!objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")) {
            for (Map<String, String> object : dataObjects) {
                objectListBW.write(object.get("id") + "," + object.get("locations"));
                objectListBW.newLine();
            }
        }
        objectListBW.close();
        //metadata list
        if (!objectPlacementPolicy.equalsIgnoreCase("REPLICATION_PLACE")){
        objectListFile = new File("/tmp/stripe_list.csv");
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

    private void addObjectLocationsToMetadata()
    {
        for (Map<String,String> KV : metadataObjects) {
            Set<String> dataObjects = tokenizeList(KV.get("data"));
            Set<String> parityObjects = tokenizeList(KV.get("parity"));
            for (String object : dataObjects){
                for (Map<String,String> entry : this.dataObjects){
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
        for (int i=0;i<numOfNodes;i++){
            Node datacenterNode = datacenterList.item(i);
            Element datacenterElement = (Element) datacenterNode;
            int storageCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
//            hostStorageCapacity[i] = storageCapacity;
            hostsContents.get(i).put("capacity",storageCapacity);
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

    public int getObjectIDForLoad(int numberOfElements, String dist)  throws NotStrictlyPositiveException {
        int objectNum = -1;
        if (dist.equals("UNIFORM"))
        {
            objectNum =  newObjectLoadRand.nextInt(numberOfElements);
        }
        else if (dist.equals("ZIPF") || dist.equals("MULTIZIPF"))
        {
            objectNum = new ZipfDistribution(newObjectLoadRand, numberOfElements, SimSettings.getInstance().getZipfExponent()).sample();
            //need to reduce by 1
            objectNum--;
        }
        return objectNum;

    }

    public String getDataObjectID(){
        String dist = SimSettings.getInstance().getObjectDistRead();
        int objectID = getObjectID(numOfDataObjects,"objects",dist);
        return (String)dataObjects.get(objectID).get("id");
    }
    public String getDataObjectID(int objectID){
        return (String)dataObjects.get(objectID).get("id");
    }

    private String getAlphaNumericString(int n)
    {
        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to AlphaNumericString variable length
            Random ran = new Random();
            int index = rand.nextInt(AlphaNumericString.length());
//                    = (int)(AlphaNumericString.length()
//                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }
        return sb.toString();
    }

    private String getBinaryString(int n)
    {
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
    private List<Map> createDataObjects(int numOfDataObjects, String objectSize){
        List<Map> listOfDataObjects = new ArrayList(numOfDataObjects);
        for (int i=0; i<numOfDataObjects; i++){
            Map<String, String> map = new HashMap<String,String>();
            map.put("id", "d" + Integer.toString(i));
            map.put("size", objectSize);
            map.put("type", "data");
            if (SimSettings.getInstance().isOrbitMode()){
                map.put("data", getBinaryString(Integer.valueOf(objectSize)));
            }
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
                // Size is as of largest data object
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
            String currentHostObjects = (String) hostsContents.get(currentHost).get("objects");
            StringTokenizer st= new StringTokenizer(currentHostObjects, " "); // Space as delimiter
            Set<String> objectsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                objectsSet.add(st.nextToken());

            //if object is not in current set
            if(!objectsSet.contains(objectID)) {
                int capacity = (int) hostsContents.get(currentHost).get("capacity");
                hostsContents.get(currentHost).put("capacity",capacity-objectSize);
                hostsContents.get(currentHost).put("objects", addLocation(objectID, currentHostObjects));
            }


            //if locations  not empty, add to unique set
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

    //Provides locations of data objects not located on the client
    private void completeMetadataForDataObjects(){
        List<Map> dataMetadataObjects = new ArrayList(numOfDataObjects);

        for (Map<String,String> KV : dataObjects){
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

/*    List<Map> listOfDataObjects = new ArrayList(numOfDataObjects);
        for (int i=0; i<numOfDataObjects; i++){
        Map<String, String> map = new HashMap<String,String>();
        map.put("id", "d" + Integer.toString(i));
        map.put("size", objectSize);
        map.put("type", "data");
        if (SimSettings.getInstance().isOrbitMode()){
            map.put("data", getBinaryString(100));
        }
//            map.put("location", "");
        listOfDataObjects.add(map);
    }
        return listOfDataObjects;*/

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
        int numOfHosts = numOfNodes;
        int host = currHost;
        currHost = (currHost+1)% numOfHosts;
        int initialCapacity=0;
        int finalCapacity=0;
        for (int i=0;i<hostsContents.size();i++)
            initialCapacity += (int)hostsContents.get(i).get("capacity");
        //go over each object
        for (Map<String,String> KV : dataObjects){
            KV.put("locations", Integer.toString(host));
            hostsContents.get(host).put("capacity", (int) hostsContents.get(host).get("capacity")-objectSize);
            hostsContents.get(host).put("objects", addLocation(KV.get("id"), (String) hostsContents.get(host).get("objects")));
            //TODO: write test
            if ((int) hostsContents.get(host).get("capacity")<0)
                System.out.println("hostStorageCapacity[host]<0");
            //next host in list
            host = currHost;
            currHost = (currHost+1)% numOfHosts;
        }
        for (int i=0;i<hostsContents.size();i++)
            finalCapacity += (int)hostsContents.get(i).get("capacity");
        overhead = (double) initialCapacity / (initialCapacity-finalCapacity);
    }

    //Object from each group are on a separate host
    private void NSFBSFInitializeDataObjectPlacement(){
        int numOfDataHosts = 2;
        int host = currHost;
        currHost = (currHost+1)% numOfDataHosts;
        //go over each object
        for (Map<String,String> KV : dataObjects){
            KV.put("locations", Integer.toString(host));
            hostsContents.get(host).put("capacity", (int) hostsContents.get(host).get("capacity")-objectSize);
            hostsContents.get(host).put("objects", addLocation(KV.get("id"), (String) hostsContents.get(host).get("objects")));
            //TODO: write test
            if ((int) hostsContents.get(host).get("capacity")<0)
                System.out.println("hostStorageCapacity[host]<0");
            //next host in list
            host = currHost;
            currHost = (currHost+1)% numOfDataHosts;
        }
    }

    //place coding objects in hosts by policy
    private void fillHostsWithCodingObjects(){
        int i=1;
        int currentHost=0;
        int deadlockCount=0;
        int stripeID;
        List<Integer> listOfStripeIDs = null;
        String dist = SimSettings.getInstance().getStripeDistPlace();
        //select stripe according to popularity policy
        if(SimSettings.getInstance().isNsfExperiment()) {
            listOfStripeIDs = getNumbersInRange(0,SimSettings.getInstance().getNumOfStripes());
            stripeID = listOfStripeIDs.get(0);
            listOfStripeIDs.remove(0);
        }
        else {

            stripeID = getObjectID(numOfStripes, "stripes",dist);
        }
        String objectName;
        Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
        NodeList datacenterList = doc.getElementsByTagName("datacenter");
        while(1==1) {
            if(SimSettings.getInstance().getRAID()==4) {
            if(SimSettings.getInstance().isNsfExperiment())
                currentHost=2;
            }
            //TODO: currently support one parity
            //get name of object by its ID
            objectName = (String)listOfStripes.get(stripeID).get(numOfDataInStripe).get("id");
            //start with host 0, get objects it contains
            String currentHostObjects = (String) hostsContents.get(currentHost).get("objects");

            String objects = (String) hostsContents.get(currentHost).get("objects");
            StringTokenizer st= new StringTokenizer(objects, " "); // Space as delimiter
            Set<String> objectsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                objectsSet.add(st.nextToken());

            //if object is already in node select another
            if(objectsSet.contains(objectName)) {
                stripeID = getObjectID(numOfStripes,"stripes",dist);
                continue;
            }
            //locations of parity
            String objectLocations = (String)listOfStripes.get(stripeID).get(numOfDataInStripe).get("locations");
            if(objectLocations!=null) {
                Set<String> locationsSet = new HashSet<String>();
                st = new StringTokenizer(objectLocations, " ");
                while (st.hasMoreTokens())
                    locationsSet.add(st.nextToken());
                //load balancing - avoid object with locationDelta or more placements than min
                if (locationsSet.size()+1>(getLowestNumberOfLocationsPerParityObject()+locationDelta)){
                    stripeID = getObjectID(numOfStripes,"stripes",dist);
                    deadlockCount++;
                    if(deadlockCount<20)
                        continue;
                }
            }

/*            Set<String> locationsSet = new HashSet<String>();
            st = new StringTokenizer(objectLocations, " ");
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            //load balancing - avoid object with locationDelta or more placements than min
            if (locationsSet.size()>=getLowestNumberOfLocationsPerParityObject()+locationDelta){
                stripeID = getObjectID(numOfStripes,"stripes",dist);
                continue;
            }*/

            //avoid having parity and data of same stripe in the same host
            boolean flag=false;
            for (int id=0; id<numOfDataInStripe;id++){
                //if host contains data objects of stripe - select new stripe and break
                if(objectsSet.contains((String)listOfStripes.get(stripeID).get(id).get("id"))){
                    if(SimSettings.getInstance().isNsfExperiment()) {
                        //return previous id to list and select next
                        listOfStripeIDs.add(stripeID);
                        stripeID = listOfStripeIDs.get(0);
                        listOfStripeIDs.remove(0);
                    }
                    else
                        stripeID = getObjectID(numOfStripes,"stripes",dist);
                    flag=true;
                    break;
                }
            }
            if(flag==true)
                continue;

            deadlockCount=0;
            //list of object locations
            String locations = (String)listOfStripes.get(stripeID).get(numOfDataInStripe).get("locations");
            //add new location to coding object (location index = numOfDataInStripe)
            listOfStripes.get(stripeID).get(numOfDataInStripe).put("locations",addLocation(Integer.toString(currentHost),locations));
            hostsContents.get(currentHost).put("capacity", (int) hostsContents.get(currentHost).get("capacity")-objectSize);
            hostsContents.get(currentHost).put("objects", addLocation(objectName, currentHostObjects));

            currentHost = (currentHost+1)%numOfNodes;
            //run until vacant host is found or return
            while ((int) hostsContents.get(currentHost).get("capacity") < objectSize)
            {
                if(i==numOfNodes) {
                    //check if hosts are full
                    for(int j=0;j<numOfNodes ; j++) {
                        Node datacenterNode = datacenterList.item(j);
                        Element datacenterElement = (Element) datacenterNode;
                        int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                        objects = (String) hostsContents.get(j).get("objects");
                        st= new StringTokenizer(objects, " "); // Space as delimiter
                        objectsSet = new HashSet<String>();
                        while (st.hasMoreTokens())
                            objectsSet.add(st.nextToken());
                        if ((objectsSet.size() * objectSize) < hostCapacity)
                            System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                    }
                    return; //all are full
                }
                currentHost = (currentHost+1)%numOfNodes;
                i++;
            }
            i=1;
            if(SimSettings.getInstance().isNsfExperiment()) {
                stripeID = listOfStripeIDs.get(0);
                listOfStripeIDs.remove(0);
            }
            else
                stripeID = getObjectID(numOfStripes,"stripes",dist);
        }

    }

    //place coding objects in hosts by policy
    //Used for replication
    private void fillHostsWithDataObjects(){
        int i=1;
        int currentHost=0;
        int objectID=0;
        int deadlockCount=0;


        String dist = SimSettings.getInstance().getObjectDistPlace();
        objectID = getObjectID(numOfDataObjects,"objects",dist);
        String objectName;
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
            objectName = (String)dataObjects.get(objectID).get("id");
            String currentHostObjects = (String) hostsContents.get(currentHost).get("objects");

            String objects = (String) hostsContents.get(currentHost).get("objects");
            StringTokenizer st= new StringTokenizer(objects, " "); // Space as delimiter
            Set<String> objectsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                objectsSet.add(st.nextToken());

            //run until vacant host is found or return
            while ((int) hostsContents.get(currentHost).get("capacity") < objectSize)
            {
                if(i==numOfNodes) {
                    //check if hosts are full
                    for(int j=0;j<numOfNodes ; j++) {
                        Node datacenterNode = datacenterList.item(j);
                        Element datacenterElement = (Element) datacenterNode;
                        int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                        objects = (String) hostsContents.get(j).get("objects");
                        st= new StringTokenizer(objects, " "); // Space as delimiter
                        objectsSet = new HashSet<String>();
                        while (st.hasMoreTokens())
                            objectsSet.add(st.nextToken());
                        if ((objectsSet.size() * objectSize) != hostCapacity)
                            System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                    }

                    return; //all are full
                }
                currentHost = (currentHost+1)%numOfNodes;
                i++;
            }

            //if object is already in node select another
            if(objectsSet.contains(objectName)) {
                objectID = getObjectID(numOfDataObjects,"objects",dist);
                deadlockCount++;
                continue;
            }
            String objectLocations = (String)dataObjects.get(objectID).get("locations");
            Set<String> locationsSet = new HashSet<String>();
            st = new StringTokenizer(objectLocations, " ");
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            //load balancing - avoid object with locationDelta or more placements than min
            if (locationsSet.size()+1>getLowestNumberOfLocationsPerDataObject()+locationDelta){
                objectID = getObjectID(numOfDataObjects,"objects",dist);
                deadlockCount++;
                if (deadlockCount < 20) {
                    objectDenied = 1;
                }
            }

            if (objectDenied!=1) {
                deadlockCount=0;
                String locations = (String)dataObjects.get(objectID).get("locations");
                //add new location
                dataObjects.get(objectID).put("locations",addLocation(Integer.toString(currentHost),locations));
                hostsContents.get(currentHost).put("capacity", (int) hostsContents.get(currentHost).get("capacity")-objectSize);
                hostsContents.get(currentHost).put("objects", addLocation(objectName, currentHostObjects));

                currentHost = (currentHost+1)%numOfNodes;
                //run until vacant host is found or return
                while ((int) hostsContents.get(currentHost).get("capacity") < objectSize)
                {
                    if(i==numOfNodes) {
                        //check if hosts are full
                        for(int j=0;j<numOfNodes ; j++) {
                            Node datacenterNode = datacenterList.item(j);
                            Element datacenterElement = (Element) datacenterNode;
                            int hostCapacity = Integer.parseInt(datacenterElement.getElementsByTagName("storage").item(0).getTextContent());
                            objects = (String) hostsContents.get(j).get("objects");
                            st= new StringTokenizer(objects, " "); // Space as delimiter
                            objectsSet = new HashSet<String>();
                            while (st.hasMoreTokens())
                                objectsSet.add(st.nextToken());
                            if ((objectsSet.size() * objectSize) != hostCapacity)
                                System.out.println("WARNING: Only " + Integer.toString(objectsSet.size()) + " objects in host: " + Integer.toString(j));
                        }

                        return; //all are full
                    }
                    currentHost = (currentHost+1)%numOfNodes;
                    i++;
                }
                i=1;
                objectID = getObjectID(numOfDataObjects,"objects",dist);
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

    private List<Integer> getNumbersInRange(int start, int end) {
        List<Integer> result = new ArrayList<>();
        for (int i = start; i < end; i++) {
            result.add(i);
        }
        return result;
    }

    private List<List<Map>> createStripes(int numOfStripes, int numOfDataInStripe, int numOfParityInStripe, int numOfDataObjects){

        List<List<Map>> listOfStripes = new ArrayList();
        List<Set<Integer>> existingStripes = new ArrayList();
        List<Integer> listOfDataObjectIDs = null;
        int deadlockCount=0;
        int objectDenied=0;
        if(SimSettings.getInstance().isNsfExperiment()){
            listOfDataObjectIDs = getNumbersInRange(0,SimSettings.getInstance().getNumOfDataObjects());
        }
        // For each stripe
        for (int i = 0; i< numOfStripes; i++){
            List<Map> dataList = new ArrayList();
            Set<Integer> listOfIndices = new HashSet<Integer>();
            String dist = SimSettings.getInstance().getObjectDistPlace();
            // Collect data objects with selected distribution
            for (int j = 0; j<numOfDataInStripe; j++) {
                int objectID=-1;
                if(SimSettings.getInstance().isNsfExperiment()){
                    objectID=listOfDataObjectIDs.get(0);
                    listOfDataObjectIDs.remove(0);
                }
                else
                    objectID = getObjectID(numOfDataObjects,"objects", dist);
                //Check if same object not used twice. If yes, repeat.
                if (listOfIndices.contains(objectID)){
                    j--;
                }
                else {
                    String objectLocations = (String)dataObjects.get(objectID).get("locations");
                    Set<String> locationsSet = new HashSet<String>();
                    StringTokenizer st = new StringTokenizer(objectLocations, " ");
                    while (st.hasMoreTokens())
                        locationsSet.add(st.nextToken());
                    if (!objectPlacementPolicy.equalsIgnoreCase("CODING_PLACE")){
                        //load balancing for data objects- avoid object with locationDelta or more placements than min
                        if (locationsSet.size() + 1 > getLowestNumberOfLocationsPerDataObject() + locationDelta) {
                            deadlockCount++;
                            if (deadlockCount < 20) {
                                objectDenied = 1;
                            }
                        }
                    }
                    if (dataList.size()>0){ //if list is not empty
                        for (Map<String, String> dataObject:dataList){ //check if new object is in same host as previous
                            if (dataObject.get("locations").equals(objectLocations)) {
                                objectDenied=1;
                            }
                        }
                    }
                    if (objectDenied==0) { //if object is approved
                        dataList.add(dataObjects.get(objectID));
                        listOfIndices.add(objectID);
                        deadlockCount=0;
                    }
                    else {
                        objectDenied = 0;
                        j--;
                    }

                }
                //from this point parities will be selected, and we want uniform selection
                dist = "UNIFORM";
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
                if (stripe.size() == 0) { //can't fill a whole stripe, just fill objects
                    List<Map> oneObjectStripe;
                    dist = SimSettings.getInstance().getObjectDistPlace();

                    do {
                        int objectID = getObjectID(numOfDataObjects,"objects",dist);
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

    private int getLowestNumberOfLocationsPerDataObject(){
        int minNumOfLocations=Integer.MAX_VALUE;
        for (int i=0;i<numOfDataObjects;i++){
            String locations = (String)dataObjects.get(i).get("locations");
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
        return dataObjects;
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
