package com.jvanier.android.sendtocar.controllers.commands;

import android.content.Context;

import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.Mixpanel;

public class ShowOtherMakes extends OpenURL {
	public ShowOtherMakes() {
		super(Constants.OTHER_MAKES_URL);
	}

	@Override
	public void perfrom(Context context) {
		super.perfrom(context);
		Mixpanel.sharedInstance().track("Show other makes", null);
	}
}
