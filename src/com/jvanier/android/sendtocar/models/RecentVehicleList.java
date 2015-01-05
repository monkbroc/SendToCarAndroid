package com.jvanier.android.sendtocar.models;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;

import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.downloaders.CarListManager;

public class RecentVehicleList {
	private static final String TAG = "RecentVehicleList";
	private static final int MAX_NUMBER_OF_VEHICLES = 5;

	private static final String FILENAME = "recent_vehicles.ser";

	private List<RecentVehicle> list;

	private static final RecentVehicleList INSTANCE = new RecentVehicleList();
	private static final String LEGACY_PREFERENCES_NAME = "SendToCarActivity";

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
		if (size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	public RecentVehicleList addRecentVehicle(RecentVehicle vehicle) {
		if (vehicle == null) {
			return this;
		}

		// Remove that vehicle from the current list if it is already there
		list.remove(vehicle);

		// Add at the front of the list
		list.add(0, vehicle);

		// Trim the list so it doesn't grow too large
		if (list.size() > MAX_NUMBER_OF_VEHICLES) {
			list.subList(MAX_NUMBER_OF_VEHICLES, list.size()).clear();
		}
		
		return this;
	}

	public RecentVehicle getRecentVehicle(int position) {
		return list.get(position);
	}

	public int removeRecentVehicle(RecentVehicle vehicle) {
		int position = list.indexOf(vehicle);
		if (position >= 0) {
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
	
	public void migrateLatestVehicleFromPreferences(Context context) {
		SharedPreferences settings = context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE);
		String makeId = settings.getString("make", "");
		String account = settings.getString("account", "");

		if (makeId.length() > 0 && account.length() > 0) {
			CarProvider p = CarListManager.sharedInstance().getCarList().get(makeId);

			if (p != null) {
				RecentVehicle latestVehicle = new RecentVehicle();
				latestVehicle.makeId = makeId;
				latestVehicle.make = p.make;
				latestVehicle.account = account;
				RecentVehicleList.sharedInstance().addRecentVehicle(latestVehicle).saveToCache(context);

				SharedPreferences.Editor settingsEditor = settings.edit();
				settingsEditor.remove("make");
				settingsEditor.remove("account");
				settingsEditor.commit();
			}
		}
	}

}
