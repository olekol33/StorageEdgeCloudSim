/*
 * Title:        EdgeCloudSim - Simulation Settings class
 * 
 * Description: 
 * SimSettings provides system wide simulation settings. It is a
 * singleton class and provides all necessary information to other modules.
 * If you need to use another simulation setting variable in your
 * config file, add related getter methot in this class.
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import edu.boun.edgecloudsim.storage.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.boun.edgecloudsim.utils.SimLogger;
import org.xml.sax.SAXException;

import static edu.boun.edgecloudsim.task_generator.LoadGeneratorModel.DATA_DOWNLOAD;
import static edu.boun.edgecloudsim.task_generator.LoadGeneratorModel.POISSON_INTERARRIVAL;


public class SimSettings {
	private static SimSettings instance = null;
	private Document edgeDevicesDoc = null;
	
	public static final double CLIENT_ACTIVITY_START_TIME = 10;

	//enumarations for the VM types
	public enum VM_TYPES { MOBILE_VM, EDGE_VM, CLOUD_VM }
	
	//enumarations for the VM types
	public enum NETWORK_DELAY_TYPES { WLAN_DELAY, MAN_DELAY, WAN_DELAY }
	
	//predifined IDs for the components.
	public static final int CLOUD_DATACENTER_ID = 1000;
	public static final int MOBILE_DATACENTER_ID = 1001;
	public static final int EDGE_ORCHESTRATOR_ID = 1002;
	public static final int GENERIC_EDGE_DEVICE_ID = 1003;

	public static final int DEVICE_TO_LOCAL_EDGE = 1100;
	public static final int EDGE_TO_EDGE = 1101;
	public static final int LOCAL_EDGE_TO_REMOTE_EDGE = 1102;
	public static final int REMOTE_EDGE_TO_LOCAL_EDGE = 1103;

	//delimiter for output file.
	public static final String DELIMITER = ";";
	
    private double SIMULATION_TIME; //minutes unit in properties file
    private double WARM_UP_PERIOD; //minutes unit in properties file

	private double MM1_QUEUE_MODEL_UPDATE_INTERVAL; //seconds unit in properties file
	private double REQUEST_PROCESSING_TIME; //seconds
    private double INTERVAL_TO_GET_VM_LOAD_LOG; //minutes unit in properties file
    private double INTERVAL_TO_GET_VM_LOCATION_LOG; //minutes unit in properties file
    private boolean FILE_LOG_ENABLED; //boolean to check file logging option
    private boolean DEEP_FILE_LOG_ENABLED; //boolean to check deep file logging option
	private boolean STORAGE_LOG_ENABLED;
	private boolean TERMINATE_FAILED_RUN; //boolean to check deep file logging option
	private boolean COUNT_FAILEDDUETOINACCESSIBILITY;

	private boolean USER_IN_NODES;
	private boolean SCALE_MM1;
	private boolean APPLY_SIGNAL_ATTENUATION;

	private boolean MEASURE_FUNCTION_TIME;
	private boolean QUEUE_ORACLE;
	private boolean OVERHEAD_SCAN;
	private ArrayList<Boolean> SIMULATION_FAILED;
	private ArrayList<Integer> TASKS_IN_INTERVAL;
	private ArrayList<Integer> FAILED_TASKS_IN_INTERVAL;

	private boolean SERVICE_RATE_SCAN;
	private boolean SR_EXP_SAME_OVERHEAD;

	private File SERVICE_RATE_PATH;

	private boolean SHIFT_DEVICE_LOCATION;
	private int SERVICE_RATE_ITERATIONS;
	private int CURRENT_SERVICE_RATE_ITERATION;
	private int REQUESTED_OBJECT_LOCATION;
	private int OBJECT_SIZE_KB;
	private boolean KEEP_SERVICE_RATE_FILE;
	private BufferedWriter serviceRateFileBW;
	private double FAIL_THRESHOLD;
	private double codingRepReqRatio;
	private double REQUEST_RATE_PERCENTAGE_OF_CAPACITY;


	private double OVERHEAD_SCAN_STD;
	private double OVERHEAD_SCAN_MEAN_RATIO;


	String outputFolder;

	private boolean GEN_EDGE_DEVICES_XML;
	private double OVERHEAD;
	private int READRATE;
	private int SERVED_REQS_PER_SEC;

	private double SIM2ORBIT_READRATE_RATIO;

	private int TASKPROCESSINGMBPS;


	private int MIN_NUM_OF_MOBILE_DEVICES;
    private int MAX_NUM_OF_MOBILE_DEVICES;
    private int MOBILE_DEVICE_COUNTER_SIZE;
	private int THIS_MOBILE_DEVICE;

    private int NUM_OF_EDGE_DATACENTERS;
    private int NUM_OF_EDGE_HOSTS;
    private int NUM_OF_EDGE_VMS;
    private int NUM_OF_PLACE_TYPES;
    
    private double WAN_PROPOGATION_DELAY; //seconds unit in properties file
    private double LAN_INTERNAL_DELAY; //seconds unit in properties file
    private double QUEUE_TIMEOUT; //seconds unit in properties file
	private double DELAY_TO_USER;

    private int BANDWITH_WLAN; //Mbps unit in properties file
    private int BANDWITH_WAN; //Mbps unit in properties file
//    private int BANDWITH_GSM; //Mbps unit in properties file

    private int NUM_OF_HOST_ON_CLOUD_DATACENTER;
    private int NUM_OF_VM_ON_CLOUD_HOST;
    private int CORE_FOR_CLOUD_VM;
    private int MIPS_FOR_CLOUD_VM; //MIPS
    private int RAM_FOR_CLOUD_VM; //MB
	private int STORAGE_FOR_CLOUD_VM; //Byte
    
//    private int CORE_FOR_VM;
//    private int MIPS_FOR_VM; //MIPS
//    private int RAM_FOR_VM; //MB
//    private int STORAGE_FOR_VM; //Byte
    
    private String[] SIMULATION_SCENARIOS;
    private String[] ORCHESTRATOR_POLICIES;

	private String[] OBJECT_PLACEMENT;

	private String currentObjectPlacementPolicy;

	private String[] FAIL_SCENARIOS;

    // mean waiting time (minute) is stored for each place types
    private double[] mobilityLookUpTable;

    //grid properties

	private int X_RANGE;
	private int Y_RANGE;
	private int HOST_RADIUS;

	private String OBJECT_DIST_READ;
	private String OBJECT_DIST_PLACE;
	private Boolean HOT_COLD_UNIFORM;
	private String[] HOT_COLD_OBJECTS;
	private String[] HOT_COLD_POPULARITY;
	private int CONGESTED_THRESHOLD;
	private int TRACE_INTERVAL_DURATION;
	private double PARITY_PROB_STEP;

//	private int MAX_CLOUD_REQUESTS;


	//storage properties
	private int RANDOM_SEED;
	private double ZIPF_EXPONENT;
	private int NUM_OF_DATA_OBJECTS;
	private int NUM_OF_STRIPES;
	private int NUM_OF_DATA_IN_STRIPE;
	private int NUM_OF_PARITY_IN_STRIPE;
	private double REDUNDANCY_SHARE;
	private String objectRequestRateArray;
	private ArrayList<String> objectDemandIntervalVector;
	private int reqRatePerSec;
	private double objectRequestRateTotal;

	private double serviceCost;

	//Host failure
	private boolean HOST_FAILURE_SCENARIO;
	private boolean DYNAMIC_FAILURE;
	private String HOST_FAILURE_ID;
	private double HOST_FAILURE_TIME;

	private boolean VARIABILITY_RUN;
	private boolean SPLIT_DEMAND_VECTOR;
	private boolean MMPP;
	private int VARIABILITY_ITERATIONS;


	//ORBIT
	private boolean ORBIT_MODE;
	private boolean SIMULATE_ORBIT_MODE;
	private boolean PARAM_SCAN_MODE;

	//Input properties
	private boolean GPS_COORDINATES_CONVERSION;
	private boolean EXTERNAL_NODES_INPUT;
	private boolean EXTERNAL_DEVICES_INPUT;
	private boolean EXTERNAL_OBJECTS_INPUT;
	private boolean EXTERNAL_OBJECTS_INPUT_FROM_PLACEMENT_CSV;
	private boolean MAP_LARGE_OBJECT_INPUT_TO_SMALL;
	private boolean EXTERNAL_REQUESTS_INPUT;
	private boolean EXPORT_RUN_FILES;
	private boolean TEST_USING_INT;
	private boolean SMOOTH_EXTERNAL_REQUESTS;
	private boolean AVOID_SPIKES_IN_EXTERNAL_REQUESTS;
	private double AVOID_SPIKES_UTILIZATION;
	private String NODES_DIRECT_PATH;
	private String DEVICES_DIRECT_PATH;
	private String OBJECTS_DIRECT_PATH;
	private String REQUESTS_DIRECT_PATH;

	//SPECIAL EXPERIMENT
	private boolean NSF_EXPERIMENT;
	private int RAID;
	private double LAMBDA0_MIN;
	private double LAMBDA0_MAX;
	private double LAMBDA1_MIN;
	private double LAMBDA1_MAX;
	private double LAMBDA0_STEP;
	private double LAMBDA1_STEP;


	//EXTERNAL INPUT
	private HashMap<Integer,String> nodesHashVector;
	private HashMap<Integer,String> devicesHashVector;
	private HashMap<String,String> objectsHashVector;
	private HashMap<String,String> reversedHashVector;
	private HashMap<String, Integer> reversedHashDevicesVector;
	private HashMap<String,StorageObject> objectHash;
	private Vector<StorageDevice> devicesVector;
	private Vector<StorageObject> objectsVector;
	private Vector<StorageRequest> storageRequests;
	private int minXpos;
	private int minYpos;
	private int xRange;
	private int yRange;
	private int numOfExternalTasks;


	private String rundir;

/*	public HashMap<Integer, String> getNodesHashVector() {
		return nodesHashVector;
	}

	public HashMap<Integer, String> getDevicesHashVector() {
		return devicesHashVector;
	}

	public HashMap<String, String> getObjectsHashVector() {
		return objectsHashVector;
	}*/

	public Vector<StorageDevice> getDevicesVector() {
		return devicesVector;
	}

	public Vector<StorageObject> getObjectsVector() {
		return objectsVector;
	}


	public StorageObject getObjectHashItem(String key) {
		return objectHash.get(key);
	}

	public HashMap<String,StorageObject> getObjectHash() {
		return objectHash;
	}

	public double getMinXpos() {
		return minXpos;
	}

	public double getMinYpos() {
		return minYpos;
	}

	public int getxRange() {
		return xRange;
	}

	public int getyRange() {
		return yRange;
	}

	public int getNumOfExternalTasks() {
		return numOfExternalTasks;
	}


	public String getRundir() {
		return rundir;
	}

	public Vector<StorageRequest> getStorageRequests() {
		if(SimSettings.getInstance().isExternalRequests()){
			ParseStorageRequests requestsParser = new ParseStorageRequests();
			storageRequests = requestsParser.prepareRequestsVector(devicesHashVector, objectsHashVector,getPathOfRequestsFile());
			numOfExternalTasks = storageRequests.size();
		}
		return storageRequests;
	}

	public void setPoissonInTaskLookUpTable(int task, double poisson_interarrival) {
		taskLookUpTable[task][2] = poisson_interarrival;
	}

	// following values are stored for each applications defined in applications.xml
    // [0] usage percentage (%)
    // [1] prob. of selecting cloud (%)
    // [2] poisson mean (sec)
    // [3] active period (sec)
    // [4] idle period (sec)
    // [5] avg data upload (KB)
    // [6] avg data download (KB)
    // [7] avg task length (MI)
    // [8] required # of cores
    // [9] vm utilization on edge (%)
    // [10] vm utilization on cloud (%)
    // [11] vm utilization on mobile (%)
    // [12] delay sensitivity [0-1]
    private double[][] taskLookUpTable = null;
    
    private String[] taskNames = null;

	private SimSettings() {
		NUM_OF_PLACE_TYPES = 1;
	}
	
	public static SimSettings getInstance() {
		if(instance == null) {
			instance = new SimSettings();
		}
		return instance;
	}

	static boolean toBoolean(String input){
		return (input != null) ? Boolean.valueOf(input) : null;
	}

	/**
	 * Reads configuration file and stores information to local variables
	 */
	public boolean initialize(String propertiesFile, String edgeDevicesFile, String applicationsFile) throws ParserConfigurationException, IOException, SAXException, TransformerException {
		boolean result = false;
		InputStream input = null;
		objectDemandIntervalVector = new ArrayList<>();
		try {
			input = new FileInputStream(propertiesFile);

			// load a properties file
			Properties prop = new Properties();
			prop.load(input);
			try {
				SIMULATION_TIME = (double)60 * Double.parseDouble(prop.getProperty("simulation_time")); //seconds
				WARM_UP_PERIOD = (double)60 * Double.parseDouble(prop.getProperty("warm_up_period")); //seconds
				MM1_QUEUE_MODEL_UPDATE_INTERVAL = Double.parseDouble(prop.getProperty("mm1_queue_model_update_interval")); //seconds
				REQUEST_PROCESSING_TIME = Double.parseDouble(prop.getProperty("request_processing_time")); //seconds
//				INTERVAL_TO_GET_VM_LOAD_LOG = (double)60 * Double.parseDouble(prop.getProperty("vm_load_check_interval")); //seconds
//				INTERVAL_TO_GET_VM_LOCATION_LOG = (double)60 * Double.parseDouble(prop.getProperty("vm_location_check_interval")); //seconds
				INTERVAL_TO_GET_VM_LOAD_LOG = (double)60 * 0.1; //seconds
				INTERVAL_TO_GET_VM_LOCATION_LOG = (double)60 * 0.1; //seconds
				FILE_LOG_ENABLED = toBoolean(prop.getProperty("file_log_enabled"));
				DEEP_FILE_LOG_ENABLED = toBoolean(prop.getProperty("deep_file_log_enabled"));
				STORAGE_LOG_ENABLED = toBoolean(prop.getProperty("storage_log_enabled"));
				TERMINATE_FAILED_RUN = toBoolean(prop.getProperty("terminate_failed_run"));
				USER_IN_NODES = toBoolean(prop.getProperty("user_in_nodes"));
				SCALE_MM1 = toBoolean(prop.getProperty("scale_mm1"));
				COUNT_FAILEDDUETOINACCESSIBILITY = toBoolean(prop.getProperty("count_failedDueToInaccessibility"));
				APPLY_SIGNAL_ATTENUATION = toBoolean(prop.getProperty("applySignalAttenuation"));
				MEASURE_FUNCTION_TIME = toBoolean(prop.getProperty("measure_function_time"));
				QUEUE_ORACLE = toBoolean(prop.getProperty("queue_oracle"));
				OVERHEAD_SCAN = toBoolean(prop.getProperty("overhead_scan"));
				SERVICE_RATE_SCAN = toBoolean(prop.getProperty("service_rate_scan"));
				SR_EXP_SAME_OVERHEAD = toBoolean(prop.getProperty("sr_exp_same_overhead"));
				SHIFT_DEVICE_LOCATION = toBoolean(prop.getProperty("shift_device_location"));
				SIM2ORBIT_READRATE_RATIO = Double.parseDouble(prop.getProperty("sim2orbit_readrate_ratio"));

				//Generate edge devices xml
				GEN_EDGE_DEVICES_XML = toBoolean(prop.getProperty("gen_edge_devices_xml"));
				if(isGenEdgeDevicesXML()) {
					OVERHEAD = Double.parseDouble(prop.getProperty("overhead"));
					SERVED_REQS_PER_SEC = Integer.parseInt(prop.getProperty("readRate")); //requests per second
					TASKPROCESSINGMBPS = Integer.parseInt(prop.getProperty("taskProcessingMbps"));
				}

				MIN_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("min_number_of_mobile_devices"));
				MAX_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("max_number_of_mobile_devices"));
				MOBILE_DEVICE_COUNTER_SIZE = Integer.parseInt(prop.getProperty("mobile_device_counter_size"));

				WAN_PROPOGATION_DELAY = Double.parseDouble(prop.getProperty("wan_propogation_delay"));
				LAN_INTERNAL_DELAY = Double.parseDouble(prop.getProperty("lan_internal_delay"));
				QUEUE_TIMEOUT = Double.parseDouble(prop.getProperty("queue_timeout"));
				DELAY_TO_USER = Double.parseDouble(prop.getProperty("delay_to_user"));
				FAIL_THRESHOLD = Double.parseDouble(prop.getProperty("fail_threshold"));
//				BANDWITH_WLAN = 1000 * Integer.parseInt(prop.getProperty("wlan_bandwidth"));
//				BANDWITH_WAN = 1000 * Integer.parseInt(prop.getProperty("wan_bandwidth"));
//				BANDWITH_GSM =  1000 * Integer.parseInt(prop.getProperty("gsm_bandwidth"));

//				NUM_OF_HOST_ON_CLOUD_DATACENTER = Integer.parseInt(prop.getProperty("number_of_host_on_cloud_datacenter"));
//				NUM_OF_VM_ON_CLOUD_HOST = Integer.parseInt(prop.getProperty("number_of_vm_on_cloud_host"));
//				CORE_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("core_for_cloud_vm"));
//				MIPS_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("mips_for_cloud_vm"));
//				RAM_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("ram_for_cloud_vm"));
//				STORAGE_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("storage_for_cloud_vm"));
				BANDWITH_WLAN = 1000 * 300;
				BANDWITH_WAN = 1000 * 20;
				NUM_OF_HOST_ON_CLOUD_DATACENTER = 1;
				NUM_OF_VM_ON_CLOUD_HOST = 1;
				CORE_FOR_CLOUD_VM = 1;
				MIPS_FOR_CLOUD_VM = 40000;
				RAM_FOR_CLOUD_VM = 128000;
				STORAGE_FOR_CLOUD_VM = 4000000;

//				RAM_FOR_VM = Integer.parseInt(prop.getProperty("ram_for_mobile_vm"));
//				CORE_FOR_VM = Integer.parseInt(prop.getProperty("core_for_mobile_vm"));
//				MIPS_FOR_VM = Integer.parseInt(prop.getProperty("mips_for_mobile_vm"));
//				STORAGE_FOR_VM = Integer.parseInt(prop.getProperty("storage_for_mobile_vm"));

				ORCHESTRATOR_POLICIES = prop.getProperty("orchestrator_policies").split(",");

				SIMULATION_SCENARIOS = prop.getProperty("simulation_scenarios").split(",");
				OBJECT_PLACEMENT = prop.getProperty("object_placement").split(",");
				FAIL_SCENARIOS = prop.getProperty("fail_scenarios").split(",");

				HOST_FAILURE_SCENARIO = toBoolean(prop.getProperty("host_failure_scenario"));
				DYNAMIC_FAILURE = toBoolean(prop.getProperty("dynamic_failure"));
				HOST_FAILURE_ID = prop.getProperty("host_failure_id");
				HOST_FAILURE_TIME = (double)60 * Double.parseDouble(prop.getProperty("host_failure_time")); //seconds;

				ORBIT_MODE = toBoolean(prop.getProperty("orbit_mode"));
				SIMULATE_ORBIT_MODE = toBoolean(prop.getProperty("simulate_orbit_mode"));
				VARIABILITY_RUN = toBoolean(prop.getProperty("variability_run"));
				SPLIT_DEMAND_VECTOR = toBoolean(prop.getProperty("split_demand_vector"));
				MMPP = toBoolean(prop.getProperty("mmpp"));
				VARIABILITY_ITERATIONS = Integer.parseInt(prop.getProperty("variability_iterations"));
				NUM_OF_EDGE_DATACENTERS = Integer.parseInt(prop.getProperty("number_of_edge_datacenters"));

				if(SERVICE_RATE_SCAN) {
					rundir = "service_rate_app";
					SERVICE_RATE_ITERATIONS = Integer.parseInt(prop.getProperty("service_rate_iterations"));
					REQUEST_RATE_PERCENTAGE_OF_CAPACITY = Double.parseDouble(prop.getProperty("request_rate_percentage_of_capacity"));
					OVERHEAD_SCAN_MEAN_RATIO = Double.parseDouble(prop.getProperty("overhead_scan_mean_ratio"));
					OVERHEAD_SCAN_STD = Double.parseDouble(prop.getProperty("overhead_scan_std"));
					REQUESTED_OBJECT_LOCATION = Integer.parseInt(prop.getProperty("requested_object_location"));
					KEEP_SERVICE_RATE_FILE = toBoolean(prop.getProperty("keep_service_rate_file"));
					if(ORBIT_MODE)
						CURRENT_SERVICE_RATE_ITERATION = Integer.parseInt(prop.getProperty("current_service_rate_iteration"));
				}
				else
					rundir = "sample_app6";

				GPS_COORDINATES_CONVERSION = Boolean.parseBoolean(prop.getProperty("gps_coordinates_conversion"));
				EXTERNAL_NODES_INPUT = Boolean.parseBoolean(prop.getProperty("external_nodes_input"));
				EXTERNAL_DEVICES_INPUT = Boolean.parseBoolean(prop.getProperty("external_devices_input"));
				EXTERNAL_OBJECTS_INPUT = Boolean.parseBoolean(prop.getProperty("external_objects_input"));
				EXTERNAL_OBJECTS_INPUT_FROM_PLACEMENT_CSV = Boolean.parseBoolean(prop.getProperty("external_objects_input_from_placement_csv"));
				MAP_LARGE_OBJECT_INPUT_TO_SMALL = Boolean.parseBoolean(prop.getProperty("map_large_object_input_to_small"));
				if (MAP_LARGE_OBJECT_INPUT_TO_SMALL)
					EXTERNAL_OBJECTS_INPUT_FROM_PLACEMENT_CSV = false;
				EXTERNAL_REQUESTS_INPUT = Boolean.parseBoolean(prop.getProperty("external_requests_input"));
				EXPORT_RUN_FILES = Boolean.parseBoolean(prop.getProperty("export_run_files"));
				TEST_USING_INT = Boolean.parseBoolean(prop.getProperty("test_using_int"));
				SMOOTH_EXTERNAL_REQUESTS = Boolean.parseBoolean(prop.getProperty("smooth_external_requests"));
				AVOID_SPIKES_IN_EXTERNAL_REQUESTS = Boolean.parseBoolean(prop.getProperty("avoid_spikes_in_external_requests"));
				AVOID_SPIKES_UTILIZATION = Double.parseDouble(prop.getProperty("avoid_spikes_utilization"));
				if (SMOOTH_EXTERNAL_REQUESTS && AVOID_SPIKES_IN_EXTERNAL_REQUESTS)
					throw new IllegalStateException("Can't support SMOOTH_EXTERNAL_REQUESTS and" +
							" AVOID_SPIKES_IN_EXTERNAL_REQUESTS ");
				NODES_DIRECT_PATH = prop.getProperty("nodes_direct_path");
				DEVICES_DIRECT_PATH = prop.getProperty("devices_direct_path");
				OBJECTS_DIRECT_PATH = prop.getProperty("objects_direct_path");
				REQUESTS_DIRECT_PATH = prop.getProperty("requests_direct_path");

				PARAM_SCAN_MODE = false;
				serviceCost=0;


				NSF_EXPERIMENT = toBoolean(prop.getProperty("nsf"));
				if(NSF_EXPERIMENT) {
					RAID = Integer.parseInt(prop.getProperty("raid"));
					LAMBDA0_MIN = Double.parseDouble(prop.getProperty("lambda0_min"));
					LAMBDA0_MAX = Double.parseDouble(prop.getProperty("lambda0_max"));
					LAMBDA1_MIN = Double.parseDouble(prop.getProperty("lambda1_min"));
					LAMBDA1_MAX = Double.parseDouble(prop.getProperty("lambda1_max"));
					LAMBDA0_STEP = Double.parseDouble(prop.getProperty("lambda0_step"));
					LAMBDA1_STEP = Double.parseDouble(prop.getProperty("lambda1_step"));
					LAMBDA0_MIN = Double.parseDouble(prop.getProperty("lambda0_min"));
					LAMBDA0_MAX = Double.parseDouble(prop.getProperty("lambda0_max"));
					LAMBDA0_STEP = Double.parseDouble(prop.getProperty("lambda0_step"));
				}
				else{
					REDUNDANCY_SHARE = Double.parseDouble(prop.getProperty("redundancy_share"));
				}


				X_RANGE = Integer.parseInt(prop.getProperty("x_range"));
				Y_RANGE = Integer.parseInt(prop.getProperty("y_range"));
				HOST_RADIUS = Integer.parseInt(prop.getProperty("host_radius"));
				OBJECT_DIST_READ = prop.getProperty("object_dist_read");
				HOT_COLD_UNIFORM = toBoolean(prop.getProperty("hot_cold_uniform"));
				if(HOT_COLD_UNIFORM) {
					OBJECT_DIST_READ = "UNIFORM";
					HOT_COLD_OBJECTS = prop.getProperty("hot_cold_objects").split(",");
					HOT_COLD_POPULARITY = prop.getProperty("hot_cold_popularity").split(",");
				}
				OBJECT_DIST_PLACE = prop.getProperty("object_dist_place");
				CONGESTED_THRESHOLD = Integer.parseInt(prop.getProperty("congested_threshold"));
				PARITY_PROB_STEP = Double.parseDouble(prop.getProperty("parityProbStep"));
				TRACE_INTERVAL_DURATION = Integer.parseInt(prop.getProperty("trace_interval_duration"));
//				MAX_CLOUD_REQUESTS = Integer.parseInt(prop.getProperty("max_cloud_requests"));
				RANDOM_SEED = Integer.parseInt(prop.getProperty("random_seed"));
				ZIPF_EXPONENT = Double.parseDouble(prop.getProperty("zipf_exponent"));
				NUM_OF_DATA_OBJECTS = Integer.parseInt(prop.getProperty("num_of_data_objects"));
				NUM_OF_DATA_IN_STRIPE = Integer.parseInt(prop.getProperty("num_of_data_in_stripe"));
				NUM_OF_PARITY_IN_STRIPE = Integer.parseInt(prop.getProperty("num_of_parity_in_stripe"));
				NUM_OF_STRIPES = Integer.parseInt(prop.getProperty("num_of_stripes"));


			}
			catch (Exception e){
				System.out.println("ERROR: Missing config");
				e.printStackTrace();
				System.exit(1);
			}


