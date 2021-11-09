/*
 * Title:        EdgeCloudSim - Scenario Factory
 * 
 * Description:  Sample scenario factory providing the default
 *               instances of required abstract classes
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.applications.NSF_BSF_app1;

import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.cloud_server.DefaultCloudServerManager;
import edu.boun.edgecloudsim.core.ScenarioFactory;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.StorageMobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.DefaultMobileServerManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_orchestrator.StorageEdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.DefaultEdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.mobility.StaticMobility;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.network.StorageNetworkModel;
import edu.boun.edgecloudsim.task_generator.IdleActiveStorageLoadGenerator;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;

public class SampleScenarioFactory implements ScenarioFactory {
	private int numOfMobileDevice;
	private double simulationTime;
	private String orchestratorPolicy;
	private String simScenario;
	private String objectPlacementPolicy;

	SampleScenarioFactory(int _numOfMobileDevice,
			double _simulationTime,
			String _orchestratorPolicy,
			String _simScenario,
						  String _objectPlacementPolicy){
		orchestratorPolicy = _orchestratorPolicy;
		numOfMobileDevice = _numOfMobileDevice;
		simulationTime = _simulationTime;
		simScenario = _simScenario;
		objectPlacementPolicy = _objectPlacementPolicy;
	}

	@Override
	public LoadGeneratorModel getLoadGeneratorModel() {
		return new IdleActiveStorageLoadGenerator(numOfMobileDevice, simulationTime, simScenario, orchestratorPolicy,objectPlacementPolicy);
	}

	@Override
	public EdgeOrchestrator getEdgeOrchestrator() {
		return new StorageEdgeOrchestrator(orchestratorPolicy, simScenario);
	}

	@Override
//	public MobilityModel getMobilityModel() { return new StaticMobility(numOfMobileDevice,simulationTime); }
	public MobilityModel getMobilityModel() {
		return new StaticRangeMobility(numOfMobileDevice,simulationTime);
/*		if(numOfMobileDevice>2)
			return new StaticRangeMobility(numOfMobileDevice,simulationTime);
		else
			return new StaticMobility(numOfMobileDevice,simulationTime);*/
	}

	@Override
	public NetworkModel getNetworkModel() {
		return new StorageNetworkModel(numOfMobileDevice, simScenario);
	}

	@Override
	public EdgeServerManager getEdgeServerManager() {
		return new DefaultEdgeServerManager();
	}

	@Override
	public CloudServerManager getCloudServerManager() {
		return new DefaultCloudServerManager();
	}
	
	@Override
	public MobileDeviceManager getMobileDeviceManager() throws Exception {
		return new StorageMobileDeviceManager();
	}

	@Override
	public MobileServerManager getMobileServerManager() {
		return new DefaultMobileServerManager();
	}
}
