/*
 * Title:        EdgeCloudSim - Location
 * 
 * Description:  Location class used in EdgeCloudSim
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Location implements Serializable {
	private int xPos;
	private int yPos;
	private int servingWlanId;
	private int placeTypeIndex;

	//storage
	private List<Integer> hostsInRange = new ArrayList<Integer>();

	public Location(int _placeTypeIndex, int _servingWlanId, int _xPos, int _yPos){
		servingWlanId = _servingWlanId;
		placeTypeIndex=_placeTypeIndex;
		xPos = _xPos;
		yPos = _yPos;
	}

	public Location(int _placeTypeIndex, int _servingWlanId, int _xPos, int _yPos, List<Integer> _hostsInRange ){
		servingWlanId = _servingWlanId;
		placeTypeIndex=_placeTypeIndex;
		xPos = _xPos;
		yPos = _yPos;
		hostsInRange = _hostsInRange;
	}
	
	@Override
	public boolean equals(Object other){
		boolean result = false;
	    if (other == null) return false;
	    if (!(other instanceof Location))return false;
	    if (other == this) return true;
	    
	    Location otherLocation = (Location)other;
	    if(this.xPos == otherLocation.xPos && this.yPos == otherLocation.yPos)
	    	result = true;

	    return result;
	}

	public int getServingWlanId(){
		return servingWlanId;
	}
	
	public int getPlaceTypeIndex(){
		return placeTypeIndex;
	}
	
	public int getXPos(){
		return xPos;
	}
	
	public int getYPos(){
		return yPos;
	}

	public List<Integer> getHostsInRange() {
		return hostsInRange;
	}

	public void setServingWlanId(int servingWlanId) {
		this.servingWlanId = servingWlanId;
	}
}
