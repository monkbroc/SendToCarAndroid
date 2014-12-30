package com.jvanier.android.sendtocar.controllers.commands;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;

import com.jvanier.android.sendtocar.common.Mixpanel;
import com.jvanier.android.sendtocar.controllers.TutorialActivity;

public 	class ShowTutorial implements Command {
	@Override
	public void perfrom(Context context) {
		Intent intent = new Intent(context, TutorialActivity.class);
		context.startActivity(intent);
		
		JSONObject props = new JSONObject();
		try {
			props.put("Action", "Show Tutorial");
		} catch (JSONException e) {
		}
		Mixpanel.sharedInstance().track("Perform info dialog action", props);
	}
}

