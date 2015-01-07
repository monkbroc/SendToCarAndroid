package com.jvanier.android.sendtocar.uploaders;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.Utils;
import com.jvanier.android.sendtocar.controllers.commands.OpenURL;

public class GoogleMapsUploader extends BaseUploader {
	private static final String TAG = "GoogleMapsUploader";
	private boolean latLongOnly;

	public GoogleMapsUploader(Context context, BaseUploaderHandler handler) {
		super(context, handler);
	}

	@Override
	protected Boolean doUpload() throws BackgroundTaskAbort {
		if(isCancelled()) return Boolean.FALSE;
		String post = preparePostData();
		if(isCancelled()) return Boolean.FALSE;
		String sendToCarHtml = sendToCar(post);
		if(isCancelled()) return Boolean.FALSE;
		parseSendToCar(sendToCarHtml);
		return Boolean.TRUE;
	}

	private String preparePostData() throws BackgroundTaskAbort {
		String post = null;

		try {
			ArrayList<String> postData = new ArrayList<String>();

			postData.add("account");
			postData.add(account);

			postData.add("source");
			postData.add(provider.host);

			postData.add("atx");
			postData.add(provider.makeId);

			postData.add("name");
			postData.add(address.title);

			// full address data
			final String[] codesFull = { "lat", "lng", "street", "streetnum", "city", "province", "postalcode", "country", "phone", "notes" };
			final String[] valuesFull = { address.latitude, address.longitude, address.street, address.number, address.city,
					address.province, address.postalCode, address.country,
					provider.internationalPhone ? address.internationalPhone : address.phone, notes };
			// using only latitude/longitude
			final String[] codesLL = { "lat", "lng" };
			final String[] valuesLL = { address.latitude, address.longitude };

			final String[] codes;
			final String[] values;
			if(latLongOnly) {
				codes = codesLL;
				values = valuesLL;
			} else {
				codes = codesFull;
				values = valuesFull;
			}

			for(int i = 0; i < codes.length; i++) {
				if(values[i] != null && values[i].length() > 0) {
					postData.add(codes[i]);
					postData.add(values[i]);
				}
			}

			postData.add("sxauth");
			postData.add(getCookieId());

			// URL encode
			ListIterator<String> it = postData.listIterator();
			while(it.hasNext()) {
				String s = it.next();
				it.set(Uri.encode(s));
			}

			post = TextUtils.join("|", postData);

			if(Log.isEnabled()) Log.d(TAG, "Sending to car. Post data: " + post);

		} catch(NullPointerException e) {
			if(Log.isEnabled()) Log.e(TAG, "Null pointer exception while preparing post data", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		return post;
	}

	private String getCookieId() {
		String cookie_id = null;

		if(provider != null) {

			List<Cookie> cookies = cookieStore.getCookies();

			for(int tries = 0; tries < 2; tries++) {

				Iterator<Cookie> it = cookies.iterator();
				while(it.hasNext()) {
					Cookie c = it.next();
					if(c.getName().equals("PREF") && provider.host.endsWith(c.getDomain())) {
						cookie_id = parseCookie(c);
						return cookie_id;
					}
				}

				if(tries == 0) {
					/* try to load the main Google Maps page to get a cookie */
					donwloadCookie();
				}
			}
		}

		return cookie_id;
	}

	private String parseCookie(Cookie c) {
		String cookie_id = null;
		String[] subcookies = c.getValue().split(":");
		for(int i = 0; i < subcookies.length; i++) {
			String[] sub = subcookies[i].split("=");
			if(sub.length >= 2 && sub[0].equals("ID")) {
				cookie_id = sub[1];
				if(Log.isEnabled()) Log.d(TAG, "Cookie: " + c.toString());
				break;
			}
		}
		return cookie_id;
	}

	private void donwloadCookie() {
		try {
			URI mainPage = new URI("http", provider.host, "/", "output=json", null);
			if(Log.isEnabled()) Log.d(TAG, "Downloading main Google Maps page to fill the cookie jar: " + mainPage.toString());

			HttpGet httpGet = new HttpGet();
			httpGet.setURI(mainPage);

			HttpResponse response = client.execute(httpGet, httpContext);

			if(Log.isEnabled()) Log.d(TAG, "Downloaded cookie. Status: " + response.getStatusLine().getStatusCode());
		} catch(Exception e) {
			// ignore
		}
	}

	private String sendToCar(String post) throws BackgroundTaskAbort {
		String sendToCarHtml = "";
		try {
			URI postUri = new URI("http", provider.host, "/maps/sendto", "stx=c", null);

			httpPost.setURI(postUri);
			httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
			httpPost.setEntity(new ByteArrayEntity(post.getBytes()));

			if(Log.isEnabled()) Log.d(TAG, "Uploading to " + postUri.toString());

			if(isCancelled() || httpPost.isAborted()) return null;

			HttpResponse response = client.execute(httpPost, httpContext);

			if(Log.isEnabled()) Log.d(TAG, "Uploaded to car. Status: " + response.getStatusLine().getStatusCode());

			if(isCancelled()) return null;

			if(response == null || response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			}

			sendToCarHtml = EntityUtils.toString(response.getEntity());
			if(Log.isEnabled()) Log.d(TAG, "Response: " + Utils.htmlSnippet(sendToCarHtml));
		} catch(InterruptedIOException e) {
			if(Log.isEnabled()) Log.w(TAG, "Upload to car aborted");
			return null;
		} catch(IOException | URISyntaxException e) {
			if(Log.isEnabled()) Log.e(TAG, "Exception while sending to car", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		return sendToCarHtml;
	}

	private void parseSendToCar(String sendToCarHtml) throws BackgroundTaskAbort {
		try {
			// replace ASCII character escapes \xAB with Unicode escapes \u00AB
			// since those are converted
			// automatically by the JSONObject parser to UTF-8 characters
			String html = sendToCarHtml.toString().replaceAll("\\\\x", "\\\\u00");

			JSONObject response = new JSONObject(html);

			if(isCancelled()) return;

			int status = response.getInt("status");

			if(Log.isEnabled()) Log.d(TAG, "Response JSON parsed OK. Status: " + ((status == 1) ? "Success" : "Failed"));

			if(status == 1) {
				// success
				return;
			}

			/*
			 * status 4 means redirect, used by Toyota in Europe other field in
			 * this case: redirect_url
			 */
			if(status == 4) {
				String url = response.optString("redirect_url");
				if(url.length() > 0) {
					// Open a URL to finish the upload
					new OpenURL(url).perfrom(getContext());
					throw new BackgroundTaskAbort(R.string.redirect);
				}
			}

			int errorCode = response.getInt("stcc_status");
			int errorMsg;

			if(getProvider().makeId.equals("car_bmw")) {
				errorMsg = R.string.updateBMWAssist;
			} else {
					switch(errorCode) {
				case 430:
				case 440:
					errorMsg = R.string.statusInvalidAccount;
					break;
	
				case 470:
					errorMsg = R.string.statusDestinationNotSent;
					break;
	
				case 500:
				default:
					errorMsg = R.string.errorSendToCar;
	
					// retry with lat/long only
					if(!latLongOnly) {
						latLongOnly = true;
						if(doUpload().booleanValue()) {
							return;
						}
					}
					break;
				}
			}

			if(Log.isEnabled()) Log.e(TAG, "Error code: " + errorCode + ", String: " + getContext().getString(errorMsg));

			throw new BackgroundTaskAbort(errorMsg);
		} catch(JSONException e) {
			if(Log.isEnabled()) Log.e(TAG, "Exception while parsing resposne JSON", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}
	}

}
