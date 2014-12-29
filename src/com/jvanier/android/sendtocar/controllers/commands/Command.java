package com.jvanier.android.sendtocar.controllers.commands;

import android.content.Context;

public interface Command {
	public abstract void perfrom(Context context);
}