package com.jvanier.android.sendtocar.controllers.commands;

import android.app.AlertDialog;
import android.content.Context;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.models.UserPreferences;

public class ToggleDebugMode implements Command {

	@Override
	public void perfrom(Context context) {
		boolean isDebugNew = !UserPreferences.sharedInstance().isDebug();

		UserPreferences.sharedInstance().setDebug(isDebugNew).save(context);
		if(isDebugNew) {
			Log.enableToFile(context);
		} else {
			Log.disableAndDeleteFile(context);
		}

		AlertDialog.Builder alertbox = new AlertDialog.Builder(context);
		alertbox.setTitle(R.string.debugMode);
		alertbox.setMessage(isDebugNew ? R.string.debugOn : R.string.debugOff);
		alertbox.setPositiveButton(R.string.ok, null);
		alertbox.show();
	}
}
