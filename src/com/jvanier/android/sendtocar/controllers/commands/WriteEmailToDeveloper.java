package com.jvanier.android.sendtocar.controllers.commands;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.Mixpanel;

public class WriteEmailToDeveloper implements Command {
	@Override
	public void perfrom(Context context) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("mailto:" + Constants.DEVELOPER_EMAIL));
		try {
			context.startActivity(intent);
			Mixpanel.sharedInstance().track("Email developer", null);
		} catch(ActivityNotFoundException e) {
			Toast toast = Toast.makeText(context, R.string.errorNoApp, Toast.LENGTH_SHORT);
			toast.show();
		}
	}
}
