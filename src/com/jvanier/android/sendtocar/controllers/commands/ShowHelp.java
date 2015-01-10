package com.jvanier.android.sendtocar.controllers.commands;

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
		Mixpanel.sharedInstance().track("Show website", null);
	}
}
