/*
 * Title:        EdgeCloudSim - Load Generator Model
 * 
 * Description: 
 * LoadGeneratorModel is an abstract class which is used for 
 * deciding task generation pattern via a task list. For those who
 * wants to add a custom Load Generator Model to EdgeCloudSim should
 * extend this class and provide a concreate instance via ScenarioFactory
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.task_generator;

import java.util.List;

import edu.boun.edgecloudsim.utils.TaskProperty;

public abstract class LoadGeneratorModel {
	protected List<TaskProperty> taskList;
	protected int numberOfMobileDevices;
	protected double simulationTime;
	protected String simScenario;

	public static final int USAGE_PERCENTAGE = 0;
	public static final int PROB_CLOUD_SELECTION = 1;
	public static final int POISSON_INTERARRIVAL = 2;
	public static final int ACTIVE_PERIOD = 3;
	public static final int IDLE_PERIOD = 4;
	public static final int DATA_UPLOAD = 5;
	public static final int DATA_DOWNLOAD = 6;
	public static final int TASK_LENGTH = 7;
	public static final int REQUIRED_CORE = 8;
//	public static final int VM_UTILIZATION_ON_EDGE = 9;
//	public static final int VM_UTILIZATION_ON_CLOUD = 10;
//	public static final int VM_UTILIZATION_ON_MOBILE = 11;
//	public static final int DELAY_SENSITIVITY = 12;
//	public static final int SAMPLING_METHOD = 13;


	public static final int LIST_DATA_UPLOAD = 0;
	public static final int LIST_DATA_DOWNLOAD = 1;
	public static final int LIST_TASK_LENGTH = 2;

//	public static final int SAMPLING_WR = 0;
//	public static final int SAMPLING_WOR = 1;


	public LoadGeneratorModel(int _numberOfMobileDevices, double _simulationTime, String _simScenario){
		numberOfMobileDevices=_numberOfMobileDevices;
		simulationTime=_simulationTime;
		simScenario=_simScenario;
	};
	
	/*
	 * each task has a virtual start time
	 * it will be used while generating task
	 */
	public List<TaskProperty> getTaskList() {
		return taskList;
	}

	public void addTaskToList(TaskProperty task) {
		taskList.add(task);
	}

	/*
	 * fill task list according to related task generation model
	 */
	public abstract void initializeModel();
	
	/*
	 * returns the task type (index) that the mobile device uses
	 */
	public abstract int getTaskTypeOfDevice(int deviceId);
}
