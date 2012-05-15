package com.jvanier.android.sendtocar;

import java.io.Serializable;

public class CarProvider implements Comparable<CarProvider>, Serializable {

	private static final long serialVersionUID = -5403127983230092895L;
	public String host;
	public String id;
	public int type;
	public String make;
	public String account;
	public String destination_tag;
	public boolean use_destination_tag;
	public String system;
	public boolean international_phone;
	public boolean show_phone;
	public boolean show_notes;
	
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