//			double place1_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L1_mean_waiting_time"));
//			double place2_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L2_mean_waiting_time"));
//			double place3_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L3_mean_waiting_time"));
			//mean waiting time (minute)
//			mobilityLookUpTable = new double[]{
//				place1_mean_waiting_time, //ATTRACTIVENESS_L1
//				place2_mean_waiting_time, //ATTRACTIVENESS_L2
//				place3_mean_waiting_time  //ATTRACTIVENESS_L3
//		    };
			

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
					result = true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		parseApplicatinosXML(applicationsFile);



		//Service rate experiment
		this.codingRepReqRatio=1;
		if(SERVICE_RATE_SCAN && !SR_EXP_SAME_OVERHEAD) {
			int basicNumberOfNodes = NUM_OF_EDGE_DATACENTERS;
			System.out.println("Different overhead experiment");
			if(!GEN_EDGE_DEVICES_XML || NUM_OF_EDGE_DATACENTERS != 6 || OBJECT_PLACEMENT.length>1 || OVERHEAD != 2)
				throw new IllegalStateException("This mode currently not supported");
//			if(OVERHEAD%1 != 0)
//				throw new IllegalStateException("Overhead is not integer");
//			int ecNodes = (int) (basicNumberOfNodes*((OVERHEAD/2)*1.5));
			if(SimSettings.getInstance().getObjectPlacement()[0].equals("CODING_PLACE")) {
				OVERHEAD = 1.5;
				NUM_OF_STRIPES = NUM_OF_DATA_OBJECTS/2;
			}
			int ecNodes = (int) (basicNumberOfNodes*((1.5)));
			int repNodes = (int) (basicNumberOfNodes*((OVERHEAD)));
			this.codingRepReqRatio = (double)ecNodes/repNodes;
			if(OBJECT_PLACEMENT[0].equals("CODING_PLACE")){
				setNumOfEdgeDatacenters(ecNodes);
			}
			else if(OBJECT_PLACEMENT[0].equals("REPLICATION_PLACE")){
				setNumOfEdgeDatacenters(repNodes);
			}
		}



		//checks if we are in external nodes mode
		List<Object> nodesReturn = null;
		if(SimSettings.instance.isExternalNodes()){
			try{
				NUM_OF_DATA_OBJECTS = countNumberLinesInFile(getPathOfObjectsFile(), true);
				ParseStorageNodes nodeParser = new ParseStorageNodes();
				nodesReturn = nodeParser.prepareNodesHashVector(getPathOfNodesFile());
				nodesHashVector = (HashMap<Integer,String>)nodesReturn.get(0);
				minXpos = nodeParser.getxMin();
				minYpos = nodeParser.getyMin();
				xRange = nodeParser.getxRange();
				yRange = nodeParser.getyRange();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		else
			parseEdgeDevicesXML(edgeDevicesFile, null);

		//checks if we are in external objects mode
		//updates the number of objects accordingly
		if(SimSettings.getInstance().isExternalObjects()){
			ParseStorageObject objectParser = new ParseStorageObject();
			objectsHashVector = objectParser.prepareObjectsHashVector(nodesHashVector, getPathOfObjectsFile());
			reversedHashVector = reverseHash(objectsHashVector);

			if(NUM_OF_DATA_OBJECTS != objectsHashVector.size())
				throw new IllegalStateException("Mismatch in NUM_OF_DATA_OBJECTS");
//			NUM_OF_STRIPES = NUM_OF_DATA_OBJECTS / NUM_OF_DATA_IN_STRIPE;//if external input, use relative share of parities
			NUM_OF_STRIPES = (int)((OVERHEAD-1) * NUM_OF_DATA_OBJECTS);
			objectsVector = objectParser.getObjectsVector();
			objectHash = objectParser.getObjectHash();
		}

		//checks if we are in external devices mode
		//update the min/max number of mobile devices accordingly
		if(SimSettings.instance.isExternalDevices()){
			try{
				ParseStorageDevices deviceParser = new ParseStorageDevices();
				devicesHashVector = deviceParser.prepareDevicesVector(getPathOfDevicesFile(), nodesReturn);
				reversedHashDevicesVector = reverseHashDevices(devicesHashVector);
				devicesVector = deviceParser.getDevicesVector();
				MIN_NUM_OF_MOBILE_DEVICES = devicesHashVector.size();
				MAX_NUM_OF_MOBILE_DEVICES = devicesHashVector.size();
			}catch (Exception e){
				e.printStackTrace();
			}
		}//proceed
		return result;
	}

	//was added by Harel
	private HashMap<String, String> reverseHash(HashMap<String,String> map){
		HashMap<String,String> reversedHash = new HashMap<>();
		for(Map.Entry<String,String> m : map.entrySet()){
			reversedHash.put(m.getValue(),m.getKey());
		}
		return reversedHash;
	}

	//was added by Harel
	private HashMap<String, Integer> reverseHashDevices(HashMap<Integer,String> map){
		HashMap<String,Integer> reversedHash = new HashMap<>();
		for(Map.Entry<Integer,String> m : map.entrySet()){
			reversedHash.put(m.getValue(),m.getKey());
		}
		return reversedHash;
	}
	/**
	 * returns the parsed XML document for edge_devices.xml
	 */
	public Document getEdgeDevicesDocument(){
		return edgeDevicesDoc;
	}


	/**
	 * returns simulation time (in seconds unit) from properties file
	 */
	public double getSimulationTime()
	{
		return SIMULATION_TIME;
	}

	public void setX_RANGE(int x_RANGE) {
		X_RANGE = x_RANGE;
	}

	public void setY_RANGE(int y_RANGE) {
		Y_RANGE = y_RANGE;
	}

	/**
	 * returns warm up period (in seconds unit) from properties file
	 */
	public double getWarmUpPeriod()
	{
		return WARM_UP_PERIOD; 
	}


	public double getRequestProcessingTime() {
		return REQUEST_PROCESSING_TIME;
	}

	public double getMm1QueueModelUpdateInterval() {
		return MM1_QUEUE_MODEL_UPDATE_INTERVAL;
	}

	/**
	 * returns VM utilization log collection interval (in seconds unit) from properties file
	 */
	public double getVmLoadLogInterval()
	{
		return INTERVAL_TO_GET_VM_LOAD_LOG; 
	}

	/**
	 * returns VM location log collection interval (in seconds unit) from properties file
	 */
	public double getVmLocationLogInterval()
	{
		return INTERVAL_TO_GET_VM_LOCATION_LOG; 
	}

	/**
	 * returns deep statistics logging status from properties file
	 */
	public boolean getDeepFileLoggingEnabled()
	{
		return DEEP_FILE_LOG_ENABLED; 
	}


	public boolean isStorageLogEnabled() {
		return STORAGE_LOG_ENABLED;
	}

	/**
	 * returns deep statistics logging status from properties file
	 */
	public boolean getFileLoggingEnabled()
	{
		return FILE_LOG_ENABLED; 
	}
	
	/**
	 * returns WAN propogation delay (in second unit) from properties file
	 */
	public double getWanPropogationDelay()
	{
		return WAN_PROPOGATION_DELAY;
	}

	/**
	 * returns internal LAN propogation delay (in second unit) from properties file
	 */
	public double getInternalLanDelay()
	{
		return LAN_INTERNAL_DELAY;
	}

	public double getQueueTimeout() {
		return QUEUE_TIMEOUT;
	}

	public double getDelayToUser() {
		return DELAY_TO_USER;
	}

	/**
	 * returns WLAN bandwidth (in Mbps unit) from properties file
	 */
	public int getWlanBandwidth()
	{
		return BANDWITH_WLAN;
	}

	/**
	 * returns WAN bandwidth (in Mbps unit) from properties file
	 */
	public int getWanBandwidth()
	{
		return BANDWITH_WAN; 
	}

/*	public int getGsmBandwidth()
	{
		return BANDWITH_GSM;
	}*/
	public double getOverhead() {
		return OVERHEAD;
	}

	public int getReadRate() {
		return READRATE;
	}

	public int getTaskProcessingMbps() {
		return TASKPROCESSINGMBPS;
	}
	/**
	 * returns the minimum number of the mobile devices used in the simulation
	 */
	public int getMinNumOfMobileDev()
	{
		return MIN_NUM_OF_MOBILE_DEVICES;
	}

	/**
	 * returns the maximunm number of the mobile devices used in the simulation
	 */
	public int getMaxNumOfMobileDev()
	{
		return MAX_NUM_OF_MOBILE_DEVICES;
	}

	/**
	 * returns the number of increase on mobile devices
	 * while iterating from min to max mobile device
	 */
	public int getMobileDevCounterSize()
	{
		return MOBILE_DEVICE_COUNTER_SIZE;
	}

	public int getThisMobileDevice() {
		return THIS_MOBILE_DEVICE;
	}

	public void setThisMobileDevice(int THIS_MOBILE_DEVICE) {
		this.THIS_MOBILE_DEVICE = THIS_MOBILE_DEVICE;
	}

	/**
	 * returns the number of edge datacenters
	 */
	public int getNumOfEdgeDatacenters()
	{
		return NUM_OF_EDGE_DATACENTERS;
	}

	/**
	 * returns the number of edge hosts running on the datacenters
	 */
	public int getNumOfEdgeHosts()
	{
		return NUM_OF_EDGE_HOSTS;
	}

	/**
	 * returns the number of edge VMs running on the hosts
	 */
	public int getNumOfEdgeVMs()
	{
		return NUM_OF_EDGE_VMS;
	}
	
	/**
	 * returns the number of different place types
	 */
	public int getNumOfPlaceTypes()
	{
		return NUM_OF_PLACE_TYPES;
	}

	/**
	 * returns the number of cloud datacenters
	 */
	public int getNumOfCloudHost()
	{
		return NUM_OF_HOST_ON_CLOUD_DATACENTER;
	}

	/**
	 * returns the number of cloud VMs per Host
	 */
	public int getNumOfCloudVMsPerHost()
	{
		return NUM_OF_VM_ON_CLOUD_HOST;
	}

	/**
	 * returns the total number of cloud VMs
	 */
	public int getNumOfCloudVMs()
	{
		return NUM_OF_VM_ON_CLOUD_HOST * NUM_OF_HOST_ON_CLOUD_DATACENTER;
	}

	/**
	 * returns the number of cores for cloud VMs
	 */
	public int getCoreForCloudVM()
	{
		return CORE_FOR_CLOUD_VM;
	}

	/**
	 * returns MIPS of the central cloud VMs
	 */
	public int getMipsForCloudVM()
	{
		return MIPS_FOR_CLOUD_VM;
	}

	/**
	 * returns RAM of the central cloud VMs
	 */
	public int getRamForCloudVM()
	{
		return RAM_FOR_CLOUD_VM;
	}

	/**
	 * returns Storage of the central cloud VMs
	 */
	public int getStorageForCloudVM()
	{
		return STORAGE_FOR_CLOUD_VM;
	}


	/**
	 * returns simulation screnarios as string
	 */
	public String[] getSimulationScenarios()
	{
		return SIMULATION_SCENARIOS;
	}

	/**
	 * returns orchestrator policies as string
	 */
	public String[] getOrchestratorPolicies()
	{
		return ORCHESTRATOR_POLICIES;
	}

	public String[] getObjectPlacement() {
		return OBJECT_PLACEMENT;
	}


	public String[] getFailScenarios() {
		return FAIL_SCENARIOS;
	}

	public boolean isHostFailureScenario() {
		return HOST_FAILURE_SCENARIO;
	}

	public boolean isDynamicFailure() {
		return DYNAMIC_FAILURE;
	}

	public void setHostFailureScenario(boolean HOST_FAILURE_SCENARIO) {
		this.HOST_FAILURE_SCENARIO = HOST_FAILURE_SCENARIO;
	}

	public int[] getHostFailureID() {
		return Arrays.asList(HOST_FAILURE_ID.split(",")).stream().mapToInt(Integer::parseInt).toArray();
	}


	public boolean isTerminateFailedRun() {
		return TERMINATE_FAILED_RUN;
	}

	public boolean isApplySignalAttenuation() {
		return APPLY_SIGNAL_ATTENUATION;
	}

	public boolean isQueueOracle() {
		return QUEUE_ORACLE;
	}

	public boolean isMeasurefunctionTime() {
		return MEASURE_FUNCTION_TIME;
	}

	public boolean isCountFailedduetoinaccessibility() {
		return COUNT_FAILEDDUETOINACCESSIBILITY;
	}


	public boolean isUserInNodes() {
		return USER_IN_NODES;
	}


	public boolean isScaleMm1() {
		return SCALE_MM1;
	}

	public boolean isOverheadScan() {
		return OVERHEAD_SCAN;
	}


	public ArrayList<Boolean> isSimulationFailed() {
		return SIMULATION_FAILED;
	}

	public int getTasksInInterval(int interval) {
		return TASKS_IN_INTERVAL.get(interval);
	}

	public int getFailedTasksInInterval(int interval) {
		return FAILED_TASKS_IN_INTERVAL.get(interval);
	}

	public void setSimulationFailed(int interval) {
		this.SIMULATION_FAILED.add(interval, true);
	}

	public void setSimulationCompleted(int interval) {
		this.SIMULATION_FAILED.add(interval, false);
	}

	public void setEntireSimulationFailed() {
		this.SIMULATION_FAILED.set(0, true);
	}

	public void setTasksInInterval(int interval, int tasksInInterval, int failedTasksInInterval) {
		this.TASKS_IN_INTERVAL.add(interval, tasksInInterval);
		this.FAILED_TASKS_IN_INTERVAL.add(interval, failedTasksInInterval);
	}

	public void resetSimulationFailed(){
		SIMULATION_FAILED = new ArrayList<>();
		TASKS_IN_INTERVAL = new ArrayList<>();
		FAILED_TASKS_IN_INTERVAL = new ArrayList<>();
//		Arrays.fill(SIMULATION_FAILED, false);
//		Arrays.fill(TASKS_IN_INTERVAL, 0);
//		Arrays.fill(FAILED_TASKS_IN_INTERVAL, 0);
	}

	public int getServiceRateIterations() {
		return SERVICE_RATE_ITERATIONS;
	}
	public int getCurrentServiceRateIteration() {
		if(SERVICE_RATE_SCAN)
			return CURRENT_SERVICE_RATE_ITERATION;
		else
			return 0;
	}


	public int getRequestedObjectLocation() {
		return REQUESTED_OBJECT_LOCATION;
	}

	public int getObjectSize() {
		return OBJECT_SIZE_KB;
	}

	public void setObjectSize(int OBJECT_SIZE_KB) {
		this.OBJECT_SIZE_KB = OBJECT_SIZE_KB;
	}

	public boolean isKeepServiceRateFile() {
		return KEEP_SERVICE_RATE_FILE;
	}


	public void setCurrentServiceRateIteration(int CURRENT_SERVICE_RATE_ITERATION) {
		this.CURRENT_SERVICE_RATE_ITERATION = CURRENT_SERVICE_RATE_ITERATION;
	}

	public void setServiceRateFileBW(BufferedWriter serviceRateFileBW) {
		this.serviceRateFileBW = serviceRateFileBW;
	}

	public boolean isShiftDeviceLocation() {
		return SHIFT_DEVICE_LOCATION;
	}

	public boolean isServiceRateScan() {
		return SERVICE_RATE_SCAN;
	}

	public File getServiceratePath() {
		return SERVICE_RATE_PATH;
	}

	public void setServiceRatePath(File SERVICE_RATE_PATH) {
		this.SERVICE_RATE_PATH = SERVICE_RATE_PATH;
	}

	public boolean isGenEdgeDevicesXML() {
		return GEN_EDGE_DEVICES_XML;
	}


	public double getFailThreshold() {
		return FAIL_THRESHOLD;
	}


	public double getCodingRepReqRatio() {
		return codingRepReqRatio;
	}


	public double getOverheadScanStd() {
		return OVERHEAD_SCAN_STD;
	}


	public double getOverheadScanMeanRatio() {
		return OVERHEAD_SCAN_MEAN_RATIO;
	}

	public double getRequestRatePercentageOfCapacity() {
		return REQUEST_RATE_PERCENTAGE_OF_CAPACITY;
	}


	public String getOutputFolder() {
		return outputFolder;
	}
	public String getSerializableFolder() {
		return outputFolder + "/serial";
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public double getHostFailureTime() {
		return HOST_FAILURE_TIME;
	}

	public boolean isOrbitMode() {
		return ORBIT_MODE;
	}


	public boolean isSimulateOrbitMode() {
		return SIMULATE_ORBIT_MODE;
	}

//	public HashMap<String, String> getReversedHashVector() {
//		return reversedHashVector;
//	}

	public HashMap<String, Integer> getReversedHashDevicesVector() {
		return reversedHashDevicesVector;
	}

	public boolean isGpsConversionRequired(){
		return GPS_COORDINATES_CONVERSION;
	}

	public boolean isExternalNodes(){ //TODO: set number_of_edge_datacenters by number of nodes
		return EXTERNAL_NODES_INPUT;
	}

	public boolean isExternalDevices(){
		return EXTERNAL_DEVICES_INPUT;
	}

	public boolean isExternalObjects(){
		return EXTERNAL_OBJECTS_INPUT;
	}
	public boolean isExternalObjectsFromPlacementCSV(){
		return EXTERNAL_OBJECTS_INPUT_FROM_PLACEMENT_CSV;
	}
	public boolean isMapLargeObjectInputToSmall(){
		return MAP_LARGE_OBJECT_INPUT_TO_SMALL;
	}

	public boolean getExternalObjectLocationCSV(){
		return (EXTERNAL_OBJECTS_INPUT_FROM_PLACEMENT_CSV || MAP_LARGE_OBJECT_INPUT_TO_SMALL);
	}

	public boolean isExternalRequests(){
		return EXTERNAL_REQUESTS_INPUT;
	}


	public boolean isExportRunFiles() {
		return EXPORT_RUN_FILES;
	}

	public boolean isItIntTest(){
		return TEST_USING_INT;
	}


	public boolean isSmoothExternalRequests() {
		return SMOOTH_EXTERNAL_REQUESTS;
	}

	public boolean isAvoidSpikesInExternalRequests() {
		return AVOID_SPIKES_IN_EXTERNAL_REQUESTS;
	}

	public double getAvoidSpikesUtilization() {
		return AVOID_SPIKES_UTILIZATION;
	}

	public String getPathOfNodesFile(){
		if(NODES_DIRECT_PATH.equals("")) {
			return "scripts/" + rundir+ "/input_files/nodes.csv";
		}else{
			return NODES_DIRECT_PATH;
		}
	}

	public String getPathOfDevicesFile(){
		if(DEVICES_DIRECT_PATH.equals("")) {
			return "scripts/" + rundir+ "/input_files/devices.csv";
		}else{
			return DEVICES_DIRECT_PATH;
		}
	}

	public String getPathOfObjectsFile(){
		if(OBJECTS_DIRECT_PATH.equals("")) {
			return "scripts/" + rundir+ "/input_files/objects.csv";
		}else{
			return OBJECTS_DIRECT_PATH;
		}
	}

	public String getPathOfObjectsCsvFile(boolean coding){
		String suffix = "ORBIT";
		if (SimSettings.getInstance().isMapLargeObjectInputToSmall())
			suffix = "LARGE";
		if(OBJECTS_DIRECT_PATH.equals("")) {
			if (coding)
				return "scripts/" + rundir+ "/input_files/SIMRESULT_SERVICE_RATE_CODING_PLACE_PLACEMENT_" + suffix +
						".csv";
			else
				return "scripts/" + rundir+ "/input_files/SIMRESULT_SERVICE_RATE_REPLICATION_PLACE_PLACEMENT_" + suffix
						+".csv";
		}else{
			return OBJECTS_DIRECT_PATH;
		}
	}

	public String getPathOfRequestsFile(){
		if(REQUESTS_DIRECT_PATH.equals("")) {
			return "scripts/" + rundir+ "/input_files/requests.csv";
		}else{
			return REQUESTS_DIRECT_PATH;
		}
	}

	public boolean isVariabilityRun() {
		return VARIABILITY_RUN;
	}

	public boolean isSplitDemandVector() {
		return SPLIT_DEMAND_VECTOR;
	}

	public boolean isMMPP() {
		return MMPP;
	}

	public int getVariabilityIterations() {
		return VARIABILITY_ITERATIONS;
	}

	public boolean isParamScanMode() {
		return PARAM_SCAN_MODE;
	}

	public void checkRunMode(){
		boolean mode=false;
		if (isOrbitMode()) {
			System.out.println("ORBIT Mode");
			if (!mode)
				mode = true;
			else {
				System.out.println("ERROR: Multiple modes");
				System.exit(0);
			}
		}
		if (isParamScanMode()) {
			System.out.println("Param Scan Mode");
			if (!mode)
				mode = true;
			else {
				System.out.println("ERROR: Multiple modes");
				System.exit(0);
			}
		}
		if (isNsfExperiment()) {
			System.out.println("NSF Experiment Mode");
			if (!mode)
				mode = true;
			else {
				System.out.println("ERROR: Multiple modes");
				System.exit(0);
			}
		}
		if (isHostFailureScenario()) {
			System.out.println("Host Failure Mode");
			if (!mode)
				mode = true;
			else {
				System.out.println("Multiple modes");
			}
		}
		if (!mode)
			System.out.println("No special modes");

	}

	public void setNumOfEdgeDatacenters(int nodes) {
		NUM_OF_EDGE_DATACENTERS = nodes;
	}


	public double getLambda0Min() {
		return LAMBDA0_MIN;
	}

	public double getLambda0Max() {
		return LAMBDA0_MAX;
	}

	public double getLambda1Min() {
		return LAMBDA1_MIN;
	}

	public double getLambda1Max() {
		return LAMBDA1_MAX;
	}

	public double getLambda0step() {
		return LAMBDA0_STEP;
	}
	public void setREADRATE(int READRATE) {
		this.READRATE = READRATE;
	}

	public double getLambda1step() {
		return LAMBDA1_STEP;
	}
	
	/**
	 * returns mobility characteristic within an array
	 * the result includes mean waiting time (minute) or each place type
	 */ 
	public double[] getMobilityLookUpTable()
	{
		return mobilityLookUpTable;
	}

	/**
	 * returns application characteristic within two dimensional array
	 * the result includes the following values for each application type
	 * [0] usage percentage (%)
	 * [1] prob. of selecting cloud (%)
	 * [2] poisson mean (sec)
	 * [3] active period (sec)
	 * [4] idle period (sec)
	 * [5] avg data upload (KB)
	 * [6] avg data download (KB)
	 * [7] avg task length (MI)
	 * [8] required # of cores
	 * [9] vm utilization on edge (%)
	 * [10] vm utilization on cloud (%)
	 * [11] vm utilization on mobile (%)
	 * [12] delay sensitivity [0-1]
	 */ 
	public double[][] getTaskLookUpTable()
	{
		return taskLookUpTable;
	}
	
	public String getTaskName(int taskType)
	{
		return taskNames[taskType];
	}

	public int getXRange() {
		return X_RANGE;
	}

	public int getYRange() {
		return Y_RANGE;
	}

	public int getHostRadius() {
		return HOST_RADIUS;
	}
	public int getServedReqsPerSec() {
		return SERVED_REQS_PER_SEC;
	}

	public double getSim2orbitReadrateRatio() {
		return SIM2ORBIT_READRATE_RATIO;
	}


	public String getObjectDistRead() {
		return OBJECT_DIST_READ;
	}
	public String getObjectDistPlace() {
		return OBJECT_DIST_PLACE;
	}


	public Boolean getHotColdUniform() {
		return HOT_COLD_UNIFORM;
	}


	public String[] getHotColdObjectRatio() {
		return HOT_COLD_OBJECTS;
	}

	public String[] getHotColdPopularityRatio() {
		return HOT_COLD_POPULARITY;
	}

	public void setObjectDistRead(String OBJECT_DIST_READ) {
		this.OBJECT_DIST_READ = OBJECT_DIST_READ;
	}

	public int getCongestedThreshold() {
		return CONGESTED_THRESHOLD;
	}


	public int getTraceIntervalDuration() {
		return TRACE_INTERVAL_DURATION;
	}


	public double getParityProbStep() {
		return PARITY_PROB_STEP;
	}

	public int getRandomSeed() {
		return RANDOM_SEED;
	}

	public void setRandomSeed(int RANDOM_SEED) {
		this.RANDOM_SEED = RANDOM_SEED;
	}

	public double getZipfExponent() {
		return ZIPF_EXPONENT;
	}


	public void setZipfExponent(double ZIPF_EXPONENT) {
		this.ZIPF_EXPONENT = ZIPF_EXPONENT;
	}

	public int getNumOfDataObjects() {
		return NUM_OF_DATA_OBJECTS;
	}

	public void setNumOfDataObjects(int NUM_OF_DATA_OBJECTS) {
		this.NUM_OF_DATA_OBJECTS = NUM_OF_DATA_OBJECTS;
	}




	public boolean isNsfExperiment() {
		return NSF_EXPERIMENT;
	}

	public void setNsfExperiment(boolean NSF_EXPERIMENT) {
		this.NSF_EXPERIMENT = NSF_EXPERIMENT;
	}

	public void setParamScanMode(boolean PARAM_SCAN_MODE) {
		this.PARAM_SCAN_MODE = PARAM_SCAN_MODE;
	}

	public int getRAID() {
		return RAID;
	}

//	public void setRAID(int RAID) {
//		this.RAID = RAID;
//	}
	public void setNumOfStripes(int NUM_OF_STRIPES) {
		this.NUM_OF_STRIPES = NUM_OF_STRIPES;
	}
	public int getNumOfStripes() {
		return NUM_OF_STRIPES;
	}


	public int getNumOfDataInStripe() {
		return NUM_OF_DATA_IN_STRIPE;
	}

	public int getNumOfParityInStripe() {
		return NUM_OF_PARITY_IN_STRIPE;
	}


	public double getRedundancyShare() {
		return REDUNDANCY_SHARE;
	}

	public double getServiceCost() {
		return serviceCost;
	}

	public void setServiceCost(double serviceCost) {
		this.serviceCost = serviceCost;
	}


	public String getObjectRequestRateArray() {
		return objectRequestRateArray;
	}

	public String getObjectDemandIntervalVector(int interval) {
		return objectDemandIntervalVector.get(interval);
	}

	public void appendDemandVector(String demandVector) {
		this.objectDemandIntervalVector.add(demandVector);
	}

	public double getObjectRequestRateTotal() {
		return objectRequestRateTotal;
	}


	public int getReqRatePerSec() {
		return reqRatePerSec;
	}

	public void setReqRatePerSec(int reqRatePerSec) {
		this.reqRatePerSec = reqRatePerSec;
	}



	private int countNumberLinesInFile(String filepath, boolean ignoreHeader)  {
		try {
			Path file = Paths.get(filepath);
			int count = (int)Files.lines(file).count();
			if(ignoreHeader)
				count--;
			return count;
		} catch (Exception e) {
			e.getStackTrace();
			return 0;
		}
	}


	public void setObjectRequestRateArray(double[] objectRequestRateDoubleArray) {
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(4);
		this.objectRequestRateArray = "";
		objectRequestRateTotal=0;
		for (int i=0; i<objectRequestRateDoubleArray.length-1;i++){
			objectRequestRateTotal += objectRequestRateDoubleArray[i];
			objectRequestRateArray += df.format(objectRequestRateDoubleArray[i]) + ",";
		}
		objectRequestRateArray += String.valueOf(df.format(objectRequestRateDoubleArray[objectRequestRateDoubleArray.length-1]));
	}

	private void isAttribtuePresent(Element element, String key) {
        String value = element.getAttribute(key);
//        if (value.isEmpty() || value == null){
        if (value.isEmpty()){
        	throw new IllegalArgumentException("Attribure '" + key + "' is not found in '" + element.getNodeName() +"'");
        }
	}

	private void isElementPresent(Element element, String key) {
		try {
			String value = element.getElementsByTagName(key).item(0).getTextContent();
	        if (value.isEmpty()){
	        	throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
	        }
		} catch (Exception e) {
			throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
		}
	}
	
	private void parseApplicatinosXML(String filePath)
	{
		Document doc;
		try {	
			File devicesFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(devicesFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			taskLookUpTable = new double[appList.getLength()][14];
			taskNames = new String[appList.getLength()];
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);
	
				Element appElement = (Element) appNode;
				isAttribtuePresent(appElement, "name");
				isElementPresent(appElement, "usage_percentage");
//				isElementPresent(appElement, "prob_cloud_selection");
				isElementPresent(appElement, "req_per_sec");
//				isElementPresent(appElement, "poisson_interarrival");
				isElementPresent(appElement, "active_period");
				isElementPresent(appElement, "idle_period");
				isElementPresent(appElement, "data_upload");
				isElementPresent(appElement, "data_download");
//				isElementPresent(appElement, "task_length");
//				isElementPresent(appElement, "required_core");
//				isElementPresent(appElement, "vm_utilization_on_edge");
//				isElementPresent(appElement, "vm_utilization_on_cloud");
//				isElementPresent(appElement, "vm_utilization_on_mobile");
//				isElementPresent(appElement, "delay_sensitivity");

				String taskName = appElement.getAttribute("name");
				taskNames[i] = taskName;
				
				double usage_percentage = Double.parseDouble(appElement.getElementsByTagName("usage_percentage").item(0).getTextContent());
//				double prob_cloud_selection = Double.parseDouble(appElement.getElementsByTagName("prob_cloud_selection").item(0).getTextContent());
//				double poisson_interarrival = Double.parseDouble(appElement.getElementsByTagName("poisson_interarrival").item(0).getTextContent());
				double poisson_interarrival = 1/Double.parseDouble(appElement.getElementsByTagName("req_per_sec").item(0).getTextContent());
				double active_period = Double.parseDouble(appElement.getElementsByTagName("active_period").item(0).getTextContent());
				double idle_period = Double.parseDouble(appElement.getElementsByTagName("idle_period").item(0).getTextContent());
				double data_upload = Double.parseDouble(appElement.getElementsByTagName("data_upload").item(0).getTextContent());
				double data_download = Double.parseDouble(appElement.getElementsByTagName("data_download").item(0).getTextContent());
//				double task_length = Double.parseDouble(appElement.getElementsByTagName("task_length").item(0).getTextContent());
//				double required_core = Double.parseDouble(appElement.getElementsByTagName("required_core").item(0).getTextContent());
//				double vm_utilization_on_edge = Double.parseDouble(appElement.getElementsByTagName("vm_utilization_on_edge").item(0).getTextContent());
//				double vm_utilization_on_cloud = Double.parseDouble(appElement.getElementsByTagName("vm_utilization_on_cloud").item(0).getTextContent());
//				double vm_utilization_on_mobile = Double.parseDouble(appElement.getElementsByTagName("vm_utilization_on_mobile").item(0).getTextContent());
//				double delay_sensitivity = Double.parseDouble(appElement.getElementsByTagName("delay_sensitivity").item(0).getTextContent());
				//Oleg
				int readRate = SimSettings.getInstance().getServedReqsPerSec()*(int)(data_download); //reqs to B
				SimSettings.getInstance().setREADRATE(readRate);
/*
				try {
					double sampling_method = Integer.parseInt(appElement.getElementsByTagName("sampling_method").item(0).getTextContent());
					taskLookUpTable[i][13] = sampling_method; //0 - with replacement, 1 - without replacement
				}
				catch (Exception e)
				{
					SimLogger.printLine("The simulation has been terminated due to an unexpected error");
					e.printStackTrace();
					System.exit(0);
				}
*/

			    taskLookUpTable[i][0] = usage_percentage; //usage percentage [0-100]
//			    taskLookUpTable[i][1] = prob_cloud_selection; //prob. of selecting cloud [0-100]
			    taskLookUpTable[i][2] = poisson_interarrival; //poisson mean (sec)
			    taskLookUpTable[i][3] = active_period; //active period (sec)
			    taskLookUpTable[i][4] = idle_period; //idle period (sec)
			    taskLookUpTable[i][5] = data_upload; //avg data upload (KB)
			    taskLookUpTable[i][6] = data_download; //avg data download (KB)
//			    taskLookUpTable[i][7] = task_length; //avg task length (MI)
//			    taskLookUpTable[i][8] = required_core; //required # of core
//			    taskLookUpTable[i][9] = vm_utilization_on_edge; //vm utilization on edge vm [0-100]
//			    taskLookUpTable[i][10] = vm_utilization_on_cloud; //vm utilization on cloud vm [0-100]
//			    taskLookUpTable[i][11] = vm_utilization_on_mobile; //vm utilization on mobile vm [0-100]
//			    taskLookUpTable[i][12] = delay_sensitivity; //delay_sensitivity [0-1]

			}
	
		} catch (Exception e) {
			SimLogger.printLine("Edge Devices XML cannot be parsed! Terminating simulation...");
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void printServiceRate(int numOfUsers){
		double objectSize = taskLookUpTable[0][DATA_DOWNLOAD]/1000; //MB
		double reqsPerSec = 1/taskLookUpTable[0][POISSON_INTERARRIVAL];
		double systemServiceRate = getNumOfEdgeDatacenters() * getReadRate();
		double systemRequestRate = numOfUsers*objectSize*reqsPerSec;
		double serviceRate = systemRequestRate/systemServiceRate;
		System.out.println("Total request rate: " +String.valueOf(serviceRate) + "  mu-total");
	}

	private void generateEdgeDevicesXML(String filePath, Vector<StorageNode> nodesVector) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		edgeDevicesDoc = dBuilder.newDocument();
		Element root = edgeDevicesDoc.createElement("edge_devices");
		edgeDevicesDoc.appendChild(root);
		int objectSize = (int)SimSettings.getInstance().getTaskLookUpTable()[0][6];
		int dataSize = NUM_OF_DATA_OBJECTS*objectSize; //TODO: change to long
		double maxCapPerNode = (dataSize*OVERHEAD)/NUM_OF_EDGE_DATACENTERS;
		double storedInNode = maxCapPerNode;
		if(storedInNode%1!=0)
			throw new IllegalStateException("Total data size is not int");
		if(maxCapPerNode % objectSize >0)
			storedInNode += objectSize - maxCapPerNode % objectSize; //round up to fit all
		int storage = (int)storedInNode;
		int readRate = SimSettings.getInstance().getReadRate();
		int taskProcessingMbps = SimSettings.getInstance().getTaskProcessingMbps();
		int hostRadius = 2*SimSettings.getInstance().getHostRadius()+1; //such that each device in one node
		int x_pos=hostRadius;
		int y_pos=hostRadius;
		int wlan_id=0;
		if(isExternalNodes()){
			x_pos=(int)nodesVector.get(0).getxPos();
			y_pos=(int)nodesVector.get(0).getyPos();
			readRate=(int)nodesVector.get(0).getServiceRate();
		}

		// datacenter element
		for (int i=0; i<NUM_OF_EDGE_HOSTS; i++){
			Element datacenter = edgeDevicesDoc.createElement("datacenter");
			root.appendChild(datacenter);
			// location element
			Element location = edgeDevicesDoc.createElement("location");
			datacenter.appendChild(location);

			Element x_pos_xml = edgeDevicesDoc.createElement("x_pos");
			x_pos_xml.appendChild(edgeDevicesDoc.createTextNode(String.valueOf(x_pos)));
			location.appendChild(x_pos_xml);
			Element y_pos_xml = edgeDevicesDoc.createElement("y_pos");
			y_pos_xml.appendChild(edgeDevicesDoc.createTextNode(String.valueOf(y_pos)));
			location.appendChild(y_pos_xml);
			Element wlan_id_xml = edgeDevicesDoc.createElement("wlan_id");
			wlan_id_xml.appendChild(edgeDevicesDoc.createTextNode(String.valueOf(wlan_id)));
			location.appendChild(wlan_id_xml);
			x_pos += hostRadius;
			y_pos += hostRadius;

			wlan_id++;

			Element host = edgeDevicesDoc.createElement("host");
			datacenter.appendChild(host);
			Element storage_xml = edgeDevicesDoc.createElement("storage");
			storage_xml.appendChild(edgeDevicesDoc.createTextNode(String.valueOf(storage)));
			host.appendChild(storage_xml);
			Element readRate_xml = edgeDevicesDoc.createElement("readRate");
			readRate_xml.appendChild(edgeDevicesDoc.createTextNode(String.valueOf(readRate)));
			host.appendChild(readRate_xml);
			Element taskProcessingMbps_xml = edgeDevicesDoc.createElement("taskProcessingMbps");
			taskProcessingMbps_xml.appendChild(edgeDevicesDoc.createTextNode(String.valueOf(taskProcessingMbps)));
			host.appendChild(taskProcessingMbps_xml);
			if(isExternalNodes() && i<NUM_OF_EDGE_HOSTS-1){
				x_pos=(int)nodesVector.get(i+1).getxPos();
				y_pos=(int)nodesVector.get(i+1).getyPos();
				readRate=(int)nodesVector.get(0).getServiceRate();
			}
		}
		// create the xml file
		//transform the DOM Object to an XML File
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource domSource = new DOMSource(edgeDevicesDoc);
		StreamResult streamResult = new StreamResult(new File(filePath));
		transformer.transform(domSource, streamResult);

		SimSettings.getInstance().setX_RANGE(x_pos + hostRadius);
		SimSettings.getInstance().setY_RANGE(y_pos + hostRadius);

		System.out.println(String.format("Generated edge_devices.xml with %d nodes, node capacity %d",NUM_OF_EDGE_DATACENTERS,storage));

	}

	public void parseEdgeDevicesXML(String filePath, Vector<StorageNode> nodesVector) {
		try {
			if (SimSettings.getInstance().isGenEdgeDevicesXML()){
				NUM_OF_EDGE_HOSTS= SimSettings.getInstance().getNumOfEdgeDatacenters();
				NUM_OF_EDGE_DATACENTERS= NUM_OF_EDGE_HOSTS;
				NUM_OF_EDGE_VMS=NUM_OF_EDGE_DATACENTERS;
				File genfile = new File(filePath);
				genfile = genfile.getParentFile();
				genfile = new File(genfile,"edge_devices.xml");
				filePath = genfile.toString();
				generateEdgeDevicesXML(filePath, nodesVector);
				return;
			}
			File devicesFile = new File(filePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			edgeDevicesDoc = dBuilder.parse(devicesFile);
			edgeDevicesDoc.getDocumentElement().normalize();

			NodeList datacenterList = edgeDevicesDoc.getElementsByTagName("datacenter");
			for (int i = 0; i < datacenterList.getLength(); i++) {
				NUM_OF_EDGE_DATACENTERS++;
				Node datacenterNode = datacenterList.item(i);

				Element datacenterElement = (Element) datacenterNode;

				Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
				isElementPresent(location, "wlan_id");
				isElementPresent(location, "x_pos");
				isElementPresent(location, "y_pos");

				NodeList hostList = datacenterElement.getElementsByTagName("host");
				for (int j = 0; j < hostList.getLength(); j++) {
					NUM_OF_EDGE_HOSTS++;
					Node hostNode = hostList.item(j);

					Element hostElement = (Element) hostNode;
					isElementPresent(hostElement, "storage");
					isElementPresent(hostElement, "readRate");

					//Oleg: removed VM feature and set only 1 to exist
					NUM_OF_EDGE_VMS=NUM_OF_EDGE_HOSTS;
				}
			}
	
		} catch (Exception e) {
			SimLogger.printLine("Edge Devices XML cannot be parsed! Terminating simulation...");
			e.printStackTrace();
			System.exit(0);
		}
	}
}
