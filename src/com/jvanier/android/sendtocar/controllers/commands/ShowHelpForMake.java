package com.jvanier.android.sendtocar.controllers.commands;

import java.text.MessageFormat;

import com.jvanier.android.sendtocar.common.Constants;

public class ShowHelpForMake extends OpenURL {
	public ShowHelpForMake(String makeId) {
		super(MessageFormat.format(Constants.MAKE_URL, makeId));
	}
}
