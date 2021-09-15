package edu.boun.edgecloudsim.storage;


public class StorageRequest {
    private String deviceName;
    private Double time;
    private String objectID;
    private int ioTaskID;
    private int taskPriority;
    private Double taskDeadline;
    private final int defaultTaskPriority = -1;
    private final double defaultTaskDeadline = -1.0;

//constructors
    public StorageRequest( String _deviceName, Double _time, String _objectID, int _ioTaskID, int _taskPriority, Double _taskDeadline) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = _taskPriority;
        taskDeadline = _taskDeadline;
    }

    //if the field "taskPriority" is empty, a default value would be inserted to that attribute
    public StorageRequest(String _deviceName, Double _time, String _objectID, int _ioTaskID, Double _taskDeadline) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = defaultTaskPriority;
        taskDeadline = _taskDeadline;
    }

    //if the field "taskDeadline" is empty, a default value would be inserted to that attribute
    public StorageRequest(String _deviceName, Double _time, String _objectID, int _ioTaskID, int _taskPriority) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = _taskPriority;
        taskDeadline = defaultTaskDeadline;
    }

    //if the fields "taskDeadline" and "taskPriority" are empty, a default values would be inserted to that attributes
    public StorageRequest(String _deviceName, Double _time, String _objectID, int _ioTaskID) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = defaultTaskPriority;
        taskDeadline = defaultTaskDeadline;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public Double getTime() {
        return time;
    }

    public void setTime(Double time) {
        this.time = time;
    }

    public String getObjectID() {
        return objectID;
    }

    public int getTaskPriority() {
        return taskPriority;
    }

    public Double getTaskDeadline() {
        return taskDeadline;
    }

    public int getIoTaskID() {
        return ioTaskID;
    }
}
//This section has been written by Harel