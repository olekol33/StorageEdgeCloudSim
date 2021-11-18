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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import edu.boun.edgecloudsim.storage.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.boun.edgecloudsim.utils.SimLogger;

//changed by Harel

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

	//delimiter for output file.
	public static final String DELIMITER = ";";
	
    private double SIMULATION_TIME; //minutes unit in properties file
    private double WARM_UP_PERIOD; //minutes unit in properties file
    private double INTERVAL_TO_GET_VM_LOAD_LOG; //minutes unit in properties file
    private double INTERVAL_TO_GET_VM_LOCATION_LOG; //minutes unit in properties file
    private boolean FILE_LOG_ENABLED; //boolean to check file logging option
    private boolean DEEP_FILE_LOG_ENABLED; //boolean to check deep file logging option
	private boolean STORAGE_LOG_ENABLED;
	private boolean CLEAN_OUTPUT_FOLDER_PER_CONFIGURATION; //boolean to check deep file logging option
	private boolean COUNT_FAILEDDUETOINACCESSIBILITY;
	private boolean OVERHEAD_SCAN;

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

	private String[] FAIL_SCENARIOS;

    // mean waiting time (minute) is stored for each place types
    private double[] mobilityLookUpTable;

    //grid properties

	private int X_RANGE;
	private int Y_RANGE;
	private int HOST_RADIUS;

	private String OBJECT_DIST_READ;
	private String OBJECT_DIST_PLACE;
	private int CONGESTED_THRESHOLD;
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

	//Host failure
	private boolean HOST_FAILURE_SCENARIO;
	private boolean DYNAMIC_FAILURE;
	private String HOST_FAILURE_ID;
	private double HOST_FAILURE_TIME;

	private boolean VARIABILITY_RUN;
	private boolean MMPP;
	private int VARIABILITY_ITERATIONS;


	//ORBIT
	private boolean ORBIT_MODE;
	private boolean SIMULATE_ORBIT_MODE;
	private boolean PARAM_SCAN_MODE;
	private int NUMBER_OF_EDGE_NODES;

	//Input properties
	private boolean GPS_COORDINATES_CONVERSION;
	private boolean EXTERNAL_NODES_INPUT;
	private boolean EXTERNAL_DEVICES_INPUT;
	private boolean EXTERNAL_OBJECTS_INPUT;
	private boolean EXTERNAL_REQUESTS_INPUT;
	private boolean TEST_USING_INT;
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
	private Vector<StorageDevice> devicesVector;
	private Vector<StorageObject> objectsVector;
	private Vector<StorageRequest> storageRequests;
	private int minXpos;
	private int minYpos;
	private int xRange;
	private int yRange;
	private int numOfExternalTasks;

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

	public double getMinXpos() {
		return minXpos;
	}

	public double getMinYpos() {
		return minYpos;
	}

	public int getxRange() {
		return xRange;
	}

	public double getyRange() {
		return yRange;
	}

	public int getNumOfExternalTasks() {
		return numOfExternalTasks;
	}

	public Vector<StorageRequest> getStorageRequests() {
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
		NUM_OF_PLACE_TYPES = 0;
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
	public boolean initialize(String propertiesFile, String edgeDevicesFile, String applicationsFile){
		boolean result = false;
		InputStream input = null;
		try {
			input = new FileInputStream(propertiesFile);

			// load a properties file
			Properties prop = new Properties();
			prop.load(input);
			try {
				SIMULATION_TIME = (double)60 * Double.parseDouble(prop.getProperty("simulation_time")); //seconds
				WARM_UP_PERIOD = (double)60 * Double.parseDouble(prop.getProperty("warm_up_period")); //seconds
				INTERVAL_TO_GET_VM_LOAD_LOG = (double)60 * Double.parseDouble(prop.getProperty("vm_load_check_interval")); //seconds
				INTERVAL_TO_GET_VM_LOCATION_LOG = (double)60 * Double.parseDouble(prop.getProperty("vm_location_check_interval")); //seconds
				FILE_LOG_ENABLED = toBoolean(prop.getProperty("file_log_enabled"));
				DEEP_FILE_LOG_ENABLED = toBoolean(prop.getProperty("deep_file_log_enabled"));
				STORAGE_LOG_ENABLED = toBoolean(prop.getProperty("storage_log_enabled"));
				CLEAN_OUTPUT_FOLDER_PER_CONFIGURATION = toBoolean(prop.getProperty("terminate_failed_run"));
				COUNT_FAILEDDUETOINACCESSIBILITY = toBoolean(prop.getProperty("count_failedDueToInaccessibility"));
				OVERHEAD_SCAN = toBoolean(prop.getProperty("overhead_scan"));

				MIN_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("min_number_of_mobile_devices"));
				MAX_NUM_OF_MOBILE_DEVICES = Integer.parseInt(prop.getProperty("max_number_of_mobile_devices"));
				MOBILE_DEVICE_COUNTER_SIZE = Integer.parseInt(prop.getProperty("mobile_device_counter_size"));

				WAN_PROPOGATION_DELAY = Double.parseDouble(prop.getProperty("wan_propogation_delay"));
				LAN_INTERNAL_DELAY = Double.parseDouble(prop.getProperty("lan_internal_delay"));
				BANDWITH_WLAN = 1000 * Integer.parseInt(prop.getProperty("wlan_bandwidth"));
				BANDWITH_WAN = 1000 * Integer.parseInt(prop.getProperty("wan_bandwidth"));
//				BANDWITH_GSM =  1000 * Integer.parseInt(prop.getProperty("gsm_bandwidth"));

				NUM_OF_HOST_ON_CLOUD_DATACENTER = Integer.parseInt(prop.getProperty("number_of_host_on_cloud_datacenter"));
				NUM_OF_VM_ON_CLOUD_HOST = Integer.parseInt(prop.getProperty("number_of_vm_on_cloud_host"));
				CORE_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("core_for_cloud_vm"));
				MIPS_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("mips_for_cloud_vm"));
				RAM_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("ram_for_cloud_vm"));
				STORAGE_FOR_CLOUD_VM = Integer.parseInt(prop.getProperty("storage_for_cloud_vm"));

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
				MMPP = toBoolean(prop.getProperty("mmpp"));
				VARIABILITY_ITERATIONS = Integer.parseInt(prop.getProperty("variability_iterations"));
				NUMBER_OF_EDGE_NODES = Integer.parseInt(prop.getProperty("number_of_edge_nodes"));

				GPS_COORDINATES_CONVERSION = Boolean.parseBoolean(prop.getProperty("gps_coordinates_conversion"));
				EXTERNAL_NODES_INPUT = Boolean.parseBoolean(prop.getProperty("external_nodes_input"));
				EXTERNAL_DEVICES_INPUT = Boolean.parseBoolean(prop.getProperty("external_devices_input"));
				EXTERNAL_OBJECTS_INPUT = Boolean.parseBoolean(prop.getProperty("external_objects_input"));
				EXTERNAL_REQUESTS_INPUT = Boolean.parseBoolean(prop.getProperty("external_requests_input"));
				TEST_USING_INT = Boolean.parseBoolean(prop.getProperty("test_using_int"));
				NODES_DIRECT_PATH = prop.getProperty("nodes_direct_path");
				DEVICES_DIRECT_PATH = prop.getProperty("devices_direct_path");
				OBJECTS_DIRECT_PATH = prop.getProperty("objects_direct_path");
				REQUESTS_DIRECT_PATH = prop.getProperty("requests_direct_path");

				PARAM_SCAN_MODE = false;


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
				OBJECT_DIST_PLACE = prop.getProperty("object_dist_place");
				CONGESTED_THRESHOLD = Integer.parseInt(prop.getProperty("congested_threshold"));
				PARITY_PROB_STEP = Double.parseDouble(prop.getProperty("parityProbStep"));
