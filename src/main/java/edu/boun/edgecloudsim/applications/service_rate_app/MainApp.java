/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Simple App
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.service_rate_app;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.storage.OrbitReader;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.xml.sax.SAXException;
import org.apache.commons.math3.distribution.NormalDistribution;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class MainApp {

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) throws ParserConfigurationException, IOException, TransformerException, SAXException {
		System.out.println("####### Service Rate Experiment #######");
		//disable console output of cloudsim library
		Log.disable();
		
		//enable console ourput and file output of this application
		SimLogger.enablePrintLog();


		RandomGenerator rand;
		File serviceRatePath;
		BufferedWriter serviceRateFileBW=null;
		int iterationNumber = 1;
		String configFile = "";
		String outputFolderPath = "";
		String edgeDevicesFile = "";
		String applicationsFile = "";
		File serviceRateFile = null;
		FileWriter serviceRateFileFW = null;
		//For Orbit
		int currentHost=0;
		int currentClient=0;
		String runType = "";

//		String[] codingPolicies = {"IF_CONGESTED_READ_PARITY"};
//		String[] replicationPolicies = {"IF_CONGESTED_READ_PARITY","NEAREST_HOST","CLOUD_OR_NEAREST_IF_CONGESTED"};
//		String[] dataParityPolicies = {"IF_CONGESTED_READ_PARITY"};
		if (args.length == 5){
			configFile = args[0];
			edgeDevicesFile = args[1];
			applicationsFile = args[2];
			outputFolderPath = args[3];
			iterationNumber = Integer.parseInt(args[4]);
		}
		else if (args.length == 1){
			configFile = args[0];
			applicationsFile = "scripts/sample_app5/config/applications.xml";
			edgeDevicesFile = "scripts/sample_app5/config/edge_devices.xml";
			outputFolderPath = "sim_results/ite" + iterationNumber;
		}
		else if (args.length == 3){
			configFile = args[0];
			applicationsFile = args[1];
			edgeDevicesFile = args[2];
			outputFolderPath = "sim_results/ite" + iterationNumber;
		}
		else{
			MainApp o = new MainApp();
			Package pack = o.getClass().getPackage();
			String packageName = pack.getName();
			String expName = packageName.split("\\.")[4];
			SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
			configFile = "scripts/" + expName + "/config/default_config.properties";
			applicationsFile = "scripts/" + expName + "/config/applications.xml";
			edgeDevicesFile = "scripts/" + expName + "/config/edge_devices.xml";
//			configFile = "scripts/sample_app5/config/default_config.properties";
//			applicationsFile = "scripts/sample_app5/config/applications.xml";
//			edgeDevicesFile = "scripts/sample_app5/config/edge_devices.xml";
			outputFolderPath = "sim_results/ite" + iterationNumber;
		}

		//load settings from configuration file
		SimSettings SS = SimSettings.getInstance();
		if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
			SimLogger.printLine("cannot initialize simulation settings!");
			System.exit(0);
		}
		if(SS.getFileLoggingEnabled()){
			SimLogger.enableFileLog();
		}



		SS.checkRunMode();
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger.printLine("----------------------------------------------------------------------");

		int seed = SS.getRandomSeed();
		rand = new Well19937c(seed);
		SimLogger.printLine("Iteration: " + iterationNumber + ", with seed: " + seed);
		File outputFolder = new File(outputFolderPath);
		File parent = outputFolder.getParentFile();
		if (!parent.exists()) {
			System.out.println("No parent");
			parent.mkdir();
			outputFolder.mkdir();
			System.out.println("Folder created");
		} else if (!outputFolder.exists()) {
			System.out.println("No sub folder");
			parent.mkdir();
			outputFolder.mkdir();
			System.out.println("Sub folder created");
		} else {
			System.out.println("Output folder exists");
		}
		int users = SS.getMinNumOfMobileDev();
		String simScenario = SS.getSimulationScenarios()[0];
		String orchestratorPolicy = SS.getOrchestratorPolicies()[0];
		String objectPlacementPolicy = SS.getObjectPlacement()[0];
		SimSettings.getInstance().printServiceRate(users);
		serviceRatePath = new File(parent,"/service_rate");
		String srFilename = "";
		SS.setOutputFolder(outputFolderPath);

		//set minimal number of stripes
		double overhead = SS.getOverhead() - 1;
		int numOfParities = (int)(SS.getNumOfDataObjects()*overhead);
		if(numOfParities==0)
			numOfParities=1; //to avoid 0
		SS.setNumOfStripes(numOfParities);
		if(SS.isOrbitMode()){

			long currentTime;
			runType = args[0];
			if (runType.equals("host")){
				currentHost = Integer.valueOf(args[1]);
				// Storage: Generate Redis KV list on hosts
				RedisListHandler.closeConnection();
				RedisListHandler.orbitCreateList(objectPlacementPolicy, String.valueOf(currentHost));
				System.out.println("Objects placed on host " + currentHost);
				System.exit(0);
			}
			else {
//			runType = "client";
				currentHost = 0;
				currentClient = Integer.valueOf(args[1]);
				SS.setThisMobileDevice(currentClient);
			}
			currentTime = Instant.now().toEpochMilli();
		}

		//whenever not same overhead need to generate two files: for coding and replication
		//avoid deleting folder
		//in post-processing need to merge files
