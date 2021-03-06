package com.jvanier.android.sendtocar.controllers.commands;

import java.text.MessageFormat;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.Mixpanel;

public class ShowHelpForMake extends OpenURL {
	private String makeId;

	public ShowHelpForMake(String makeId) {
		super(MessageFormat.format(Constants.MAKE_URL, makeId));
		this.makeId = makeId;
	}

	@Override
	public void perfrom(Context context) {
		super.perfrom(context);

		JSONObject props = new JSONObject();
		try {
			props.put("Button", "Help");
			props.put("Make", makeId);
		} catch(JSONException e) {
		}
		Mixpanel.sharedInstance().track("Showing make info", props);
	}
}
