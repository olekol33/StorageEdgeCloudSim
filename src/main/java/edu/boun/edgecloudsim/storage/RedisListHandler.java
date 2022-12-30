package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.apache.commons.lang3.SystemUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.Slowlog;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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
        OG = SimManager.getInstance().getObjectGenerator();
//        for (Map<String,String> KV : OG.getListOfObjects())
//            jedis.hmset("object:"+KV.get("id"),KV);
//        jedis.close();
        SimLogger.print("Created Redis KV with stripes: " + numOfStripes +" , Data objects: " + numOfDataObjects +
                ", in each stripe: " + numOfDataInStripe + " + " + numOfParityInStripe + "\n");
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
/*        try {
            OG.listObjectInSystem();
        }
        catch (Exception e){
            System.out.println("Failed to generate object list");
        }*/

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
    public static List<String[]> getObjects(String objectID){
        return OG.getMdObjectHash(objectID);
/*        List<String> listOfObjects = new ArrayList<>();
        List<String> mdObjects = OG.getMdObjectNames();
        for(String object:mdObjects){
            if(object.contains(objectID+"_"))
                listOfObjects.add(object);
        }
        return listOfObjects;*/


    }

    public static String getObjectLocationsFromMetadata(String hostname, String[] mdObjectID, String readObjectID){
        throw new UnsupportedOperationException("Should use this");
/*        Jedis jedis = new Jedis(hostname, 6379);
        String locations =  jedis.hget(mdObjectID,readObjectID);
        jedis.close();
        return locations;*/

    }

    public static List<Integer> getObjectLocations(String objectID){
        return OG.getObjectLocation(objectID);
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
