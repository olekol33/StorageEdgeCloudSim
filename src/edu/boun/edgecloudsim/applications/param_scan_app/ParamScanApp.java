/*
 * Title:        EdgeCloudSim - Main Application
 * 
 * Description:  Main application for Simple App
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.param_scan_app;

import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamScanApp {

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		//disable console output of cloudsim library
		Log.disable();
		
		//enable console ourput and file output of this application
		SimLogger.enablePrintLog();

		int variabilityIteNum=1;
		int iterationNumber = 1;

		String configFile = "";
		String outputFolder = "";
		String edgeDevicesFile = "";
		String applicationsFile = "";
		String[] codingPolicies = {"IF_CONGESTED_READ_PARITY"};
		String[] replicationPolicies = {"IF_CONGESTED_READ_PARITY","NEAREST_HOST","CLOUD_OR_NEAREST_IF_CONGESTED"};
		String[] dataParityPolicies = {"IF_CONGESTED_READ_PARITY"};
//		String[] distributions = {"ZIPF","UNIFORM"};
		String[] distributions = {"UNIFORMZIPF"};
//		String[] failScenario = {"NOFAIL","FAIL"};
		String[] failScenario = {"FAIL"};
		if (args.length == 5){
			configFile = args[0];
			edgeDevicesFile = args[1];
			applicationsFile = args[2];
			outputFolder = args[3];
			iterationNumber = Integer.parseInt(args[4]);
		}
		else if (args.length == 1){
			configFile = args[0];
			applicationsFile = "scripts/param_scan_app/config/applications.xml";
			edgeDevicesFile = "scripts/param_scan_app/config/edge_devices.xml";
			outputFolder = "sim_results/ite" + iterationNumber;
		}
		else{
			SimLogger.printLine("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
			configFile = "scripts/param_scan_app/config/default_config.properties";
			applicationsFile = "scripts/param_scan_app/config/applications.xml";
			edgeDevicesFile = "scripts/param_scan_app/config/edge_devices.xml";
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
		SS.setParamScanMode(true);
		SS.checkRunMode();
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date SimulationStartDate = Calendar.getInstance().getTime();
		String now = df.format(SimulationStartDate);
		SimLogger.printLine("Simulation started at " + now);
		SimLogger.printLine("----------------------------------------------------------------------");
		double i0=1;
		int seed = SimSettings.getInstance().getRandomSeed();
		int numOfDataObjects = SimSettings.getInstance().getNumOfDataObjects();
		int numOfStripes = SimSettings.getInstance().getNumOfStripes();

		for(int j=SS.getMinNumOfMobileDev(); j<=SS.getMaxNumOfMobileDev(); j+=SS.getMobileDevCounterSize())
		{
			for(int k=0; k<SS.getSimulationScenarios().length; k++)
			{
				for(int i=0; i<SS.getOrchestratorPolicies().length; i++)
				{
					for(int p=0; p<SS.getObjectPlacement().length; p++) {
//						for(String fail:failScenario) {
						for(String fail:SS.getFailScenarios()) {
							if (fail.equals("FAIL"))
								SS.setHostFailureScenario(true);
							else
								SS.setHostFailureScenario(false);
							for (String dist : distributions) {
								if(SS.isMMPP()) {
									SS.setObjectDistRead("UNIFORM");
									SS.setStripeDistRead("UNIFORM");
								}
								else {
//									SS.setObjectDistRead("ZIPF");
//									SS.setStripeDistRead("ZIPF");
								}
//								SS.setObjectDistPlace("UNIFORM");
//								SS.setStripeDistPlace("UNIFORM");
								SimLogger.printLine("Distributions: Read/Place " + SS.getObjectDistRead() + "/" +
										SS.getObjectDistPlace());
								double step0 = SS.getLambda0step();
								double increaseTh = 1;
								double stepIncrease = 0.5;
								boolean foundLambda = false;
								//For overhead scan - reset for new run
								if(SimSettings.getInstance().isOverheadScan())
									SimSettings.getInstance().setNumOfDataObjects(numOfDataObjects);
								for (double lambda0 = SS.getLambda0Min(); lambda0 <= SS.getLambda0Max(); lambda0 = lambda0 + step0) {
									for (int iSeed = seed; iSeed < seed + SimSettings.getInstance().getVariabilityIterations(); iSeed++) {
										SimSettings.getInstance().setRandomSeed(iSeed);
										SimLogger.printLine("Current seed: " + SimSettings.getInstance().getRandomSeed());
										//increase step
										if (lambda0 > increaseTh) {
											step0 *= 2;
											increaseTh += stepIncrease;
											stepIncrease *= 2;
										}
										String objectPlacementPolicy = SS.getObjectPlacement()[p];
										String simScenario = SS.getSimulationScenarios()[k];
										String orchestratorPolicy = SS.getOrchestratorPolicies()[i];

										//Proceed only if orchestrator policy matches placement
										if (objectPlacementPolicy.equals("CODING_PLACE")) {
											if (!Arrays.asList(codingPolicies).contains(orchestratorPolicy))
												break;
//												continue;
										} else if (objectPlacementPolicy.equals("REPLICATION_PLACE")) {
											if (!Arrays.asList(replicationPolicies).contains(orchestratorPolicy))
												break;
//												continue;
										} else if (objectPlacementPolicy.equals("DATA_PARITY_PLACE")) {
											if (!Arrays.asList(dataParityPolicies).contains(orchestratorPolicy))
												break;
//												continue;
										} else {
											System.out.println("ERROR: Placement policy doesn't exist");
											System.exit(0);
										}

										//Setting lambdas for iteration
										//TODO: currently assume all are equal
										for (int t = 0; t < SimSettings.getInstance().getTaskLookUpTable().length; t++) {
											SS.setPoissonInTaskLookUpTable(t, lambda0);
										}


										Date ScenarioStartDate = Calendar.getInstance().getTime();
										now = df.format(ScenarioStartDate);
										//						System.out.println(Integer.toString(j) + simScenario + orchestratorPolicy + objectPlacementPolicy);
										// Storage: Generate Redis KV list
										RedisListHandler.closeConnection();
										RedisListHandler.createList(objectPlacementPolicy);

										//							String[] simParams = {Integer.toString(j), simScenario, orchestratorPolicy, objectPlacementPolicy};
										String[] simParams = {Integer.toString(j), simScenario, orchestratorPolicy, objectPlacementPolicy,
												Double.toString(lambda0)};

//									SimUtils.cleanOutputFolderPerConfiguration(outputFolder, simParams);


										SimLogger.printLine("Scenario started at " + now);
										SimLogger.printLine("Scenario: " + simScenario + " - Policy: " + orchestratorPolicy + " - Placement: " + objectPlacementPolicy +
												" - #iteration: " + iterationNumber + " - Distribution: " + dist + " - Fail Scenario: " + fail);
										SimLogger.printLine("Duration: " + SS.getSimulationTime() / 3600 + " hour(s) - Poisson: " +
												SS.getTaskLookUpTable()[0][LoadGeneratorModel.POISSON_INTERARRIVAL] + " - #devices: " + j);
										SimLogger.getInstance().simStarted(outputFolder, "SIMRESULT_" + simScenario + "_" + orchestratorPolicy +
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
											ScenarioFactory sampleFactory = new SampleScenarioFactory(j, SS.getSimulationTime(), orchestratorPolicy, simScenario, objectPlacementPolicy);


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
										File file2 = new File(SimLogger.getInstance().getOutputFolder(), SimLogger.getInstance().getFilePrefix() + "_TASK_FAILED.log");
										if (file2.exists()) { //failed
											if ((SimSettings.getInstance().isVariabilityRun() &&
													variabilityIteNum < SimSettings.getInstance().getVariabilityIterations()) ||
													SimSettings.getInstance().getVariabilityIterations()==1) { //rerun
											file2.delete();

//												variabilityIteNum++;
//												SimSettings.getInstance().setRandomSeed(SimSettings.getInstance().getRandomSeed() + 1);
												if (SimSettings.getInstance().isOverheadScan()) {
//													SimLogger.printLine("Rerun scenario. Seed: " + SimSettings.getInstance().getRandomSeed() + 1);
													//avoid seed increment in next iteration
													iSeed--;
													SimSettings.getInstance().setNumOfDataObjects(SimSettings.getInstance().getNumOfDataObjects() - 5);
													SimSettings.getInstance().setNumOfStripes(SimSettings.getInstance().getNumOfStripes() - 5);
													RedisListHandler.updateNumOfDataObjects();
													RedisListHandler.updateNumOfStripes();
													Pattern pattern = Pattern.compile("_OH(.*)_SEED");
													Matcher matcher = pattern.matcher(SimLogger.getInstance().getFilePrefix());
													matcher.find();
													double overhead =Double.valueOf(matcher.group(1));
													if (overhead>=8) {
														SimLogger.printLine("Failed to finish run with value");
//															variabilityIteNum = 1;
														SimSettings.getInstance().setNumOfDataObjects(numOfDataObjects);
														SimSettings.getInstance().setNumOfStripes(numOfStripes);
														String filePrefix = SimLogger.getInstance().getFilePrefix();
														//set 0 overhead
														filePrefix = filePrefix.replaceAll("OH\\d+(\\d.\\d+)?", "OH100");
														try (PrintStream out = new PrintStream(new FileOutputStream(outputFolder + "/" +
																filePrefix + "_TASK_COMPLETED.log"))) {
															out.print("Lambda: " + lambda0 + "\nSeed: " + SimSettings.getInstance().getRandomSeed() + "\nData objects: " +
																	"FAILED" + "\n");
															iSeed++;
															continue;
														} catch (Exception e) {
															e.printStackTrace();
														}
													}
												}
												else{
													lambda0 -= step0;
												}
											}
											} else if (variabilityIteNum == SimSettings.getInstance().getVariabilityIterations() &&
												SimSettings.getInstance().getVariabilityIterations() != 1) { //break
												variabilityIteNum = 1;
												file2.delete();
//												SimSettings.getInstance().setRandomSeed(seed);
												//If overhead scan, reduce by step of 5 each time
	/*											if (SimSettings.getInstance().isOverheadScan() && (lambda0 + step0) > SS.getLambda0Max()) {
													SimSettings.getInstance().setNumOfDataObjects(SimSettings.getInstance().getNumOfDataObjects() - 5);
													SimSettings.getInstance().setNumOfStripes(SimSettings.getInstance().getNumOfStripes() - 5);
													RedisListHandler.updateNumOfDataObjects();
													RedisListHandler.updateNumOfStripes();
													step0 = SS.getLambda0step();
													lambda0 = SS.getLambda0Min() - step0;
													increaseTh = 1;
													stepIncrease = 0.5;
													if (SimSettings.getInstance().getNumOfDataObjects() <= 20) {
														SimLogger.printLine("Failed to finish run with value");
														variabilityIteNum = 1;
														SimSettings.getInstance().setRandomSeed(seed);
														SimSettings.getInstance().setNumOfDataObjects(numOfDataObjects);
														SimSettings.getInstance().setNumOfStripes(numOfStripes);
														String filePrefix = SimLogger.getInstance().getFilePrefix();
														//set 0 overhead
														filePrefix = filePrefix.replaceAll("OH\\d+(\\d.\\d+)?", "OH0");
														try (PrintStream out = new PrintStream(new FileOutputStream(outputFolder + "/" +
																filePrefix + "_TASK_COMPLETED.log"))) {
															out.print("Lambda: " + lambda0 + "\nSeed: " + SimSettings.getInstance().getRandomSeed() + "\nData objects: " +
																	"FAILED" + "\n");
															break;
														} catch (Exception e) {
															e.printStackTrace();
														}
													}*/

												continue;
											}
										else { //completed
											if (!SimSettings.getInstance().isOverheadScan())

											SimSettings.getInstance().setRandomSeed(seed);
											SimSettings.getInstance().setNumOfDataObjects(numOfDataObjects);
											SimSettings.getInstance().setNumOfStripes(numOfStripes);
											RedisListHandler.updateNumOfDataObjects();
											RedisListHandler.updateNumOfStripes();
											if (SimSettings.getInstance().isVariabilityRun() && SimSettings.getInstance().isOverheadScan()) {
//												lambda0 = SS.getLambda0Min() - step0;
												variabilityIteNum = 1;
												SimSettings.getInstance().setNumOfDataObjects(numOfDataObjects);
												SimSettings.getInstance().setNumOfStripes(numOfStripes);
												RedisListHandler.updateNumOfDataObjects();
												RedisListHandler.updateNumOfStripes();
											}
											try (PrintStream out = new PrintStream(new FileOutputStream(outputFolder + "/" +
													SimLogger.getInstance().getFilePrefix() + "_TASK_COMPLETED.log"))) {
												out.print("Lambda: " + lambda0 + "\nSeed" + SimSettings.getInstance().getRandomSeed() + "\nData objects: " +
														SimSettings.getInstance().getNumOfDataObjects() + "\n");
												//next seed, avoid increment
												continue;
											} catch (Exception e) {
												e.printStackTrace();
											}
										}
//					}
									}
								}
							}
						}
					}//End of placement policies loop
				}//End of orchestrators loop
			}//End of scenarios loop
			//if all scenarios for this lambda have completed the run
/*				File file3 = new File(SimLogger.getInstance().getOutputFolder(), SimLogger.getInstance().getFilePrefix() + "_TASK_COMPLETED.log");
				if (!file.exists()) {
					try {
						file3.wr
						new FileOutputStream(file3).close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}*/

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