/*		if(SS.isServiceRateScan() && !SS.isSrExpSameOverhead()) {
			if(objectPlacementPolicy.equals("REPLICATION_PLACE"))
				filename = "SERVICE_RATE_1_N_" + String.valueOf(SS.getNumOfEdgeDatacenters()) + "_K_" +
						String.valueOf(SS.getNumOfDataObjects()) + ".csv";
			else
				filename = "SERVICE_RATE_2_N_" + String.valueOf(SS.getNumOfEdgeDatacenters()) + "_K_" +
						String.valueOf(SS.getNumOfDataObjects()) + ".csv";
		}*/
		else if (!SS.isOrbitMode() && !SS.isKeepServiceRateFile()){
			FileUtils.deleteDirectory(serviceRatePath);
			serviceRatePath.mkdir();
		}

		if (!SS.isOrbitMode()) {
			String fileHeader = "";
			for (int i = 0; i < SS.getNumOfDataObjects(); i++)
				fileHeader += String.valueOf(i) + ",";
			fileHeader += "type,reqsPerUserSec,readRate,iteration,simServiceCost,completed";
			for(int p=0;p<SS.getObjectPlacement().length;p++) {
				objectPlacementPolicy = SS.getObjectPlacement()[p];
				srFilename = "SIMRESULT_SERVICE_RATE" + "_" + objectPlacementPolicy;
				serviceRateFile = new File(serviceRatePath, srFilename + "_DEMAND.csv");
				serviceRateFileFW = new FileWriter(serviceRateFile, true);
				serviceRateFileBW = new BufferedWriter(serviceRateFileFW);
				SS.setServiceRateFileBW(serviceRateFileBW);
				SimLogger.appendToFile(serviceRateFileBW, fileHeader);
				serviceRateFileBW.close();
			}
		}


			//gaussian distribution generator
		//Mean is share of mean number of individual user requests that can be served
		double meanSystemServedReqsPerSec = SS.getServedReqsPerSec()*SS.getNumOfEdgeDatacenters()/users;
		if(objectPlacementPolicy.equals("REPLICATION_PLACE"))
			meanSystemServedReqsPerSec *= SS.getCodingRepReqRatio(); //adjust replication to same request rate as coding
		double meanSystemLambda = SS.getRequestRatePercentageOfCapacity() * meanSystemServedReqsPerSec * SS.getOverheadScanMeanRatio();
		double meanSystemStd = SS.getOverheadScanStd() * SS.getRequestRatePercentageOfCapacity() * meanSystemServedReqsPerSec;

		NormalDistribution lambdaGenerator = new NormalDistribution(rand,meanSystemLambda,meanSystemStd);
