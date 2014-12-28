package com.jvanier.android.sendtocar.controllers;

import java.util.Locale;

import android.app.Application;

import com.jvanier.android.sendtocar.downloaders.CarListManager;
import com.jvanier.android.sendtocar.models.Credentials;
import com.mixpanel.android.mpmetrics.MixpanelAPI;


public class SendToCarApp extends Application {
	MixpanelAPI mixpanel;

	@Override
	public void onCreate() {
		super.onCreate();
		
		loadCredentials();
		setupMixpanel();
		updateCarList();
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
}
