package com.jvanier.android.sendtocar.uploaders;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;

import android.content.Context;
import android.os.AsyncTask;

import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.SniHttpClient;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsGeocoder;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsGeocoder.GoogleMapsGeocoderHandler;
import com.jvanier.android.sendtocar.models.Address;
import com.jvanier.android.sendtocar.models.CarProvider;

public abstract class BaseUploader extends AsyncTask<Void, Void, Boolean> {

	public interface BaseUploaderHandler {
		public void onPreExecute(final BaseUploader self);

		public void onPostExecute(final BaseUploader self, Boolean result);
	}

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
		if(httpPost != null) {
			httpPost.abort();
		}
		if(httpGet != null) {
			httpGet.abort();
		}

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

		// Run onPreExecute now otherwise the busy dialog doesn't show up during
		// the geocoding
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
			setupHttp();
			return doUpload();
		} catch(BackgroundTaskAbort e) {
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

		client = new SniHttpClient();

		httpPost = new HttpPost();
		httpGet = new HttpGet();
	}
}
