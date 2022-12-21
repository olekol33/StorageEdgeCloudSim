/*
 * Title:        EdgeCloudSim - Edge Server Manager
 * 
 * Description: 
 * DefaultEdgeServerManager is responsible for creating datacenters, hosts and VMs.
 * It also provides the list of VMs running on the hosts.
 * This information is critical for the edge orchestrator.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.utils.Location;

import static edu.boun.edgecloudsim.task_generator.LoadGeneratorModel.DATA_DOWNLOAD;

public class DefaultEdgeServerManager extends EdgeServerManager{
	private int hostIdCounter;

	public DefaultEdgeServerManager() {
		hostIdCounter = 0;
	}

	@Override
	public void initialize() {
	}

	@Override
	public VmAllocationPolicy getVmAllocationPolicy(List<? extends Host> hostList, int dataCenterIndex) {
		return new EdgeVmAllocationPolicy_Custom(hostList,dataCenterIndex);
	}
	
	public void startDatacenters() throws Exception{
		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		try {
			if (SimSettings.getInstance().isExportRunFiles()) {
				File nodeListFile = null;
				FileWriter nodeListFW = null;
				BufferedWriter nodeListBW = null;
				nodeListFile = new File(SimSettings.getInstance().getPathOfNodesFile());
				nodeListFW = new FileWriter(nodeListFile, false);
				nodeListBW = new BufferedWriter(nodeListFW);
				nodeListBW.write("nodeName,xPos,yPos,serviceClass,capacity,serviceRate");
				nodeListBW.newLine();
				nodeListBW.close();
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			localDatacenters.add(createDatacenter(i, datacenterElement));
		}
	}

	public void createVmList(int brockerId){
		int hostCounter=0;
		int vmCounter=0;
		
		//Create VMs for each hosts
		Document doc = SimSettings.getInstance().getEdgeDevicesDocument();
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
			for (int j = 0; j < hostNodeList.getLength(); j++) {
				
				vmList.add(hostCounter, new ArrayList<EdgeVM>());
				
				Node hostNode = hostNodeList.item(j);
				Element hostElement = (Element) hostNode;
				NodeList vmNodeList = hostElement.getElementsByTagName("VM");
				//Oleg: single VM in host
//				for (int k = 0; k < vmNodeList.getLength(); k++) {
				for (int k = 0; k < 1; k++) {
					Node vmNode = vmNodeList.item(k);
					Element vmElement = (Element) vmNode;

//					String vmm = vmElement.getAttribute("vmm");
//					int numOfCores = Integer.parseInt(vmElement.getElementsByTagName("core").item(0).getTextContent());
//					double mips = Double.parseDouble(vmElement.getElementsByTagName("mips").item(0).getTextContent());
//					int ram = Integer.parseInt(vmElement.getElementsByTagName("ram").item(0).getTextContent());
//					int readRate = Integer.parseInt(vmElement.getElementsByTagName("readRate").item(0).getTextContent());
//					int taskProcessingTimeUS = Integer.parseInt(vmElement.getElementsByTagName("taskProcessingTimeUS").item(0).getTextContent());
//					int readRate = Integer.parseInt(vmElement.getElementsByTagName("readRate").item(0).getTextContent());
//					long storage = Long.parseLong(vmElement.getElementsByTagName("storage").item(0).getTextContent());
//					long bandwidth = SimSettings.getInstance().getWlanBandwidth() / (hostNodeList.getLength()+vmNodeList.getLength());
					int numOfCores = 1;
//					double mips = Integer.MAX_VALUE;
					//used to set next event to half of time between event checks (Datacenter.java)
					double mips = 1/SimSettings.getInstance().getRequestProcessingTime();
					int ram = Integer.MAX_VALUE;
					String vmm = null;
					int taskProcessingMbps = Integer.parseInt(hostElement.getElementsByTagName("taskProcessingMbps").item(0).getTextContent());
					int readRate = Integer.parseInt(hostElement.getElementsByTagName("readRate").item(0).getTextContent());
					long storage = Long.parseLong(hostElement.getElementsByTagName("storage").item(0).getTextContent());
					long bandwidth = SimSettings.getInstance().getWlanBandwidth() / (hostNodeList.getLength()+vmNodeList.getLength());
					
					//VM Parameters		
					EdgeVM vm = new EdgeVM(vmCounter, brockerId, mips, numOfCores, ram, readRate, taskProcessingMbps,
							bandwidth, storage, vmm, new CloudletSchedulerTimeShared());
					vm.setHost(this.getDatacenterList().get(hostCounter).getHostList().get(0)); //Oleg: added since removed VMs
					vmList.get(hostCounter).add(vm);
					vmCounter++;
				}

				hostCounter++;
			}
		}
	}
	
	public void terminateDatacenters(){
		for (Datacenter datacenter : localDatacenters) {
			datacenter.shutdownEntity();
		}
	}

	//average utilization of all VMs
	public double getAvgUtilization(){
		double totalUtilization = 0;
		double vmCounter = 0;
		
		// for each datacenter...
		for(int i= 0; i<localDatacenters.size(); i++) {
			List<? extends Host> list = localDatacenters.get(i).getHostList();
			// for each host...
			for (int j=0; j < list.size(); j++) {
				Host host = list.get(j);
				List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(host.getId());
				//for each vm...
				for(int vmIndex=0; vmIndex<vmArray.size(); vmIndex++){
					totalUtilization += vmArray.get(vmIndex).getCloudletScheduler().getTotalUtilizationOfCpu(CloudSim.clock());
					vmCounter++;
				}
			}
		}
		return totalUtilization / vmCounter;
	}

	private Datacenter createDatacenter(int index, Element datacenterElement) throws Exception{
		String arch = datacenterElement.getAttribute("arch");
		String os = datacenterElement.getAttribute("os");
		String vmm = datacenterElement.getAttribute("vmm");
//		double costPerBw = Double.parseDouble(datacenterElement.getElementsByTagName("costPerBw").item(0).getTextContent());
//		double costPerSec = Double.parseDouble(datacenterElement.getElementsByTagName("costPerSec").item(0).getTextContent());
//		double costPerMem = Double.parseDouble(datacenterElement.getElementsByTagName("costPerMem").item(0).getTextContent());
//		double costPerStorage = Double.parseDouble(datacenterElement.getElementsByTagName("costPerStorage").item(0).getTextContent());
		double costPerBw = 0;
		double costPerSec = 0;
		double costPerMem = 0;
		double costPerStorage = 0;
		
		List<EdgeHost> hostList=createHosts(datacenterElement);
		
		String name = "Datacenter_" + Integer.toString(index);
		double time_zone = 3.0;         // time zone this resource located
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, costPerSec, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
	
		VmAllocationPolicy vm_policy = getVmAllocationPolicy(hostList,index);
		datacenter = new Datacenter(name, characteristics, vm_policy, storageList, 0);
		
		return datacenter;
	}
	
	private List<EdgeHost> createHosts(Element datacenterElement){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more Machines
		List<EdgeHost> hostList = new ArrayList<EdgeHost>();
		
		Element location = (Element)datacenterElement.getElementsByTagName("location").item(0);
//		String attractiveness = location.getElementsByTagName("attractiveness").item(0).getTextContent();
		String attractiveness = "0";
		int wlan_id = Integer.parseInt(location.getElementsByTagName("wlan_id").item(0).getTextContent());
		int x_pos = Integer.parseInt(location.getElementsByTagName("x_pos").item(0).getTextContent());
		int y_pos = Integer.parseInt(location.getElementsByTagName("y_pos").item(0).getTextContent());
		int placeTypeIndex = Integer.parseInt(attractiveness);

		NodeList hostNodeList = datacenterElement.getElementsByTagName("host");



		
		for (int j = 0; j < hostNodeList.getLength(); j++) {
			Node hostNode = hostNodeList.item(j);
			
			Element hostElement = (Element) hostNode;
//			int numOfCores = Integer.parseInt(hostElement.getElementsByTagName("core").item(0).getTextContent());
//			double mips = Double.parseDouble(hostElement.getElementsByTagName("mips").item(0).getTextContent());
//			int ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			int numOfCores = 1;
			double mips = Double.MAX_VALUE;
//			double mips = 2/CloudSim.getMinTimeBetweenEvents();
			int ram = Integer.MAX_VALUE;
			long storage = Long.parseLong(hostElement.getElementsByTagName("storage").item(0).getTextContent());
			long bandwidth = SimSettings.getInstance().getWlanBandwidth() / hostNodeList.getLength();
			
			// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
			//    create a list to store these PEs before creating
			//    a Machine.
			List<Pe> peList = new ArrayList<Pe>();

			// 3. Create PEs and add these into the list.
			//for a quad-core machine, a list of 4 PEs is required:
			for(int i=0; i<numOfCores; i++){
				peList.add(new Pe(i, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
			}
			
			//4. Create Hosts with its id and list of PEs and add them to the list of machines
			EdgeHost host = new EdgeHost(
					hostIdCounter,
					new RamProvisionerSimple(ram),
					new BwProvisionerSimple(bandwidth), //kbps
					storage,
					peList,
					new VmSchedulerSpaceShared(peList)
				);
			
			host.setPlace(new Location(placeTypeIndex, wlan_id, x_pos, y_pos));
			hostList.add(host);
			hostIdCounter++;

			if (SimSettings.getInstance().isExportRunFiles()) {
				DecimalFormat df = new DecimalFormat();
				df.setMaximumFractionDigits(9);
				try {
					File nodeListFile = null;
					FileWriter nodeListFW = null;
					BufferedWriter nodeListBW = null;
					nodeListFile = new File(SimSettings.getInstance().getPathOfNodesFile());
					nodeListFW = new FileWriter(nodeListFile, true);
					nodeListBW = new BufferedWriter(nodeListFW);
					nodeListBW.write(String.valueOf(hostIdCounter-1) + "," + String.valueOf(x_pos) + "," + String.valueOf(y_pos) + ",0,"+
							String.valueOf(storage) + "," + String.valueOf(SimSettings.getInstance().getServedReqsPerSec()));
					nodeListBW.newLine();
					nodeListBW.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return hostList;
	}
}
