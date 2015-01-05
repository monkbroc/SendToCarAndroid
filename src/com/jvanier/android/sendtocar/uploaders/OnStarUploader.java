package com.jvanier.android.sendtocar.uploaders;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.text.format.Time;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.controllers.commands.OpenURL;

public class OnStarUploader extends BaseUploader {
	private static final String TAG = "OnStarUploader";

	private static final String ONSTAR_BASE_URL = "https://www.onstar.com/web/portal/odm?";
	
	public OnStarUploader(Context context, BaseUploaderHandler handler) {
		super(context, handler);

	}

	@Override
	protected Boolean doUpload() throws BackgroundTaskAbort {
		if(isCancelled()) return Boolean.FALSE;
		String post = preparePostDataOnStar();
		if(isCancelled()) return Boolean.FALSE;
		sendToCarOnStar(post);
		 return Boolean.TRUE;
	}
	

	private String preparePostDataOnStar() throws BackgroundTaskAbort {
		try
		{
			StringBuilder payload = new StringBuilder();
			payload.append("s_name=" + URLEncoder.encode(address.title, "UTF-8"));

			String streetaddress;
			if((address.number == null || address.number.length() == 0) &&
					(address.street == null || address.street.length() == 0)) {
				streetaddress = null;
			} else if(address.number == null || address.number.length() == 0) {
				streetaddress = address.street;
			} else {
				streetaddress = address.number + ' ' + address.street;
			}
			
			Time now = new Time();
			now.setToNow();
			String timestamp = now.toMillis(true) + "";

            // full address data
			final String[] codesMQ = {"s_lat", "s_long", "s_street", "s_city", "s_state_province", "s_postalcode", "s_country", "s_locale", "s_nounce", "s_entity_id", "SignatureMethod", "Signature" };
			final String[] valuesMQ = { address.latitude, address.longitude, streetaddress, address.city, address.province, address.postalCode, address.country, "en_US", timestamp, " " + timestamp, "https://mapquest.com.onstar.com", "RSA-SHA256", "" };
			
			final String[] codes = codesMQ;
			final String[] values = valuesMQ;

			for(int i = 0; i < codes.length; i++)
			{
				if(values[i] != null && values[i].length() > 0)
				{
					payload.append("&" + codes[i] + "=" + URLEncoder.encode(values[i], "UTF-8"));
				}
			}
			
			String post = payload.toString();
			
			Log.d(TAG, "Sending to OnStar. Post data <pre>" + post + "</pre>");
			
			return post;
			
		} catch(UnsupportedEncodingException e) {
			Log.d(TAG, "<span style=\"color: red;\">Unsupported encoding exception while preparing MapQuest post data: " + e.toString() + "</span>");
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		} catch(NullPointerException e) {
			Log.d(TAG, "<span style=\"color: red;\">Null pointer exception while preparing MapQuest post data: " + e.toString() + "</span>");
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}
	}
	
	private void sendToCarOnStar(String post) throws BackgroundTaskAbort {
		new OpenURL(ONSTAR_BASE_URL + post).perfrom(getContext());
		
		// FIXME? How to close activity from here?
		throw new BackgroundTaskAbort(R.string.redirect); 
	}
}