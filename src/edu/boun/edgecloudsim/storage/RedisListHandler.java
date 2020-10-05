//TODO: Create redis connection exceptions
//TODO: Add constants (PARITY,DATA,MD) to parity (object:md*)
package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.util.Slowlog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    //Takes list from ObjectGenerator and creates KV pairs in Redis for specific host
    public static void orbitCreateList(String objectPlacementPolicy, int currentHost){
        Jedis jedis = new Jedis(localhost, 6379);
        OG = new ObjectGenerator(objectPlacementPolicy);
        for (Map<String,String> KV : OG.getObjectsInHosts().get(currentHost)) {
            jedis.hmset("object:" + KV.get("id"), KV);
            jedis.expire("object:" + KV.get("id"),100000);
        }
        jedis.close();
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
        String dataObjects = jedis.hget("object:"+metadataID,"data");
        String parityObjects = jedis.hget("object:"+metadataID,"parity");
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





}
