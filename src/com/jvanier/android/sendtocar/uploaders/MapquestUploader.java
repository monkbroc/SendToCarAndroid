package com.jvanier.android.sendtocar.uploaders;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.HttpClientTrustAll;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.Utils;

public class MapquestUploader extends BaseUploader {

	private static final String TAG = "MapquestUploader";

	public MapquestUploader(Context context, BaseUploaderHandler handler) {
		super(context, handler);

	}

	@Override
	protected Boolean doUpload() throws BackgroundTaskAbort {
		if(isCancelled())
			return Boolean.FALSE;
		String post = preparePostData();
		if(isCancelled())
			return Boolean.FALSE;
		String sendToCarHtml = sendToCar(post);
		if(isCancelled())
			return Boolean.FALSE;
		parseSendToCar(sendToCarHtml);
		return Boolean.TRUE;
	}

	private String preparePostData() throws BackgroundTaskAbort {
		try {
			JSONObject location = new JSONObject();
			location.put("name", address.title);

			String streetaddress;
			if((address.number == null || address.number.length() == 0) && (address.street == null || address.street.length() == 0)) {
				streetaddress = null;
			} else if(address.number == null || address.number.length() == 0) {
				streetaddress = address.street;
			} else {
				streetaddress = address.number + ' ' + address.street;
			}

			// full address data
			final String[] codesMQ = { "street", "city", "state", "postalCode", "country" };
			final String[] valuesMQ = { streetaddress, address.city, address.province, address.postalCode, address.country };

			final String[] codes = codesMQ;
			final String[] values = valuesMQ;

			for(int i = 0; i < codes.length; i++) {
				if(values[i] != null && values[i].length() > 0) {
					location.put(codes[i], values[i]);
				}
			}

			if(address.latitude.length() > 0 && address.longitude.length() > 0) {
				JSONObject latLng = new JSONObject();
				latLng.put("lat", address.latitude);
				latLng.put("lng", address.longitude);
				location.put("latLng", latLng);
			}

			JSONArray locations = new JSONArray();
			locations.put(location);

			JSONObject payload = new JSONObject();
			payload.put("mobileNumber", account);
			payload.put("locations", locations);

			String post = payload.toString();

			if(Log.isEnabled())
				Log.d(TAG, "Sending to MapQuest. Post data <pre>" + post + "</pre>");

			return post;

		} catch(JSONException e) {
			if(Log.isEnabled())
				Log.e(TAG, "JSON exception while preparing MapQuest post data", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		} catch(NullPointerException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Null pointer exception while preparing MapQuest post data", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}
	}

	private String sendToCar(String post) throws BackgroundTaskAbort {
		String sendToCarHtml = "";
		try {
			sendToCarHtml = sendToCarCore(post, true, false);
		} catch(InterruptedIOException e) {
			if(Log.isEnabled())
				Log.w(TAG, "Upload to Mapquest aborted");
			return null;
		} catch(SSLPeerUnverifiedException e) {
			if(Log.isEnabled())
				Log.e(TAG, "SSLPeerUnverifiedException while sending to MapQuest. Trying again in fallback mode.");
			return sendToCarTrustAll(post);
		} catch(SSLException e) {
			if(Log.isEnabled())
				Log.e(TAG, "SSLException while sending to MapQuest. Trying again without HTTPS.");
			return sendToCarNoSSL(post);
		} catch(IOException | URISyntaxException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while sending to Mapquest", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		return sendToCarHtml;
	}

	/*
	 * Workaround for bug in Android 4.1.1 that makes SSL connections fail on 4G
	 * => Accept all certificates as valid
	 */
	private String sendToCarTrustAll(String post) throws BackgroundTaskAbort {
		String sendToCarHtml = "";
		try {
			sendToCarHtml = sendToCarCore(post, true, true);
		} catch(InterruptedIOException e) {
			if(Log.isEnabled())
				Log.w(TAG, "Upload to Mapquest aborted");
			return null;
		} catch(Exception e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while sending to Mapquest (fallback mode)", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		return sendToCarHtml;
	}

	/*
	 * Workaround for bug in Android 4.3 that makes SSL connections fail on 4G
	 * => Call without SSL
	 */
	private String sendToCarNoSSL(String post) throws BackgroundTaskAbort {
		String sendToCarHtml = "";
		try {
			sendToCarHtml = sendToCarCore(post, false, false);
		} catch(InterruptedIOException e) {
			if(Log.isEnabled())
				Log.w(TAG, "Upload to Mapquest aborted");
			return null;
		} catch(IOException | URISyntaxException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while sending to Mapquest (fallback mode)", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		return sendToCarHtml;
	}

	private String sendToCarCore(String post, boolean useSSL, boolean trustAllCertificates) throws URISyntaxException, IOException,
			BackgroundTaskAbort {
		String sendToCarHtml;

		URI postUri = new URI(useSSL ? "https" : "http", provider.host, "/FordSyncServlet/submit", null, null);

		httpPost.setURI(postUri);
		httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
		httpPost.setEntity(new ByteArrayEntity(post.getBytes()));

		if(Log.isEnabled())
			Log.d(TAG, "Uploading to " + postUri.toString());

		if(isCancelled() || httpPost.isAborted())
			return null;

		HttpClient c;
		if(trustAllCertificates) {
			HttpClient trustAll = HttpClientTrustAll.getNewHttpClient();
			c = trustAll;
		} else {
			c = client;
		}

		HttpResponse response = c.execute(httpPost, httpContext);

		if(Log.isEnabled())
			Log.d(TAG, "Uploaded to Mapquest. Status: " + response.getStatusLine().getStatusCode());

		if(isCancelled())
			return null;

		if(response == null || response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		sendToCarHtml = EntityUtils.toString(response.getEntity());
		if(Log.isEnabled())
			Log.d(TAG, "Response: " + Utils.htmlSnippet(sendToCarHtml));

		return sendToCarHtml;
	}

	private void parseSendToCar(String sendToCarHtml) throws BackgroundTaskAbort {
		JSONObject response;

		try {
			// replace ASCII character escapes \xAB with Unicode escapes \u00AB
			// since those are converted
			// automatically by the JSONObject parser to UTF-8 characters
			String html = sendToCarHtml.toString().replaceAll("\\\\x", "\\\\u00");

			response = new JSONObject(html);

			if(isCancelled())
				return;

			String result = response.getString("result");

			if(Log.isEnabled())
				Log.d(TAG, "Mapquest response JSON parsed OK. Status: " + result);

			if(result.equals("OK")) {
				// success
				return;
			}

		} catch(JSONException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while parsing Mapquest resposne JSON", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		String errorMsg = response.optString("message");
		if(Log.isEnabled())
			Log.e(TAG, "Could not send to Mapquest. Error: " + errorMsg);
		throw new BackgroundTaskAbort(errorMsg);

	}
}
