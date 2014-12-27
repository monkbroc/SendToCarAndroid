package com.jvanier.android.sendtocar.models;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CarProvider implements Comparable<CarProvider>, Serializable {
	
	final static int TYPE_CAR = 1;
	final static int TYPE_GPS = 2;

	final static int PROVIDER_GOOGLE_MAPS = 0;
	final static int PROVIDER_MAPQUEST = 1;
	final static int PROVIDER_ONSTAR = 2;
	final static int PROVIDER_HERE_COM = 3;

	private static final long serialVersionUID = 3846464104822731498L;
	public int provider;
	public String host;
	public String makeId;
	public int type;
	public String make;
	public String account;
	public String system;
	public boolean internationalPhone;
	public boolean showPhone;
	public boolean showNotes;
	public List<String> supportedCountries;
	
	public CarProvider() {
		supportedCountries = new ArrayList<String>();
	}
	
	public boolean isCountrySupported(String country) {
		return supportedCountries.contains("all") || supportedCountries.contains(country);
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