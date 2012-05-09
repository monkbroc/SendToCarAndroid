package com.jvanier.android.sendtocar;

import java.io.Serializable;

public class CarProvider implements Comparable<CarProvider>, Serializable {

	private static final long serialVersionUID = 6134613837319139773L;
	public String host;
	public String id;
	public int type;
	public String make;
	public String account;
	public String destination_tag;
	public boolean use_destination_tag;
	public String system;
	public boolean international_phone;
	
	public CarProvider() {
	}
	
	@Override
	public String toString() {
		return make + ((type == 2) ? " (GPS)" : "");
	}
	
    @Override
    public int compareTo(CarProvider other) {
    	int typediff = (this.type - other.type);
        return (typediff == 0) ? this.make.compareTo(other.make) : typediff;
    }
}