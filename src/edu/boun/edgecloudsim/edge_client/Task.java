/*
 * Title:        EdgeCloudSim - Task
 * 
 * Description: 
 * Task adds app type, task submission location, mobile device id and host id
 * information to CloudSim's Cloudlet class.
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_client;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

import edu.boun.edgecloudsim.utils.Location;

public class Task extends Cloudlet {
	private Location submittedLocation;
	private int type;
	private int mobileDeviceId;
	private int hostIndex;
	private int vmIndex;
	private int datacenterId;

	//storage
	private String stripeID;
	private String objectToRead;
	private String objectRead;
	private int hostID;
	private int paritiesToRead;
	private int ioTaskID;
	private int isParity;
	private int accessHostID;

	public Task(int _mobileDeviceId, int cloudletId, long cloudletLength, int pesNumber,
			long cloudletFileSize, long cloudletOutputSize,
			UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize,
				cloudletOutputSize, utilizationModelCpu, utilizationModelRam,
				utilizationModelBw);
		
		mobileDeviceId = _mobileDeviceId;
	}
	
	public void setSubmittedLocation(Location _submittedLocation){
		submittedLocation =_submittedLocation;
	}

	public void setAssociatedDatacenterId(int _datacenterId){
		datacenterId=_datacenterId;
	}
	
	public void setAssociatedHostId(int _hostIndex){
		hostIndex=_hostIndex;
	}

	public void setAssociatedVmId(int _vmIndex){
		vmIndex=_vmIndex;
	}
	
	public void setTaskType(int _type){
		type=_type;
	}

	public int getMobileDeviceId(){
		return mobileDeviceId;
	}
	
	public Location getSubmittedLocation(){
		return submittedLocation;
	}
	
	public int getAssociatedDatacenterId(){
		return datacenterId;
	}
	
	public int getAssociatedHostId(){
		return hostIndex;
	}

	public int getAssociatedVmId(){
		return vmIndex;
	}
	
	public int getTaskType(){
		return type;
	}

	public String getStripeID() {
		return stripeID;
	}
	public void setStripeID(String stripeID) {
		this.stripeID = stripeID;
	}
	public int getHostID() {
		return hostID;
	}

	public void setHostID(int hostID) {
		this.hostID = hostID;
	}

	public String getObjectToRead() {
		return objectToRead;
	}

	public int getIoTaskID() {
		return ioTaskID;
	}

	public void setIoTaskID(int ioTaskID) {
		this.ioTaskID = ioTaskID;
	}

	public void setObjectToRead(String objectToRead) {
		this.objectToRead = objectToRead;
	}

	public int getParitiesToRead() {
		return paritiesToRead;
	}
	public int getIsParity() {
		return isParity;
	}
	public void setIsParity(int isParity) {
		this.isParity = isParity;
	}
	public String getObjectRead() {
		return objectRead;
	}
	public void setParitiesToRead(int paritiesToRead) {
		this.paritiesToRead = paritiesToRead;
	}
	public void setObjectRead(String objectRead) {
		this.objectRead = objectRead;
	}
	public void setAccessHostID(int accessHostID) {
		this.accessHostID = accessHostID;
	}
	public int getAccessHostID() {
		return accessHostID;
	}

}
