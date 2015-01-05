package com.jvanier.android.sendtocar.models;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.Utils;


public class Credentials {
	private static final String TAG = "Credentials";
	private static final String ASSET_FILENAME = "credentials.json";
	private static final String PRODUCTION_KEY = "production";
	private static final String DEVELOPMENT_KEY = "development";

	private static final Credentials INSTANCE = new Credentials();

	public static Credentials sharedInstance() {
		return INSTANCE;
	}

	private JSONObject data;
	private String environmentKey;

	private Credentials() {
	}

	public void loadCredentials(Context context) throws IOException, JSONException {
		// If you want to use MixPanel for your fork, register with
		// them and place your API key in /assets/credentials.json
		// (This prevents me receiving events from forked
		// versions which is somewhat confusing!)
		/* Credentials file format:
		 {
		   "development": {
		     "MIXPANEL_TOKEN": "123456"
		   },
		   "production": {
		     "MIXPANEL_TOKEN": "abcdef"
		   },
		   "OTHER_KEY": "123abc"
		 }
		 */
		
		try {
			InputStream inputStream = context.getAssets().open(ASSET_FILENAME);
			String contents = Utils.ReadInputStream(inputStream);
			data = new JSONObject(contents);
			environmentKey = Utils.isDevelopment(context) ? DEVELOPMENT_KEY : PRODUCTION_KEY;
		} catch (IOException e) {
			Log.e(TAG, "No credentials JSON file found!");
			Log.e(TAG, "Add a JSON file with credentials to the build project in folder /assets/" + ASSET_FILENAME);
			throw e;
		} catch (JSONException e) {
			Log.e(TAG, "Syntax error in credentials JSON file /assets/" + ASSET_FILENAME, e);
			throw e;
		}
	}

	// Get the key first from the environment-specific key "development" or "production".
	// If it doesn't exist, get the key from the general section 
	public String get(String key) {
		String value = null;
		if(data != null) {
			JSONObject env = data.optJSONObject(environmentKey);
			value = env != null ? env.optString(key) : null;
			if(value == null) {
				value = data.optString(key);
			}
		}
		return value;
	}
}