//		NormalDistribution zipfIndexGenerator = new NormalDistribution(rand,1.5,0.5);
		for (int i = 0; i < SS.getServiceRateIterations(); i++) {
			if(!SS.isOrbitMode()) //in orbit mode this is set in config
				SS.setCurrentServiceRateIteration(i);
			else{ //if orbit run is always single, need to bring generator to same point
				for (int j=0;j<SS.getCurrentServiceRateIteration();j++)
					lambdaGenerator.sample();
			}
			double reqsPerSec = lambdaGenerator.sample();
			reqsPerSec = Math.round(Math.abs(reqsPerSec)); //half-gaussian
			SS.setPoissonInTaskLookUpTable(0,1/reqsPerSec);
//			double zipfIndex = Math.abs(zipfIndexGenerator.sample());
//			SS.setZipfExponent(zipfIndex);
			for(int p=0;p<SS.getObjectPlacement().length;p++) {
/*				if (!SS.isOrbitMode()){
					File serialFolder = new File(SS.getSerializableFolder());
					FileUtils.deleteDirectory(serialFolder);
					serialFolder.mkdir();
				}*/
				SS.setSimulationFailed(false);

				objectPlacementPolicy = SS.getObjectPlacement()[p];

				srFilename = "SIMRESULT_SERVICE_RATE" + "_" + objectPlacementPolicy;
//				String filePrefix = srFilename + "_ITE_" + Integer.toString(i);
				String filePrefix = srFilename;

				Date ScenarioStartDate = Calendar.getInstance().getTime();
				now = df.format(ScenarioStartDate);


				String[] simParams = {Integer.toString(users), simScenario, orchestratorPolicy, objectPlacementPolicy};

				SimUtils.cleanOutputFolderPerConfiguration(outputFolderPath, simParams);


				SimLogger.printLine("Scenario started at " + now);
				SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - Placement: " + objectPlacementPolicy +
						" - Service rate iteration: " + String.valueOf(i));
				SimLogger.printLine("Duration: " + SS.getSimulationTime() / 3600 + " hour(s) - Poisson: " +
						SS.getTaskLookUpTable()[0][LoadGeneratorModel.POISSON_INTERARRIVAL] + " - #devices: " + users);
				SimLogger.getInstance().simStarted(outputFolderPath,filePrefix );

				try {
					SimSettings.getInstance().setServiceCost(0);
					// First step: Initialize the CloudSim package. It should be called
					// before creating any entities.
					int num_user = 2;   // number of grid users
					Calendar calendar = Calendar.getInstance();
					boolean trace_flag = false;  // mean trace events

					//control period of time between new event is fetched (half such it ready for next event)
					double periodBetweenCloudSimEvents = 0.5*SimSettings.getInstance().getRequestProcessingTime();
					// Initialize the CloudSim library
					CloudSim.init(num_user, calendar, trace_flag, periodBetweenCloudSimEvents);

					// Generate EdgeCloudsim Scenario Factory
					ScenarioFactory sampleFactory = new SampleScenarioFactory(users, SS.getSimulationTime(), orchestratorPolicy, simScenario, objectPlacementPolicy);


					// Generate EdgeCloudSim Simulation Manager
					SimManager manager = new SimManager(sampleFactory, users, simScenario, orchestratorPolicy, objectPlacementPolicy);

					// Storage: Generate Redis KV list
					RedisListHandler.closeConnection();
					RedisListHandler.createList(objectPlacementPolicy);

					// Start simulation
					manager.startSimulation();

				} catch (Exception e) {
					SimLogger.printLine("The simulation has been terminated due to an unexpected error");
					e.printStackTrace();
					System.exit(0);
/*					if (!SimSettings.getInstance().isServiceRateScan()) {
						SimLogger.printLine("The simulation has been terminated due to an unexpected error");
						e.printStackTrace();
						System.exit(0);
					} else {
						System.out.println("Failed iteration");
					}*/
				}
				String completed = "";
				if (SS.isSimulationFailed())
					completed = "false";
				else
					completed = "true";
				String policy = "";
				if(objectPlacementPolicy.equals("CODING_PLACE"))
					policy="coding";
				else
					policy="replication";
				serviceRateFile = new File(serviceRatePath, srFilename + "_DEMAND.csv");
				serviceRateFileFW = new FileWriter(serviceRateFile, true);
				serviceRateFileBW = new BufferedWriter(serviceRateFileFW);
				SimLogger.appendToFile(serviceRateFileBW, SS.getObjectRequestRateArray() + "," + policy + "," +
//						reqsPerSec + "," + SS.getServedReqsPerSec() + "," + i + "," + SS.getServiceCost() + "," + completed);
						SS.getReqRatePerSec() + "," + SS.getServedReqsPerSec() + "," + i + "," + SS.getServiceCost() + "," + completed);

				Date ScenarioEndDate = Calendar.getInstance().getTime();
				now = df.format(ScenarioEndDate);
//				System.exit(0);
				SimLogger.printLine("Scenario finished at " + now + ". It took " + SimUtils.getTimeDifference(ScenarioStartDate, ScenarioEndDate));
				SimLogger.printLine("----------------------------------------------------------------------");
				serviceRateFileBW.close();
			}
		}//End of placement policies loop


		// Remove KV list
		RedisListHandler.closeConnection();
		Date SimulationEndDate = Calendar.getInstance().getTime();
		now = df.format(SimulationEndDate);
		SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));

		//touch to mark run has finished
/*		String hostname = "Unknown";
		try
		{
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			hostname = addr.getHostName();
		}
		catch (UnknownHostException ex)
		{
			System.out.println("Hostname can not be resolved");
		}
		try
		{
			File file = new File(outputFolder+"/done_"+hostname);
			if (!file.exists())
				new FileOutputStream(file).close();
		}
		catch (IOException e)
		{
		}*/
	}
}
