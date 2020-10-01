/*
 * Title:        EdgeCloudSim - Simulation Logger
 * 
 * Description: 
 * SimLogger is responsible for storing simulation events/results
 * in to the files in a specific format.
 * Format is decided in a way to use results in matlab efficiently.
 * If you need more results or another file format, you should modify
 * this class.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.edge_server.EdgeHost;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.utils.SimLogger.NETWORK_ERRORS;

public class SimLogger {
	public static enum TASK_STATUS {
		CREATED, UPLOADING, PROCESSING, DOWNLOADING, COMPLETED, REJECTED_DUE_TO_VM_CAPACITY, REJECTED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_MOBILITY,
		REJECTED_DUE_TO_QUEUE, REJECTED_DUE_TO_POLICY, FAILED_DUE_TO_INACCESSIBILITY  //storage
	}
	
	public static enum NETWORK_ERRORS {
		LAN_ERROR, MAN_ERROR, WAN_ERROR, NONE
	}

	private static boolean fileLogEnabled;
	private static boolean printLogEnabled;
	private String filePrefix;
	private String outputFolder;
	private Map<Integer, LogItem> taskMap;
	private LinkedList<VmLoadLogItem> vmLoadList;
	//TRUE if generate graphs in Matlab, FALSE if python
	Boolean MATLAB = Boolean.FALSE;

	private static SimLogger singleton = new SimLogger();

	/*
	 * A private Constructor prevents any other class from instantiating.
	 */
	private SimLogger() {
		fileLogEnabled = false;
		printLogEnabled = false;
	}

	/* Static 'instance' method */
	public static SimLogger getInstance() {
		return singleton;
	}

	public static void enableFileLog() {
		fileLogEnabled = true;
	}

	public static void enablePrintLog() {
		printLogEnabled = true;
	}

	public static boolean isFileLogEnabled() {
		return fileLogEnabled;
	}

	public static void disablePrintLog() {
		printLogEnabled = false;
	}

	public String getFilePrefix() {
		return filePrefix;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void appendToFile(BufferedWriter bw, String line) throws IOException {
		bw.write(line);
		bw.newLine();
	}

	public static void printLine(String msg) {
		if (printLogEnabled)
			System.out.println(msg);
	}

	public static void print(String msg) {
		if (printLogEnabled)
			System.out.print(msg);
	}

	public void simStarted(String outFolder, String fileName) {
		filePrefix = fileName;
		outputFolder = outFolder;
		taskMap = new HashMap<Integer, LogItem>();
		vmLoadList = new LinkedList<VmLoadLogItem>();
	}

	public void addLog(int taskId, int taskType, int taskLenght, int taskInputType,
			int taskOutputSize) {
		// printLine(taskId+"->"+taskStartTime);
		taskMap.put(taskId, new LogItem(taskType, taskLenght, taskInputType, taskOutputSize));
	}
	//Storage
	public void addLog(int taskId, int taskType, int taskLenght, int taskInputType,
					   int taskOutputSize, String stripeID, String objectID, int ioTaskID, int isParity, int paritiesToRead, int accessHostID) {
		taskMap.put(taskId, new LogItem(taskType, taskLenght, taskInputType, taskOutputSize,stripeID,objectID, ioTaskID, isParity, paritiesToRead,accessHostID));
	}
	public void setObjectRead(int taskId, String objectRead) {
		taskMap.get(taskId).setObjectRead(objectRead);
	}

	public void setHostId(int taskId, int hostId) {
		taskMap.get(taskId).setHostId(hostId);
	}

	public void setAccessHostID(int taskId, int accessHostID) {
		taskMap.get(taskId).setAccessHostID(accessHostID);
	}

	public void taskStarted(int taskId, double time) {
		taskMap.get(taskId).taskStarted(time);
	}

	public void setUploadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setUploadDelay(delay, delayType);
	}

	public void setDownloadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).setDownloadDelay(delay, delayType);
	}
	
	public void taskAssigned(int taskId, int datacenterId, int hostId, int vmId, int vmType) {
		taskMap.get(taskId).taskAssigned(datacenterId, hostId, vmId, vmType);
	}

	public void taskExecuted(int taskId) {
		taskMap.get(taskId).taskExecuted();
	}

	public void taskEnded(int taskId, double time) {
		taskMap.get(taskId).taskEnded(time);
	}

	public void rejectedDueToVMCapacity(int taskId, double time, int vmType) {
		taskMap.get(taskId).taskRejectedDueToVMCapacity(time, vmType);
	}

	public void rejectedDueToBandwidth(int taskId, double time, int vmType, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskRejectedDueToBandwidth(time, vmType, delayType);
	}

	public void failedDueToBandwidth(int taskId, double time, NETWORK_DELAY_TYPES delayType) {
		taskMap.get(taskId).taskFailedDueToBandwidth(time, delayType);
	}

	public void failedDueToMobility(int taskId, double time) {
		taskMap.get(taskId).taskFailedDueToMobility(time);
	}

	public void taskRejectedDueToPolicy(int taskId, double time, int vmType) {
		taskMap.get(taskId).taskRejectedDueToPolicy(time, vmType);
	}

	public void taskRejectedDueToQueue(int taskId, double time) {
		taskMap.get(taskId).taskRejectedDueToQueue(time);
	}

	public void taskFailedDueToInaccessibility(int taskId, double time, int vmType) {
		taskMap.get(taskId).taskFailedDueToInaccessibility(time, vmType);
	}

	public void addVmUtilizationLog(double time, double loadOnEdge, double loadOnCloud, double loadOnMobile) {
		vmLoadList.add(new VmLoadLogItem(time, loadOnEdge, loadOnCloud, loadOnMobile));
	}

	private void logObjectPopularity() throws IOException {
		File objectFile = null, objectDistFile = null;
		FileWriter objectFW = null, objectDistFW = null;
		BufferedWriter objectBW = null, objectDistBW = null;


		objectFile = new File(outputFolder, filePrefix + "_OBJECTS_IN_HOSTS.log");
		objectFW = new FileWriter(objectFile, true);
		objectBW = new BufferedWriter(objectFW);

		objectDistFile = new File(outputFolder, filePrefix + "_OBJECT_DISTRIBUTION.log");
		objectDistFW = new FileWriter(objectDistFile, true);
		objectDistBW = new BufferedWriter(objectDistFW);

		appendToFile(objectBW, "host;numOfObjects;dataObjects;meanData;medianData;parityObjects;meanParity;medianParity");
		appendToFile(objectDistBW, "Object Name;Object Type;Occurrences");

		ObjectGenerator OG = RedisListHandler.getOG();
/*		HashMap<Integer, HashMap<String, Object>> dd = OG.getObjectsInHosts().entrySet();
		for (Map<Integer, HashMap<String, Object>> host : OG.getObjectsInHosts().entrySet())
			jedis.hmset("object:"+KV.get("id"),KV);*/
		for (Map.Entry<Integer, HashMap<String, Object>> entry : OG.getObjectsInHosts().entrySet()) {
			String objects = (String)entry.getValue().get("objects");
			StringTokenizer st= new StringTokenizer(objects, " "); // Space as delimiter
			Set<String> objectsSet = new HashSet<String>();
			while (st.hasMoreTokens())
				objectsSet.add(st.nextToken());
			List<Integer> dataObjectPriorities = new ArrayList<Integer>();
			List<Integer> parityObjectPriorities = new ArrayList<Integer>();
			List<List<Map>> listOfStripes =  OG.getListOfStripes();
//			locationsSet.add(Integer.toString(currentHost));
			for (String object : objectsSet){
					if (object.startsWith("d")) {
						String objectID = object.replaceAll("[^\\d.]", "");
						dataObjectPriorities.add(Integer.valueOf(objectID));
					}
					else if (object.startsWith("p")){
						int i=0;
						for (List<Map> stripe : listOfStripes){
							//assumes one parity
							//search each stripe for the parity
							if (stripe.get(SimSettings.getInstance().getNumOfDataInStripe()).get("id").equals(object)) {
								parityObjectPriorities.add(i);
								break;
							}
							else
								i++;
						}
					}
			}
			Collections.sort(dataObjectPriorities);
			Collections.sort(parityObjectPriorities);
			IntSummaryStatistics dataStats = dataObjectPriorities.stream()
					.mapToInt((x) -> x)
					.summaryStatistics();
			int dataMiddle = (dataObjectPriorities.get(dataObjectPriorities.size()/2) +dataObjectPriorities.get((dataObjectPriorities.size()/2)-1) )/2;
			if (parityObjectPriorities.size()==0) {
				appendToFile(objectBW, Integer.toString(entry.getKey())+SimSettings.DELIMITER +Integer.toString(objectsSet.size())+
						SimSettings.DELIMITER + Integer.toString(dataObjectPriorities.size()) + SimSettings.DELIMITER +
						Double.toString(dataStats.getAverage()) + SimSettings.DELIMITER+Integer.toString(dataMiddle) +SimSettings.DELIMITER+
						Integer.toString(parityObjectPriorities.size()) + SimSettings.DELIMITER + "" +
						SimSettings.DELIMITER + "");
			}
			else if (parityObjectPriorities.size()==1) {
				appendToFile(objectBW, Integer.toString(entry.getKey())+SimSettings.DELIMITER +Integer.toString(objectsSet.size())+
						SimSettings.DELIMITER + Integer.toString(dataObjectPriorities.size()) + SimSettings.DELIMITER +
						Double.toString(dataStats.getAverage()) + SimSettings.DELIMITER+Integer.toString(dataMiddle) +SimSettings.DELIMITER+
						Integer.toString(parityObjectPriorities.size()) + SimSettings.DELIMITER + parityObjectPriorities.get(0) +
						SimSettings.DELIMITER + parityObjectPriorities.get(0));
			}
			else {
				IntSummaryStatistics parityStats = parityObjectPriorities.stream()
						.mapToInt((x) -> x)
						.summaryStatistics();
				int parityMiddle = (parityObjectPriorities.get(parityObjectPriorities.size() / 2) + parityObjectPriorities.get((parityObjectPriorities.size() / 2) - 1)) / 2;
				appendToFile(objectBW, Integer.toString(entry.getKey()) + SimSettings.DELIMITER + Integer.toString(objectsSet.size()) +
						SimSettings.DELIMITER + Integer.toString(dataObjectPriorities.size()) + SimSettings.DELIMITER +
						Double.toString(dataStats.getAverage()) + SimSettings.DELIMITER + Integer.toString(dataMiddle) + SimSettings.DELIMITER +
						Integer.toString(parityObjectPriorities.size()) + SimSettings.DELIMITER + Double.toString(parityStats.getAverage()) +
						SimSettings.DELIMITER + Integer.toString(parityMiddle));
			}
		}
		List<Map> listOfObjects = new ArrayList<Map>(OG.getDataObjects());
		listOfObjects.addAll(OG.getParityObjects());
		for (Map<String,String> KV : listOfObjects) {

			String locations = (String)KV.get("locations");
			StringTokenizer st= new StringTokenizer(locations, " "); // Space as delimiter
			Set<String> locationsSet = new HashSet<String>();
			while (st.hasMoreTokens())
				locationsSet.add(st.nextToken());

			appendToFile(objectDistBW, KV.get("id") + SimSettings.DELIMITER + KV.get("type") + SimSettings.DELIMITER+ locationsSet.size());
		}
		objectBW.close();
		objectDistBW.close();
	}

	public void simStopped() throws IOException {
		int numOfAppTypes = SimSettings.getInstance().getTaskLookUpTable().length;

		File successFile = null, failFile = null, vmLoadFile = null, locationFile = null, gridLocationFile = null, objectsFile = null, readObjectsFile = null;
		FileWriter successFW = null, failFW = null, vmLoadFW = null, locationFW = null, gridLocationFW = null, objectsFW = null, readObjectsFW = null;
		BufferedWriter successBW = null, failBW = null, vmLoadBW = null, locationBW = null, gridLocationBW = null, objectsBW = null, readObjectsBW = null;;

		logObjectPopularity();

		// Save generic results to file for each app type. last index is average
		// of all app types
		File[] genericFiles = new File[numOfAppTypes + 1];
		FileWriter[] genericFWs = new FileWriter[numOfAppTypes + 1];
		BufferedWriter[] genericBWs = new BufferedWriter[numOfAppTypes + 1];

		// extract following values for each app type. last index is average of
		// all app types
		int[] uncompletedTask = new int[numOfAppTypes + 1];
		int[] uncompletedTaskOnCloud = new int[numOfAppTypes + 1];
		int[] uncompletedTaskOnEdge = new int[numOfAppTypes + 1];
		int[] uncompletedTaskOnMobile = new int[numOfAppTypes + 1];

		int[] completedTask = new int[numOfAppTypes + 1];
		int[] completedTaskOnCloud = new int[numOfAppTypes + 1];
		int[] completedTaskOnEdge = new int[numOfAppTypes + 1];
		int[] completedTaskOnMobile = new int[numOfAppTypes + 1];

		int[] failedTask = new int[numOfAppTypes + 1];
		int[] failedTaskOnCloud = new int[numOfAppTypes + 1];
		int[] failedTaskOnEdge = new int[numOfAppTypes + 1];
		int[] failedTaskOnMobile = new int[numOfAppTypes + 1];

		double[] networkDelay = new double[numOfAppTypes + 1];
		double[] wanDelay = new double[numOfAppTypes + 1];
		double[] manDelay = new double[numOfAppTypes + 1];
		double[] lanDelay = new double[numOfAppTypes + 1];
		
		double[] wanUsage = new double[numOfAppTypes + 1];
		double[] manUsage = new double[numOfAppTypes + 1];
		double[] lanUsage = new double[numOfAppTypes + 1];

		double[] serviceTime = new double[numOfAppTypes + 1];
		double[] serviceTimeOnCloud = new double[numOfAppTypes + 1];
		double[] serviceTimeOnEdge = new double[numOfAppTypes + 1];
		double[] serviceTimeOnMobile = new double[numOfAppTypes + 1];

		double[] processingTime = new double[numOfAppTypes + 1];
		double[] processingTimeOnCloud = new double[numOfAppTypes + 1];
		double[] processingTimeOnEdge = new double[numOfAppTypes + 1];
		double[] processingTimeOnMobile = new double[numOfAppTypes + 1];

		int[] failedTaskDueToVmCapacity = new int[numOfAppTypes + 1];
		int[] failedTaskDueToVmCapacityOnCloud = new int[numOfAppTypes + 1];
		int[] failedTaskDueToVmCapacityOnEdge = new int[numOfAppTypes + 1];
		int[] failedTaskDueToVmCapacityOnMobile = new int[numOfAppTypes + 1];
		
		double[] cost = new double[numOfAppTypes + 1];
		int[] failedTaskDuetoBw = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoLanBw = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoManBw = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoWanBw = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoMobility = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoPolicy = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoQueue = new int[numOfAppTypes + 1];
		int[] failedTaskDuetoInaccessibility = new int[numOfAppTypes + 1];

		//Oleg: For object log
//		System.out.println("array size: " + taskMap.size());
		int numOfObjectsInStripe;
		if (SimManager.getInstance().getObjectPlacementPolicy().equalsIgnoreCase("REPLICATION_PLACE"))
			numOfObjectsInStripe = SimSettings.getInstance().getNumOfDataInStripe();
		else
			numOfObjectsInStripe = SimSettings.getInstance().getNumOfDataInStripe()+
				SimSettings.getInstance().getNumOfParityInStripe();
		String [] objectID = new String[taskMap.size()];
		int [] hostID = new int[taskMap.size()];
		int [] ioTaskID = new int[taskMap.size()];
		int [] isParity = new int[taskMap.size()];
		int [] accessID = new int[taskMap.size()];
		double [] objectReadDelay = new double[taskMap.size()];
		String [] readSource = new String[taskMap.size()];
		TASK_STATUS [] objectStatusID = new TASK_STATUS[taskMap.size()];
		Arrays.fill(hostID,-1);
		Arrays.fill(ioTaskID,-1);
		Arrays.fill(isParity,-1);
		Arrays.fill(accessID,-1);

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(4);

//		double[] timeToReadStripe = new double[IdleActiveStorageLoadGenerator.getNumOfIOTasks()];
//		int[] objectsReadFromStripe = new int[IdleActiveStorageLoadGenerator.getNumOfIOTasks()];
//		Arrays.fill(objectsReadFromStripe,0);
		Map<Integer, ArrayList<Double>> timeToReadStripe = new HashMap<Integer, ArrayList<Double>>();
		Map<Integer, String> nameToReadStripe = new HashMap<Integer, String>();
		final double NOT_FINISHED = -1;
		final double REJECTED = -2;

		// open all files and prepare them for write
		if (fileLogEnabled) {
			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				successFile = new File(outputFolder, filePrefix + "_SUCCESS.log");
				successFW = new FileWriter(successFile, true);
				successBW = new BufferedWriter(successFW);

				failFile = new File(outputFolder, filePrefix + "_FAIL.log");
				failFW = new FileWriter(failFile, true);
				failBW = new BufferedWriter(failFW);
			}

/*			vmLoadFile = new File(outputFolder, filePrefix + "_VM_LOAD.log");
			vmLoadFW = new FileWriter(vmLoadFile, true);
			vmLoadBW = new BufferedWriter(vmLoadFW);*/

			locationFile = new File(outputFolder, filePrefix + "_LOCATION.log");
			locationFW = new FileWriter(locationFile, true);
			locationBW = new BufferedWriter(locationFW);

			gridLocationFile = new File(outputFolder, filePrefix + "_GRID_LOCATION.log");
			gridLocationFW = new FileWriter(gridLocationFile, true);
			gridLocationBW = new BufferedWriter(gridLocationFW);
			
			//Objects log
			objectsFile = new File(outputFolder, filePrefix + "_OBJECTS.log");
			objectsFW = new FileWriter(objectsFile, true);
			objectsBW = new BufferedWriter(objectsFW);
//			appendToFile(objectsBW, "ObjectID;HostID;AccessID;ReadSrc;Read Delay;Status");
			appendToFile(objectsBW, "ioTaskID;ObjectID;isParity;HostID;AccessID;ReadSrc;Read Delay");

			//readObjects log
			readObjectsFile = new File(outputFolder, filePrefix + "_READOBJECTS.log");
			readObjectsFW = new FileWriter(readObjectsFile, true);
			readObjectsBW = new BufferedWriter(readObjectsFW);
			appendToFile(readObjectsBW, "ioID;latency;Read Cost;type;Objects Read");

			for (int i = 0; i < numOfAppTypes + 1; i++) {
				//print only summary for now
				if (i<numOfAppTypes)
					continue;
				String fileName = "ALL_APPS_GENERIC.log";

				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just
					// discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;

					fileName = SimSettings.getInstance().getTaskName(i) + "_GENERIC.log";
				}

				genericFiles[i] = new File(outputFolder, filePrefix + "_" + fileName);
				genericFWs[i] = new FileWriter(genericFiles[i], true);
				genericBWs[i] = new BufferedWriter(genericFWs[i]);
				if(MATLAB)
					appendToFile(genericBWs[i], "#auto generated file!");
			}

			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				appendToFile(successBW, "#auto generated file!");
				appendToFile(failBW, "#auto generated file!");
			}
