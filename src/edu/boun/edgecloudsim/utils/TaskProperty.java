/*
 * Title:        EdgeCloudSim - EdgeTask
 * 
 * Description: 
 * A custom class used in Load Generator Model to store tasks information
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;

public class TaskProperty {
    private double startTime;
    private long length, inputFileSize, outputFileSize;
    private int taskType;
    private int pesNumber;
    private int mobileDeviceId;
	//storage
	private String stripeID;
	private String objectToRead;
	private String objectRead;
	private int paritiesToRead;
	private int ioTaskID;
	private int isParity;
	private int accessHostID;
    
    public TaskProperty(double _startTime, int _mobileDeviceId, int _taskType, int _pesNumber, long _length, long _inputFileSize, long _outputFileSize) {
    	startTime=_startTime;
    	mobileDeviceId=_mobileDeviceId;
    	taskType=_taskType;
    	pesNumber = _pesNumber;
    	length = _length;
    	outputFileSize = _inputFileSize;
       	inputFileSize = _outputFileSize;
	}
    
    public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, ExponentialDistribution[][] expRngList) {
    	mobileDeviceId=_mobileDeviceId;
    	startTime=_startTime;
    	taskType=_taskType;
    	
    	inputFileSize = (long)expRngList[_taskType][LoadGeneratorModel.LIST_DATA_UPLOAD].sample();
    	outputFileSize =(long)expRngList[_taskType][LoadGeneratorModel.LIST_DATA_DOWNLOAD].sample();
    	length = (long)expRngList[_taskType][LoadGeneratorModel.LIST_TASK_LENGTH].sample();
    	pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][LoadGeneratorModel.REQUIRED_CORE];
	}

	//Storage
	public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, String _stripeID, String _objectID, int _ioTaskID,
						int _isParity, ExponentialDistribution[][] expRngList) {
		mobileDeviceId=_mobileDeviceId;
		startTime=_startTime;
		taskType=_taskType;
		stripeID = _stripeID;
		objectToRead = _objectID;
		objectRead = _objectID;
		ioTaskID = _ioTaskID;
		paritiesToRead = RedisListHandler.getNumOfParityInStripe();
		isParity = _isParity;

		inputFileSize = (long)expRngList[_taskType][LoadGeneratorModel.LIST_DATA_UPLOAD].sample();
		//TODO: Change size to object size
		outputFileSize =(long)expRngList[_taskType][LoadGeneratorModel.LIST_DATA_DOWNLOAD].sample();
		length = (long)expRngList[_taskType][LoadGeneratorModel.LIST_TASK_LENGTH].sample();

		pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][LoadGeneratorModel.REQUIRED_CORE];


	}
    
    public double getStartTime(){
    	return startTime;
    }
    
    public long getLength(){
    	return length;
    }
    
    public long getInputFileSize(){
    	return inputFileSize;
    }
    
    public long getOutputFileSize(){
    	return outputFileSize;
    }

    public int getTaskType(){
    	return taskType;
    }
    
    public int getPesNumber(){
    	return pesNumber;
    }
    
    public int getMobileDeviceId(){
    	return mobileDeviceId;
    }

	public String getStripeID() {
		return stripeID;
	}

	public String getObjectToRead() {
		return objectToRead;
	}

	public String getObjectRead() {
		return objectRead;
	}

	public int getParitiesToRead() {
		return paritiesToRead;
	}

	public int getIoTaskID() {
		return ioTaskID;
	}
	public int getIsParity() {
		return isParity;
	}

	public void setAccessHostID(int accessHostID) {
		this.accessHostID = accessHostID;
	}
}
