package edu.boun.edgecloudsim.storage_advanced;


public class StorageDevice {

    private int deviceName;
    private double xPos;
    private double yPos;
    private double time;

    public StorageDevice(int _deviceName, double _xPos, double _yPos, double _time) {
        deviceName = _deviceName;
        xPos = _xPos;
        yPos = _yPos;
        time = _time;
    }


    public int getDeviceName() {
        return deviceName;
    }

    public double getxPos() {
        return xPos;
    }

    public void setxPos(double xPos) {
        this.xPos = xPos;
    }

    public double getyPos() {
        return yPos;
    }

    public void setyPos(double yPos) {
        this.yPos = yPos;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

}

//This section has been written by Harel