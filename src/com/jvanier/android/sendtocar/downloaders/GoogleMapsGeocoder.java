package com.jvanier.android.sendtocar.downloaders;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.SniHttpClient;
import com.jvanier.android.sendtocar.common.Utils;
import com.jvanier.android.sendtocar.models.Address;

public class GoogleMapsGeocoder extends AsyncTask<Address, Void, Address> {
	public interface GoogleMapsGeocoderHandler {
		public void onPreExecute(final GoogleMapsGeocoder self);

		public void onPostExecute(final GoogleMapsGeocoder self, Address geocodedAddress);
	}

	private static final String GEOCODER_URL_TEMPLATE = "http://maps.googleapis.com/maps/api/geocode/json?address={0}&sensor=false";

	private static final String TAG = "GoogleMapsGeocoder";

	private GoogleMapsGeocoderHandler handler;
	protected HttpGet httpGet;

	private boolean updateLatLngOnly;

	public GoogleMapsGeocoder(GoogleMapsGeocoderHandler handler) {
		this.handler = handler;
	}

	public void setUpdateLatLngOnly(boolean updateLatLngOnly) {
		this.updateLatLngOnly = updateLatLngOnly;
	}

	public void cancelGeocode() {
		cancel(false);
		httpGet.abort();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		setupHttp();

		if(handler != null) {
			handler.onPreExecute(this);
		}
	}

	@Override
	protected void onPostExecute(Address geocodedAddress) {
		super.onPostExecute(geocodedAddress);
		if(handler != null) {
			handler.onPostExecute(this, geocodedAddress);
		}
	}

	private void setupHttp() {
		httpGet = new HttpGet();
	}

	@Override
	protected Address doInBackground(Address... addresses) {
		try {
			if(addresses != null && addresses.length > 0) {
				Address address = addresses[0];
				String addressQuery = buildAddressQuery(address);
				return geocodeAddress(address, addressQuery);
			}
		} catch(BackgroundTaskAbort e) {
			return null;
		}
		return null;
	}

	private String buildAddressQuery(Address address) {
		StringBuilder str = new StringBuilder();

		if(address.hasAddressDetails()) {
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
		} else {
			str.append(address.displayAddress);
		}

		return str.toString();
	}

	private Address geocodeAddress(Address address, String addressQuery) throws BackgroundTaskAbort {

		String geoURI = "";
		try {
			geoURI = MessageFormat.format(GEOCODER_URL_TEMPLATE, URLEncoder.encode(addressQuery, "utf-8"));
		} catch(UnsupportedEncodingException e1) {
			// ignore
		}

		String geoHtml = "";
		try {
			if(Log.isEnabled()) Log.d(TAG, "Updating latitude/longitude  " + geoURI);
			httpGet.setURI(new URI(geoURI));
			DefaultHttpClient client = new SniHttpClient();
			HttpResponse response = client.execute(httpGet);

			if(Log.isEnabled()) Log.d(TAG, "Updating lat/long. Status: " + response.getStatusLine().getStatusCode());

			if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}

			geoHtml = EntityUtils.toString(response.getEntity());

			if(Log.isEnabled()) Log.d(TAG, "Response: " + Utils.htmlSnippet(geoHtml));

		} catch(Exception e) {
			if(Log.isEnabled()) Log.e(TAG, "Exception while geocoding address", e);
		}

		try {

			JSONObject geoJSON = new JSONObject(geoHtml);
			JSONArray results = geoJSON.getJSONArray("results");
			if(results.length() > 0) {
				/*
				 * Example JSON { "results" : [ { "address_components" : [ {
				 * "long_name" : "1600", "short_name" : "1600", "types" : [
				 * "street_number" ] }, { "long_name" : "Amphitheatre Pkwy",
				 * "short_name" : "Amphitheatre Pkwy", "types" : [ "route" ] },
				 * { "long_name" : "Mountain View", "short_name" :
				 * "Mountain View", "types" : [ "locality", "political" ] }, {
				 * "long_name" : "Santa Clara", "short_name" : "Santa Clara",
				 * "types" : [ "administrative_area_level_2", "political" ] }, {
				 * "long_name" : "California", "short_name" : "CA", "types" : [
				 * "administrative_area_level_1", "political" ] }, { "long_name"
				 * : "United States", "short_name" : "US", "types" : [
				 * "country", "political" ] }, { "long_name" : "94043",
				 * "short_name" : "94043", "types" : [ "postal_code" ] } ],
				 * "formatted_address" :
				 * "1600 Amphitheatre Pkwy, Mountain View, CA 94043, USA",
				 * "geometry" : { "location" : { "lat" : 37.42291810, "lng" :
				 * -122.08542120 }, "location_type" : "ROOFTOP", "viewport" : {
				 * "northeast" : { "lat" : 37.42426708029149, "lng" :
				 * -122.0840722197085 }, "southwest" : { "lat" :
				 * 37.42156911970850, "lng" : -122.0867701802915 } } }, "types"
				 * : [ "street_address" ] } ], "status" : "OK" }
				 */
				JSONObject location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");

				String lat = location.getString("lat");
				String lng = location.getString("lng");

				address.latitude = lat;
				address.longitude = lng;

				if(!updateLatLngOnly) {
					JSONArray addressComponents = results.getJSONObject(0).getJSONArray("address_components");
					for(int i = 0; i < addressComponents.length(); i++) {
						JSONObject addressComponent = addressComponents.getJSONObject(i);
						String longValue = addressComponent.getString("long_name");
						String shortValue = addressComponent.getString("short_name");

						JSONArray componentTypes = addressComponent.getJSONArray("types");
						for(int t = 0; t < componentTypes.length(); t++) {
							String type = componentTypes.getString(t);

							switch(type) {
							case "street_number":
								address.number = longValue;
								break;
							case "route":
								address.street = longValue;
								break;
							case "locality":
								address.city = longValue;
								break;
							case "administrative_area_level_1":
								address.province = longValue;
								break;
							case "country":
								address.country = shortValue;
								break;
							case "postal_code":
								address.postalCode = longValue;
								break;
							}
						}
					}
				}

				return address;
			}

		} catch(JSONException e) {
			if(Log.isEnabled()) Log.e(TAG, "Exception while parsing geocoding JSON", e);
		}

		return null;
	}

}
