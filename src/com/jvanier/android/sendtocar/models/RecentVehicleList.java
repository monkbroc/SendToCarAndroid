package com.jvanier.android.sendtocar.models;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class RecentVehicleList {
	private static final String TAG = "RecentVehicleList";
	private static final int MAX_NUMBER_OF_VEHICLES = 5;

	private static final String FILENAME = "recent_vehicles.ser";

	private List<RecentVehicle> list;

	private static final RecentVehicleList INSTANCE = new RecentVehicleList();

	public static RecentVehicleList sharedInstance() {
		return INSTANCE;
	}

	// Make singleton constructor private
	private RecentVehicleList() {
		createDefaultList();
	}
	
	private void createDefaultList() {
		list = new ArrayList<RecentVehicle>();
	}

	public int size() {
		return list.size();
	}
	
	public RecentVehicle latestVehicle() {
		if(size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}
	
	public void addRecentVehicle(RecentVehicle vehicle) {
		if(vehicle == null) {
			return;
		}
		
	    // Remove that vehicle from the current list if it is already there
		list.remove(vehicle);

		// Add at the front of the list
		list.add(0, vehicle);
		
	    // Trim the list so it doesn't grow too large
		if(list.size() > MAX_NUMBER_OF_VEHICLES) {
			list.subList(MAX_NUMBER_OF_VEHICLES, list.size()).clear();
		}
	}
	
	public RecentVehicle getRecentVehicle(int position) {
		return list.get(position);
	}
	
	public int removeRecentVehicle(RecentVehicle vehicle) {
		int position = list.indexOf(vehicle);
		if(position >= 0) {
			list.remove(position);
		}
		return position;
	}

	@SuppressWarnings("unchecked")
	public void loadFromCache(Context context) {
		try {
			InputStream in = context.openFileInput(FILENAME);

			ObjectInputStream objIn = new ObjectInputStream(in);
			list = (List<RecentVehicle>) objIn.readObject();
			objIn.close();

			Log.d(TAG, "Read recent vehicles list with " + list.size() + " vehicles");
		} catch (Exception e) {
			Log.d(TAG, "Recent vehicles list not loaded from cache");
		}
	}
	
	public void saveToCache(Context context) {
		try {
			FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
			ObjectOutputStream out = new ObjectOutputStream(fos);
			out.writeObject(list);
			out.close();
			Log.d(TAG, "Wrote recent vehicles list with " + list.size() + " vechiles to cache");
		} catch (IOException e) {
			// do nothing
		}
	}
}
