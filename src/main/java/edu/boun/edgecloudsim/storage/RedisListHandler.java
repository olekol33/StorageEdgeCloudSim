package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.util.Slowlog;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RedisListHandler {

    private static double zipfExponent = SimSettings.getInstance().getZipfExponent();
    private static int numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
    private static int numOfStripes = SimSettings.getInstance().getNumOfStripes();
    private static int numOfDataInStripe = SimSettings.getInstance().getNumOfDataInStripe();
    private static int numOfParityInStripe = SimSettings.getInstance().getNumOfParityInStripe();
    private static String localhost = "127.0.0.1";

    public static ObjectGenerator getOG() {
        return OG;
    }

    private static ObjectGenerator OG;

    public static long slowlogGet(String hostname, long entries){
        Jedis jedis = new Jedis(hostname, 6379);
//        String locations =  jedis.hget("object:d4","locations");
        List<Slowlog> list1 = jedis.slowlogGet(entries);
//        List<Slowlog> list = Slowlog.from(jedis.getClient().getObjectMultiBulkReply());
        long time = list1.get(0).getExecutionTime();
        jedis.close();

        return time;
    }

    //Takes list from ObjectGenerator and creates KV pairs in Redis
    public static void createList(String objectPlacementPolicy){
        Jedis jedis = new Jedis(localhost, 6379);
        OG = new ObjectGenerator(objectPlacementPolicy);
        for (Map<String,String> KV : OG.getListOfObjects())
            jedis.hmset("object:"+KV.get("id"),KV);
        jedis.close();
        SimLogger.print("Created Redis KV with stripes: " + numOfStripes +" , Data objects: " + numOfDataObjects +
                ", in each stripe: " + numOfDataInStripe + " + " + numOfParityInStripe + "\n");
    }

    //Generate list of all object locations in the system for orchestration
    private static void listObjectInSystem(ObjectGenerator OG) throws IOException {
        String tmpFolder = "";
        if(SystemUtils.IS_OS_WINDOWS)
            tmpFolder = SimSettings.getInstance().getOutputFolder();
        else
            tmpFolder = "/tmp/";
        Path pLoc = Paths.get("/tmp/Object_Locations.txt");
        Path pDist = Paths.get("/tmp/OBJECT_DISTRIBUTION.txt");
        if(Files.exists(pLoc) && Files.exists(pDist) && SystemUtils.IS_OS_LINUX){
            SimLogger.print("Object locations and distribution files exist"+"\n");
            return;
        }
        File objectFile = new File(tmpFolder,"Object_Locations.txt");
        File objectDistFile = new File(tmpFolder,"OBJECT_DISTRIBUTION.txt");
        FileWriter objectFW = new FileWriter(objectFile, false),
                objectDistFW = new FileWriter(objectDistFile, false);
        BufferedWriter objectBW = new BufferedWriter(objectFW),
                objectDistBW = new BufferedWriter(objectDistFW);


        objectBW.write("object,locations\n");
        objectDistBW.write("Object Name,Object Type,Occurrences");
        objectDistBW.newLine();
        for (Map<String,String> KV : OG.getListOfObjects()) {
            if(KV.get("type").equals("metadata"))
                continue;
            objectBW.write("object:" + KV.get("id")+","+KV.get("locations"));
            objectBW.newLine();

            //OBJECT_DISTRIBUTION
            String locations = (String)KV.get("locations");
            StringTokenizer st= new StringTokenizer(locations, " "); // Space as delimiter
            Set<String> locationsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            objectDistBW.write(KV.get("id")+","+KV.get("type")+","+locationsSet.size());
            objectDistBW.newLine();
        }
        objectBW.close();
        objectDistBW.close();
    }

    //Takes list from ObjectGenerator and creates KV pairs in Redis for specific host
    public static void orbitCreateList(String objectPlacementPolicy, String currentHost){
        Jedis jedis;
        jedis = new Jedis(localhost, 6379);

        OG = new ObjectGenerator(objectPlacementPolicy);
        //Generate Redis objects for this host
        for (Map<String,String> KV : OG.getListOfObjects()) {
            if(KV.get("type").equals("metadata"))
                continue;
            String locations = KV.get("locations");
            StringTokenizer st = new StringTokenizer(locations, " "); // Space as delimiter
            Set<String> locationsSet = new HashSet<String>();
            while (st.hasMoreTokens())
                locationsSet.add(st.nextToken());
            if (locationsSet.contains(currentHost))
                jedis.hmset("object:" + KV.get("id"), KV);
//            jedis.expire("object:" + KV.get("id"),100000);
        }
        jedis.close();
        try {
            listObjectInSystem(OG);
        }
        catch (Exception e){
            System.out.println("Failed to generate object list");
        }

        SimLogger.print("Created Redis KV on host: " + currentHost + " with stripes: " + numOfStripes +" , Data objects: " + numOfDataObjects +
                ", in each stripe: " + numOfDataInStripe + " + " + numOfParityInStripe + "\n");
    }

    public static void orbitPlaceMetadata(String objectPlacementPolicy){
        Jedis jedis = new Jedis(localhost, 6379);
        OG = new ObjectGenerator(objectPlacementPolicy);
        for (Map<String,String> KV : OG.getMetadataObjects())
            jedis.hmset("object:"+KV.get("id"),KV);
        jedis.close();
    }

    //Terminate the connection
    public static void closeConnection(){
        Jedis jedis = new Jedis(localhost, 6379);
        jedis.flushAll();
//        jedis.shutdown();
        jedis.quit();
    }
    //Get key list by pattern
    public static List<String> getObjectsFromRedis(String pattern){
        Jedis jedis = new Jedis(localhost, 6379);
        int batch = 100;
        List<String> listOfObjects = new ArrayList<>();
        //Scan in batches
        ScanParams scanParams = new ScanParams().count(batch).match(pattern);
        String cur = redis.clients.jedis.ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> scanResult = jedis.scan(cur, scanParams);
            listOfObjects = Stream.concat(listOfObjects.stream(), scanResult.getResult().stream())
                    .collect(Collectors.toList());
            cur = scanResult.getCursor();
        } while (!cur.equals(redis.clients.jedis.ScanParams.SCAN_POINTER_START));
        jedis.close();
        return listOfObjects;

    }

    public static String getObjectLocationsFromMetadata(String hostname, String mdObjectID, String readObjectID){
        Jedis jedis = new Jedis(hostname, 6379);
        String locations =  jedis.hget(mdObjectID,readObjectID);
        jedis.close();
        return locations;
    }

    public static String getObjectLocations(String hostname, String objectID){
        Jedis jedis = new Jedis(hostname, 6379);
        String locations =  jedis.hget("object:"+objectID,"locations");
        jedis.close();
        return locations;
    }

    public static String getObjectLocations(String objectID){
        Jedis jedis = new Jedis(localhost, 6379);
        String locations =  jedis.hget("object:"+objectID,"locations");
        jedis.close();
        return locations;
    }

    public static String getObjectSize(String objectID){
        Jedis jedis = new Jedis(localhost, 6379);
        String size =  jedis.hget("object:"+objectID,"size");
        jedis.close();
        return size;
    }

    public static String getObjectID(String objectName){
        Jedis jedis = new Jedis(localhost, 6379);
        String id =  jedis.hget(objectName,"id");
        jedis.close();
        return id;
    }


    //Returns data object IDs in index 0 and parity in index 1
    public static String[] getStripeObjects(String metadataID){
        Jedis jedis = new Jedis(localhost, 6379);
        if (!metadataID.contains("object:"))
            metadataID = "object:"+metadataID;
        String dataObjects = jedis.hget(metadataID,"data");
        String parityObjects = jedis.hget(metadataID,"parity");
        jedis.close();
        return new String[] {dataObjects,parityObjects};

    }
