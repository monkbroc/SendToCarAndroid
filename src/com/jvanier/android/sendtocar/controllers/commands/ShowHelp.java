package com.jvanier.android.sendtocar.controllers.commands;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.Mixpanel;

public class ShowHelp extends OpenURL {
	public ShowHelp() {
		super(Constants.HELP_URL);
	}

	@Override
	public void perfrom(Context context) {
		super.perfrom(context);
		
		JSONObject props = new JSONObject();
		try {
			props.put("Action", "Show website");
		} catch (JSONException e) {
		}
		Mixpanel.sharedInstance().track("Perform info dialog action", props);
	}
}
