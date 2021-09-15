package edu.boun.edgecloudsim.storage;

import java.util.List;


public class StorageObject {
    private String objName;
    private String objSize;
    private List<String> objLocations; //contains a Nodes List
    private List<Double> objLocationsProb; //contains the probability of each Node
    private int objClass;
    private final String type;

    public StorageObject(String _objName, String _objSize, List<String> _objLocations, List<Double> _objLocationsProb, int _objClass) {
        this.objName = _objName;
        this.objSize = _objSize;
        this.objLocations = _objLocations;
        this.objLocationsProb = _objLocationsProb;
        this.objClass = _objClass;
        this.type = "data";
    }

    public String getObjName() {
        return objName;
    }

    public String getObjSize() {
        return objSize;
    }

    public void setObjSize(String objSize) {
        this.objSize = objSize;
    }

    public String getType() {
        return type;
    }

    public List<String> getObjLocations() {
        return objLocations;
    }

    public List<Double> getObjLocationsProb() {
        return objLocationsProb;
    }

    public int getObjClass() {
        return objClass;
    }
}
//This section has been written by Harel