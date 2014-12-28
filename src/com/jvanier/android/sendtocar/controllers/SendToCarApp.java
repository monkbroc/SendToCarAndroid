package com.jvanier.android.sendtocar.controllers;

import java.util.Locale;

import android.app.Application;

import com.jvanier.android.sendtocar.downloaders.CarListManager;
import com.jvanier.android.sendtocar.models.Credentials;
import com.jvanier.android.sendtocar.models.RecentVehicle;
import com.jvanier.android.sendtocar.models.RecentVehicleList;
import com.mixpanel.android.mpmetrics.MixpanelAPI;


public class SendToCarApp extends Application {
	MixpanelAPI mixpanel;

	@Override
	public void onCreate() {
		super.onCreate();
		
		loadCredentials();
		setupMixpanel();
		updateCarList();
		loadRecentVehiclesList();
	}
	
	private void loadCredentials() {		
		Credentials.sharedInstance().loadCredentials(this);
	}
	
	private void setupMixpanel() {		
		String mixpanelToken = Credentials.sharedInstance().get("MIXPANEL_TOKEN");
		mixpanel = MixpanelAPI.getInstance(this, mixpanelToken);
	}
	
	public MixpanelAPI getMixpanel() {
		return mixpanel;
	}

	private void updateCarList() {
		CarListManager.sharedInstance().updateCarList(this, Locale.getDefault().getLanguage());		
	}
	
	private void loadRecentVehiclesList() {
		RecentVehicleList.sharedInstance().loadFromCache(this);
		// FIXME: remove
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
	}
}
