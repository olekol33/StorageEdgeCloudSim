/*
 * Title:        EdgeCloudSim - Network Model
 * 
 * Description: 
 * SampleNetworkModel uses
 * -> the result of an empirical study for the WLAN and WAN delays
 * The experimental network model is developed
 * by taking measurements from the real life deployments.
 *   
 * -> MMPP/MMPP/1 queue model for MAN delay
 * MAN delay is observed via a single server queue model with
 * Markov-modulated Poisson process (MMPP) arrivals.
 *   
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.network;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import org.cloudbus.cloudsim.core.CloudSim;

public class SampleNetworkModel extends NetworkModel {
	public static enum NETWORK_TYPE {WLAN, LAN};
	public static enum LINK_TYPE {DOWNLOAD, UPLOAD};
	public static double MAN_BW = 1300*1024; //Kbps

	@SuppressWarnings("unused")
	protected int manClients;
	protected int[] wanClients;
	protected int[] wlanClients;

	protected double lastMM1QueeuUpdateTime;
	protected double ManPoissonMeanForDownload; //seconds
	protected double ManPoissonMeanForUpload; //seconds

	protected double avgManTaskInputSize; //bytes
	protected double avgManTaskOutputSize; //bytes

	//record last n task statistics during MM1_QUEUE_MODEL_UPDATE_INTEVAL seconds to simulate mmpp/m/1 queue model
	protected double totalManTaskInputSize;
	protected double totalManTaskOutputSize;
	protected double numOfManTaskForDownload;
	protected double numOfManTaskForUpload;

	//Oleg: Numbers based on measurements of a real network
	public static final double[] experimentalWlanDelay = {
		/*1 Client*/ 88040.279 /*(Kbps)*/,
		/*2 Clients*/ 45150.982 /*(Kbps)*/,
		/*3 Clients*/ 30303.641 /*(Kbps)*/,
		/*4 Clients*/ 27617.211 /*(Kbps)*/,
		/*5 Clients*/ 24868.616 /*(Kbps)*/,
		/*6 Clients*/ 22242.296 /*(Kbps)*/,
		/*7 Clients*/ 20524.064 /*(Kbps)*/,
		/*8 Clients*/ 18744.889 /*(Kbps)*/,
		/*9 Clients*/ 17058.827 /*(Kbps)*/,
		/*10 Clients*/ 15690.455 /*(Kbps)*/,
		/*11 Clients*/ 14127.744 /*(Kbps)*/,
		/*12 Clients*/ 13522.408 /*(Kbps)*/,
		/*13 Clients*/ 13177.631 /*(Kbps)*/,
		/*14 Clients*/ 12811.330 /*(Kbps)*/,
		/*15 Clients*/ 12584.387 /*(Kbps)*/,
		/*15 Clients*/ 12135.161 /*(Kbps)*/,
		/*16 Clients*/ 11705.638 /*(Kbps)*/,
		/*17 Clients*/ 11276.116 /*(Kbps)*/,
		/*18 Clients*/ 10846.594 /*(Kbps)*/,
		/*19 Clients*/ 10417.071 /*(Kbps)*/,
		/*20 Clients*/ 9987.549 /*(Kbps)*/,
		/*21 Clients*/ 9367.587 /*(Kbps)*/,
		/*22 Clients*/ 8747.625 /*(Kbps)*/,
		/*23 Clients*/ 8127.663 /*(Kbps)*/,
		/*24 Clients*/ 7907.701 /*(Kbps)*/,
		/*25 Clients*/ 7887.739 /*(Kbps)*/,
		/*26 Clients*/ 7690.831 /*(Kbps)*/,
		/*27 Clients*/ 7393.922 /*(Kbps)*/,
		/*28 Clients*/ 7297.014 /*(Kbps)*/,
		/*29 Clients*/ 7100.106 /*(Kbps)*/,
		/*30 Clients*/ 6903.197 /*(Kbps)*/,
		/*31 Clients*/ 6701.986 /*(Kbps)*/,
		/*32 Clients*/ 6500.776 /*(Kbps)*/,
		/*33 Clients*/ 6399.565 /*(Kbps)*/,
		/*34 Clients*/ 6098.354 /*(Kbps)*/,
		/*35 Clients*/ 5897.143 /*(Kbps)*/,
		/*36 Clients*/ 5552.127 /*(Kbps)*/,
		/*37 Clients*/ 5207.111 /*(Kbps)*/,
		/*38 Clients*/ 4862.096 /*(Kbps)*/,
		/*39 Clients*/ 4517.080 /*(Kbps)*/,
		/*40 Clients*/ 4172.064 /*(Kbps)*/,
		/*41 Clients*/ 4092.922 /*(Kbps)*/,
		/*42 Clients*/ 4013.781 /*(Kbps)*/,
		/*43 Clients*/ 3934.639 /*(Kbps)*/,
		/*44 Clients*/ 3855.498 /*(Kbps)*/,
		/*45 Clients*/ 3776.356 /*(Kbps)*/,
		/*46 Clients*/ 3697.215 /*(Kbps)*/,
		/*47 Clients*/ 3618.073 /*(Kbps)*/,
		/*48 Clients*/ 3538.932 /*(Kbps)*/,
		/*49 Clients*/ 3459.790 /*(Kbps)*/,
		/*50 Clients*/ 3380.649 /*(Kbps)*/,
		/*51 Clients*/ 3274.611 /*(Kbps)*/,
		/*52 Clients*/ 3168.573 /*(Kbps)*/,
		/*53 Clients*/ 3062.536 /*(Kbps)*/,
		/*54 Clients*/ 2956.498 /*(Kbps)*/,
		/*55 Clients*/ 2850.461 /*(Kbps)*/,
		/*56 Clients*/ 2744.423 /*(Kbps)*/,
		/*57 Clients*/ 2638.386 /*(Kbps)*/,
		/*58 Clients*/ 2532.348 /*(Kbps)*/,
		/*59 Clients*/ 2426.310 /*(Kbps)*/,
		/*60 Clients*/ 2320.273 /*(Kbps)*/,
		/*61 Clients*/ 2283.828 /*(Kbps)*/,
		/*62 Clients*/ 2247.383 /*(Kbps)*/,
		/*63 Clients*/ 2210.939 /*(Kbps)*/,
		/*64 Clients*/ 2174.494 /*(Kbps)*/,
		/*65 Clients*/ 2138.049 /*(Kbps)*/,
		/*66 Clients*/ 2101.604 /*(Kbps)*/,
		/*67 Clients*/ 2065.160 /*(Kbps)*/,
		/*68 Clients*/ 2028.715 /*(Kbps)*/,
		/*69 Clients*/ 1992.270 /*(Kbps)*/,
		/*70 Clients*/ 1955.825 /*(Kbps)*/,
		/*71 Clients*/ 1946.788 /*(Kbps)*/,
		/*72 Clients*/ 1937.751 /*(Kbps)*/,
		/*73 Clients*/ 1928.714 /*(Kbps)*/,
		/*74 Clients*/ 1919.677 /*(Kbps)*/,
		/*75 Clients*/ 1910.640 /*(Kbps)*/,
		/*76 Clients*/ 1901.603 /*(Kbps)*/,
		/*77 Clients*/ 1892.566 /*(Kbps)*/,
		/*78 Clients*/ 1883.529 /*(Kbps)*/,
		/*79 Clients*/ 1874.492 /*(Kbps)*/,
		/*80 Clients*/ 1865.455 /*(Kbps)*/,
		/*81 Clients*/ 1833.185 /*(Kbps)*/,
		/*82 Clients*/ 1800.915 /*(Kbps)*/,
		/*83 Clients*/ 1768.645 /*(Kbps)*/,
		/*84 Clients*/ 1736.375 /*(Kbps)*/,
		/*85 Clients*/ 1704.106 /*(Kbps)*/,
		/*86 Clients*/ 1671.836 /*(Kbps)*/,
		/*87 Clients*/ 1639.566 /*(Kbps)*/,
		/*88 Clients*/ 1607.296 /*(Kbps)*/,
		/*89 Clients*/ 1575.026 /*(Kbps)*/,
		/*90 Clients*/ 1542.756 /*(Kbps)*/,
		/*91 Clients*/ 1538.544 /*(Kbps)*/,
		/*92 Clients*/ 1534.331 /*(Kbps)*/,
		/*93 Clients*/ 1530.119 /*(Kbps)*/,
		/*94 Clients*/ 1525.906 /*(Kbps)*/,
		/*95 Clients*/ 1521.694 /*(Kbps)*/,
		/*96 Clients*/ 1517.481 /*(Kbps)*/,
		/*97 Clients*/ 1513.269 /*(Kbps)*/,
		/*98 Clients*/ 1509.056 /*(Kbps)*/,
		/*99 Clients*/ 1504.844 /*(Kbps)*/,
		/*100 Clients*/ 1500.631 /*(Kbps)*/,
		//added by Oleg using extrapolation
		/*101 Clients*/ 1486.61557299114 /*(Kbps)*/,
		/*102 Clients*/ 1472.62191030317 /*(Kbps)*/,
		/*103 Clients*/ 1458.62824761518 /*(Kbps)*/,
		/*104 Clients*/ 1444.6345849272 /*(Kbps)*/,
		/*105 Clients*/ 1430.64092223921 /*(Kbps)*/,
		/*106 Clients*/ 1416.64725955123 /*(Kbps)*/,
		/*107 Clients*/ 1402.65359686324 /*(Kbps)*/,
		/*108 Clients*/ 1388.65993417527 /*(Kbps)*/,
		/*109 Clients*/ 1374.66627148728 /*(Kbps)*/,
		/*110 Clients*/ 1360.6726087993 /*(Kbps)*/,
		/*111 Clients*/ 1346.67894611131 /*(Kbps)*/,
		/*112 Clients*/ 1332.68528342333 /*(Kbps)*/,
		/*113 Clients*/ 1318.69162073534 /*(Kbps)*/,
		/*114 Clients*/ 1304.69795804736 /*(Kbps)*/,
		/*115 Clients*/ 1290.70429535937 /*(Kbps)*/,
		/*116 Clients*/ 1276.7106326714 /*(Kbps)*/,
		/*117 Clients*/ 1262.71696998341 /*(Kbps)*/,
		/*118 Clients*/ 1248.72330729543 /*(Kbps)*/,
		/*119 Clients*/ 1234.72964460744 /*(Kbps)*/,
		/*120 Clients*/ 1220.73598191946 /*(Kbps)*/,
		/*121 Clients*/ 1206.74231923147 /*(Kbps)*/,
		/*122 Clients*/ 1192.7486565435 /*(Kbps)*/,
		/*123 Clients*/ 1178.75499385551 /*(Kbps)*/,
		/*124 Clients*/ 1164.76133116753 /*(Kbps)*/,
		/*125 Clients*/ 1150.76766847954 /*(Kbps)*/,
		/*126 Clients*/ 1136.77400579156 /*(Kbps)*/,
		/*127 Clients*/ 1122.78034310357 /*(Kbps)*/,
		/*128 Clients*/ 1108.7866804156 /*(Kbps)*/,
		/*129 Clients*/ 1094.7930177276 /*(Kbps)*/,
		/*130 Clients*/ 1080.79935503963 /*(Kbps)*/,
		/*131 Clients*/ 1066.80569235164 /*(Kbps)*/,
		/*132 Clients*/ 1052.81202966366 /*(Kbps)*/,
		/*133 Clients*/ 1038.81836697567 /*(Kbps)*/,
		/*134 Clients*/ 1024.82470428769 /*(Kbps)*/,
		/*135 Clients*/ 1010.8310415997 /*(Kbps)*/,
		/*136 Clients*/ 996.837378911727 /*(Kbps)*/,
		/*137 Clients*/ 982.843716223737 /*(Kbps)*/,
		/*138 Clients*/ 968.85005353576 /*(Kbps)*/,
		/*139 Clients*/ 954.856390847769 /*(Kbps)*/,
		/*140 Clients*/ 940.862728159793 /*(Kbps)*/,
		/*141 Clients*/ 926.869065471802 /*(Kbps)*/,
		/*142 Clients*/ 912.875402783826 /*(Kbps)*/,
		/*143 Clients*/ 898.881740095835 /*(Kbps)*/,
		/*144 Clients*/ 884.888077407859 /*(Kbps)*/,
		/*145 Clients*/ 870.894414719868 /*(Kbps)*/,
		/*146 Clients*/ 856.900752031891 /*(Kbps)*/,
		/*147 Clients*/ 842.907089343901 /*(Kbps)*/,
		/*148 Clients*/ 828.913426655924 /*(Kbps)*/,
		/*149 Clients*/ 814.919763967934 /*(Kbps)*/,
		/*150 Clients*/ 800.926101279957 /*(Kbps)*/,
		/*151 Clients*/ 786.932438591967 /*(Kbps)*/,
		/*152 Clients*/ 772.93877590399 /*(Kbps)*/,
		/*153 Clients*/ 758.945113216 /*(Kbps)*/,
		/*154 Clients*/ 744.951450528023 /*(Kbps)*/,
		/*155 Clients*/ 730.957787840033 /*(Kbps)*/,
		/*156 Clients*/ 716.964125152056 /*(Kbps)*/,
		/*157 Clients*/ 702.970462464066 /*(Kbps)*/,
		/*158 Clients*/ 688.976799776089 /*(Kbps)*/,
		/*159 Clients*/ 674.983137088099 /*(Kbps)*/,
		/*160 Clients*/ 660.989474400122 /*(Kbps)*/,
		/*161 Clients*/ 646.995811712132 /*(Kbps)*/,
		/*162 Clients*/ 633.002149024155 /*(Kbps)*/,
		/*163 Clients*/ 619.008486336164 /*(Kbps)*/,
		/*164 Clients*/ 605.014823648188 /*(Kbps)*/,
		/*165 Clients*/ 591.021160960197 /*(Kbps)*/,
		/*166 Clients*/ 577.027498272221 /*(Kbps)*/,
		/*167 Clients*/ 563.03383558423 /*(Kbps)*/,
		/*168 Clients*/ 549.040172896254 /*(Kbps)*/,
		/*169 Clients*/ 535.046510208263 /*(Kbps)*/,
		/*170 Clients*/ 521.052847520286 /*(Kbps)*/,
		/*171 Clients*/ 507.059184832296 /*(Kbps)*/,
		/*172 Clients*/ 493.065522144319 /*(Kbps)*/,
		/*173 Clients*/ 479.071859456329 /*(Kbps)*/,
		/*174 Clients*/ 465.078196768352 /*(Kbps)*/,
		/*175 Clients*/ 451.084534080362 /*(Kbps)*/,
		/*176 Clients*/ 437.090871392385 /*(Kbps)*/,
		/*177 Clients*/ 423.097208704395 /*(Kbps)*/,
		/*178 Clients*/ 409.103546016418 /*(Kbps)*/,
		/*179 Clients*/ 395.109883328428 /*(Kbps)*/,
		/*180 Clients*/ 381.116220640451 /*(Kbps)*/,
		/*181 Clients*/ 367.122557952461 /*(Kbps)*/,
		/*182 Clients*/ 353.128895264484 /*(Kbps)*/,
		/*183 Clients*/ 339.135232576494 /*(Kbps)*/,
		/*184 Clients*/ 325.141569888517 /*(Kbps)*/,
		/*185 Clients*/ 311.147907200527 /*(Kbps)*/,
		/*186 Clients*/ 297.15424451255 /*(Kbps)*/,
		/*187 Clients*/ 283.16058182456 /*(Kbps)*/,
		/*188 Clients*/ 269.166919136583 /*(Kbps)*/,
		/*189 Clients*/ 255.173256448592 /*(Kbps)*/,
		/*190 Clients*/ 241.179593760616 /*(Kbps)*/,
		/*191 Clients*/ 227.185931072625 /*(Kbps)*/,
		/*192 Clients*/ 213.192268384649 /*(Kbps)*/,
		/*193 Clients*/ 199.198605696658 /*(Kbps)*/,
		/*194 Clients*/ 185.204943008682 /*(Kbps)*/,
		/*195 Clients*/ 171.211280320691 /*(Kbps)*/,
		/*196 Clients*/ 157.217617632715 /*(Kbps)*/,
		/*197 Clients*/ 143.223954944724 /*(Kbps)*/,
		/*198 Clients*/ 129.230292256747 /*(Kbps)*/,
		/*199 Clients*/ 115.236629568757 /*(Kbps)*/

	};
	
	public static final double[] experimentalWanDelay = {
		/*1 Client*/ 20703.973 /*(Kbps)*/,
		/*2 Clients*/ 12023.957 /*(Kbps)*/,
		/*3 Clients*/ 9887.785 /*(Kbps)*/,
		/*4 Clients*/ 8915.775 /*(Kbps)*/,
		/*5 Clients*/ 8259.277 /*(Kbps)*/,
		/*6 Clients*/ 7560.574 /*(Kbps)*/,
		/*7 Clients*/ 7262.140 /*(Kbps)*/,
		/*8 Clients*/ 7155.361 /*(Kbps)*/,
		/*9 Clients*/ 7041.153 /*(Kbps)*/,
		/*10 Clients*/ 6994.595 /*(Kbps)*/,
		/*11 Clients*/ 6653.232 /*(Kbps)*/,
		/*12 Clients*/ 6111.868 /*(Kbps)*/,
		/*13 Clients*/ 5570.505 /*(Kbps)*/,
		/*14 Clients*/ 5029.142 /*(Kbps)*/,
		/*15 Clients*/ 4487.779 /*(Kbps)*/,
		/*16 Clients*/ 3899.729 /*(Kbps)*/,
		/*17 Clients*/ 3311.680 /*(Kbps)*/,
		/*18 Clients*/ 2723.631 /*(Kbps)*/,
		/*19 Clients*/ 2135.582 /*(Kbps)*/,
		/*20 Clients*/ 1547.533 /*(Kbps)*/,
		/*21 Clients*/ 1500.252 /*(Kbps)*/,
		/*22 Clients*/ 1452.972 /*(Kbps)*/,
		/*23 Clients*/ 1405.692 /*(Kbps)*/,
		/*24 Clients*/ 1358.411 /*(Kbps)*/,
		/*25 Clients*/ 1311.131 /*(Kbps)*/
	};
	
	public SampleNetworkModel(int _numberOfMobileDevices, String _simScenario) {
		super(_numberOfMobileDevices, _simScenario);
	}

	@Override
	public void initialize() {
		wanClients = new int[SimSettings.getInstance().getNumOfEdgeDatacenters()];  //we have one access point for each datacenter
		wlanClients = new int[SimSettings.getInstance().getNumOfEdgeDatacenters()];  //we have one access point for each datacenter

		int numOfApp = SimSettings.getInstance().getTaskLookUpTable().length;
		SimSettings SS = SimSettings.getInstance();
		for(int taskIndex=0; taskIndex<numOfApp; taskIndex++) {
			if(SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.USAGE_PERCENTAGE] == 0) {
				SimLogger.printLine("Usage percantage of task " + taskIndex + " is 0! Terminating simulation...");
				System.exit(0);
			}
			else{
				double weight = SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.USAGE_PERCENTAGE]/(double)100;
				
				//assume half of the tasks use the MAN at the beginning
				ManPoissonMeanForDownload += ((SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.POISSON_INTERARRIVAL])*weight) * 4;
				ManPoissonMeanForUpload = ManPoissonMeanForDownload;
				
				avgManTaskInputSize += SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.DATA_UPLOAD]*weight;
				avgManTaskOutputSize += SS.getTaskLookUpTable()[taskIndex][LoadGeneratorModel.DATA_DOWNLOAD]*weight;
			}
		}

		ManPoissonMeanForDownload = ManPoissonMeanForDownload/numOfApp;
		ManPoissonMeanForUpload = ManPoissonMeanForUpload/numOfApp;
		avgManTaskInputSize = avgManTaskInputSize/numOfApp;
		avgManTaskOutputSize = avgManTaskOutputSize/numOfApp;
		
		lastMM1QueeuUpdateTime = SimSettings.CLIENT_ACTIVITY_START_TIME;
		totalManTaskOutputSize = 0;
		numOfManTaskForDownload = 0;
		totalManTaskInputSize = 0;
		numOfManTaskForUpload = 0;
	}

    /**
    * source device is always mobile device in our simulation scenarios!
    */
	@Override
	public double getUploadDelay(int sourceDeviceId, int destDeviceId, Task task) {
		double delay = 0;
		
		//special case for man communication
		//TODO:check this case
		if(sourceDeviceId == destDeviceId && sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
			return delay = getManUploadDelay();
		}
		
		Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(sourceDeviceId,CloudSim.clock());

		//mobile device to cloud server
		if(destDeviceId == SimSettings.CLOUD_DATACENTER_ID){
			delay = getWanUploadDelay(accessPointLocation, task.getCloudletFileSize());
		}
		//mobile device to edge device (wifi access point)
		else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
			delay = getWlanUploadDelay(accessPointLocation, task.getCloudletFileSize());
		}
		
		return delay;
	}

    /**
    * destination device is always mobile device in our simulation scenarios!
    */
	@Override
	public double getDownloadDelay(int sourceDeviceId, int destDeviceId, Task task) {
		double delay = 0;
		
		//special case for man communication
		if(sourceDeviceId == destDeviceId && sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID){
			return delay = getManDownloadDelay();
		}
		Location accessPointLocation = SimManager.getInstance().getMobilityModel().getLocation(destDeviceId,CloudSim.clock());
		
		//cloud server to mobile device
		if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID){
			delay = getWanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
		}
		//edge device (wifi access point) to mobile device
		else{
			delay = getWlanDownloadDelay(accessPointLocation, task.getCloudletOutputSize());
		}
		
		return delay;
	}

	@Override
	public void uploadStarted(Location accessPointLocation, int destDeviceId) {
		if(destDeviceId == SimSettings.CLOUD_DATACENTER_ID)
			wanClients[accessPointLocation.getServingWlanId()]++;
		else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID)
			wlanClients[accessPointLocation.getServingWlanId()]++;
		else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID+1)
			manClients++;
		else {
			SimLogger.printLine("Error - unknown device id in uploadStarted(). Terminating simulation...");
			System.exit(0);
		}
	}

	@Override
	public void uploadFinished(Location accessPointLocation, int destDeviceId) {
		if(destDeviceId == SimSettings.CLOUD_DATACENTER_ID)
			wanClients[accessPointLocation.getServingWlanId()]--;
		else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID)
			wlanClients[accessPointLocation.getServingWlanId()]--;
		else if (destDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID+1)
			manClients--;
		else {
			SimLogger.printLine("Error - unknown device id in uploadFinished(). Terminating simulation...");
			System.exit(0);
		}
	}

	@Override
	public void downloadStarted(Location accessPointLocation, int sourceDeviceId) {
		if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID)
			wanClients[accessPointLocation.getServingWlanId()]++;
		else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
			wlanClients[accessPointLocation.getServingWlanId()]++;
		}
		else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID+1)
			manClients++;
		else {
			SimLogger.printLine("Error - unknown device id in downloadStarted(). Terminating simulation...");
			System.exit(0);
		}
