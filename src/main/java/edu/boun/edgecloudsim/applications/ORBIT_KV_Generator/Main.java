package edu.boun.edgecloudsim.applications.ORBIT_KV_Generator;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.xml.sax.SAXException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.MigrateParams;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws ParserConfigurationException, IOException, TransformerException, SAXException {
        //TESTING
        //load settings from configuration file
        int currentHost=-1;
        String type = "";
        if (args.length == 2){
            type = args[0];
            currentHost = Integer.valueOf(args[1]);
        }
        else if (args.length == 1){
            currentHost = Integer.valueOf(args[0]);
        }
        else {
            type = "host";
            currentHost = 0;
        }

        SimSettings SS = SimSettings.getInstance();
        String configFile = "scripts/ORBIT_KV_Generator/default_config.properties";
        String applicationsFile = "scripts/ORBIT_KV_Generator/applications.xml";
        String edgeDevicesFile = "scripts/ORBIT_KV_Generator/edge_devices.xml";
        if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
            SimLogger.printLine("cannot initialize simulation settings!");
            System.exit(0);
        }
        RedisListHandler.closeConnection();

		RedisListHandler.orbitCreateList("CODING_PLACE",String.valueOf(currentHost));

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);

        if (args.length==0) {
            Jedis jedis = new Jedis("node1-1.sb5.orbit-lab.org", 6379);
            System.out.println("Expire:" + jedis.expire("object:d40",100000));
            System.out.println("PTTL on node1: " + jedis.pttl("object:d40"));
            MigrateParams params = new MigrateParams();
            params.copy();
            params.replace();
            System.out.println("Start migrate");
            jedis.migrate("node1-2.sb5.orbit-lab.org",6379, 0,5000, params, "object:d40");
            Jedis jedisHostname = new Jedis("127.0.0.1", 6379);
            System.out.println("PTTL on node2: " + jedisHostname.pttl("object:d40"));
            jedis.close();
            jedisHostname.close();
//            System.out.println(RedisListHandler.getObjectLocations("node1-1.sb5.orbit-lab.org","d4"));
/*            System.out.println(RedisListHandler.getObjectLocations("node1-1.sb5.orbit-lab.org","d4"));
            System.out.println("Execution time: " + RedisListHandler.slowlogGet("node1-1.sb5.orbit-lab.org",1));
            System.out.println(RedisListHandler.getObjectLocations("node1-1.sb5.orbit-lab.org","d0"));
            System.out.println("Execution time: " + RedisListHandler.slowlogGet("node1-1.sb5.orbit-lab.org",1));
            System.out.println(RedisListHandler.getObjectLocations("node1-1.sb5.orbit-lab.org","d1"));
            System.out.println("Execution time: " + RedisListHandler.slowlogGet("node1-1.sb5.orbit-lab.org",1));*/
        }

    }
}
