package com.jvanier.android.sendtocar.controllers;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;

import com.jvanier.android.sendtocar.common.Mixpanel;
import com.jvanier.android.sendtocar.downloaders.CarListManager;
import com.jvanier.android.sendtocar.models.Credentials;
import com.jvanier.android.sendtocar.models.RecentVehicleList;
import com.jvanier.android.sendtocar.models.UserPreferences;
import com.mixpanel.android.mpmetrics.MixpanelAPI;


public class SendToCarApp extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		
		loadCredentials();
		loadPreferences();
		setupMixpanel();
		updateCarList();
		loadRecentVehiclesList();
	}
	
	private void loadCredentials() {
		try {
			Credentials.sharedInstance().loadCredentials(this);
		} catch (Exception e) {
			// Crash the program early if the credentials are not supplied at build time
			throw new RuntimeException(e);
		}
	}
	
	private void loadPreferences() {
		UserPreferences.sharedInstance().load(getApplicationContext());
	}

	private void setupMixpanel() {		
		String mixpanelToken = Credentials.sharedInstance().get("MIXPANEL_TOKEN");
		MixpanelAPI mixpanel = Mixpanel.initializeSharedInstance(this, mixpanelToken);
		
		JSONObject props = new JSONObject();
		try {
			props.put("OS Language", Locale.getDefault().getLanguage().toLowerCase(Locale.US));
			props.put("OS Country", Locale.getDefault().getCountry().toLowerCase(Locale.US));
		} catch (JSONException e) {
		}

		mixpanel.registerSuperProperties(props);
	}

	private void updateCarList() {
		CarListManager.sharedInstance().updateCarList(this, Locale.getDefault().getLanguage().toLowerCase(Locale.US));		
	}
	
	private void loadRecentVehiclesList() {
		RecentVehicleList.sharedInstance().loadFromCache(this);
		// FIXME: remove
		/*
		RecentVehicle vehicle = new RecentVehicle();
		vehicle.make = "Ford";
		vehicle.makeId = "car_ford";
		vehicle.account = "2484810771";
		RecentVehicleList.sharedInstance().addRecentVehicle(vehicle);
		vehicle = new RecentVehicle();
		vehicle.make = "BMW";
		vehicle.makeId = "car_bmw";
		vehicle.account = "test@example.com";
		RecentVehicleList.sharedInstance().addRecentVehicle(vehicle);
		*/
	}
}
