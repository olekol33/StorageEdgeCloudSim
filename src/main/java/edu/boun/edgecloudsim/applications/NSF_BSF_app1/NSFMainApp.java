/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Simple App
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.NSF_BSF_app1;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class NSFMainApp {

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		//disable console output of cloudsim library
		Log.disable();
		
		//enable console ourput and file output of this application
		SimLogger.enablePrintLog();
		
		int iterationNumber = 1;
		String configFile = "";
		String outputFolder = "";
		String edgeDevicesFile = "";
		String applicationsFile = "";
		String[] codingPolicies = {"IF_CONGESTED_READ_PARITY"};
		String[] replicationPolicies = {"IF_CONGESTED_READ_PARITY","NEAREST_HOST","CLOUD_OR_NEAREST_IF_CONGESTED"};
		String[] dataParityPolicies = {"IF_CONGESTED_READ_PARITY"};
		if (args.length == 5){
			configFile = args[0];
			edgeDevicesFile = args[1];
			applicationsFile = args[2];
			outputFolder = args[3];
			iterationNumber = Integer.parseInt(args[4]);
		}
		else if (args.length == 1){
			configFile = args[0];
			applicationsFile = "scripts/NSF_BSF_app1/config/applications.xml";
			edgeDevicesFile = "scripts/NSF_BSF_app1/config/edge_devices.xml";
			outputFolder = "sim_results/ite" + iterationNumber;
		}
		else if (args.length == 2){
			configFile = args[0];
			iterationNumber = Integer.parseInt(args[1]);
			applicationsFile = "scripts/NSF_BSF_app1/config/applications.xml";
			edgeDevicesFile = "scripts/NSF_BSF_app1/config/edge_devices.xml";
			outputFolder = "sim_results/ite" + iterationNumber;
		}
		else{
			SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
			configFile = "scripts/NSF_BSF_app1/config/default_config.properties";
			applicationsFile = "scripts/NSF_BSF_app1/config/applications.xml";
			edgeDevicesFile = "scripts/NSF_BSF_app1/config/edge_devices.xml";
			outputFolder = "sim_results/ite" + iterationNumber;
		}
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
			System.out.println("Sub dolder created");
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
		//this is a special experiment
		SS.setNsfExperiment(true);
		SS.checkRunMode();
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger.printLine("----------------------------------------------------------------------");
		double i0=1;
		double i1=1;

		for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
		{
			for(int k=0; k<SS.getSimulationScenarios().length; k++)
			{
				for(int i=0; i<SS.getOrchestratorPolicies().length; i++)
				{
					for(int p=0; p<SS.getObjectPlacement().length; p++) {
						//exponential growing step
						double step0=SS.getLambda0step();
						for(double lambda0= SS.getLambda0Min(); lambda0<=SS.getLambda0Max();lambda0=lambda0+step0) {
							step0*=i0;
							i0+=0.05;
							double step1=SS.getLambda1step();
							i1=1;
							for(double lambda1= SS.getLambda1Min(); lambda1<=SS.getLambda1Max();lambda1=lambda1+step1) {
								step1*=i1;
								i1+=0.05;

								String objectPlacementPolicy = SS.getObjectPlacement()[p];
								String simScenario = SS.getSimulationScenarios()[k];
								String orchestratorPolicy = SS.getOrchestratorPolicies()[i];

								//Proceed only if orchestrator policy matches placement
								if (objectPlacementPolicy.equals("CODING_PLACE")) {
									if (!Arrays.asList(codingPolicies).contains(orchestratorPolicy))
										continue;
								} else if (objectPlacementPolicy.equals("REPLICATION_PLACE")) {
									if (!Arrays.asList(replicationPolicies).contains(orchestratorPolicy))
										continue;
								} else if (objectPlacementPolicy.equals("DATA_PARITY_PLACE")) {
									if (!Arrays.asList(dataParityPolicies).contains(orchestratorPolicy))
										continue;
								} else {
									System.out.println("ERROR: Placement policy doesn't exist");
									System.exit(0);
								}

								//Setting lambdas for iteration
								SS.setPoissonInTaskLookUpTable(0,lambda0);
								SS.setPoissonInTaskLookUpTable(1,lambda1);


								Date ScenarioStartDate = Calendar.getInstance().getTime();
								now = df.format(ScenarioStartDate);
//						System.out.println(Integer.toString(j) + simScenario + orchestratorPolicy + objectPlacementPolicy);
								// Storage: Generate Redis KV list
								RedisListHandler.closeConnection();
								RedisListHandler.createList(objectPlacementPolicy);

								String[] simParams = {Integer.toString(j), simScenario, orchestratorPolicy, objectPlacementPolicy,
										Double.toString(lambda0)+'_'+Double.toString(lambda1)};

								SimUtils.cleanOutputFolderPerConfiguration(outputFolder, simParams);


								SimLogger.printLine("Scenario started at " + now);
								SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - Placement: " + objectPlacementPolicy +
										" - #iteration: " + iterationNumber + " - lambda0: " + lambda0 + " - lambda1: " + lambda1);
								SimLogger.printLine("Duration: " + SS.getSimulationTime() / 3600 + " hour(s) - Poisson: " +
										SS.getTaskLookUpTable()[0][LoadGeneratorModel.POISSON_INTERARRIVAL] + " - #devices: " + j);
								SimLogger.getInstance().simStarted(outputFolder, "SIMRESULT_" + lambda0 + "_" + lambda1 + "_" +
										simScenario + "_" + orchestratorPolicy +
										"_" + objectPlacementPolicy + "_" + j + "DEVICES");

								try {
									// First step: Initialize the CloudSim package. It should be called
									// before creating any entities.
									int num_user = 2;   // number of grid users
									Calendar calendar = Calendar.getInstance();
									boolean trace_flag = false;  // mean trace events

									// Initialize the CloudSim library
									CloudSim.init(num_user, calendar, trace_flag, 0.01);

									// Generate EdgeCloudsim Scenario Factory
									ScenarioFactory sampleFactory = new SampleScenarioFactory(j, SS.getSimulationTime(),
											orchestratorPolicy, simScenario, objectPlacementPolicy);


									// Generate EdgeCloudSim Simulation Manager
									SimManager manager = new SimManager(sampleFactory, j, simScenario, orchestratorPolicy, objectPlacementPolicy);

									// Start simulation
									manager.startSimulation();

								} catch (Exception e) {
									SimLogger.printLine("The simulation has been terminated due to an unexpected error");
									e.printStackTrace();
									System.exit(0);
								}

								Date ScenarioEndDate = Calendar.getInstance().getTime();
								now = df.format(ScenarioEndDate);
								SimLogger.printLine("Scenario finished at " + now + ". It took " + SimUtils.getTimeDifference(ScenarioStartDate, ScenarioEndDate));
								SimLogger.printLine("----------------------------------------------------------------------");
							}
						}
					}//End of placement policies loop
				}//End of orchestrators loop
			}//End of scenarios loop
		}//End of mobile devices loop
		// Remove KV list
		RedisListHandler.closeConnection();
		Date SimulationEndDate = Calendar.getInstance().getTime();
		now = df.format(SimulationEndDate);
		SimLogger.printLine("Simulation finished at " + now +  ". It took " + SimUtils.getTimeDifference(SimulationStartDate,SimulationEndDate));

		//touch to mark run has finished
		String hostname = "Unknown";
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
			file = new File(outputFolder+"/done_"+hostname);
			if (!file.exists())
				new FileOutputStream(file).close();
		}
		catch (IOException e)
		{
		}


	}
}
