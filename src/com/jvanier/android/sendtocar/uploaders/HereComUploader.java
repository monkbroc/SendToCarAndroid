package com.jvanier.android.sendtocar.uploaders;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.HttpClientTrustAll;
import com.jvanier.android.sendtocar.common.Utils;

public class HereComUploader extends BaseUploader {

	private static final String TAG = "HereComUploader";
	private String csrfToken;

	public HereComUploader(Context context, BaseUploaderHandler handler) {
		super(context, handler);
	}

	@Override
	protected Boolean doUpload() throws BackgroundTaskAbort {
		if (isCancelled())
			return Boolean.FALSE;
		String post = preparePostData();
		if (isCancelled())
			return Boolean.FALSE;
		sendToCar(post);
		return Boolean.TRUE;
	}

	private String preparePostData() throws BackgroundTaskAbort {
		try {
			NumberFormat numberFormat = DecimalFormat.getInstance(Locale.US);

			JSONArray placePosition = new JSONArray();
			placePosition.put(numberFormat.parse(getAddress().latitude).doubleValue());
			placePosition.put(numberFormat.parse(getAddress().longitude).doubleValue());

			JSONObject placeLocation = new JSONObject();
			placeLocation.put("position", placePosition);
			String placeLocationJson = placeLocation.toString();

			JSONObject destination = new JSONObject();
			destination.put("carId", getAccount());
			destination.put("manufacturer", getProvider().makeId);
			destination.put("placeName", getAddress().title);
			// strangely enough placeLocation must be a JSON-encoded string, not
			// a
			// nested JSON object
			destination.put("placeLocation", placeLocationJson);

			String post = destination.toString();

			Log.d(TAG, "Sending to car. Post data: " + post);

			return post;
		} catch (JSONException e) {
			Log.e(TAG, "JSON exception while preparing Here.com post data", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		} catch (ParseException e) {
			Log.e(TAG, "Latitude/longitude parse exception while preparing Here.com post data", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}
	}

	private void populateHereComCookieAndCSRF(boolean useSSL, boolean trustAllCertificates) {
		try {
			URI mainPage = new URI((useSSL ? "https" : "http"), provider.host, "/", null, null);
			Log.d(TAG, "Downloading main Here.com page to fill the cookie jar and get the CSRF token");

			HttpGet httpGet = new HttpGet();
			httpGet.setURI(mainPage);
			// must set the User-Agent field to a non-mobile browser otherwise
			// Here.com sends garbage
			httpGet.setHeader("User-Agent", Constants.SENDTOCAR_USERAGENT);

			HttpResponse response = getClient(trustAllCertificates).execute(httpGet, httpContext);

			Log.d(TAG, "Downloaded cookie. Status: " + response.getStatusLine().getStatusCode());

			String body = EntityUtils.toString(response.getEntity());
			csrfToken = parseCSRF(body);

		} catch (Exception e) {
			Log.e(TAG, "Error getting Here.com cookie", e);
			// ignore
		}
	}

	private HttpClient getClient(boolean trustAllCertificates) {
		if(trustAllCertificates) {	
			HttpClient trustAll = HttpClientTrustAll.getNewHttpClient();
			return trustAll;
		} else {
			return client;
		}
	}

	private String parseCSRF(String body) {
		Pattern pattern = Pattern.compile("csrf: \"([^\"]*)\"");
		Matcher matcher = pattern.matcher(body);
		if (matcher.find()) {
			Log.d(TAG, "CSRF block found");
			return matcher.group(0);
		} else {
			Log.d(TAG, "CSRF block not found");
			return "";
		}
	}

	private String sendToCar(String post) throws BackgroundTaskAbort {
		String sendToCarHtml = "";
		try {
			sendToCarHtml = sendToCarCore(post, true, false);
		} catch (InterruptedIOException e) {
			Log.w(TAG, "Upload to Here.com aborted");
			return null;
		} catch(SSLPeerUnverifiedException e) {
			Log.e(TAG, "SSLPeerUnverifiedException while sending to MapQuest. Trying again in fallback mode.");
			return sendToCarTrustAll(post);
		} catch (SSLException e) {
			Log.e(TAG, "SSLException while sending to Here.com. Trying again without HTTPS.");
			return sendToCarNoSSL(post);
		} catch (Exception e) {
			Log.e(TAG, "Exception while sending to Here.com", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		return sendToCarHtml;
	}

	/* Workaround for bug in Android 4.1.1 that makes SSL connections fail on 4G
	 * => Accept all certificates as valid */
	private String sendToCarTrustAll(String post) throws BackgroundTaskAbort {
		String sendToCarHtml = "";
		try
		{
			sendToCarHtml = sendToCarCore(post, true, true);
		} catch(InterruptedIOException e) {
			Log.w(TAG, "Upload to Mapquest aborted");
			return null;
		} catch(Exception e) {
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
		} catch (InterruptedIOException e) {
			Log.w(TAG, "Upload to Here.com aborted");
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Exception while sending to Here.com (fallback mode)", e);
			throw new BackgroundTaskAbort(R.string.errorSendToCar);
		}

		return sendToCarHtml;
	}

	private String sendToCarCore(String post, boolean useSSL, boolean trustAllCertificates) throws BackgroundTaskAbort, URISyntaxException, ClientProtocolException, IOException {
		String sendToCarHtml = "";
		populateHereComCookieAndCSRF(useSSL, trustAllCertificates);

		URI postUri = new URI((useSSL ? "https" : "http"), provider.host, "/api/sendToCar/message", null, null);

		httpPost.setURI(postUri);
		httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
		httpPost.addHeader("x-csrf-token", csrfToken);
		// must set the User-Agent field to a non-mobile browser otherwise
		// Here.com sends garbage
		httpPost.setHeader("User-Agent", Constants.SENDTOCAR_USERAGENT);
		httpPost.setEntity(new ByteArrayEntity(post.getBytes()));

		Log.d(TAG, "Uploading to " + postUri.toString());

		if (isCancelled() || httpPost.isAborted())
			return null;

		HttpResponse response = getClient(trustAllCertificates).execute(httpPost, httpContext);

		int statusCode = response.getStatusLine().getStatusCode();

		Log.d(TAG, "Uploaded to car. Status: " + statusCode);

		if (isCancelled())
			return null;

		if (statusCode != HttpURLConnection.HTTP_OK) {
			switch (statusCode) {
			case 409:
				throw new BackgroundTaskAbort(R.string.statusInvalidAccount);
			default:
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			}
		}
		
		sendToCarHtml = EntityUtils.toString(response.getEntity());
		Log.d(TAG, "Response: " + Utils.htmlSnippet(sendToCarHtml));

		return sendToCarHtml;
	}
}
