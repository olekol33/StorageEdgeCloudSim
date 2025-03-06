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
			/*2 Client*/ 66595.6305 /*(Kbps)*/,
			/*3 Client*/ 45150.982 /*(Kbps)*/,
			/*4 Client*/ 37727.3115 /*(Kbps)*/,
			/*5 Client*/ 30303.641 /*(Kbps)*/,
			/*6 Client*/ 28960.426 /*(Kbps)*/,
			/*7 Client*/ 27617.211 /*(Kbps)*/,
			/*8 Client*/ 26242.9135 /*(Kbps)*/,
			/*9 Client*/ 24868.616 /*(Kbps)*/,
			/*10 Client*/ 23555.456 /*(Kbps)*/,
			/*11 Client*/ 22242.296 /*(Kbps)*/,
			/*12 Client*/ 21383.18 /*(Kbps)*/,
			/*13 Client*/ 20524.064 /*(Kbps)*/,
			/*14 Client*/ 19634.4765 /*(Kbps)*/,
			/*15 Client*/ 18744.889 /*(Kbps)*/,
			/*16 Client*/ 17901.858 /*(Kbps)*/,
			/*17 Client*/ 17058.827 /*(Kbps)*/,
			/*18 Client*/ 16374.641 /*(Kbps)*/,
			/*19 Client*/ 15690.455 /*(Kbps)*/,
			/*20 Client*/ 14909.0995 /*(Kbps)*/,
			/*21 Client*/ 14127.744 /*(Kbps)*/,
			/*22 Client*/ 13825.076 /*(Kbps)*/,
			/*23 Client*/ 13522.408 /*(Kbps)*/,
			/*24 Client*/ 13350.0195 /*(Kbps)*/,
			/*25 Client*/ 13177.631 /*(Kbps)*/,
			/*26 Client*/ 12994.4805 /*(Kbps)*/,
			/*27 Client*/ 12811.33 /*(Kbps)*/,
			/*28 Client*/ 12697.8585 /*(Kbps)*/,
			/*29 Client*/ 12584.387 /*(Kbps)*/,
			/*30 Client*/ 12359.774 /*(Kbps)*/,
			/*31 Client*/ 12135.161 /*(Kbps)*/,
			/*32 Client*/ 11920.3995 /*(Kbps)*/,
			/*33 Client*/ 11705.638 /*(Kbps)*/,
			/*34 Client*/ 11490.877 /*(Kbps)*/,
			/*35 Client*/ 11276.116 /*(Kbps)*/,
			/*36 Client*/ 11061.355 /*(Kbps)*/,
			/*37 Client*/ 10846.594 /*(Kbps)*/,
			/*38 Client*/ 10631.8325 /*(Kbps)*/,
			/*39 Client*/ 10417.071 /*(Kbps)*/,
			/*40 Client*/ 10202.31 /*(Kbps)*/,
			/*41 Client*/ 9987.549 /*(Kbps)*/,
			/*42 Client*/ 9677.568 /*(Kbps)*/,
			/*43 Client*/ 9367.587 /*(Kbps)*/,
			/*44 Client*/ 9057.606 /*(Kbps)*/,
			/*45 Client*/ 8747.625 /*(Kbps)*/,
			/*46 Client*/ 8437.644 /*(Kbps)*/,
			/*47 Client*/ 8127.663 /*(Kbps)*/,
			/*48 Client*/ 8017.682 /*(Kbps)*/,
			/*49 Client*/ 7907.701 /*(Kbps)*/,
			/*50 Client*/ 7897.72 /*(Kbps)*/,
			/*51 Client*/ 7887.739 /*(Kbps)*/,
			/*52 Client*/ 7789.285 /*(Kbps)*/,
			/*53 Client*/ 7690.831 /*(Kbps)*/,
			/*54 Client*/ 7542.3765 /*(Kbps)*/,
			/*55 Client*/ 7393.922 /*(Kbps)*/,
			/*56 Client*/ 7345.468 /*(Kbps)*/,
			/*57 Client*/ 7297.014 /*(Kbps)*/,
			/*58 Client*/ 7198.56 /*(Kbps)*/,
			/*59 Client*/ 7100.106 /*(Kbps)*/,
			/*60 Client*/ 7001.6515 /*(Kbps)*/,
			/*61 Client*/ 6903.197 /*(Kbps)*/,
			/*62 Client*/ 6802.5915 /*(Kbps)*/,
			/*63 Client*/ 6701.986 /*(Kbps)*/,
			/*64 Client*/ 6601.381 /*(Kbps)*/,
			/*65 Client*/ 6500.776 /*(Kbps)*/,
			/*66 Client*/ 6450.1705 /*(Kbps)*/,
			/*67 Client*/ 6399.565 /*(Kbps)*/,
			/*68 Client*/ 6248.9595 /*(Kbps)*/,
			/*69 Client*/ 6098.354 /*(Kbps)*/,
			/*70 Client*/ 5997.7485 /*(Kbps)*/,
			/*71 Client*/ 5897.143 /*(Kbps)*/,
			/*72 Client*/ 5724.635 /*(Kbps)*/,
			/*73 Client*/ 5552.127 /*(Kbps)*/,
			/*74 Client*/ 5379.619 /*(Kbps)*/,
			/*75 Client*/ 5207.111 /*(Kbps)*/,
			/*76 Client*/ 5034.6035 /*(Kbps)*/,
			/*77 Client*/ 4862.096 /*(Kbps)*/,
			/*78 Client*/ 4689.588 /*(Kbps)*/,
			/*79 Client*/ 4517.08 /*(Kbps)*/,
			/*80 Client*/ 4344.572 /*(Kbps)*/,
			/*81 Client*/ 4172.064 /*(Kbps)*/,
			/*82 Client*/ 4132.493 /*(Kbps)*/,
			/*83 Client*/ 4092.922 /*(Kbps)*/,
			/*84 Client*/ 4053.3515 /*(Kbps)*/,
			/*85 Client*/ 4013.781 /*(Kbps)*/,
			/*86 Client*/ 3974.21 /*(Kbps)*/,
			/*87 Client*/ 3934.639 /*(Kbps)*/,
			/*88 Client*/ 3895.0685 /*(Kbps)*/,
			/*89 Client*/ 3855.498 /*(Kbps)*/,
			/*90 Client*/ 3815.927 /*(Kbps)*/,
			/*91 Client*/ 3776.356 /*(Kbps)*/,
			/*92 Client*/ 3736.7855 /*(Kbps)*/,
			/*93 Client*/ 3697.215 /*(Kbps)*/,
			/*94 Client*/ 3657.644 /*(Kbps)*/,
			/*95 Client*/ 3618.073 /*(Kbps)*/,
			/*96 Client*/ 3578.5025 /*(Kbps)*/,
			/*97 Client*/ 3538.932 /*(Kbps)*/,
			/*98 Client*/ 3499.361 /*(Kbps)*/,
			/*99 Client*/ 3459.79 /*(Kbps)*/,
			/*100 Client*/ 3420.2195 /*(Kbps)*/,
			/*101 Client*/ 3380.649 /*(Kbps)*/,
			/*102 Client*/ 3327.63 /*(Kbps)*/,
			/*103 Client*/ 3274.611 /*(Kbps)*/,
			/*104 Client*/ 3221.592 /*(Kbps)*/,
			/*105 Client*/ 3168.573 /*(Kbps)*/,
			/*106 Client*/ 3115.5545 /*(Kbps)*/,
			/*107 Client*/ 3062.536 /*(Kbps)*/,
			/*108 Client*/ 3009.517 /*(Kbps)*/,
			/*109 Client*/ 2956.498 /*(Kbps)*/,
			/*110 Client*/ 2903.4795 /*(Kbps)*/,
			/*111 Client*/ 2850.461 /*(Kbps)*/,
			/*112 Client*/ 2797.442 /*(Kbps)*/,
			/*113 Client*/ 2744.423 /*(Kbps)*/,
			/*114 Client*/ 2691.4045 /*(Kbps)*/,
			/*115 Client*/ 2638.386 /*(Kbps)*/,
			/*116 Client*/ 2585.367 /*(Kbps)*/,
			/*117 Client*/ 2532.348 /*(Kbps)*/,
			/*118 Client*/ 2479.329 /*(Kbps)*/,
			/*119 Client*/ 2426.31 /*(Kbps)*/,
			/*120 Client*/ 2373.2915 /*(Kbps)*/,
			/*121 Client*/ 2320.273 /*(Kbps)*/,
			/*122 Client*/ 2302.0505 /*(Kbps)*/,
			/*123 Client*/ 2283.828 /*(Kbps)*/,
			/*124 Client*/ 2265.6055 /*(Kbps)*/,
			/*125 Client*/ 2247.383 /*(Kbps)*/,
			/*126 Client*/ 2229.161 /*(Kbps)*/,
			/*127 Client*/ 2210.939 /*(Kbps)*/,
			/*128 Client*/ 2192.7165 /*(Kbps)*/,
			/*129 Client*/ 2174.494 /*(Kbps)*/,
			/*130 Client*/ 2156.2715 /*(Kbps)*/,
			/*131 Client*/ 2138.049 /*(Kbps)*/,
			/*132 Client*/ 2119.8265 /*(Kbps)*/,
			/*133 Client*/ 2101.604 /*(Kbps)*/,
			/*134 Client*/ 2083.382 /*(Kbps)*/,
			/*135 Client*/ 2065.16 /*(Kbps)*/,
			/*136 Client*/ 2046.9375 /*(Kbps)*/,
			/*137 Client*/ 2028.715 /*(Kbps)*/,
			/*138 Client*/ 2010.4925 /*(Kbps)*/,
			/*139 Client*/ 1992.27 /*(Kbps)*/,
			/*140 Client*/ 1974.0475 /*(Kbps)*/,
			/*141 Client*/ 1955.825 /*(Kbps)*/,
			/*142 Client*/ 1951.3065 /*(Kbps)*/,
			/*143 Client*/ 1946.788 /*(Kbps)*/,
			/*144 Client*/ 1942.2695 /*(Kbps)*/,
			/*145 Client*/ 1937.751 /*(Kbps)*/,
			/*146 Client*/ 1933.2325 /*(Kbps)*/,
			/*147 Client*/ 1928.714 /*(Kbps)*/,
			/*148 Client*/ 1924.1955 /*(Kbps)*/,
			/*149 Client*/ 1919.677 /*(Kbps)*/,
			/*150 Client*/ 1915.1585 /*(Kbps)*/,
			/*151 Client*/ 1910.64 /*(Kbps)*/,
			/*152 Client*/ 1906.1215 /*(Kbps)*/,
			/*153 Client*/ 1901.603 /*(Kbps)*/,
			/*154 Client*/ 1897.0845 /*(Kbps)*/,
			/*155 Client*/ 1892.566 /*(Kbps)*/,
			/*156 Client*/ 1888.0475 /*(Kbps)*/,
			/*157 Client*/ 1883.529 /*(Kbps)*/,
			/*158 Client*/ 1879.0105 /*(Kbps)*/,
			/*159 Client*/ 1874.492 /*(Kbps)*/,
			/*160 Client*/ 1869.9735 /*(Kbps)*/,
			/*161 Client*/ 1865.455 /*(Kbps)*/,
			/*162 Client*/ 1849.32 /*(Kbps)*/,
			/*163 Client*/ 1833.185 /*(Kbps)*/,
			/*164 Client*/ 1817.05 /*(Kbps)*/,
			/*165 Client*/ 1800.915 /*(Kbps)*/,
			/*166 Client*/ 1784.78 /*(Kbps)*/,
			/*167 Client*/ 1768.645 /*(Kbps)*/,
			/*168 Client*/ 1752.51 /*(Kbps)*/,
			/*169 Client*/ 1736.375 /*(Kbps)*/,
			/*170 Client*/ 1720.2405 /*(Kbps)*/,
			/*171 Client*/ 1704.106 /*(Kbps)*/,
			/*172 Client*/ 1687.971 /*(Kbps)*/,
			/*173 Client*/ 1671.836 /*(Kbps)*/,
			/*174 Client*/ 1655.701 /*(Kbps)*/,
			/*175 Client*/ 1639.566 /*(Kbps)*/,
			/*176 Client*/ 1623.431 /*(Kbps)*/,
			/*177 Client*/ 1607.296 /*(Kbps)*/,
			/*178 Client*/ 1591.161 /*(Kbps)*/,
			/*179 Client*/ 1575.026 /*(Kbps)*/,
			/*180 Client*/ 1558.891 /*(Kbps)*/,
			/*181 Client*/ 1542.756 /*(Kbps)*/,
			/*182 Client*/ 1540.65 /*(Kbps)*/,
			/*183 Client*/ 1538.544 /*(Kbps)*/,
			/*184 Client*/ 1536.4375 /*(Kbps)*/,
			/*185 Client*/ 1534.331 /*(Kbps)*/,
			/*186 Client*/ 1532.225 /*(Kbps)*/,
			/*187 Client*/ 1530.119 /*(Kbps)*/,
			/*188 Client*/ 1528.0125 /*(Kbps)*/,
			/*189 Client*/ 1525.906 /*(Kbps)*/,
			/*190 Client*/ 1523.8 /*(Kbps)*/,
			/*191 Client*/ 1521.694 /*(Kbps)*/,
			/*192 Client*/ 1519.5875 /*(Kbps)*/,
			/*193 Client*/ 1517.481 /*(Kbps)*/,
			/*194 Client*/ 1515.375 /*(Kbps)*/,
			/*195 Client*/ 1513.269 /*(Kbps)*/,
			/*196 Client*/ 1511.1625 /*(Kbps)*/,
			/*197 Client*/ 1509.056 /*(Kbps)*/,
			/*198 Client*/ 1506.95 /*(Kbps)*/,
			/*199 Client*/ 1504.844 /*(Kbps)*/,
			/*200 Client*/ 1502.7375 /*(Kbps)*/,
			/*201 Client*/ 1500.631 /*(Kbps)*/

