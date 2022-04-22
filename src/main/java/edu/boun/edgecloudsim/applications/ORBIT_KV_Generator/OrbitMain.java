/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Simple App
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.ORBIT_KV_Generator;

import edu.boun.edgecloudsim.applications.ORBIT_KV_Generator.SampleScenarioFactory;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.storage.ObjectGenerator;
import edu.boun.edgecloudsim.storage.OrbitReader;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class OrbitMain {

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) throws ParserConfigurationException, IOException, TransformerException, SAXException {
		//disable console output of cloudsim library
		Log.disable();
		
		//enable console ourput and file output of this application
		SimLogger.enablePrintLog();
		
		int iterationNumber = 1;
		int currentHost=0;
		int currentClient=0;
		long currentTime;
		String runType = "";
		String configFile = "";
		String outputFolder = "";
		String edgeDevicesFile = "";
		String applicationsFile = "";


		runType = args[0];
		if (runType.equals("host")){
			currentHost = Integer.valueOf(args[1]);
		}
		else {
//			runType = "client";
			currentHost = 0;
			currentClient = Integer.valueOf(args[1]);
		}
		currentTime = Instant.now().toEpochMilli();



		configFile = "scripts/ORBIT_KV_Generator/default_config.properties";
		applicationsFile = "scripts/ORBIT_KV_Generator/applications.xml";
		edgeDevicesFile = "scripts/ORBIT_KV_Generator/edge_devices.xml";
		outputFolder = "sim_results/ite" + iterationNumber;

		File file = new File(outputFolder);
		File parent = file.getParentFile();
		if (!parent.exists()) {
			System.out.println("No parent");
			parent.mkdir();
			file.mkdir();
			System.out.println("Folder created");
		}
		else if (!file.exists()) {
			System.out.println("No sub folder");
			parent.mkdir();
			file.mkdir();
			System.out.println("Sub folder created");
		}
		else {
			System.out.println("Output folder exists");
		}
		//load settings from configuration file
		SimSettings SS = SimSettings.getInstance();
		if(SS.initialize(configFile, edgeDevicesFile, applicationsFile) == false){
			SimLogger.printLine("cannot initialize simulation settings!");
			System.exit(0);
		}
		if(SS.getFileLoggingEnabled()){
			SimLogger.enableFileLog();
//			SimUtils.cleanOutputFolder(outputFolder);
		}
		SS.setOutputFolder(outputFolder);
		SS.setThisMobileDevice(currentClient);
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger.printLine("----------------------------------------------------------------------");


//		for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
//		{
//			for(int k=0; k<SS.getSimulationScenarios().length; k++)
//			{
//				for(int i=0; i<SS.getOrchestratorPolicies().length; i++)
//				{
//					for(int p=0; p<SS.getObjectPlacement().length; p++) {

		String objectPlacementPolicy  = SS.getObjectPlacement()[0];
		String simScenario = SS.getSimulationScenarios()[0];
		String orchestratorPolicy = SS.getOrchestratorPolicies()[0];
		int j=SS.getMinNumOfMobileDev();
		Date ScenarioStartDate = Calendar.getInstance().getTime();
		now = df.format(ScenarioStartDate);
		// Storage: Generate Redis KV list on hosts
		if (runType.equals("host")) {
			RedisListHandler.closeConnection();
			RedisListHandler.orbitCreateList(objectPlacementPolicy, String.valueOf(currentHost));
			System.out.println("Objects placed on host " + currentHost);
			System.exit(0);
		}
		//place metadata on clients
		else {
/*							ObjectGenerator OG = new ObjectGenerator(objectPlacementPolicy);
			RedisListHandler.orbitPlaceMetadata();*/
		}

		String[] simParams = {Integer.toString(j), simScenario, orchestratorPolicy, objectPlacementPolicy};

		SimUtils.cleanOutputFolderPerConfiguration(outputFolder, simParams);


		SimLogger.printLine("Scenario started at " + now);
		SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy +  " - Placement: " + objectPlacementPolicy +
				" - #iteration: " + iterationNumber);
		SimLogger.printLine("Duration: " + SS.getSimulationTime() / 3600 + " hour(s) - Poisson: " +
				SS.getTaskLookUpTable()[0][LoadGeneratorModel.POISSON_INTERARRIVAL] + " - #devices: " + j);
//						SimLogger.getInstance().simStarted(outputFolder, "SIMRESULT_" + simScenario + "_" + orchestratorPolicy +
//								"_" + objectPlacementPolicy + "_" + j + "DEVICES");
		SimLogger.getInstance().simStarted(outputFolder, "SIMRESULT_" + simScenario + "_" + orchestratorPolicy +
				"_" + objectPlacementPolicy + "_" + j + "DEVICES",runType,currentHost,currentTime);

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 2;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag, 0.01);

			// Generate EdgeCloudsim Scenario Factory
			ScenarioFactory sampleFactory = new SampleScenarioFactory(j, SS.getSimulationTime(), orchestratorPolicy, simScenario,
					objectPlacementPolicy);


			// Generate EdgeCloudSim Simulation Manager
			SimManager manager = new SimManager(sampleFactory, j, simScenario, orchestratorPolicy,objectPlacementPolicy);

			// Start simulation - TODO remove?
			if (runType.equals("client")) {
				OrbitReader orbitClient = new OrbitReader(currentHost);
				orbitClient.clientRun();
				RedisListHandler.closeConnection();
			}

//								manager.startSimulation();

		} catch (Exception e) {
			SimLogger.printLine("The simulation has been terminated due to an unexpected error");
			e.printStackTrace();
			System.exit(0);
		}

		Date ScenarioEndDate = Calendar.getInstance().getTime();
		now = df.format(ScenarioEndDate);
		SimLogger.printLine("Scenario finished at " + now + ". It took " + SimUtils.getTimeDifference(ScenarioStartDate, ScenarioEndDate));
		SimLogger.printLine("----------------------------------------------------------------------");
//					}//End of placement policies loop
//				}//End of orchestrators loop
//			}//End of scenarios loop
//		}//End of mobile devices loop
		// Remove KV list
		RedisListHandler.closeConnection();
		Date SimulationEndDate = Calendar.getInstance().getTime();
		now = df.format(SimulationEndDate);
		SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));
	}
}
