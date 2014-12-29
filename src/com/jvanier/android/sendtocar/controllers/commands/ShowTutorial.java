package com.jvanier.android.sendtocar.controllers.commands;

import android.content.Context;
import android.content.Intent;

import com.jvanier.android.sendtocar.controllers.TutorialActivity;

public 	class ShowTutorial implements Command {
	@Override
	public void perfrom(Context context) {
		Intent intent = new Intent(context, TutorialActivity.class);
		context.startActivity(intent);
	}
}