/*		*//*1 Client*//* 88040.279 *//*(Kbps)*//*,
		*//*2 Clients*//* 45150.982 *//*(Kbps)*//*,
		*//*3 Clients*//* 30303.641 *//*(Kbps)*//*,
		*//*4 Clients*//* 27617.211 *//*(Kbps)*//*,
		*//*5 Clients*//* 24868.616 *//*(Kbps)*//*,
		*//*6 Clients*//* 22242.296 *//*(Kbps)*//*,
		*//*7 Clients*//* 20524.064 *//*(Kbps)*//*,
		*//*8 Clients*//* 18744.889 *//*(Kbps)*//*,
		*//*9 Clients*//* 17058.827 *//*(Kbps)*//*,
		*//*10 Clients*//* 15690.455 *//*(Kbps)*//*,
		*//*11 Clients*//* 14127.744 *//*(Kbps)*//*,
		*//*12 Clients*//* 13522.408 *//*(Kbps)*//*,
		*//*13 Clients*//* 13177.631 *//*(Kbps)*//*,
		*//*14 Clients*//* 12811.330 *//*(Kbps)*//*,
		*//*15 Clients*//* 12584.387 *//*(Kbps)*//*,
		*//*15 Clients*//* 12135.161 *//*(Kbps)*//*,
		*//*16 Clients*//* 11705.638 *//*(Kbps)*//*,
		*//*17 Clients*//* 11276.116 *//*(Kbps)*//*,
		*//*18 Clients*//* 10846.594 *//*(Kbps)*//*,
		*//*19 Clients*//* 10417.071 *//*(Kbps)*//*,
		*//*20 Clients*//* 9987.549 *//*(Kbps)*//*,
		*//*21 Clients*//* 9367.587 *//*(Kbps)*//*,
		*//*22 Clients*//* 8747.625 *//*(Kbps)*//*,
		*//*23 Clients*//* 8127.663 *//*(Kbps)*//*,
		*//*24 Clients*//* 7907.701 *//*(Kbps)*//*,
		*//*25 Clients*//* 7887.739 *//*(Kbps)*//*,
		*//*26 Clients*//* 7690.831 *//*(Kbps)*//*,
		*//*27 Clients*//* 7393.922 *//*(Kbps)*//*,
		*//*28 Clients*//* 7297.014 *//*(Kbps)*//*,
		*//*29 Clients*//* 7100.106 *//*(Kbps)*//*,
		*//*30 Clients*//* 6903.197 *//*(Kbps)*//*,
		*//*31 Clients*//* 6701.986 *//*(Kbps)*//*,
		*//*32 Clients*//* 6500.776 *//*(Kbps)*//*,
		*//*33 Clients*//* 6399.565 *//*(Kbps)*//*,
		*//*34 Clients*//* 6098.354 *//*(Kbps)*//*,
		*//*35 Clients*//* 5897.143 *//*(Kbps)*//*,
		*//*36 Clients*//* 5552.127 *//*(Kbps)*//*,
		*//*37 Clients*//* 5207.111 *//*(Kbps)*//*,
		*//*38 Clients*//* 4862.096 *//*(Kbps)*//*,
		*//*39 Clients*//* 4517.080 *//*(Kbps)*//*,
		*//*40 Clients*//* 4172.064 *//*(Kbps)*//*,
		*//*41 Clients*//* 4092.922 *//*(Kbps)*//*,
		*//*42 Clients*//* 4013.781 *//*(Kbps)*//*,
		*//*43 Clients*//* 3934.639 *//*(Kbps)*//*,
		*//*44 Clients*//* 3855.498 *//*(Kbps)*//*,
		*//*45 Clients*//* 3776.356 *//*(Kbps)*//*,
		*//*46 Clients*//* 3697.215 *//*(Kbps)*//*,
		*//*47 Clients*//* 3618.073 *//*(Kbps)*//*,
		*//*48 Clients*//* 3538.932 *//*(Kbps)*//*,
		*//*49 Clients*//* 3459.790 *//*(Kbps)*//*,
		*//*50 Clients*//* 3380.649 *//*(Kbps)*//*,
		*//*51 Clients*//* 3274.611 *//*(Kbps)*//*,
		*//*52 Clients*//* 3168.573 *//*(Kbps)*//*,
		*//*53 Clients*//* 3062.536 *//*(Kbps)*//*,
		*//*54 Clients*//* 2956.498 *//*(Kbps)*//*,
		*//*55 Clients*//* 2850.461 *//*(Kbps)*//*,
		*//*56 Clients*//* 2744.423 *//*(Kbps)*//*,
		*//*57 Clients*//* 2638.386 *//*(Kbps)*//*,
		*//*58 Clients*//* 2532.348 *//*(Kbps)*//*,
		*//*59 Clients*//* 2426.310 *//*(Kbps)*//*,
		*//*60 Clients*//* 2320.273 *//*(Kbps)*//*,
		*//*61 Clients*//* 2283.828 *//*(Kbps)*//*,
		*//*62 Clients*//* 2247.383 *//*(Kbps)*//*,
		*//*63 Clients*//* 2210.939 *//*(Kbps)*//*,
		*//*64 Clients*//* 2174.494 *//*(Kbps)*//*,
		*//*65 Clients*//* 2138.049 *//*(Kbps)*//*,
		*//*66 Clients*//* 2101.604 *//*(Kbps)*//*,
		*//*67 Clients*//* 2065.160 *//*(Kbps)*//*,
		*//*68 Clients*//* 2028.715 *//*(Kbps)*//*,
		*//*69 Clients*//* 1992.270 *//*(Kbps)*//*,
		*//*70 Clients*//* 1955.825 *//*(Kbps)*//*,
		*//*71 Clients*//* 1946.788 *//*(Kbps)*//*,
		*//*72 Clients*//* 1937.751 *//*(Kbps)*//*,
		*//*73 Clients*//* 1928.714 *//*(Kbps)*//*,
		*//*74 Clients*//* 1919.677 *//*(Kbps)*//*,
		*//*75 Clients*//* 1910.640 *//*(Kbps)*//*,
		*//*76 Clients*//* 1901.603 *//*(Kbps)*//*,
		*//*77 Clients*//* 1892.566 *//*(Kbps)*//*,
		*//*78 Clients*//* 1883.529 *//*(Kbps)*//*,
		*//*79 Clients*//* 1874.492 *//*(Kbps)*//*,
		*//*80 Clients*//* 1865.455 *//*(Kbps)*//*,
		*//*81 Clients*//* 1833.185 *//*(Kbps)*//*,
		*//*82 Clients*//* 1800.915 *//*(Kbps)*//*,
		*//*83 Clients*//* 1768.645 *//*(Kbps)*//*,
		*//*84 Clients*//* 1736.375 *//*(Kbps)*//*,
		*//*85 Clients*//* 1704.106 *//*(Kbps)*//*,
		*//*86 Clients*//* 1671.836 *//*(Kbps)*//*,
		*//*87 Clients*//* 1639.566 *//*(Kbps)*//*,
		*//*88 Clients*//* 1607.296 *//*(Kbps)*//*,
		*//*89 Clients*//* 1575.026 *//*(Kbps)*//*,
		*//*90 Clients*//* 1542.756 *//*(Kbps)*//*,
		*//*91 Clients*//* 1538.544 *//*(Kbps)*//*,
		*//*92 Clients*//* 1534.331 *//*(Kbps)*//*,
		*//*93 Clients*//* 1530.119 *//*(Kbps)*//*,
		*//*94 Clients*//* 1525.906 *//*(Kbps)*//*,
		*//*95 Clients*//* 1521.694 *//*(Kbps)*//*,
		*//*96 Clients*//* 1517.481 *//*(Kbps)*//*,
		*//*97 Clients*//* 1513.269 *//*(Kbps)*//*,
		*//*98 Clients*//* 1509.056 *//*(Kbps)*//*,
		*//*99 Clients*//* 1504.844 *//*(Kbps)*//*,
		*//*100 Clients*//* 1500.631 *//*(Kbps)*/
	};
	
	public static final double[] experimentalWanDelay = {
		/*1  Clients*/  20703.973 /*(Kbps)*/,
		/*2  Clients*/  18533.969 /*(Kbps)*/,
		/*3  Clients*/  16363.965 /*(Kbps)*/,
		/*4  Clients*/  14193.961 /*(Kbps)*/,
		/*5  Clients*/  12023.957 /*(Kbps)*/,
		/*6  Clients*/  11489.914 /*(Kbps)*/,
		/*7  Clients*/  10955.871 /*(Kbps)*/,
		/*8  Clients*/  10421.828 /*(Kbps)*/,
		/*9  Clients*/  9887.785 /*(Kbps)*/,
		/*10  Clients*/  9644.7825 /*(Kbps)*/,
		/*11  Clients*/  9401.78 /*(Kbps)*/,
		/*12  Clients*/  9158.7775 /*(Kbps)*/,
		/*13  Clients*/  8915.775 /*(Kbps)*/,
		/*14  Clients*/  8751.6505 /*(Kbps)*/,
		/*15  Clients*/  8587.526 /*(Kbps)*/,
		/*16  Clients*/  8423.4015 /*(Kbps)*/,
		/*17  Clients*/  8259.277 /*(Kbps)*/,
		/*18  Clients*/  8084.60125 /*(Kbps)*/,
		/*19  Clients*/  7909.9255 /*(Kbps)*/,
		/*20  Clients*/  7735.24975 /*(Kbps)*/,
		/*21  Clients*/  7560.574 /*(Kbps)*/,
		/*22  Clients*/  7485.9655 /*(Kbps)*/,
		/*23  Clients*/  7411.357 /*(Kbps)*/,
		/*24  Clients*/  7336.7485 /*(Kbps)*/,
		/*25  Clients*/  7262.14 /*(Kbps)*/,
		/*26  Clients*/  7235.44525 /*(Kbps)*/,
		/*27  Clients*/  7208.7505 /*(Kbps)*/,
		/*28  Clients*/  7182.05575 /*(Kbps)*/,
		/*29  Clients*/  7155.361 /*(Kbps)*/,
		/*30  Clients*/  7126.809 /*(Kbps)*/,
		/*31  Clients*/  7098.257 /*(Kbps)*/,
		/*32  Clients*/  7069.705 /*(Kbps)*/,
		/*33  Clients*/  7041.153 /*(Kbps)*/,
		/*34  Clients*/  7029.5135 /*(Kbps)*/,
		/*35  Clients*/  7017.874 /*(Kbps)*/,
		/*36  Clients*/  7006.2345 /*(Kbps)*/,
		/*37  Clients*/  6994.595 /*(Kbps)*/,
		/*38  Clients*/  6909.25425 /*(Kbps)*/,
		/*39  Clients*/  6823.9135 /*(Kbps)*/,
		/*40  Clients*/  6738.57275 /*(Kbps)*/,
		/*41  Clients*/  6653.232 /*(Kbps)*/,
		/*42  Clients*/  6517.891 /*(Kbps)*/,
		/*43  Clients*/  6382.55 /*(Kbps)*/,
		/*44  Clients*/  6247.209 /*(Kbps)*/,
		/*45  Clients*/  6111.868 /*(Kbps)*/,
		/*46  Clients*/  5976.52725 /*(Kbps)*/,
		/*47  Clients*/  5841.1865 /*(Kbps)*/,
		/*48  Clients*/  5705.84575 /*(Kbps)*/,
		/*49  Clients*/  5570.505 /*(Kbps)*/,
		/*50  Clients*/  5435.16425 /*(Kbps)*/,
		/*51  Clients*/  5299.8235 /*(Kbps)*/,
		/*52  Clients*/  5164.48275 /*(Kbps)*/,
		/*53  Clients*/  5029.142 /*(Kbps)*/,
		/*54  Clients*/  4893.80125 /*(Kbps)*/,
		/*55  Clients*/  4758.4605 /*(Kbps)*/,
		/*56  Clients*/  4623.11975 /*(Kbps)*/,
		/*57  Clients*/  4487.779 /*(Kbps)*/,
		/*58  Clients*/  4340.7665 /*(Kbps)*/,
		/*59  Clients*/  4193.754 /*(Kbps)*/,
		/*60  Clients*/  4046.7415 /*(Kbps)*/,
		/*61  Clients*/  3899.729 /*(Kbps)*/,
		/*62  Clients*/  3752.71675 /*(Kbps)*/,
		/*63  Clients*/  3605.7045 /*(Kbps)*/,
		/*64  Clients*/  3458.69225 /*(Kbps)*/,
		/*65  Clients*/  3311.68 /*(Kbps)*/,
		/*66  Clients*/  3164.66775 /*(Kbps)*/,
		/*67  Clients*/  3017.6555 /*(Kbps)*/,
		/*68  Clients*/  2870.64325 /*(Kbps)*/,
		/*69  Clients*/  2723.631 /*(Kbps)*/,
		/*70  Clients*/  2576.61875 /*(Kbps)*/,
		/*71  Clients*/  2429.6065 /*(Kbps)*/,
		/*72  Clients*/  2282.59425 /*(Kbps)*/,
		/*73  Clients*/  2135.582 /*(Kbps)*/,
		/*74  Clients*/  1988.56975 /*(Kbps)*/,
		/*75  Clients*/  1841.5575 /*(Kbps)*/,
		/*76  Clients*/  1694.54525 /*(Kbps)*/,
		/*77  Clients*/  1547.533 /*(Kbps)*/,
		/*78  Clients*/  1535.71275 /*(Kbps)*/,
		/*79  Clients*/  1523.8925 /*(Kbps)*/,
		/*80  Clients*/  1512.07225 /*(Kbps)*/,
		/*81  Clients*/  1500.252 /*(Kbps)*/,
		/*82  Clients*/  1488.432 /*(Kbps)*/,
		/*83  Clients*/  1476.612 /*(Kbps)*/,
		/*84  Clients*/  1464.792 /*(Kbps)*/,
		/*85  Clients*/  1452.972 /*(Kbps)*/,
		/*86  Clients*/  1441.152 /*(Kbps)*/,
		/*87  Clients*/  1429.332 /*(Kbps)*/,
		/*88  Clients*/  1417.512 /*(Kbps)*/,
		/*89  Clients*/  1405.692 /*(Kbps)*/,
		/*90  Clients*/  1393.87175 /*(Kbps)*/,
		/*91  Clients*/  1382.0515 /*(Kbps)*/,
		/*92  Clients*/  1370.23125 /*(Kbps)*/,
		/*93  Clients*/  1358.411 /*(Kbps)*/,
		/*94  Clients*/  1346.591 /*(Kbps)*/,
		/*95  Clients*/  1334.771 /*(Kbps)*/,
		/*96  Clients*/  1322.951 /*(Kbps)*/,
		/*97  Clients*/  1311.131 /*(Kbps)*/,
		/*98  Clients*/  1299.31099785947 /*(Kbps)*/,
		/*99  Clients*/  1287.49099571894 /*(Kbps)*/,
		/*100  Clients*/  1275.67099362961 /*(Kbps)*/,
		/*101  Clients*/  1263.85099154029 /*(Kbps)*/,
		/*102  Clients*/  1252.03098945096 /*(Kbps)*/,
		/*103  Clients*/  1240.21098736163 /*(Kbps)*/,
		/*104  Clients*/  1228.3909852723 /*(Kbps)*/,
		/*105  Clients*/  1216.57098318297 /*(Kbps)*/,
		/*106  Clients*/  1204.75098109364 /*(Kbps)*/,
		/*107  Clients*/  1192.93097900431 /*(Kbps)*/,
		/*108  Clients*/  1181.11097691498 /*(Kbps)*/,
		/*109  Clients*/  1169.29097482565 /*(Kbps)*/,
		/*110  Clients*/  1157.47097273632 /*(Kbps)*/,
		/*111  Clients*/  1145.650970647 /*(Kbps)*/,
		/*112  Clients*/  1133.83096855767 /*(Kbps)*/,
		/*113  Clients*/  1122.01096646834 /*(Kbps)*/,
		/*114  Clients*/  1110.19096437901 /*(Kbps)*/,
		/*115  Clients*/  1098.37096228968 /*(Kbps)*/,
		/*116  Clients*/  1086.55096020035 /*(Kbps)*/,
		/*117  Clients*/  1074.73095811102 /*(Kbps)*/,
		/*118  Clients*/  1062.91095602169 /*(Kbps)*/,
		/*119  Clients*/  1051.09095393236 /*(Kbps)*/,
		/*120  Clients*/  1039.27095184303 /*(Kbps)*/,
		/*121  Clients*/  1027.4509497537 /*(Kbps)*/,
		/*122  Clients*/  1015.63094766437 /*(Kbps)*/,
		/*123  Clients*/  1003.81094557505 /*(Kbps)*/,
		/*124  Clients*/  991.990943485719 /*(Kbps)*/,
		/*125  Clients*/  980.170941396387 /*(Kbps)*/,
		/*126  Clients*/  968.350939307058 /*(Kbps)*/,
		/*127  Clients*/  956.530937217729 /*(Kbps)*/,
		/*128  Clients*/  944.7109351284 /*(Kbps)*/,
		/*129  Clients*/  932.890933039071 /*(Kbps)*/,
		/*130  Clients*/  921.070930949742 /*(Kbps)*/,
		/*131  Clients*/  909.250928860413 /*(Kbps)*/,
		/*132  Clients*/  897.430926771084 /*(Kbps)*/,
		/*133  Clients*/  885.610924681755 /*(Kbps)*/,
		/*134  Clients*/  873.790922592425 /*(Kbps)*/,
		/*135  Clients*/  861.970920503096 /*(Kbps)*/,
		/*136  Clients*/  850.150918413767 /*(Kbps)*/,
		/*137  Clients*/  838.330916324438 /*(Kbps)*/,
		/*138  Clients*/  826.510914235109 /*(Kbps)*/,
		/*139  Clients*/  814.69091214578 /*(Kbps)*/,
		/*140  Clients*/  802.87091005645 /*(Kbps)*/,
		/*141  Clients*/  791.050907967121 /*(Kbps)*/,
		/*142  Clients*/  779.230905877792 /*(Kbps)*/,
		/*143  Clients*/  767.410903788463 /*(Kbps)*/,
		/*144  Clients*/  755.590901699134 /*(Kbps)*/,
		/*145  Clients*/  743.770899609805 /*(Kbps)*/,
		/*146  Clients*/  731.950897520476 /*(Kbps)*/,
		/*147  Clients*/  720.130895431147 /*(Kbps)*/,
		/*148  Clients*/  708.310893341818 /*(Kbps)*/,
		/*149  Clients*/  696.490891252488 /*(Kbps)*/,
		/*150  Clients*/  684.670889163159 /*(Kbps)*/,
		/*151  Clients*/  672.85088707383 /*(Kbps)*/,
		/*152  Clients*/  661.030884984501 /*(Kbps)*/,
		/*153  Clients*/  649.210882895172 /*(Kbps)*/,
		/*154  Clients*/  637.390880805843 /*(Kbps)*/,
		/*155  Clients*/  625.570878716514 /*(Kbps)*/,
		/*156  Clients*/  613.750876627185 /*(Kbps)*/,
		/*157  Clients*/  601.930874537856 /*(Kbps)*/,
		/*158  Clients*/  590.110872448526 /*(Kbps)*/,
		/*159  Clients*/  578.290870359197 /*(Kbps)*/,
		/*160  Clients*/  566.470868269868 /*(Kbps)*/,
		/*161  Clients*/  554.650866180539 /*(Kbps)*/,
		/*162  Clients*/  542.83086409121 /*(Kbps)*/,
		/*163  Clients*/  531.010862001881 /*(Kbps)*/,
		/*164  Clients*/  519.190859912552 /*(Kbps)*/,
		/*165  Clients*/  507.370857823222 /*(Kbps)*/,
		/*166  Clients*/  495.550855733893 /*(Kbps)*/,
		/*167  Clients*/  483.730853644564 /*(Kbps)*/,
		/*168  Clients*/  471.910851555235 /*(Kbps)*/,
		/*169  Clients*/  460.090849465906 /*(Kbps)*/,
		/*170  Clients*/  448.270847376577 /*(Kbps)*/,
		/*171  Clients*/  436.450845287248 /*(Kbps)*/,
		/*172  Clients*/  424.630843197918 /*(Kbps)*/,
		/*173  Clients*/  412.810841108589 /*(Kbps)*/,
		/*174  Clients*/  400.99083901926 /*(Kbps)*/,
		/*175  Clients*/  389.170836929931 /*(Kbps)*/,
		/*176  Clients*/  377.350834840602 /*(Kbps)*/,
		/*177  Clients*/  365.530832751273 /*(Kbps)*/,
		/*178  Clients*/  353.710830661944 /*(Kbps)*/,
		/*179  Clients*/  341.890828572615 /*(Kbps)*/,
		/*180  Clients*/  330.070826483285 /*(Kbps)*/,
		/*181  Clients*/  318.250824393956 /*(Kbps)*/,
		/*182  Clients*/  306.430822304627 /*(Kbps)*/,
		/*183  Clients*/  294.610820215298 /*(Kbps)*/,
		/*184  Clients*/  282.790818125969 /*(Kbps)*/,
		/*185  Clients*/  270.97081603664 /*(Kbps)*/,
		/*186  Clients*/  259.150813947311 /*(Kbps)*/,
		/*187  Clients*/  247.330811857982 /*(Kbps)*/,
		/*188  Clients*/  235.510809768653 /*(Kbps)*/,
		/*189  Clients*/  223.690807679323 /*(Kbps)*/,
		/*190  Clients*/  211.870805589994 /*(Kbps)*/,
		/*191  Clients*/  200.050803500665 /*(Kbps)*/,
		/*192  Clients*/  188.230801411336 /*(Kbps)*/,
		/*193  Clients*/  176.410799322007 /*(Kbps)*/
	};