/*    //Returns random list of stripes
    public static List<String> getRandomStripeListForDevice(int numOfStripesToRead){
        List<String> listOfMetadataObjects = getObjectsFromRedis("object:md*");
        List<String> listForDevice = new ArrayList<>(numOfStripesToRead);
//        Random random = new Random();
//        random.setSeed(ObjectGenerator.seed);
        for (int i=0; i<numOfStripesToRead; i++) {
            //Get index such that 0<=index<= size of list
            getRandomGenerator().nextInt(listOfMetadataObjects.size());
//            int metadataIndex = random.nextInt(listOfMetadataObjects.size());
            int metadataIndex = getRandomGenerator().nextInt(listOfMetadataObjects.size());
            String objectID = getObjectID(listOfMetadataObjects.get(metadataIndex));
            listForDevice.add(objectID);
        }
        return listForDevice;
    }*/


/*    public static List<String> getZipfStripeListForDevice(int numOfStripesToRead){
        List<String> listOfMetadataObjects = getObjectsFromRedis("object:md*");
        List<String> listForDevice = new ArrayList<>(numOfStripesToRead);
        for (int i=0; i<numOfStripesToRead; i++) {
            //Get index such that 0<=index<= size of list
            int metadataIndex = new ZipfDistribution(getRandomGenerator(), numOfStripes, zipfExponent).sample();
            //need to reduce by 1
            metadataIndex--;
//            int metadataIndex = random.nextInt(listOfMetadataObjects.size());
            String objectID = getObjectID(listOfMetadataObjects.get(metadataIndex));
            listForDevice.add(objectID);
        }
        return listForDevice;
    }*/


    public static int getNumOfDataObjects() {
        return numOfDataObjects;
    }

    public static int getNumOfStripes() {
        return numOfStripes;
    }

    public static int getNumOfDataInStripe() {
        return numOfDataInStripe;
    }

    public static int getNumOfParityInStripe() {
        return numOfParityInStripe;
    }


    public static void updateNumOfDataObjects() {
        RedisListHandler.numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
    }

    public static void updateNumOfStripes() {
        RedisListHandler.numOfStripes = SimSettings.getInstance().getNumOfStripes();
    }





}
