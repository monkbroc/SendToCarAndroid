package com.jvanier.android.sendtocar.common;

import org.apache.http.client.methods.HttpUriRequest;

import android.annotation.SuppressLint;

@SuppressLint("NewApi")
public class AbortHttpRequest {
	public static void abortRequests(final HttpUriRequest... requests) {
		new Thread(new Runnable() {
			public void run() {
				doAbort(requests);
			}
		}).start();
	}

	private static void doAbort(HttpUriRequest... requests) {
		for(HttpUriRequest request : requests) {
			if(request != null) {
				request.abort();
			}
		}
	}
}
