package com.jvanier.android.sendtocar;

import android.app.Application;


public class SendToCarApp extends Application {
	private DebugLog log; 

	@Override
	public void onCreate() {
		super.onCreate();
		
		//log = new DebugLogFile(this);
		log = new DebugLogDummy();
	}

	public DebugLog getLog() {
		return log;
	}
	
}
