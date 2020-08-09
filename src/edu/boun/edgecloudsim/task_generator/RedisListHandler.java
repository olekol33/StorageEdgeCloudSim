package edu.boun.edgecloudsim.task_generator;

import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;


public class RedisListHandler {


    public static void createList(){
        Jedis jedis = new Jedis("localhost", 6379);
//        List<List<Map>> listOfStripes = ObjectGenerator.getListOfStripes();
        ObjectGenerator OG = new ObjectGenerator(50,100,2,1);
        List<List<Map>> listOfStripes = OG.getListOfStripes();
        int i = 0;
        for (List<Map> stripe : listOfStripes){
            for (Map<String,String> KV : stripe){
                jedis.hmset("object:"+KV.get("id"),KV);
            }
        }

        //Closing
        jedis.flushAll();
        jedis.close();
    }

}
