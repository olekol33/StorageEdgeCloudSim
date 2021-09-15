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

//changed by Harel

public class Task extends Cloudlet {
	private Location submittedLocation;
	private int type;
	private int mobileDeviceId;
	private int hostIndex;
	private int vmIndex;
	private int datacenterId;

	//storage
	private String stripeID;
	private String objectRead;
	private int hostID;
	private int paritiesToRead;
	private int ioTaskID;
	private int isParity;
	private int accessHostID;
	private long length, inputFileSize, outputFileSize;

	private double start_time;
	private int taskPriority;
	private double taskDeadline;


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

	//storage constructor - added by Harel
	public Task(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
				UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
				UtilizationModel utilizationModelBw, int _mobileDeviceId, int _taskType, double _startTime,
				String _objectID, int _ioTaskID, int _taskPriority, double _taskDeadline, int _isParity){
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize,
				cloudletOutputSize, utilizationModelCpu, utilizationModelRam,
				utilizationModelBw);
		mobileDeviceId = _mobileDeviceId;
		type = _taskType;
		start_time = _startTime;
		objectRead = _objectID;
		ioTaskID = _ioTaskID;
		isParity = _isParity;
		taskPriority = _taskPriority;
		taskDeadline = _taskDeadline;
	}

	public double getStart_time() {
		return start_time;
	}

	public int getTaskPriority() {
		return taskPriority;
	}

	public double getTaskDeadline() {
		return taskDeadline;
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

	public int getIoTaskID() {
		return ioTaskID;
	}

	public void setIoTaskID(int ioTaskID) {
		this.ioTaskID = ioTaskID;
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

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public long getInputFileSize() {
		return inputFileSize;
	}

	public void setInputFileSize(long inputFileSize) {
		this.inputFileSize = inputFileSize;
	}

	public long getOutputFileSize() {
		return outputFileSize;
	}

	public void setOutputFileSize(long outputFileSize) {
		this.outputFileSize = outputFileSize;
	}

	public void setStart_time(double start_time) {
		this.start_time = start_time;
	}

	public void setTaskPriority(int taskPriority) {
		this.taskPriority = taskPriority;
	}

	public void setTaskDeadline(double taskDeadline) {
		this.taskDeadline = taskDeadline;
	}
}
