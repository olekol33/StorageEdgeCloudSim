//Written by Oleg Kolosov
// Create stripes of object in Redis for the simulator
//TODO: Check location distribution
package edu.boun.edgecloudsim.storage;


import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectGenerator {
    //TODO: parametrize
    private int numOfDataObjects = 100;
    private int numOfStripes = 500;
    private int numOfDataInStripe = 2;
    private int numOfParityInStripe = 1;
    private RandomGenerator rand = null;
    static public int seed = 42;
    private String objectSize = "16"; //bytes
    private double zipfExponent = 1.07;
    private List<List<Map>> listOfStripes;
    //TODO: fix
    private static int getNumOfDataInStripeStatic = 2;

    public List<List<Map>> getListOfStripes() {
        return listOfStripes;
    }

    public ObjectGenerator(int numOfDataObjects, int numOfStripes, int numOfDataInStripe, int numOfParityInStripe) {
        this.numOfDataObjects = numOfDataObjects;
        this.numOfStripes = numOfStripes;
        this.numOfDataInStripe = numOfDataInStripe;
        this.numOfParityInStripe = numOfParityInStripe;
        listOfStripes = createStripes(numOfStripes,numOfDataInStripe,numOfParityInStripe,numOfDataObjects);
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
    private int getZipf(int numberOfElements)  throws NotStrictlyPositiveException {
        return new ZipfDistribution(getRandomGenerator(), numberOfElements, this.zipfExponent).sample();
    }

    private int getObject(int numberOfElements)  throws NotStrictlyPositiveException {
        String objectDist = SimSettings.getInstance().getObjectDist();
        int objectNum = -1;
        if (objectDist.equals("RANDOM"))
        {
            Random ran = new Random();
            objectNum =  ran.nextInt(numberOfElements);
        }
        else if (objectDist.equals("ZIPF"))
        {
            objectNum = new ZipfDistribution(getRandomGenerator(), numberOfElements, this.zipfExponent).sample();
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
        int hostID = Math.abs(getRandomGenerator().nextInt()%numOfHosts);
        List<Integer> hosts = new ArrayList<>(numofObjectsInStripe);
        for (int i=0; i<numofObjectsInStripe;i++){
            hosts.add((hostID+i)%numOfHosts);
        }
        return hosts;
    }
    //Add host ID to each object according to placement policy
    private List<Map> placeObjects(int numOfHosts, int numofObjectsInStripe, List<Map> stripe){
        List<Integer> listOfPlacements = sequentialRandomPlacement(numOfHosts, numofObjectsInStripe);
        int i=0;
        for (Map<String,String> KV : stripe){
            String locations = KV.get("locations");
            if (locations != null){
            StringTokenizer st= new StringTokenizer(locations, " "); // Space as delimiter
            Set<String> locationsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            locationsSet.add(Integer.toString(listOfPlacements.get(i)));
            locations = "";
            for (String loc:locationsSet)
                locations += loc + " ";
            KV.put("locations", locations);}
            else
                KV.put("locations", Integer.toString(listOfPlacements.get(i)));
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


    private List<List<Map>> createStripes(int numOfStripes, int numOfDataInStripe, int numOfParityInStripe, int numOfDataObjects){

        List<List<Map>> listOfStripes = new ArrayList();
        List<Map> dataObjects = createDataObjects(numOfDataObjects, this.objectSize);
        List<Set<Integer>> existingStripes = new ArrayList();
        // For each stripe
        for (int i = 0; i< numOfStripes; i++){
            List<Map> dataList = new ArrayList();
            Set<Integer> listOfIndices = new HashSet<Integer>();
            // Collect data objects with selected distribution
            for (int j = 0; j<numOfDataInStripe; j++) {
                int objectID = getObject(numOfDataObjects);
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
            stripe = placeObjects(SimSettings.getInstance().getNumOfEdgeDatacenters(),numOfDataInStripe+numOfParityInStripe,stripe);
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

}
