package com.jvanier.android.sendtocar.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class CarList implements Serializable {
	private static final long serialVersionUID = 8114219603971246792L;

	private HashMap<String, CarProvider> data;
	private List<CarProvider> list;
	private long downloadDate;
	private String language;
	
	private static final int maxAgeInDays = 7;
	private static final int millisPerDay = 86400000;
	
	public CarList(String language) {
		data = new HashMap<String, CarProvider>();
		
		this.downloadDate = System.currentTimeMillis();
		this.language = language;
	}
	
	public void addCarProvider(CarProvider car) {
		if(car == null) {
			return;
		}
		
		data.put(car.makeId, car);
	}
	
	public List<CarProvider> asList() {
		if(list == null) {
			list = new ArrayList<CarProvider>();
			list.addAll(data.values());
			Collections.sort(list);
		}
		return list;
	}
	
	public CarProvider get(String makeId) {
		return data.get(makeId);
	}

	public boolean timeToReDownload() {
		long now = System.currentTimeMillis();
		return (now - downloadDate) > maxAgeInDays * millisPerDay;
	}

	public boolean languageChanged(String language) {
		return (this.language == null || !this.language.equals(language));
	}
	
	public boolean isEmpty() {
		return data.isEmpty();
	}
	
	public int size() {
		return data.size();
	}
}
