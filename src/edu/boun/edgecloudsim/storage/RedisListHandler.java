//TODO: Create redis connection exceptions
//TODO: Add constants (PARITY,DATA,MD) to parity (object:md*)
package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.utils.SimLogger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RedisListHandler {


    //Takes list from ObjectGenerator and creates KV pairs in Redis
    public static void createList(){
        int numOfDataObjects = 50;
        int numOfStripes = 100;
        int numOfDataInStripe = 2;
        int numOfParityInStripe = 1;
        Jedis jedis = new Jedis("localhost", 6379);
//        List<List<Map>> listOfStripes = ObjectGenerator.getListOfStripes();
        ObjectGenerator OG = new ObjectGenerator(numOfDataObjects,numOfStripes,numOfDataInStripe,numOfParityInStripe);
        List<List<Map>> listOfStripes = OG.getListOfStripes();
        int i = 0;
        for (List<Map> stripe : listOfStripes){
            for (Map<String,String> KV : stripe){
                jedis.hmset("object:"+KV.get("id"),KV);
            }
        }
        jedis.close();
        SimLogger.print("Created Redis KV with stripes: " + numOfStripes +" , Data objects: " + numOfDataObjects +
                ", in each stripe: " + numOfDataInStripe + " + " + numOfParityInStripe + "\n");
        //Closing
//        closeConnection();

    }
    //Terminate the connection
    public static void closeConnection(){
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.flushAll();
        jedis.quit();
    }
    //Get key list by pattern
    public static List<String> getObjectsFromRedis(String pattern){
        Jedis jedis = new Jedis("localhost", 6379);
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

    public static String getObjectLocations(String objectID){
        Jedis jedis = new Jedis("localhost", 6379);
        String locations =  jedis.hget("object:"+objectID,"locations");
        jedis.close();
        return locations;
    }

    public static String getObjectSize(String objectID){
        Jedis jedis = new Jedis("localhost", 6379);
        String size =  jedis.hget("object:"+objectID,"size");
        return size;
    }

    public static String getObjectID(String objectName){
        Jedis jedis = new Jedis("localhost", 6379);
        String id =  jedis.hget(objectName,"id");
        return id;
    }


    //Returns data object IDs in index 0 and parity in index 1
    public static String[] getStripeObjects(String metadataID){
        Jedis jedis = new Jedis("localhost", 6379);
        String dataObjects = jedis.hget("object:"+metadataID,"data");
        String parityObjects = jedis.hget("object:"+metadataID,"parity");
        return new String[] {dataObjects,parityObjects};

    }
    //Returns random list of stripes
    public static List<String> getRandomStripeListForDevice(int numOfStripesToRead, int seed){
        List<String> listOfMetadataObjects = getObjectsFromRedis("object:md*");
        List<String> listForDevice = new ArrayList<>(numOfStripesToRead);
        Random random = new Random();
//        random.setSeed(seed);
        for (int i=0; i<numOfStripesToRead; i++) {
            //Get index such that 0<=index<= size of list
            int metadataIndex = random.nextInt(listOfMetadataObjects.size());
            String objectID = getObjectID(listOfMetadataObjects.get(metadataIndex));
            listForDevice.add(objectID);
        }
        return listForDevice;
    }





}
