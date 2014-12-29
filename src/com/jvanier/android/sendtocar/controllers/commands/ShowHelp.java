package com.jvanier.android.sendtocar.controllers.commands;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.Constants;

public class ShowHelp implements Command {
	@Override
	public void perfrom(Context context) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Constants.HELP_URL));
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast toast = Toast.makeText(context, R.string.errorNoApp, Toast.LENGTH_SHORT);
			toast.show();
		}
	}
}
