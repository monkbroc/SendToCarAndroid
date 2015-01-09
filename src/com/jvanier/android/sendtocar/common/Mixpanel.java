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

	// Anonymize emails, usernames and phone numbers
	// foo@bar.com will become aaa@aaa.aaa
	// MYNAME will become AAAAAA
	// 88812345678 will become 1111111111
	// @formatter:off
	public static String anonymizeAccount(String account) {
		return account.replaceAll("[a-z]", "a")
				.replaceAll("[A-Z]", "A")
				.replaceAll("[0-9]", "1");
	}
	// @formatter:on
}