/*		*//*1 Client*//* 20703.973 *//*(Kbps)*//*,
		*//*2 Clients*//* 12023.957 *//*(Kbps)*//*,
		*//*3 Clients*//* 9887.785 *//*(Kbps)*//*,
		*//*4 Clients*//* 8915.775 *//*(Kbps)*//*,
		*//*5 Clients*//* 8259.277 *//*(Kbps)*//*,
		*//*6 Clients*//* 7560.574 *//*(Kbps)*//*,
		*//*7 Clients*//* 7262.140 *//*(Kbps)*//*,
		*//*8 Clients*//* 7155.361 *//*(Kbps)*//*,
		*//*9 Clients*//* 7041.153 *//*(Kbps)*//*,
		*//*10 Clients*//* 6994.595 *//*(Kbps)*//*,
		*//*11 Clients*//* 6653.232 *//*(Kbps)*//*,
		*//*12 Clients*//* 6111.868 *//*(Kbps)*//*,
		*//*13 Clients*//* 5570.505 *//*(Kbps)*//*,
		*//*14 Clients*//* 5029.142 *//*(Kbps)*//*,
		*//*15 Clients*//* 4487.779 *//*(Kbps)*//*,
		*//*16 Clients*//* 3899.729 *//*(Kbps)*//*,
		*//*17 Clients*//* 3311.680 *//*(Kbps)*//*,
		*//*18 Clients*//* 2723.631 *//*(Kbps)*//*,
		*//*19 Clients*//* 2135.582 *//*(Kbps)*//*,
		*//*20 Clients*//* 1547.533 *//*(Kbps)*//*,
		*//*21 Clients*//* 1500.252 *//*(Kbps)*//*,
		*//*22 Clients*//* 1452.972 *//*(Kbps)*//*,
		*//*23 Clients*//* 1405.692 *//*(Kbps)*//*,
		*//*24 Clients*//* 1358.411 *//*(Kbps)*//*,
		*//*25 Clients*//* 1311.131 *//*(Kbps)*/

	
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
	@Override
	public double getUploadDelay(int sourceDeviceId, int destDeviceId, int uploadType, Task task){
		return 0;
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
//			return delay = getManUploadDelay();
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
//			return delay = getManDownloadDelay();
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
	}

	double getWlanDownloadDelay(Location accessPointLocation, double dataSize) {
		int numOfWlanUser = wlanClients[accessPointLocation.getServingWlanId()];
		double taskSizeInKb = dataSize * (double)8; //KB to Kb
		double result=0;
		
		if(numOfWlanUser < experimentalWlanDelay.length)
			result = taskSizeInKb /*Kb*/ / (experimentalWlanDelay[numOfWlanUser] * (double) 3 ) /*Kbps*/; //802.11ac is around 3 times faster than 802.11n

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

//	protected double calculateMM1(double propogationDelay, double bandwidth /*Kbps*/, double PoissonMean, double avgTaskSize /*KB*/, int deviceCount){
	public static double calculateMM1(double propogationDelay, double mu , double lambda){

		//Oleg: Little's law: total time a customer spends in the system
		//lamda*(double)deviceCount = (numOfManTaskForDownload/lastInterval)
		double result = 1 / (mu-lambda);

		result += propogationDelay;
		
		return (result > 1) ? -1 : result;
	}

	public void updateMM1QueueModel(){
		double lastInterval = CloudSim.clock() - lastMM1QueeuUpdateTime;
		lastMM1QueeuUpdateTime = CloudSim.clock();

		if(numOfManTaskForDownload != 0){
			ManPoissonMeanForDownload = lastInterval / (numOfManTaskForDownload / (double)numberOfMobileDevices);
			avgManTaskOutputSize = totalManTaskOutputSize / numOfManTaskForDownload;
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