/*			if(MATLAB) {
				appendToFile(vmLoadBW, "#auto generated file!");
				appendToFile(locationBW, "#auto generated file!");
			}
			else {
				appendToFile(vmLoadBW, "#time;loadOnEdge;loadOnCloud;loadOnMobile");
				appendToFile(locationBW, "#Time;Attractiveness 0;Attractiveness 1;Attractiveness 2");
			}*/
			appendToFile(gridLocationBW, "ItemType;ItemID;xPos;yPos");
		}

		//Oleg
//		int o=0;

		// extract the result of each task and write it to the file if required
		for (Map.Entry<Integer, LogItem> entry : taskMap.entrySet()) {
			Integer key = entry.getKey();
			LogItem value = entry.getValue();


			if (value.isInWarmUpPeriod())
			{
				ArrayList<Double> delays = new ArrayList<Double>(numOfObjectsInStripe);
				if (timeToReadStripe.get(value.getIoTaskID()) != null)
					delays = timeToReadStripe.get(value.getIoTaskID());
				else //placeholder
					delays.add(0, (double)100);
				if(value.getIsParity()==0)
					//put at location 0
					delays.set(0, REJECTED);
				else
					//or append
					delays.add(1, REJECTED);
				timeToReadStripe.put(value.getIoTaskID(),delays);
				continue;
			}


			if (value.getStatus() == SimLogger.TASK_STATUS.COMPLETED) {
				completedTask[value.getTaskType()]++;

				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					completedTaskOnCloud[value.getTaskType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					completedTaskOnMobile[value.getTaskType()]++;
				else
					completedTaskOnEdge[value.getTaskType()]++;

				//Oleg
				objectID[key-1] = value.getObjectRead();
				hostID[key-1] = value.getHostId();
				ioTaskID[key-1] = value.getIoTaskID();
				accessID[key-1] = value.getAccessHostID();
				isParity[key-1] = value.getIsParity();
				objectReadDelay[key-1] = value.getNetworkDelay();
				objectStatusID[key-1] = SimLogger.TASK_STATUS.COMPLETED;
				if (value.getDatacenterId()==SimSettings.CLOUD_DATACENTER_ID)
					readSource[key-1] = "Cloud";
				else if (value.getDatacenterId()==SimSettings.GENERIC_EDGE_DEVICE_ID)
					readSource[key-1] = "Edge";

				//KV pairs of IO tasks and list of latencies
				ArrayList<Double> delays = new ArrayList<Double>(numOfObjectsInStripe);
				if (timeToReadStripe.get(value.getIoTaskID()) != null)
					delays = timeToReadStripe.get(value.getIoTaskID());
				else //placeholder
					delays.add(0, (double)100);
				if(value.getIsParity()==0) {
					//put at location 0
					delays.set(0, value.getNetworkDelay());
				}
				else
					//or append
					delays.add(1,value.getNetworkDelay());
				timeToReadStripe.put(value.getIoTaskID(),delays);
			}
			else if(value.getStatus() == SimLogger.TASK_STATUS.CREATED ||
					value.getStatus() == SimLogger.TASK_STATUS.UPLOADING ||
					value.getStatus() == SimLogger.TASK_STATUS.PROCESSING ||
					value.getStatus() == SimLogger.TASK_STATUS.DOWNLOADING)
			{
				ArrayList<Double> delays = new ArrayList<Double>(numOfObjectsInStripe);
				if (timeToReadStripe.get(value.getIoTaskID()) != null)
					delays = timeToReadStripe.get(value.getIoTaskID());
				else //placeholder
					delays.add(0, (double)100);
				if(value.getIsParity()==0)
					//put at location 0
					delays.set(0, NOT_FINISHED);
				else
					//or append
					delays.add(1, NOT_FINISHED);
				timeToReadStripe.put(value.getIoTaskID(),delays);
//				nameToReadStripe.put(value.getIoTaskID(),value.getStripeID());


				uncompletedTask[value.getTaskType()]++;
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					uncompletedTaskOnCloud[value.getTaskType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					uncompletedTaskOnMobile[value.getTaskType()]++;
				else
					uncompletedTaskOnEdge[value.getTaskType()]++;
				//Oleg
				objectStatusID[key-1] = value.getStatus();
			}
			else {
				//rejected
				ArrayList<Double> delays = new ArrayList<Double>(numOfObjectsInStripe);
				if (timeToReadStripe.get(value.getIoTaskID()) != null)
					delays = timeToReadStripe.get(value.getIoTaskID());
				else //placeholder
					delays.add(0, (double)100);
				if(value.getIsParity()==0)
					//put at location 0
					delays.set(0, NOT_FINISHED);
				else
					//or append
					delays.add(1, NOT_FINISHED);
				timeToReadStripe.put(value.getIoTaskID(),delays);

				failedTask[value.getTaskType()]++;

				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					failedTaskOnCloud[value.getTaskType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					failedTaskOnMobile[value.getTaskType()]++;
				else
					failedTaskOnEdge[value.getTaskType()]++;
				//Oleg
				objectStatusID[key-1] = value.getStatus();
			}

			if (value.getStatus() == SimLogger.TASK_STATUS.COMPLETED) {
				cost[value.getTaskType()] += value.getCost();
				serviceTime[value.getTaskType()] += value.getServiceTime();
				networkDelay[value.getTaskType()] += value.getNetworkDelay();
				processingTime[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
				
				if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) != 0) {
					lanUsage[value.getTaskType()]++;
					lanDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY);
				}
				if(value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) != 0) {
					manUsage[value.getTaskType()]++;
					manDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY);
				}
				if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY) != 0) {
					wanUsage[value.getTaskType()]++;
					wanDelay[value.getTaskType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
				}

				//TODO: check why networkDelay not sum of delays
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal()) {
					serviceTimeOnCloud[value.getTaskType()] += value.getServiceTime();
					processingTimeOnCloud[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
				}
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal()) {
					serviceTimeOnMobile[value.getTaskType()] += value.getServiceTime();
					processingTimeOnMobile[value.getTaskType()] += value.getServiceTime();
				}
				else {
					serviceTimeOnEdge[value.getTaskType()] += value.getServiceTime();
					processingTimeOnEdge[value.getTaskType()] += (value.getServiceTime() - value.getNetworkDelay());
				}

				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(successBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY) {
				failedTaskDueToVmCapacity[value.getTaskType()]++;
				
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					failedTaskDueToVmCapacityOnCloud[value.getTaskType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					failedTaskDueToVmCapacityOnMobile[value.getTaskType()]++;
				else
					failedTaskDueToVmCapacityOnEdge[value.getTaskType()]++;
				
				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH
					|| value.getStatus() == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH) {
				failedTaskDuetoBw[value.getTaskType()]++;
				if (value.getNetworkError() == NETWORK_ERRORS.LAN_ERROR)
					failedTaskDuetoLanBw[value.getTaskType()]++;
				else if (value.getNetworkError() == NETWORK_ERRORS.MAN_ERROR)
					failedTaskDuetoManBw[value.getTaskType()]++;
				else if (value.getNetworkError() == NETWORK_ERRORS.WAN_ERROR)
					failedTaskDuetoWanBw[value.getTaskType()]++;

				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY) {
				failedTaskDuetoMobility[value.getTaskType()]++;
				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			}
			else if (value.getStatus() == TASK_STATUS.REJECTED_DUE_TO_QUEUE) {
				failedTaskDuetoQueue[value.getTaskType()]++;
			}
			else if (value.getStatus() == TASK_STATUS.REJECTED_DUE_TO_POLICY) {
				failedTaskDuetoPolicy[value.getTaskType()]++;
			}
			else if (value.getStatus() == TASK_STATUS.FAILED_DUE_TO_INACCESSIBILITY) {
				failedTaskDuetoInaccessibility[value.getTaskType()]++;
			}
		}

		// calculate total values
		uncompletedTask[numOfAppTypes] = IntStream.of(uncompletedTask).sum();
		uncompletedTaskOnCloud[numOfAppTypes] = IntStream.of(uncompletedTaskOnCloud).sum();
		uncompletedTaskOnEdge[numOfAppTypes] = IntStream.of(uncompletedTaskOnEdge).sum();
		uncompletedTaskOnMobile[numOfAppTypes] = IntStream.of(uncompletedTaskOnMobile).sum();

		completedTask[numOfAppTypes] = IntStream.of(completedTask).sum();
		completedTaskOnCloud[numOfAppTypes] = IntStream.of(completedTaskOnCloud).sum();
		completedTaskOnEdge[numOfAppTypes] = IntStream.of(completedTaskOnEdge).sum();
		completedTaskOnMobile[numOfAppTypes] = IntStream.of(completedTaskOnMobile).sum();

		failedTask[numOfAppTypes] = IntStream.of(failedTask).sum();
		failedTaskOnCloud[numOfAppTypes] = IntStream.of(failedTaskOnCloud).sum();
		failedTaskOnEdge[numOfAppTypes] = IntStream.of(failedTaskOnEdge).sum();
		failedTaskOnMobile[numOfAppTypes] = IntStream.of(failedTaskOnMobile).sum();

		networkDelay[numOfAppTypes] = DoubleStream.of(networkDelay).sum();
		lanDelay[numOfAppTypes] = DoubleStream.of(lanDelay).sum();
		manDelay[numOfAppTypes] = DoubleStream.of(manDelay).sum();
		wanDelay[numOfAppTypes] = DoubleStream.of(wanDelay).sum();
		
		lanUsage[numOfAppTypes] = DoubleStream.of(lanUsage).sum();
		manUsage[numOfAppTypes] = DoubleStream.of(manUsage).sum();
		wanUsage[numOfAppTypes] = DoubleStream.of(wanUsage).sum();

		serviceTime[numOfAppTypes] = DoubleStream.of(serviceTime).sum();
		serviceTimeOnCloud[numOfAppTypes] = DoubleStream.of(serviceTimeOnCloud).sum();
		serviceTimeOnEdge[numOfAppTypes] = DoubleStream.of(serviceTimeOnEdge).sum();
		serviceTimeOnMobile[numOfAppTypes] = DoubleStream.of(serviceTimeOnMobile).sum();

		processingTime[numOfAppTypes] = DoubleStream.of(processingTime).sum();
		processingTimeOnCloud[numOfAppTypes] = DoubleStream.of(processingTimeOnCloud).sum();
		processingTimeOnEdge[numOfAppTypes] = DoubleStream.of(processingTimeOnEdge).sum();
		processingTimeOnMobile[numOfAppTypes] = DoubleStream.of(processingTimeOnMobile).sum();

		failedTaskDueToVmCapacity[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacity).sum();
		failedTaskDueToVmCapacityOnCloud[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnCloud).sum();
		failedTaskDueToVmCapacityOnEdge[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnEdge).sum();
		failedTaskDueToVmCapacityOnMobile[numOfAppTypes] = IntStream.of(failedTaskDueToVmCapacityOnMobile).sum();
		
		cost[numOfAppTypes] = DoubleStream.of(cost).sum();
		failedTaskDuetoBw[numOfAppTypes] = IntStream.of(failedTaskDuetoBw).sum();
		failedTaskDuetoWanBw[numOfAppTypes] = IntStream.of(failedTaskDuetoWanBw).sum();
		failedTaskDuetoManBw[numOfAppTypes] = IntStream.of(failedTaskDuetoManBw).sum();
		failedTaskDuetoLanBw[numOfAppTypes] = IntStream.of(failedTaskDuetoLanBw).sum();
		failedTaskDuetoMobility[numOfAppTypes] = IntStream.of(failedTaskDuetoMobility).sum();
		failedTaskDuetoPolicy[numOfAppTypes] = IntStream.of(failedTaskDuetoPolicy).sum();
		failedTaskDuetoQueue[numOfAppTypes] = IntStream.of(failedTaskDuetoQueue).sum();
		failedTaskDuetoInaccessibility[numOfAppTypes] = IntStream.of(failedTaskDuetoInaccessibility).sum();

		// calculate server load
		double totalVmLoadOnEdge = 0;
		double totalVmLoadOnCloud = 0;
		double totalVmLoadOnMobile = 0;
		for (VmLoadLogItem entry : vmLoadList) {
			totalVmLoadOnEdge += entry.getEdgeLoad();
			totalVmLoadOnCloud += entry.getCloudLoad();
			totalVmLoadOnMobile += entry.getMobileLoad();
/*			if (fileLogEnabled)
				appendToFile(vmLoadBW, entry.toString());*/
		}

		if (fileLogEnabled) {
			// write location info to file
			for (int t = 1; t < (SimSettings.getInstance().getSimulationTime()
					/ SimSettings.getInstance().getVmLocationLogInterval()); t++) {
				int[] locationInfo = new int[SimSettings.getInstance().getNumOfPlaceTypes()];
				Double time = t * SimSettings.getInstance().getVmLocationLogInterval();

				if (time < SimSettings.getInstance().getWarmUpPeriod())
					continue;

				for (int i = 0; i < SimManager.getInstance().getNumOfMobileDevice(); i++) {

					Location loc = SimManager.getInstance().getMobilityModel().getLocation(i, time);
					int placeTypeIndex = loc.getPlaceTypeIndex();
					locationInfo[placeTypeIndex]++;
				}

				locationBW.write(time.toString());
				for (int i = 0; i < locationInfo.length; i++)
					locationBW.write(SimSettings.DELIMITER + locationInfo[i]);

				locationBW.newLine();
			}

			//Oleg: Log location of mobile devices after warm up period (assuming no mobility)
			for (int i = 0; i < SimManager.getInstance().getEdgeServerManager().getDatacenterList().size(); i++) {
				List<? extends EdgeHost> hostList = SimManager.getInstance().getEdgeServerManager().getDatacenterList().get(i).getHostList();
				for (int j = 0; j < hostList.size(); j++) {
					EdgeHost host = hostList.get(j);
					gridLocationBW.write("Host" + SimSettings.DELIMITER + host.getId() + SimSettings.DELIMITER +
							host.getLocation().getXPos() + SimSettings.DELIMITER + host.getLocation().getYPos());
					gridLocationBW.newLine();

				}
			}
			for (int i = 0; i < SimManager.getInstance().getNumOfMobileDevice(); i++) {

				Location loc = SimManager.getInstance().getMobilityModel().getLocation(i, SimSettings.getInstance().getWarmUpPeriod());

				gridLocationBW.write("Mobile" + SimSettings.DELIMITER + i + SimSettings.DELIMITER + loc.getXPos() +
						SimSettings.DELIMITER + loc.getYPos());
				gridLocationBW.newLine();
			}

			//Oleg:Create list of objects reads
			for (int i = 0; i < taskMap.size(); i++) {
				if (objectID[i] == null)
					continue;
				objectsBW.write((ioTaskID[i] + SimSettings.DELIMITER + objectID[i] + SimSettings.DELIMITER + isParity[i] +
						SimSettings.DELIMITER + hostID[i] + SimSettings.DELIMITER + accessID[i] +
						SimSettings.DELIMITER + readSource[i] + SimSettings.DELIMITER + df.format(objectReadDelay[i])));
//						SimSettings.DELIMITER + objectStatusID[i]));
				objectsBW.newLine();
			}
			//Oleg: Analyze readObjects
//			Iterator it = timeToReadStripe.entrySet().iterator();
//			while (it.hasNext()){
			int notFinished=0;
			for (Integer key : timeToReadStripe.keySet()){
				double readDelay=100;
				double readCost=0;
				int objectsRead=0;
				String dataTypeRead = "data";
//				Map<Integer, SortedSet<Double>> pair = it.next();
//				Map<Integer, SortedSet<Double>> pair = (Map.Entry)it.next();
//				SortedSet<Double> set = pair.getValue();
//				TreeSet<Double> set = new TreeSet<Double>();
				ArrayList<Double> list = timeToReadStripe.get(key);
				if (list.size()>numOfObjectsInStripe) {
					System.out.println("ERROR: Illegal number of objects in stripe");
					System.exit(0);
				}

				//warm up period
				if (list.contains(REJECTED))
					continue;
				//TODO: support case of not finished
				//TODO: check case one element and NOT_FINISHED
				//if one of the tasks hasn't finished
				if (list.contains(NOT_FINISHED))
				{
					//TODO: check this
					if((list.get(0).equals(NOT_FINISHED)) && list.size()==1){
//						System.out.println("Entire list not finished");
						notFinished++;
						continue;
					}

					//if data was read
					if (!list.get(0).equals(NOT_FINISHED)) {
						readDelay = list.get(0);
						readCost = readDelay;
						dataTypeRead = "data";
						objectsRead=1;
					}
					//if also parity wasn't read
					else if(list.subList(1, numOfObjectsInStripe).contains(NOT_FINISHED)) {
						notFinished++;
						continue;
					}
					//parity finished
					else {
						dataTypeRead = "parity";
						readDelay = Collections.max(list.subList(1, numOfObjectsInStripe));
						for(Double d : list.subList(1, numOfObjectsInStripe)) {
							readCost += d;
							objectsRead++;
						}

					}
				}
				//if only one object read
				else if (list.size() ==1) {
					readDelay = list.get(0);
					readCost=readDelay;
					objectsRead=1;
				}
				//shouldn't happen
				else if (list.size() < (numOfObjectsInStripe)){
					readDelay = list.get(0);
					for(Double d : list) {
						readCost += d;
						objectsRead++;
					}
					System.out.println("Less than " + numOfObjectsInStripe + " objects read");
				}
				else {
					try {
						for(Double d : list) {
							readCost += d;
						}
						objectsRead=list.size();
						//if data and parity were read, take the best one
						//parity delay
						readDelay = Collections.max(list.subList(1, numOfObjectsInStripe));

						if (readDelay > list.get(0)) {
//							System.out.println("ioTaskID = " + key + ", data = " + list.get(0) + ", parity = " + readDelay + ", read data");
							readDelay = list.get(0);
							dataTypeRead = "data";
						}
						else {
							dataTypeRead = "parity";
//							System.out.println("ioTaskID = " + key + ", data = " + list.get(0) + ", parity = " + readDelay + ", read parity");
						}
					}
					catch (Exception e){
						System.out.println("Failed to extract stripe latency");
					}
				}

				readObjectsBW.write((key + SimSettings.DELIMITER + df.format(readDelay))+
						SimSettings.DELIMITER +  df.format(readCost)+  SimSettings.DELIMITER +dataTypeRead +
						SimSettings.DELIMITER + objectsRead);
				readObjectsBW.newLine();
//				it.remove(); // avoids a ConcurrentModificationException

			}
			System.out.println("Not finished: " + notFinished);

			for (int i = 0; i < numOfAppTypes + 1; i++) {
				//print only summary for now
				if (i<numOfAppTypes)
					continue;

				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just
					// discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;
				}

				// check if the divisor is zero in order to avoid division by
				// zero problem
				double _serviceTime = (completedTask[i] == 0) ? 0.0 : (serviceTime[i] / (double) completedTask[i]);
				double _networkDelay = (completedTask[i] == 0) ? 0.0 : (networkDelay[i] / ((double) completedTask[i] - (double)completedTaskOnMobile[i]));
				double _processingTime = (completedTask[i] == 0) ? 0.0 : (processingTime[i] / (double) completedTask[i]);
				double _vmLoadOnEdge = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnEdge / (double) vmLoadList.size());
				double _vmLoadOnClould = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnCloud / (double) vmLoadList.size());
				double _vmLoadOnMobile = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnMobile / (double) vmLoadList.size());
				double _cost = (completedTask[i] == 0) ? 0.0 : (cost[i] / (double) completedTask[i]);

				double _lanDelay = (lanUsage[i] == 0) ? 0.0
						: (lanDelay[i] / (double) lanUsage[i]);
				double _manDelay = (manUsage[i] == 0) ? 0.0
						: (manDelay[i] / (double) manUsage[i]);
				double _wanDelay = (wanUsage[i] == 0) ? 0.0
						: (wanDelay[i] / (double) wanUsage[i]);

				// write generic results
				String genericResult1 = Integer.toString(completedTask[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTask[i]) + SimSettings.DELIMITER 
						+ Integer.toString(uncompletedTask[i]) + SimSettings.DELIMITER 
						+ Integer.toString(failedTaskDuetoBw[i]) + SimSettings.DELIMITER
						+ Double.toString(_serviceTime) + SimSettings.DELIMITER 
						+ Double.toString(_processingTime) + SimSettings.DELIMITER 
						+ Double.toString(_networkDelay) + SimSettings.DELIMITER
						+ Double.toString(0) + SimSettings.DELIMITER 
						+ Double.toString(_cost) + SimSettings.DELIMITER 
						+ Integer.toString(failedTaskDueToVmCapacity[i]) + SimSettings.DELIMITER 
						+ Integer.toString(failedTaskDuetoMobility[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskDuetoPolicy[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskDuetoQueue[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskDuetoInaccessibility[i]);

				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
						: (serviceTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
				double _processingTimeOnEdge = (completedTaskOnEdge[i] == 0) ? 0.0
						: (processingTimeOnEdge[i] / (double) completedTaskOnEdge[i]);
				String genericResult2 = Integer.toString(completedTaskOnEdge[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskOnEdge[i]) + SimSettings.DELIMITER
						+ Integer.toString(uncompletedTaskOnEdge[i]) + SimSettings.DELIMITER
						+ Integer.toString(0) + SimSettings.DELIMITER
						+ Double.toString(_serviceTimeOnEdge) + SimSettings.DELIMITER
						+ Double.toString(_processingTimeOnEdge) + SimSettings.DELIMITER
						+ Double.toString(0.0) + SimSettings.DELIMITER 
						+ Double.toString(_vmLoadOnEdge) + SimSettings.DELIMITER 
						+ Integer.toString(failedTaskDueToVmCapacityOnEdge[i]);

				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
						: (serviceTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
				double _processingTimeOnCloud = (completedTaskOnCloud[i] == 0) ? 0.0
						: (processingTimeOnCloud[i] / (double) completedTaskOnCloud[i]);
				String genericResult3 = Integer.toString(completedTaskOnCloud[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskOnCloud[i]) + SimSettings.DELIMITER
						+ Integer.toString(uncompletedTaskOnCloud[i]) + SimSettings.DELIMITER
						+ Integer.toString(0) + SimSettings.DELIMITER
						+ Double.toString(_serviceTimeOnCloud) + SimSettings.DELIMITER
						+ Double.toString(_processingTimeOnCloud) + SimSettings.DELIMITER 
						+ Double.toString(0.0) + SimSettings.DELIMITER
						+ Double.toString(_vmLoadOnClould) + SimSettings.DELIMITER 
						+ Integer.toString(failedTaskDueToVmCapacityOnCloud[i]);
				
				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
						: (serviceTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
				double _processingTimeOnMobile = (completedTaskOnMobile[i] == 0) ? 0.0
						: (processingTimeOnMobile[i] / (double) completedTaskOnMobile[i]);
				String genericResult4 = Integer.toString(completedTaskOnMobile[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskOnMobile[i]) + SimSettings.DELIMITER
						+ Integer.toString(uncompletedTaskOnMobile[i]) + SimSettings.DELIMITER
						+ Integer.toString(0) + SimSettings.DELIMITER
						+ Double.toString(_serviceTimeOnMobile) + SimSettings.DELIMITER
						+ Double.toString(_processingTimeOnMobile) + SimSettings.DELIMITER 
						+ Double.toString(0.0) + SimSettings.DELIMITER
						+ Double.toString(_vmLoadOnMobile) + SimSettings.DELIMITER 
						+ Integer.toString(failedTaskDueToVmCapacityOnMobile[i]);
				
				String genericResult5 = Double.toString(_lanDelay) + SimSettings.DELIMITER
						+ Double.toString(_manDelay) + SimSettings.DELIMITER
						+ Double.toString(_wanDelay) + SimSettings.DELIMITER
						+ 0 + SimSettings.DELIMITER //for future use
						+ Integer.toString(failedTaskDuetoLanBw[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskDuetoManBw[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedTaskDuetoWanBw[i]);
				if(MATLAB) {
					appendToFile(genericBWs[i], genericResult1);
					appendToFile(genericBWs[i], genericResult2);
					appendToFile(genericBWs[i], genericResult3);
					appendToFile(genericBWs[i], genericResult4);
					appendToFile(genericBWs[i], genericResult5);
				}
				else {
					appendToFile(genericBWs[i], "#completedTask;failedTask;uncompletedTask;failedTaskDuetoBw;serviceTime;processingTime;networkDelay;" +
							"0;cost;failedTaskDueToVmCapacity;failedTaskDuetoMobility;failedTaskDuetoPolicy;failedTaskDuetoQueue;failedTaskDuetoInaccessibility");
					appendToFile(genericBWs[i], genericResult1);
					appendToFile(genericBWs[i], "#completedTaskOnEdge;failedTaskOnEdge;uncompletedTaskOnEdge;0;" +
							"serviceTimeOnEdge;processingTimeOnEdge;0;vmLoadOnEdgefailedTaskDueToVmCapacityOnEdge");
					appendToFile(genericBWs[i], genericResult2);
					appendToFile(genericBWs[i], "#completedTaskOnCloud;failedTaskOnCloud;uncompletedTaskOnCloud;0;" +
							"serviceTimeOnCloud;processingTimeOnCloud;0;vmLoadOnClould;failedTaskDueToVmCapacityOnCloud");
					appendToFile(genericBWs[i], genericResult3);
					appendToFile(genericBWs[i], "#completedTaskOnMobile;failedTaskOnMobile;uncompletedTaskOnMobile;0;serviceTimeOnMobile;" +
							"processingTimeOnMobile;0;vmLoadOnMobile;failedTaskDueToVmCapacityOnMobile");
					appendToFile(genericBWs[i], genericResult4);
					appendToFile(genericBWs[i], "#lanDelay;manDelay;wanDelay;0;failedTaskDuetoLanBw;" +
							"failedTaskDuetoManBw;failedTaskDuetoWanBw");
					appendToFile(genericBWs[i], genericResult5);
				}
			}

			// close open files
			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				successBW.close();
				failBW.close();
			}
//			vmLoadBW.close();
			locationBW.close();
			gridLocationBW.close();
			readObjectsBW.close();
			objectsBW.close();
			for (int i = 0; i < numOfAppTypes + 1; i++) {
				//print only summary for now
				if (i<numOfAppTypes)
					continue;
				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just
					// discard it
					if (SimSettings.getInstance().getTaskLookUpTable()[i][0] == 0)
						continue;
				}
				genericBWs[i].close();
			}
		}

		// printout important results
		printLine("# of tasks (Edge/Cloud/Mobile): "
				+ (failedTask[numOfAppTypes] + completedTask[numOfAppTypes]) + "("
				+ (failedTaskOnEdge[numOfAppTypes] + completedTaskOnEdge[numOfAppTypes]) + "/" 
				+ (failedTaskOnCloud[numOfAppTypes]+ completedTaskOnCloud[numOfAppTypes]) + "/" 
				+ (failedTaskOnMobile[numOfAppTypes]+ completedTaskOnMobile[numOfAppTypes]) + ")");
		
		printLine("# of failed tasks (Edge/Cloud/Mobile): "
				+ failedTask[numOfAppTypes] + "("
				+ failedTaskOnEdge[numOfAppTypes] + "/"
				+ failedTaskOnCloud[numOfAppTypes] + "/"
				+ failedTaskOnMobile[numOfAppTypes] + ")");
		
		printLine("# of completed tasks (Edge/Cloud/Mobile): "
				+ completedTask[numOfAppTypes] + "("
				+ completedTaskOnEdge[numOfAppTypes] + "/"
				+ completedTaskOnCloud[numOfAppTypes] + "/"
				+ completedTaskOnMobile[numOfAppTypes] + ")");
		
		printLine("# of uncompleted tasks (Edge/Cloud/Mobile): "
				+ uncompletedTask[numOfAppTypes] + "("
				+ uncompletedTaskOnEdge[numOfAppTypes] + "/"
				+ uncompletedTaskOnCloud[numOfAppTypes] + "/"
				+ uncompletedTaskOnMobile[numOfAppTypes] + ")");

		printLine("# of failed tasks due to vm capacity (Edge/Cloud/Mobile): "
				+ failedTaskDueToVmCapacity[numOfAppTypes] + "("
				+ failedTaskDueToVmCapacityOnEdge[numOfAppTypes] + "/"
				+ failedTaskDueToVmCapacityOnCloud[numOfAppTypes] + "/"
				+ failedTaskDueToVmCapacityOnMobile[numOfAppTypes] + ")");
		
		printLine("# of failed tasks due to Mobility/Network(WLAN/MAN/WAN): "
				+ failedTaskDuetoMobility[numOfAppTypes]
				+ "/" + failedTaskDuetoBw[numOfAppTypes] 
				+ "(" + failedTaskDuetoLanBw[numOfAppTypes] 
				+ "/" + failedTaskDuetoManBw[numOfAppTypes] 
				+ "/" + failedTaskDuetoWanBw[numOfAppTypes] + ")");
		
		printLine("percentage of failed tasks: "
				+ String.format("%.6f", ((double) failedTask[numOfAppTypes] * (double) 100)
						/ (double) (completedTask[numOfAppTypes] + failedTask[numOfAppTypes]))
				+ "%");

		printLine("average service time: "
				+ String.format("%.6f", serviceTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
				+ " seconds. (" + "on Edge: "
				+ String.format("%.6f", serviceTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
				+ ", " + "on Cloud: "
				+ String.format("%.6f", serviceTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
				+ ", " + "on Mobile: "
				+ String.format("%.6f", serviceTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
				+ ")");

		printLine("average processing time: "
				+ String.format("%.6f", processingTime[numOfAppTypes] / (double) completedTask[numOfAppTypes])
				+ " seconds. (" + "on Edge: "
				+ String.format("%.6f", processingTimeOnEdge[numOfAppTypes] / (double) completedTaskOnEdge[numOfAppTypes])
				+ ", " + "on Cloud: " 
				+ String.format("%.6f", processingTimeOnCloud[numOfAppTypes] / (double) completedTaskOnCloud[numOfAppTypes])
				+ ", " + "on Mobile: " 
				+ String.format("%.6f", processingTimeOnMobile[numOfAppTypes] / (double) completedTaskOnMobile[numOfAppTypes])
				+ ")");

		printLine("average network delay: "
				+ String.format("%.6f", networkDelay[numOfAppTypes] / ((double) completedTask[numOfAppTypes] - (double) completedTaskOnMobile[numOfAppTypes]))
				+ " seconds. (" + "LAN delay: "
				+ String.format("%.6f", lanDelay[numOfAppTypes] / (double) lanUsage[numOfAppTypes])
				+ ", " + "MAN delay: "
				+ String.format("%.6f", manDelay[numOfAppTypes] / (double) manUsage[numOfAppTypes])
				+ ", " + "WAN delay: "
				+ String.format("%.6f", wanDelay[numOfAppTypes] / (double) wanUsage[numOfAppTypes]) + ")");

		printLine("average server utilization Edge/Cloud/Mobile: " 
				+ String.format("%.6f", totalVmLoadOnEdge / (double) vmLoadList.size()) + "/"
				+ String.format("%.6f", totalVmLoadOnCloud / (double) vmLoadList.size()) + "/"
				+ String.format("%.6f", totalVmLoadOnMobile / (double) vmLoadList.size()));
		
		printLine("average cost: " + cost[numOfAppTypes] / completedTask[numOfAppTypes] + "$");

		// clear related collections (map list etc.)
		taskMap.clear();
		vmLoadList.clear();
	}
}

class VmLoadLogItem {
	private double time;
	private double vmLoadOnEdge;
	private double vmLoadOnCloud;
	private double vmLoadOnMobile;

	VmLoadLogItem(double _time, double _vmLoadOnEdge, double _vmLoadOnCloud, double _vmLoadOnMobile) {
		time = _time;
		vmLoadOnEdge = _vmLoadOnEdge;
		vmLoadOnCloud = _vmLoadOnCloud;
		vmLoadOnMobile = _vmLoadOnMobile;
	}

	public double getEdgeLoad() {
		return vmLoadOnEdge;
	}

	public double getCloudLoad() {
		return vmLoadOnCloud;
	}
	
	public double getMobileLoad() {
		return vmLoadOnMobile;
	}
	
	public String toString() {
		return time + 
				SimSettings.DELIMITER + vmLoadOnEdge +
				SimSettings.DELIMITER + vmLoadOnCloud +
				SimSettings.DELIMITER + vmLoadOnMobile;
	}
}

class LogItem {
	private SimLogger.TASK_STATUS status;
	private SimLogger.NETWORK_ERRORS networkError;
	private int datacenterId;
	private int hostId;
	private int vmId;
	private int vmType;
	private int taskType;
	private int taskLenght;
	private int taskInputType;
	private int taskOutputSize;
	private double taskStartTime;
	private double taskEndTime;
	private double lanUploadDelay;
	private double manUploadDelay;
	private double wanUploadDelay;
	private double lanDownloadDelay;
	private double manDownloadDelay;
	private double wanDownloadDelay;
	private double bwCost;
	private double cpuCost;
	private boolean isInWarmUpPeriod;
	//storage
	private String stripeID;
	private String objectRead;
	private int paritiesToRead;
	private int ioTaskID;
	private int isParity;
	private int accessHostID;

	LogItem(int _taskType, int _taskLenght, int _taskInputType, int _taskOutputSize) {
		taskType = _taskType;
		taskLenght = _taskLenght;
		taskInputType = _taskInputType;
		taskOutputSize = _taskOutputSize;
		networkError = NETWORK_ERRORS.NONE;
		status = SimLogger.TASK_STATUS.CREATED;
		taskEndTime = 0;
	}
	//Storage
	LogItem(int _taskType, int _taskLenght, int _taskInputType, int _taskOutputSize, String _stripeID, String _objectID, int _ioTaskID,
			int _isParity, int _paritiesToRead, int _accessHostID) {
		taskType = _taskType;
		taskLenght = _taskLenght;
		taskInputType = _taskInputType;
		taskOutputSize = _taskOutputSize;
		networkError = NETWORK_ERRORS.NONE;
		status = SimLogger.TASK_STATUS.CREATED;
		taskEndTime = 0;
		stripeID = _stripeID;
		objectRead = _objectID;
		ioTaskID = _ioTaskID;
		isParity = _isParity;
		paritiesToRead = _paritiesToRead;
		accessHostID = _accessHostID;
	}
	
	public void taskStarted(double time) {
		taskStartTime = time;
		status = SimLogger.TASK_STATUS.UPLOADING;
		
		if (time < SimSettings.getInstance().getWarmUpPeriod())
			isInWarmUpPeriod = true;
		else
			isInWarmUpPeriod = false;
	}
	
	public void setUploadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanUploadDelay = delay;
	}
	
	public void setDownloadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanDownloadDelay = delay;
	}
	
	public void taskAssigned(int _datacenterId, int _hostId, int _vmId, int _vmType) {
		status = SimLogger.TASK_STATUS.PROCESSING;
		datacenterId = _datacenterId;
		hostId = _hostId;
		vmId = _vmId;
		vmType = _vmType;
	}

	public void taskExecuted() {
		status = SimLogger.TASK_STATUS.DOWNLOADING;
	}

	public void setObjectRead(String _objectRead) {
		objectRead = _objectRead;
	}

	public void taskEnded(double time) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.COMPLETED;
	}

	public void taskRejectedDueToVMCapacity(double time, int _vmType) {
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY;
	}

	public void taskRejectedDueToBandwidth(double time, int _vmType, NETWORK_DELAY_TYPES delayType) {
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
	}


	public void taskRejectedDueToPolicy(double time, int _vmType) {
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_POLICY;
	}

	public void taskRejectedDueToQueue(double time) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.REJECTED_DUE_TO_QUEUE;
	}

	public void taskFailedDueToInaccessibility(double time, int _vmType) {
		vmType = _vmType;
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.FAILED_DUE_TO_INACCESSIBILITY;
	}

	public void taskFailedDueToBandwidth(double time, NETWORK_DELAY_TYPES delayType) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
	}

	public void taskFailedDueToMobility(double time) {
		taskEndTime = time;
		status = SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY;
	}

	public void setCost(double _bwCost, double _cpuCos) {
		bwCost = _bwCost;
		cpuCost = _cpuCos;
	}

	public boolean isInWarmUpPeriod() {
		return isInWarmUpPeriod;
	}

	public double getCost() {
		return bwCost + cpuCost;
	}

	public int getIoTaskID() {
		return ioTaskID;
	}

	public int getIsParity() {
		return isParity;
	}

	public int getAccessHostID() {
		return accessHostID;
	}

	public int getDatacenterId() {
		return datacenterId;
	}

	public double getNetworkUploadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanUploadDelay;
		
		return result;
	}

	public double getNetworkDownloadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(NETWORK_DELAY_TYPES delayType){
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay + lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay + manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay + wanUploadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(){
		return  lanUploadDelay +
				manUploadDelay +
				wanUploadDelay +
				lanDownloadDelay +
				manDownloadDelay +
				wanDownloadDelay;
	}

	public String getStripeID() {
		return stripeID;
	}


	public String getObjectRead() {
		return objectRead;
	}
	
	public double getServiceTime() {
		return taskEndTime - taskStartTime;
	}

	public SimLogger.TASK_STATUS getStatus() {
		return status;
	}

	public SimLogger.NETWORK_ERRORS getNetworkError() {
		return networkError;
	}
	
	public int getVmType() {
		return vmType;
	}

	public int getTaskType() {
		return taskType;
	}

	public int getHostId() {
		return hostId;
	}

	public void setAccessHostID(int accessHostID) {
		this.accessHostID = accessHostID;
	}

	public void setHostId(int hostId) {
		this.hostId = hostId;
	}

	public String toString(int taskId) {
		String result = taskId + SimSettings.DELIMITER + datacenterId + SimSettings.DELIMITER + hostId
				+ SimSettings.DELIMITER + vmId + SimSettings.DELIMITER + vmType + SimSettings.DELIMITER + taskType
				+ SimSettings.DELIMITER + taskLenght + SimSettings.DELIMITER + taskInputType + SimSettings.DELIMITER
				+ taskOutputSize + SimSettings.DELIMITER + taskStartTime + SimSettings.DELIMITER + taskEndTime
				+ SimSettings.DELIMITER;

		if (status == SimLogger.TASK_STATUS.COMPLETED){
			result += getNetworkDelay() + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
		}
		else if (status == SimLogger.TASK_STATUS.REJECTED_DUE_TO_VM_CAPACITY)
			result += "1"; // failure reason 1
		else if (status == SimLogger.TASK_STATUS.REJECTED_DUE_TO_BANDWIDTH)
			result += "2"; // failure reason 2
		else if (status == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_BANDWIDTH)
			result += "3"; // failure reason 3
		else if (status == SimLogger.TASK_STATUS.UNFINISHED_DUE_TO_MOBILITY)
			result += "4"; // failure reason 4
		else
			result += "0"; // default failure reason
		return result;
	}


}
