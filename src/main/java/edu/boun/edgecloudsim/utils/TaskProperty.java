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

import edu.boun.edgecloudsim.storage.StorageObject;
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
	private String objectRead;
	private int paritiesToRead;
	private int ioTaskID;
	private int isParity;
	private int accessHostID;
	private int hostID;

	private int taskPriority;
	private double taskDeadline;
	private String hashedName; // prefix d + number

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
//    	length = (long)expRngList[_taskType][LoadGeneratorModel.LIST_TASK_LENGTH].sample();
    	length = 0;
//    	pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][LoadGeneratorModel.REQUIRED_CORE];
    	pesNumber = 0;
	}

	//Storage
	public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, String _objectID, int _ioTaskID,
						int _isParity, ExponentialDistribution[][] expRngList) {
		mobileDeviceId=_mobileDeviceId;
		startTime=_startTime;
		taskType=_taskType;
		objectRead = _objectID;
		ioTaskID = _ioTaskID;
		paritiesToRead = 0;
		isParity = _isParity;

		try {
			inputFileSize = (long) expRngList[_taskType][LoadGeneratorModel.LIST_DATA_UPLOAD].sample();
		}
		catch (Exception e){
			inputFileSize = 0;
		}

//		outputFileSize =(long)expRngList[_taskType][LoadGeneratorModel.LIST_DATA_DOWNLOAD].sample();
		outputFileSize =(long)expRngList[_taskType][LoadGeneratorModel.LIST_DATA_DOWNLOAD].getMean();
//		length = (long)expRngList[_taskType][LoadGeneratorModel.LIST_TASK_LENGTH].sample();
		length = 0;

//		pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][LoadGeneratorModel.REQUIRED_CORE];
		pesNumber = 0;
	}

	//Storage
	public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, String _objectID, int _ioTaskID,
						int _isParity, int _paritiesToRead, long _inputFileSize, long _outputFileSize, long _length) {
		mobileDeviceId=_mobileDeviceId;
		startTime=_startTime;
		taskType=_taskType;
		objectRead = _objectID;
		ioTaskID = _ioTaskID;
		paritiesToRead = _paritiesToRead;
		isParity = _isParity;

		inputFileSize = _inputFileSize;
		outputFileSize =_outputFileSize;
		length = _length;

		pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][LoadGeneratorModel.REQUIRED_CORE];
	}

	//Storage - Harel
	//TODO: check!
	public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, String _objectID, int _ioTaskID,
						int _taskPriority, double _taskDeadline, int _isParity) {

		startTime=_startTime;
		mobileDeviceId=_mobileDeviceId;
		taskType=_taskType;
		pesNumber = (int)SimSettings.getInstance().getTaskLookUpTable()[_taskType][LoadGeneratorModel.REQUIRED_CORE];
		inputFileSize = -1;

		objectRead = _objectID;
		ioTaskID = _ioTaskID;
		taskPriority = _taskPriority;
		taskDeadline = _taskDeadline;
		isParity = _isParity;

		//finding the object original name (from the hash map)
		/*
		String originName = "";
		int hashSize = SimSettings.getInstance().getObjectsHashVector().size();
		for(int i = 0; i < hashSize; i++){
			if(SimSettings.getInstance().getObjectsHashVector().get("d" + i).equals(objectRead)){
				originName = "d" + i;
				hashedName = originName;
				break;
			}
		}
		try {
			if (originName.equals("")) {
				throw new Exception("ERROR: The task name " + _objectID + " does not much any object in the hash vector!");
			}
		}catch(Exception e){
			e.printStackTrace();
		}

		//finding the size of the object
		int size = SimSettings.getInstance().getObjectsVector().size();
		//for testing purposes - delete!
		//Vector<StorageObject> temp = SimSettings.getInstance().getObjectsVector();
		for(int i = 0; i < size; i++){
			StorageObject sObject = SimSettings.getInstance().getObjectsVector().get(i);
			if(sObject.getObjName().equals(originName)){
				inputFileSize = Long.parseLong(sObject.getObjSize());
				break;
			}
		}
		try {
			if (inputFileSize == -1) {
				throw new Exception("ERROR: The task name " + _objectID + " does not much any object in the objects input!");
			}
		}catch (Exception e){
			e.printStackTrace();
		}*/

		//finding the size of the object
		int size = SimSettings.getInstance().getObjectsVector().size();
		//for testing purposes - delete!
		//Vector<StorageObject> temp = SimSettings.getInstance().getObjectsVector();
		for(int i = 0; i < size; i++){
			StorageObject sObject = SimSettings.getInstance().getObjectsVector().get(i);
			if(sObject.getObjName().equals(objectRead)){
				inputFileSize = Long.parseLong(sObject.getObjSize());
				break;
			}
		}
		try {
			if (inputFileSize == -1) {
				throw new Exception("ERROR: The task name " + _objectID + " does not much any object in the objects input!");
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		length = inputFileSize;
		outputFileSize = inputFileSize;

	}


	public TaskProperty(int _mobileDeviceId, int _taskType, double _startTime, String _objectID, int _ioTaskID,
						int _isParity, int _paritiesToRead, long _inputFileSize, long _outputFileSize, long _length,
						int _hostID) {
		mobileDeviceId=_mobileDeviceId;
		startTime=_startTime;
		taskType=_taskType;
		objectRead = _objectID;
		ioTaskID = _ioTaskID;
		paritiesToRead = _paritiesToRead;
		isParity = _isParity;

		inputFileSize = _inputFileSize;
		outputFileSize =_outputFileSize;
		length = _length;
		hostID = _hostID;

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


	public int getHostID() {
		return hostID;
	}

	public void setAccessHostID(int accessHostID) {
		this.accessHostID = accessHostID;
	}

	public int getTaskPriority() {
		return taskPriority;
	}

	public double getTaskDeadline() {
		return taskDeadline;
	}

	public String getHashedName() {
		return hashedName;
	}
}
