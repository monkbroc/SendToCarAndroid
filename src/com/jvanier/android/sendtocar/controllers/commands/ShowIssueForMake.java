package com.jvanier.android.sendtocar.controllers.commands;

import java.text.MessageFormat;

import com.jvanier.android.sendtocar.common.Constants;

public class ShowIssueForMake extends OpenURL {
	public ShowIssueForMake(String makeId) {
		super(MessageFormat.format(Constants.ISSUE_URL, makeId));
	}
}
