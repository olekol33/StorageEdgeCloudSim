package edu.boun.edgecloudsim.storage_advanced;


public class StorageNode {
    private int nodeName;
    private double xPos;
    private double yPos;
    private int serviceClass;
    private int capacity;
    private int serviceRate;

    public StorageNode(int _nodeName, double _xPos, double _yPos, int _serviceClass, int _capacity, int _serviceRate) {
        nodeName = _nodeName;
        xPos = _xPos;
        yPos = _yPos;
        serviceClass = _serviceClass;
        capacity = _capacity;
        serviceRate = _serviceRate;
    }

    public int getNodeName() {
        return nodeName;
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

    public int getCapacity() {
        return capacity;
    }

    public int getServiceRate() {
        return serviceRate;
    }

    public int getServiceClass() {
        return serviceClass;
    }
}
