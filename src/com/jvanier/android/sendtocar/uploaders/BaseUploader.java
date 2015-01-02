package com.jvanier.android.sendtocar.uploaders;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Utils;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsGeocoder;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsGeocoder.GoogleMapsGeocoderHandler;
import com.jvanier.android.sendtocar.models.Address;
import com.jvanier.android.sendtocar.models.CarProvider;


public abstract class BaseUploader extends AsyncTask<Void, Void, Boolean> {
	
	public interface BaseUploaderHandler {
		public void onPreExecute(final BaseUploader self);
		public void onPostExecute(final BaseUploader self, Boolean result);
	}

	private static final String TAG = "BaseUploader";

	private Context context;
	private BaseUploaderHandler handler;
	private String errorMessage;
	private int errorStringId;
	
	protected Address address;
	protected String account;
	protected CarProvider provider;
	protected String language;
	protected String notes;
	
	protected BasicHttpContext httpContext;
	protected BasicCookieStore cookieStore;
	protected DefaultHttpClient client;
	protected HttpPost httpPost;
	protected HttpGet httpGet;

	private GoogleMapsGeocoder geocoder;

	public BaseUploader(Context context, BaseUploaderHandler handler) {
		this.handler = handler;
		this.context = context;
	}

	public Context getContext() {
		return context;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public int getErrorStringId() {
		return errorStringId;
	}

	public Address getAddress() {
		return address;
	}

	public String getAccount() {
		return account;
	}

	public CarProvider getProvider() {
		return provider;
	}

	public String getLanguage() {
		return language;
	}

	public String getNotes() {
		return notes;
	}
	public void cancelUpload() {
		cancel(false);
		httpPost.abort();
		httpGet.abort();
		
		if(geocoder != null) {
			geocoder.cancelGeocode();
			geocoder = null;
		}
	}
	
	public void sendDestination(Address address, String account, CarProvider provider, String language, String notes) {
		this.address = address;
		this.account = account;
		this.provider = provider;
		this.language = language;
		this.notes = notes;
		
		setupHttp();
		
		// Run onPreExecute now otherwise the busy dialog doesn't show up during the geocoding 
		if(handler != null) {
			handler.onPreExecute(this);
		}
		
		if(!address.hasAddressDetails() || !address.hasLatitudeLongitude()) {
			// Geocode first
			geocoder = new GoogleMapsGeocoder(new GoogleMapsGeocoderUploaderHandler());
			if(address.hasAddressDetails() && !address.hasLatitudeLongitude()) {
				geocoder.setUpdateLatLngOnly(true);
			}
			geocoder.execute(address);
		} else {
			// Upload right away
			execute((Void) null);
		}
	}
	
	private class GoogleMapsGeocoderUploaderHandler implements GoogleMapsGeocoderHandler {

		@Override
		public void onPreExecute(GoogleMapsGeocoder self) {
		}

		@Override
		public void onPostExecute(GoogleMapsGeocoder self, Address geocodedAddress) {
			if(geocodedAddress != null) {
				address = geocodedAddress;
			}
			geocoder = null;

			// Upload after geocoding, regardless of error
			execute((Void) null);
		}
	}
	
	@Override
	protected Boolean doInBackground(Void... ignored) {
		try {
			if(address.latitude.length() == 0 || address.longitude.length() == 0) {
				updateAddressLatLong(address);
			}
			
			return doUpload();
		} catch (BackgroundTaskAbort e) {
			errorStringId = e.messageId;
			errorMessage = e.getMessage();
			return Boolean.FALSE;
		}
	}

	protected abstract Boolean doUpload() throws BackgroundTaskAbort;

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if(handler != null) {
			handler.onPostExecute(this, result);
		}
	}

	private void setupHttp() {
		httpContext = new BasicHttpContext();
		cookieStore = new BasicCookieStore();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		client = new DefaultHttpClient();
		httpPost = new HttpPost();
		httpGet = new HttpGet();
	}

	// TODO: put it its own class
	private void updateAddressLatLong(Address address) throws BackgroundTaskAbort {
		
		StringBuilder str = new StringBuilder();
		
		if(address.number.length() > 0) {
			str.append(address.number).append(" ");
		}
		
		if(address.street.length() > 0) {
			str.append(address.street).append(",");
		}
		
		if(address.city.length() > 0) {
			str.append(address.city).append(",");
		}
		if(address.province.length() > 0) {
			str.append(address.province).append(",");
		}
		if(address.postalCode.length() > 0) {
			str.append(address.postalCode).append(",");
		}
		if(address.country.length() > 0) {
			str.append(address.country).append(",");
		}

		String geoURI = "";
		try {
			geoURI = "http://maps.googleapis.com/maps/api/geocode/json?address=" +
					URLEncoder.encode(str.toString(), "utf-8") + "&sensor=false";
		} catch (UnsupportedEncodingException e1) {
			// ignore
		} 

		String geoHtml = "";
		try
		{
			Log.d(TAG, "Updating latitude/longitude  " + geoURI);
			httpGet.setURI(new URI(geoURI));
			HttpResponse response = client.execute(httpGet, httpContext);

			Log.d(TAG, "Updating lat/long. Status: " + response.getStatusLine().getStatusCode());

			if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
			{
				return;
			}

			geoHtml = EntityUtils.toString(response.getEntity());

			Log.d(TAG, "Response: " + Utils.htmlSnippet(geoHtml));


		} catch(Exception e) {
			Log.e(TAG, "Exception while geocoding address", e);
		}
		
		try {
			
			JSONObject geoJSON = new JSONObject(geoHtml);
			JSONArray results = geoJSON.getJSONArray("results");
			if(results.length() > 0) {
				JSONObject location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
				
				String lat = location.getString("lat");
				String lng = location.getString("lng");
				
				address.latitude = lat;
				address.longitude = lng;
			}
			
		} catch(JSONException e) {
			Log.e(TAG, "Exception while parsing geocoding JSON", e);
		}
	}
	

}
