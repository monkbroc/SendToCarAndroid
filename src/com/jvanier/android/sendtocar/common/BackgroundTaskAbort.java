package com.jvanier.android.sendtocar.common;

public class BackgroundTaskAbort extends Exception {
	private static final long serialVersionUID = 2800070333002771844L;
	public int messageId;
	
	public BackgroundTaskAbort() {
	}
	
	public BackgroundTaskAbort(int messageId) {
		this.messageId = messageId;
	}

	public BackgroundTaskAbort(String errorMsg) {
		super(errorMsg);
	}
}
