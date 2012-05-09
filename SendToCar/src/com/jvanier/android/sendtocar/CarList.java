package com.jvanier.android.sendtocar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class CarList implements Serializable {

	private static final long serialVersionUID = 5824633241455942317L;
	private HashMap<String, CarProvider> carsData;
	private long downloadDate;
	private String language;
	private String country;
	
	private static final int maxAgeInDays = 7;
	private static final int millisPerDay = 86400000;
	
	public CarList(String country, String language) {
		carsData = new HashMap<String, CarProvider>();
		
		this.downloadDate = System.currentTimeMillis();
		this.country = country;
		this.language = language;
	}
	
	public void add(CarProvider car) {
		if(car == null) {
			return;
		}
		
		if(!carsData.containsKey(car.id)) {
			if(country != null) {
				for(CarProvider existing: carsData.values()) {
					if(car.make.equals(existing.make)) {
						car.make = car.make + " (" + country.toUpperCase() + ")";
					}
				}
			}
			carsData.put(car.id, car);
		}
	}
	
	public ArrayList<CarProvider> getList() {
		ArrayList<CarProvider> list = new ArrayList<CarProvider>();
		list.addAll(carsData.values());
		Collections.sort(list);
		return list;
	}

	public boolean timeToReDownload() {
		long now = System.currentTimeMillis();
		return (now - downloadDate) > maxAgeInDays * millisPerDay;
	}

	public boolean localeChanged(String country, String language) {
		return (this.country == null || !this.country.equals(country)) ||
				(this.language == null || !this.language.equals(language));
	}
	
}