//				MAX_CLOUD_REQUESTS = Integer.parseInt(prop.getProperty("max_cloud_requests"));
				RANDOM_SEED = Integer.parseInt(prop.getProperty("random_seed"));
				ZIPF_EXPONENT = Double.parseDouble(prop.getProperty("zipf_exponent"));
				NUM_OF_DATA_OBJECTS = Integer.parseInt(prop.getProperty("num_of_data_objects"));
				NUM_OF_STRIPES = Integer.parseInt(prop.getProperty("num_of_stripes"));
				NUM_OF_DATA_IN_STRIPE = Integer.parseInt(prop.getProperty("num_of_data_in_stripe"));
				NUM_OF_PARITY_IN_STRIPE = Integer.parseInt(prop.getProperty("num_of_parity_in_stripe"));


			}
			catch (Exception e){
				System.out.println("ERROR: Missing config");
				e.printStackTrace();
				System.exit(1);
			}


			double place1_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L1_mean_waiting_time"));
			double place2_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L2_mean_waiting_time"));
			double place3_mean_waiting_time = Double.parseDouble(prop.getProperty("attractiveness_L3_mean_waiting_time"));
			//mean waiting time (minute)
			mobilityLookUpTable = new double[]{
				place1_mean_waiting_time, //ATTRACTIVENESS_L1
				place2_mean_waiting_time, //ATTRACTIVENESS_L2
				place3_mean_waiting_time  //ATTRACTIVENESS_L3
		    };
			

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

		//checks if we are in external nodes mode
		if(SimSettings.instance.isExternalNodes()){
			try{
				ParseStorageNodes nodeParser = new ParseStorageNodes();
				nodesHashVector = nodeParser.prepareNodesHashVector(getPathOfNodesFile());
				minXpos = nodeParser.getxMin();
				minYpos = nodeParser.getyMin();
				xRange = nodeParser.getxRange();
				yRange = nodeParser.getyRange();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		parseEdgeDevicesXML(edgeDevicesFile);

		//checks if we are in external devices mode
		//update the min/max number of mobile devices accordingly
		if(SimSettings.instance.isExternalDevices()){
			try{
				ParseStorageDevices deviceParser = new ParseStorageDevices();
				devicesHashVector = deviceParser.prepareDevicesVector(getPathOfDevicesFile());
				reversedHashDevicesVector = reverseHashDevices(devicesHashVector);
				devicesVector = deviceParser.getDevicesVector();
				MIN_NUM_OF_MOBILE_DEVICES = devicesHashVector.size();
				MAX_NUM_OF_MOBILE_DEVICES = devicesHashVector.size();
			}catch (Exception e){
				e.printStackTrace();
			}
		}//proceed

		//checks if we are in external objects mode
		//updates the number of objects accordingly
		if(SimSettings.getInstance().isExternalObjects()){
			ParseStorageObject objectParser = new ParseStorageObject();
			objectsHashVector = objectParser.prepareObjectsHashVector(nodesHashVector, getPathOfObjectsFile());
			reversedHashVector = reverseHash(objectsHashVector);
			NUM_OF_DATA_OBJECTS = objectsHashVector.size();
			objectsVector = objectParser.getObjectsVector();
		}

		if(SimSettings.getInstance().isExternalRequests()){
			ParseStorageRequests requestsParser = new ParseStorageRequests();
			storageRequests = requestsParser.prepareRequestsVector(devicesHashVector, objectsHashVector,getPathOfRequestsFile());
			numOfExternalTasks = storageRequests.size();
		}
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

	/**
	 * returns warm up period (in seconds unit) from properties file
	 */
	public double getWarmUpPeriod()
	{
		return WARM_UP_PERIOD; 
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
	public int getNumOfCoudHost()
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
		return CLEAN_OUTPUT_FOLDER_PER_CONFIGURATION;
	}


	public boolean isCountFailedduetoinaccessibility() {
		return COUNT_FAILEDDUETOINACCESSIBILITY;
	}

	public boolean isOverheadScan() {
		return OVERHEAD_SCAN;
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

	public boolean isExternalNodes(){
		return EXTERNAL_NODES_INPUT;
	}

	public boolean isExternalDevices(){
		return EXTERNAL_DEVICES_INPUT;
	}

	public boolean isExternalObjects(){
		return EXTERNAL_OBJECTS_INPUT;
	}

	public boolean isExternalRequests(){
		return EXTERNAL_REQUESTS_INPUT;
	}

	public boolean isItIntTest(){
		return TEST_USING_INT;
	}

	public String getPathOfNodesFile(){
		return NODES_DIRECT_PATH;
	}

	public String getPathOfDevicesFile(){
		return DEVICES_DIRECT_PATH;
	}

	public String getPathOfObjectsFile(){
		return OBJECTS_DIRECT_PATH;
	}

	public String getPathOfRequestsFile(){
		return REQUESTS_DIRECT_PATH;
	}

	public boolean isVariabilityRun() {
		return VARIABILITY_RUN;
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

	public int getNumberOfEdgeNodes() {
		return NUMBER_OF_EDGE_NODES;
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

	public String getObjectDistRead() {
		return OBJECT_DIST_READ;
	}
	public String getObjectDistPlace() {
		return OBJECT_DIST_PLACE;
	}

	public void setObjectDistRead(String OBJECT_DIST_READ) {
		this.OBJECT_DIST_READ = OBJECT_DIST_READ;
	}

	public int getCongestedThreshold() {
		return CONGESTED_THRESHOLD;
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

	public int getNumOfDataObjects() {
		return NUM_OF_DATA_OBJECTS;
	}

	public void setNumOfDataObjects(int NUM_OF_DATA_OBJECTS) {
		this.NUM_OF_DATA_OBJECTS = NUM_OF_DATA_OBJECTS;
	}


	public void setNumOfStripes(int NUM_OF_STRIPES) {
		this.NUM_OF_STRIPES = NUM_OF_STRIPES;
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
//	        if (value.isEmpty() || value == null){
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
				isElementPresent(appElement, "prob_cloud_selection");
				isElementPresent(appElement, "poisson_interarrival");
				isElementPresent(appElement, "active_period");
				isElementPresent(appElement, "idle_period");
				isElementPresent(appElement, "data_upload");
				isElementPresent(appElement, "data_download");
				isElementPresent(appElement, "task_length");
				isElementPresent(appElement, "required_core");
				isElementPresent(appElement, "vm_utilization_on_edge");
				isElementPresent(appElement, "vm_utilization_on_cloud");
				isElementPresent(appElement, "vm_utilization_on_mobile");
				isElementPresent(appElement, "delay_sensitivity");

				String taskName = appElement.getAttribute("name");
				taskNames[i] = taskName;
				
				double usage_percentage = Double.parseDouble(appElement.getElementsByTagName("usage_percentage").item(0).getTextContent());
				double prob_cloud_selection = Double.parseDouble(appElement.getElementsByTagName("prob_cloud_selection").item(0).getTextContent());
				double poisson_interarrival = Double.parseDouble(appElement.getElementsByTagName("poisson_interarrival").item(0).getTextContent());
				double active_period = Double.parseDouble(appElement.getElementsByTagName("active_period").item(0).getTextContent());
				double idle_period = Double.parseDouble(appElement.getElementsByTagName("idle_period").item(0).getTextContent());
				double data_upload = Double.parseDouble(appElement.getElementsByTagName("data_upload").item(0).getTextContent());
				double data_download = Double.parseDouble(appElement.getElementsByTagName("data_download").item(0).getTextContent());
				double task_length = Double.parseDouble(appElement.getElementsByTagName("task_length").item(0).getTextContent());
				double required_core = Double.parseDouble(appElement.getElementsByTagName("required_core").item(0).getTextContent());
				double vm_utilization_on_edge = Double.parseDouble(appElement.getElementsByTagName("vm_utilization_on_edge").item(0).getTextContent());
				double vm_utilization_on_cloud = Double.parseDouble(appElement.getElementsByTagName("vm_utilization_on_cloud").item(0).getTextContent());
				double vm_utilization_on_mobile = Double.parseDouble(appElement.getElementsByTagName("vm_utilization_on_mobile").item(0).getTextContent());
				double delay_sensitivity = Double.parseDouble(appElement.getElementsByTagName("delay_sensitivity").item(0).getTextContent());
				//Oleg
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

			    taskLookUpTable[i][0] = usage_percentage; //usage percentage [0-100]
			    taskLookUpTable[i][1] = prob_cloud_selection; //prob. of selecting cloud [0-100]
			    taskLookUpTable[i][2] = poisson_interarrival; //poisson mean (sec)
			    taskLookUpTable[i][3] = active_period; //active period (sec)
			    taskLookUpTable[i][4] = idle_period; //idle period (sec)
			    taskLookUpTable[i][5] = data_upload; //avg data upload (KB)
			    taskLookUpTable[i][6] = data_download; //avg data download (KB)
			    taskLookUpTable[i][7] = task_length; //avg task length (MI)
			    taskLookUpTable[i][8] = required_core; //required # of core
			    taskLookUpTable[i][9] = vm_utilization_on_edge; //vm utilization on edge vm [0-100]
			    taskLookUpTable[i][10] = vm_utilization_on_cloud; //vm utilization on cloud vm [0-100]
			    taskLookUpTable[i][11] = vm_utilization_on_mobile; //vm utilization on mobile vm [0-100]
			    taskLookUpTable[i][12] = delay_sensitivity; //delay_sensitivity [0-1]

			}
	
		} catch (Exception e) {
			SimLogger.printLine("Edge Devices XML cannot be parsed! Terminating simulation...");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void parseEdgeDevicesXML(String filePath)
	{
		try {	
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
				isAttribtuePresent(datacenterElement, "arch");
				isAttribtuePresent(datacenterElement, "os");
				isAttribtuePresent(datacenterElement, "vmm");
				isElementPresent(datacenterElement, "costPerBw");
				isElementPresent(datacenterElement, "costPerSec");
				isElementPresent(datacenterElement, "costPerMem");
				isElementPresent(datacenterElement, "costPerStorage");

				Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
				isElementPresent(location, "attractiveness");
				isElementPresent(location, "wlan_id");
				isElementPresent(location, "x_pos");
				isElementPresent(location, "y_pos");
				
				String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
				int placeTypeIndex = Integer.parseInt(attractiveness);
				if(NUM_OF_PLACE_TYPES < placeTypeIndex+1)
					NUM_OF_PLACE_TYPES = placeTypeIndex+1;

				NodeList hostList = datacenterElement.getElementsByTagName("host");
				for (int j = 0; j < hostList.getLength(); j++) {
				    NUM_OF_EDGE_HOSTS++;
					Node hostNode = hostList.item(j);
					
					Element hostElement = (Element) hostNode;
					isElementPresent(hostElement, "core");
					isElementPresent(hostElement, "mips");
					isElementPresent(hostElement, "ram");
					isElementPresent(hostElement, "storage");
					isElementPresent(hostElement, "readRate");

					NodeList vmList = hostElement.getElementsByTagName("VM");
					for (int k = 0; k < vmList.getLength(); k++) {
					    NUM_OF_EDGE_VMS++;
						Node vmNode = vmList.item(k);
						
						Element vmElement = (Element) vmNode;
						isAttribtuePresent(vmElement, "vmm");
						isElementPresent(vmElement, "core");
						isElementPresent(vmElement, "mips");
						isElementPresent(vmElement, "ram");
						isElementPresent(vmElement, "storage");
						isElementPresent(vmElement, "readRate");
					}
				}
			}
	
		} catch (Exception e) {
			SimLogger.printLine("Edge Devices XML cannot be parsed! Terminating simulation...");
			e.printStackTrace();
			System.exit(0);
		}
	}
}
