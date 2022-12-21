package edu.boun.edgecloudsim.storage;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.MigrateParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.*;

public class OrbitReader {
    private long startTime;
    private int clientID;
    private List<TaskProperty> clientTaskList;
    private Random random;
    private List<OrbitReadLogItem> orbitLogList;
    private final String localhost = "127.0.0.1";


    public OrbitReader(int _clientID) {
        startTime = SimLogger.getInstance().getStartTime();
        clientID = _clientID;
        random= new Random();
        RandomGenerator rand = new Well19937c(ObjectGenerator.seed);
        random.setSeed(ObjectGenerator.seed);
        orbitLogList = new ArrayList<OrbitReadLogItem>();
        clientTaskList = new ArrayList<TaskProperty>();
        initialize();

    }

    private void initialize(){
        LoadGeneratorModel loadGeneratorModel = SimManager.getInstance().getLoadGeneratorModel();
        List<TaskProperty> taskList = loadGeneratorModel.getTaskList();
        for(int i=0; i< taskList.size(); i++){
            if (taskList.get(i).getMobileDeviceId()==clientID)
                clientTaskList.add(taskList.get(i));
        }
    }

    private long[] readObject (String srcHost, String objectID){
        String source="";
        String destination = "node1-1.sb5.orbit-lab.org";
        String objectName = "object:"+objectID;
//        System.out.println("objectName: " + objectName);

        long pttlBefore, pttlAfter;
        if (srcHost.equals("0")) {
//            source = "node1-1.sb5.orbit-lab.org";
            Jedis jedisLocalhost = new Jedis(localhost, 6379);
//            pttlBefore = jedisLocalhost.pttl(objectName);
//            pttlAfter = jedisLocalhost.pttl(objectName);
            pttlBefore = Instant.now().toEpochMilli();
            pttlAfter = Instant.now().toEpochMilli();
            return new long[] {pttlBefore,pttlAfter};
        }
        else if (srcHost.equals("1")) {
            source = "node1-2.sb5.orbit-lab.org";
        }
        Jedis jedisSrc = new Jedis(source, 6379);
        Jedis jedisLocalhost = new Jedis(localhost, 6379);
//        pttlBefore = jedisSrc.pttl(objectName);
        pttlBefore = Instant.now().toEpochMilli();
//        System.out.println("pttlBefore=" + pttlBefore);

        MigrateParams params = new MigrateParams();
        params.copy();
        params.replace();
        System.out.println("Migrating " + objectName + " from source " + source + " to dest " + destination);
        jedisSrc.migrate(destination,6379, 0,5000, params, objectName);
        Map<String, String> hgetall = jedisLocalhost.hgetAll(objectName);
        System.out.println("At time " + Instant.now().toEpochMilli() + "read object " + hgetall.get("id"));

        pttlAfter = Instant.now().toEpochMilli();
//        pttlAfter = jedisLocalhost.pttl(objectName);
        System.out.println("pttlAfter=" + pttlAfter);
        jedisSrc.close();
        jedisLocalhost.close();
//        System.out.println("Finished reading: " + objectName + ", pttlBefore=" + pttlBefore + ", pttlAfter=" + pttlAfter);
        return new long[] {pttlBefore,pttlAfter};
    }

    public void clientRun(){
        while (1==1){
            long currentTime = Instant.now().toEpochMilli();
            long[] pttl = new long[2];
            if (currentTime >= clientTaskList.get(0).getStartTime()+startTime){
                String objectID = clientTaskList.get(0).getObjectRead();
//                System.out.println("Read object: " + objectID);
                List<String[]> mdObjects = RedisListHandler.getObjects("object:md*_"+objectID+"_*");
                //no parities
                //TODO:check not accidentally taking another object
//                if (mdObjects.size()==0)
                if (mdObjects==null)
                    mdObjects = RedisListHandler.getObjects("object:md_"+objectID);
                //TODO: currently selects random stripe
                int mdObjectID = random.nextInt(mdObjects.size());
                Set<String> locations = ObjectGenerator.tokenizeList(RedisListHandler.getObjectLocationsFromMetadata(localhost, mdObjects.get(mdObjectID), objectID));
                //TODO: currently takes first location
                String readSrc = locations.iterator().next();
                pttl = readObject(readSrc,objectID);

                orbitLogList.add(new OrbitReadLogItem(currentTime,clientTaskList.get(0).getIoTaskID(),objectID,pttl[0],pttl[1],readSrc,Integer.toString(clientID)));
                clientTaskList.remove(0);
                //TODO:send request, get stripe, orchestrator
            }
            if (clientTaskList.size()==0){
                printLog(orbitLogList);
                System.out.println("Finished");
                System.exit(0);
            }
        }
    }

    protected void printLog(List<OrbitReadLogItem> orbitLogList){
        String savestr = "./" + SimLogger.getInstance().getFilePrefix() + "_ORBIT_READ.log";
        File f = new File(savestr);
        PrintWriter out=null;
        try {
            out = new PrintWriter(new FileOutputStream(new File(savestr), false));
            out.append("Time;ioTaskID;objectID;pttlBefore;pttlAfter;delay;srcHost;dstHost");
            out.append("\n");
        }
        catch (Exception e){
            System.out.println("Failed to generate log file");
            System.exit(0);
        }
        for (OrbitReadLogItem entry : orbitLogList){
            out.append(entry.getTime()  + SimSettings.DELIMITER + entry.getIoTaskID() + SimSettings.DELIMITER + entry.getObjectID() + SimSettings.DELIMITER +
                    entry.getPttlBefore() + SimSettings.DELIMITER + entry.getPttlAfter() + SimSettings.DELIMITER + entry.getDelay() + SimSettings.DELIMITER +
                    entry.getSrcHost() + SimSettings.DELIMITER + entry.getDstHost());
            out.append("\n");
        }
        out.close();
    }



/*    		for(int i=0; i< loadGeneratorModel.getTaskList().size(); i++)
    schedule(getId(), loadGeneratorModel.getTaskList().get(i).getStartTime(), CREATE_TASK, loadGeneratorModel.getTaskList().get(i));*/
}

class OrbitReadLogItem {
    private long time;
    private int ioTaskID;
    private String objectID;
    private long pttlBefore;
    private long pttlAfter;
    private long delay;
    private String srcHost;
    private String dstHost;


    public OrbitReadLogItem(long time, int ioTaskID, String objectID, long pttlBefore, long pttlAfter, String srcHost, String dstHost) {
        this.time = time;
        this.ioTaskID = ioTaskID;
        this.objectID = objectID;
        this.pttlBefore = pttlBefore;
        this.pttlAfter = pttlAfter;
        this.delay = pttlBefore-pttlAfter;
        this.srcHost = srcHost;
        this.dstHost = dstHost;
    }



    public long getTime() {
        return time;
    }

    public int getIoTaskID() {
        return ioTaskID;
    }

    public String getObjectID() {
        return objectID;
    }

    public long getPttlBefore() {
        return pttlBefore;
    }

    public long getPttlAfter() {
        return pttlAfter;
    }

    public long getDelay() {
        return delay;
    }

    public String getSrcHost() {
        return srcHost;
    }

    public String getDstHost() {
        return dstHost;
    }
}