//		System.out.println("manClients: " + manClients); //To remove
	}

	@Override
	public void downloadFinished(Location accessPointLocation, int sourceDeviceId) {
		if(sourceDeviceId == SimSettings.CLOUD_DATACENTER_ID)
			wanClients[accessPointLocation.getServingWlanId()]--;
		else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
			wlanClients[accessPointLocation.getServingWlanId()]--;
		}
		else if(sourceDeviceId == SimSettings.GENERIC_EDGE_DEVICE_ID+1)
			manClients--;
		else {
			SimLogger.printLine("Error - unknown device id in downloadFinished(). Terminating simulation...");
			System.exit(0);
		}
//		System.out.println("Users in " + accessPointLocation.getServingWlanId() + ": " + wlanClients[accessPointLocation.getServingWlanId()]); //TO remove
	}

	double getWlanDownloadDelay(Location accessPointLocation, double dataSize) {
		int numOfWlanUser = wlanClients[accessPointLocation.getServingWlanId()];
		double taskSizeInKb = dataSize * (double)8; //KB to Kb
		double result=0;
		
		if(numOfWlanUser < experimentalWlanDelay.length)
			result = taskSizeInKb /*Kb*/ / (experimentalWlanDelay[numOfWlanUser] * (double) 3 ) /*Kbps*/; //802.11ac is around 3 times faster than 802.11n

		//System.out.println("--> " + numOfWlanUser + " user, " + taskSizeInKb + " KB, " +result + " sec");
		return result;
	}
	
	//wlan upload and download delay is symmetric in this model
	private double getWlanUploadDelay(Location accessPointLocation, double dataSize) {
		return getWlanDownloadDelay(accessPointLocation, dataSize);
	}
	
	double getWanDownloadDelay(Location accessPointLocation, double dataSize) {
		int numOfWanUser = wanClients[accessPointLocation.getServingWlanId()];
		double taskSizeInKb = dataSize * (double)8; //KB to Kb
		double result=0;
		
		if(numOfWanUser < experimentalWanDelay.length)
			result = taskSizeInKb /*Kb*/ / (experimentalWanDelay[numOfWanUser]) /*Kbps*/;
		
		//System.out.println("--> " + numOfWanUser + " user, " + taskSizeInKb + " KB, " +result + " sec");
		
		return result;
	}
	
	//wan upload and download delay is symmetric in this model
	private double getWanUploadDelay(Location accessPointLocation, double dataSize) {
		return getWanDownloadDelay(accessPointLocation, dataSize);
	}
	
	private double calculateMM1(double propogationDelay, double bandwidth /*Kbps*/, double PoissonMean, double avgTaskSize /*KB*/, int deviceCount){
		double mu=0, lamda=0;
		
		avgTaskSize = avgTaskSize * 8; //convert from KB to Kb

        lamda = ((double)1/(double)PoissonMean); //task per seconds
		mu = bandwidth /*Kbps*/ / avgTaskSize /*Kb*/; //task per seconds
		//Oleg: Little's law: total time a customer spends in the system
		//lamda*(double)deviceCount = (numOfManTaskForDownload/lastInterval)
		double result = (double)1 / (mu-lamda*(double)deviceCount);
		
		if(result < 0)
		{
			System.out.println("Delay is negative in calculateMM1");
			return 0;
		}

//		System.out.println("delay: " + result); //To remove
		result += propogationDelay;
		if(result>15)
			System.out.println("Delay too large in calculateMM1");
		
		return (result > 15) ? 0 : result;
	}
	
	double getManDownloadDelay() {
		double result = calculateMM1(SimSettings.getInstance().getInternalLanDelay(),
				MAN_BW,
				ManPoissonMeanForDownload,
				avgManTaskOutputSize,
				numberOfMobileDevices);
		totalManTaskOutputSize += avgManTaskOutputSize;
		numOfManTaskForDownload++;
//			System.out.println("totalManTaskOutputSize: " + totalManTaskOutputSize + " numOfManTaskForDownload: "+ numOfManTaskForDownload); //TO remove
		//System.out.println("--> " + SimManager.getInstance().getNumOfMobileDevice() + " user, " +result + " sec");
		return result;
	}
	
	private double getManUploadDelay() {
		double result = calculateMM1(SimSettings.getInstance().getInternalLanDelay(),
				MAN_BW,
				ManPoissonMeanForUpload,
				avgManTaskInputSize,
				numberOfMobileDevices);
		
		totalManTaskInputSize += avgManTaskInputSize;
		numOfManTaskForUpload++;

		//System.out.println(CloudSim.clock() + " -> " + SimManager.getInstance().getNumOfMobileDevice() + " user, " + result + " sec");
		
		return result;
	}
	
	public void updateMM1QueeuModel(){
		double lastInterval = CloudSim.clock() - lastMM1QueeuUpdateTime;
		lastMM1QueeuUpdateTime = CloudSim.clock();

		if(numOfManTaskForDownload != 0){
			ManPoissonMeanForDownload = lastInterval / (numOfManTaskForDownload / (double)numberOfMobileDevices);
			avgManTaskOutputSize = totalManTaskOutputSize / numOfManTaskForDownload;
//			System.out.println("numOfManTaskForDownload: " + numOfManTaskForDownload + " avgManTaskOutputSize: "+ avgManTaskOutputSize); //TO remove
		}
		if(numOfManTaskForUpload != 0){
			ManPoissonMeanForUpload = lastInterval / (numOfManTaskForUpload / (double)numberOfMobileDevices);
			avgManTaskInputSize = totalManTaskInputSize / numOfManTaskForUpload;
		}
		
		totalManTaskOutputSize = 0;
		numOfManTaskForDownload = 0;
		totalManTaskInputSize = 0;
		numOfManTaskForUpload = 0;
	}
}
