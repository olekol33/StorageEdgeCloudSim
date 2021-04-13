package edu.boun.edgecloudsim.storage_advanced;

public class StorageRequest {
    private String deviceName;
    private Double time;
    private String objectID;
    private int ioTaskID;
    private int taskPriority;
    private Double taskDeadline;


    public StorageRequest( String _deviceName, Double _time, String _objectID, int _ioTaskID, int _taskPriority, Double _taskDeadline) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = _taskPriority;
        taskDeadline = _taskDeadline;
    }

    //if the field "taskPriority" is empty
    public StorageRequest(String _deviceName, Double _time, String _objectID, int _ioTaskID, Double _taskDeadline) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = -1;
        taskDeadline = _taskDeadline;
    }

    //if the field "taskDeadline" is empty
    public StorageRequest(String _deviceName, Double _time, String _objectID, int _ioTaskID, int _taskPriority) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = _taskPriority;
        taskDeadline = -1.0;
    }

    //if the fields "taskDeadline" and "taskPriority" are empty
    public StorageRequest(String _deviceName, Double _time, String _objectID, int _ioTaskID) {
        deviceName = _deviceName;
        time = _time;
        objectID = _objectID;
        ioTaskID = _ioTaskID;
        taskPriority = -1;
        taskDeadline = -1.0;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public void setObjectID(String objectID) {
        this.objectID = objectID;
    }

    public int getTaskPriority() {
        return taskPriority;
    }

    public void setTaskPriority(int taskPriority) {
        this.taskPriority = taskPriority;
    }

    public Double getTaskDeadline() {
        return taskDeadline;
    }

    public void setTaskDeadline(Double taskDeadline) {
        this.taskDeadline = taskDeadline;
    }

    public int getIoTaskID() {
        return ioTaskID;
    }

    public void setIoTaskID(int ioTaskID) {
        this.ioTaskID = ioTaskID;
    }
}
