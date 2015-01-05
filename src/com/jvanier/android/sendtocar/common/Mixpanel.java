package com.jvanier.android.sendtocar.common;

import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

public class Mixpanel {
	private static MixpanelAPI MIXPANEL_API_INSTANCE;

	public static MixpanelAPI initializeSharedInstance(Context context, String token) {
		MIXPANEL_API_INSTANCE = MixpanelAPI.getInstance(context, token);
		return sharedInstance();
	}

	public static MixpanelAPI sharedInstance() {
		return MIXPANEL_API_INSTANCE;
	}
}
