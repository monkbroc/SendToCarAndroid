package com.jvanier.android.sendtocar.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CarProvider implements Comparable<CarProvider>, Serializable {
	private static final long serialVersionUID = 2510316148371117429L;

	public final static int TYPE_CAR = 1;
	public final static int TYPE_GPS = 2;

	public final static int PROVIDER_GOOGLE_MAPS = 0;
	public final static int PROVIDER_MAPQUEST = 1;
	public final static int PROVIDER_ONSTAR = 2;
	public final static int PROVIDER_HERE_COM = 3;

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

	public void addSupportedCountry(String country) {
		supportedCountries.add(country);
	}
}