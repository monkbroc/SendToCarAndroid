package com.jvanier.android.sendtocar.controllers;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;

import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.Mixpanel;
import com.jvanier.android.sendtocar.common.Utils;
import com.jvanier.android.sendtocar.downloaders.CarListManager;
import com.jvanier.android.sendtocar.models.Credentials;
import com.jvanier.android.sendtocar.models.RecentVehicleList;
import com.jvanier.android.sendtocar.models.UserPreferences;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class SendToCarApp extends Application {
	private static final String TAG = "SendToCarApp";

	@Override
	public void onCreate() {
		super.onCreate();

		loadPreferences();
		setupLog();

		loadCredentials();
		setupMixpanel();
		updateCarList();
		loadRecentVehiclesList();
	}

	private void loadPreferences() {
		UserPreferences.sharedInstance().load(getApplicationContext());
	}

	private void setupLog() {
		Context context = getApplicationContext();
		if(Utils.isDevelopment(context)) {
			Log.enableToLogCat();
		}

		if(UserPreferences.sharedInstance().isDebug()) {
			Log.enableToFile(context);
		}

		if(Log.isEnabled()) Log.d(TAG, "Application started");
	}

	private void loadCredentials() {
		try {
			Credentials.sharedInstance().loadCredentials(this);
		} catch(Exception e) {
			// Crash the program early if the credentials are not supplied at
			// build time
			throw new RuntimeException(e);
		}
	}

	private void setupMixpanel() {
		String mixpanelToken = Credentials.sharedInstance().get("MIXPANEL_TOKEN");
		MixpanelAPI mixpanel = Mixpanel.initializeSharedInstance(this, mixpanelToken);

		JSONObject props = new JSONObject();
		try {
			props.put("OS Language", Locale.getDefault().getLanguage().toLowerCase(Locale.US));
			props.put("OS Country", UserPreferences.sharedInstance().getCountry());
		} catch(JSONException e) {
		}

		mixpanel.registerSuperProperties(props);
		mixpanel.track("App open", null);
	}

	private void updateCarList() {
		CarListManager.sharedInstance().updateCarList(this, Locale.getDefault().getLanguage().toLowerCase(Locale.US));
	}

	private void loadRecentVehiclesList() {
		RecentVehicleList.sharedInstance().loadFromCache(this);
	}
}
