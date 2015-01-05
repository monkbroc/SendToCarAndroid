package com.jvanier.android.sendtocar.downloaders;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.SniHttpClient;
import com.jvanier.android.sendtocar.common.Utils;
import com.jvanier.android.sendtocar.models.Address;
import com.jvanier.android.sendtocar.models.GoogleMapsHosts;

public class GoogleMapsAddressLoader extends AsyncTask<String, Void, GoogleMapsAddressLoader.Result> {
	private static final String TAG = "GoogleMapsAddressLoader";

	public interface GoogleMapsAddressLoaderHandler {
		public void onPreExecute(final GoogleMapsAddressLoader loader);

		public void onPostExecute(final GoogleMapsAddressLoader loader, Result result);
	}

	private GoogleMapsAddressLoaderHandler handler;
	private HttpGet httpGet;
	private HttpContext httpContext;
	private DefaultHttpClient client;

	private HttpHost mapHost;
	private Result result;

	public class Result {
		public boolean success;
		public Address address;
		public boolean approximateAddress;
		public int messageId;
	}

	public GoogleMapsAddressLoader(GoogleMapsAddressLoaderHandler handler) {
		this.handler = handler;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if(handler != null) {
			handler.onPreExecute(this);
		}
	}

	@Override
	protected void onPostExecute(Result result) {
		super.onPostExecute(result);
		if(handler != null) {
			handler.onPostExecute(this, result);
		}
	}

	public void cancelDownload() {
		cancel(false);
		httpGet.abort();
	}

	@Override
	protected Result doInBackground(String... urls) {
		try {
			result = new Result();

			setupHttp();
			String url = getUrl(urls);
			if(isCancelled())
				return result;
			String mapHtml = downloadMap(url);
			if(isCancelled())
				return result;
			verifyGoogleMapHost();
			if(isCancelled())
				return result;
			parseAddressData(mapHtml);
			if(isCancelled())
				return result;

			result.success = true;
			return result;

		} catch(BackgroundTaskAbort e) {
			result.messageId = e.messageId;
			return result;
		}
	}

	private void setupHttp() {
		httpContext = new BasicHttpContext();

		client = new SniHttpClient();

		RedirectHandler jsonMapRedirectHandler = new JSONMapRedirectHandler();
		client.setRedirectHandler(jsonMapRedirectHandler);

		httpGet = new HttpGet();
	}

	private String getUrl(String[] urls) throws BackgroundTaskAbort {
		if(urls.length > 0 && urls[0] != null) {
			return urls[0];
		}
		throw new BackgroundTaskAbort(R.string.errorNoUrl);
	}

	private String downloadMap(String url) throws BackgroundTaskAbort {
		String mapHtml = "";
		try {
			if(Log.isEnabled())
				Log.d(TAG, "Downloading " + url);

			httpGet.setURI(new URI(url));
			HttpResponse response = client.execute(httpGet, httpContext);

			if(Log.isEnabled())
				Log.d(TAG, "Downloaded address. Status: " + response.getStatusLine().getStatusCode());

			if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
				if(Log.isEnabled())
					Log.e(TAG, "Bad HTTP code while downloading address: " + response.getStatusLine().getStatusCode());
				throw new BackgroundTaskAbort(R.string.errorGoogleMapsLongPressBug);
			}

			mapHtml = EntityUtils.toString(response.getEntity());

			if(Log.isEnabled())
				Log.d(TAG, "Response: " + Utils.htmlSnippet(mapHtml));

			/*
			 * Get the name of the Google server that sent the map after any
			 * redirects
			 */
			mapHost = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			// HttpUriRequest currentReq = (HttpUriRequest)
			// httpContext.getAttribute(
			// ExecutionContext.HTTP_REQUEST);
			// String currentUrl = mapHost.toURI() + currentReq.getURI();

			if(Log.isEnabled())
				Log.d(TAG, "Google Maps host: " + mapHost.getHostName());

		} catch(URISyntaxException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while downloading address: " + e.toString());
			throw new BackgroundTaskAbort(R.string.errorDownload);

		} catch(IOException e) {
			if(Log.isEnabled())
				Log.d(TAG, "Exception while downloading address: " + e.toString());
			throw new BackgroundTaskAbort(R.string.errorDownload);
		}

		return mapHtml;
	}

	private void verifyGoogleMapHost() throws BackgroundTaskAbort {
		if(mapHost == null || !GoogleMapsHosts.doesHostExist(mapHost.getHostName())) {
			throw new BackgroundTaskAbort(R.string.errorNotGoogleMaps);
		}
	}

	private void parseAddressData(String mapHtml) throws BackgroundTaskAbort {
		result.approximateAddress = false;

		// find the JSON section called "markers"
		String startStr = "{sxcar:true,markers:";
		int startPos = mapHtml.indexOf(startStr);
		if(startPos == -1) {
			// if addresses in this country are not supported, do a geocode
			// lookup for an approximate address
			startStr = "{sxcar:false,markers:";
			startPos = mapHtml.indexOf(startStr);
		}

		if(startPos >= 0) {
			String match = mapHtml.substring(startPos + startStr.length());

			// replace ASCII character escapes \xAB with Unicode escapes \u00AB
			// since those are converted
			// automatically by the JSONObject parser to UTF-8 characters
			String rawData = match.replaceAll("\\\\x", "\\\\u00");

			try {
				JSONArray markers = new JSONArray(rawData);
				if(markers.length() >= 1) {
					JSONObject mapData = markers.getJSONObject(0);

					Address address = new Address();
					address.title = Utils.decodeHtml(mapData.optString("sxti", ""));
					address.street = Utils.decodeHtml(mapData.optString("sxst", ""));
					address.number = Utils.decodeHtml(mapData.optString("sxsn", ""));
					address.city = Utils.decodeHtml(mapData.optString("sxct", ""));
					address.province = Utils.decodeHtml(mapData.optString("sxpr", ""));
					address.postalCode = Utils.decodeHtml(mapData.optString("sxpo", ""));
					address.country = Utils.decodeHtml(mapData.optString("sxcn", ""));
					address.internationalPhone = Utils.decodeHtml(mapData.optString("sxph", ""));
					address.phone = "";

					if(mapData.has("infoWindow")) {
						JSONObject info = mapData.getJSONObject("infoWindow");
						if(info.has("title") && address.title.length() == 0) {
							address.title = Utils.decodeHtml(info.optString("title", ""));
						}

						if(info.has("addressLines")) {
							JSONArray lines = info.getJSONArray("addressLines");
							StringBuilder sb = new StringBuilder();
							for(int i = 0; i < lines.length(); i++) {
								if(i != 0) {
									sb.append("\n");
								}
								sb.append(lines.getString(i));
							}
							address.displayAddress = Utils.decodeHtml(sb.toString());
							if(address.title.length() == 0) {
								// use first line of address as title
								address.title = lines.optString(0, "");
							}
						}

						if(info.has("phones")) {
							JSONArray phones = info.getJSONArray("phones");
							if(phones.length() > 0) {
								address.phone = Utils.decodeHtml(phones.getString(0).replaceAll("\\D", ""));
							}
						}
					}

					if(mapData.has("latlng")) {
						JSONObject latlng = mapData.getJSONObject("latlng");
						address.latitude = latlng.optString("lat");
						address.longitude = latlng.optString("lng");

						/*
						 * Get reverse geocoded address from Google Maps API if
						 * the address is completely empty
						 */
						if(address.number.length() == 0 && address.city.length() == 0 && address.province.length() == 0
								&& address.country.length() == 0) {
							result.approximateAddress = true;
							downloadGeocodedAddress(address);
						}

					}

					result.address = address;

					if(Log.isEnabled())
						Log.d(TAG, "Address JSON parsed OK.");
				} else {
					if(Log.isEnabled())
						Log.e(TAG, "Wrong format while parsing address JSON");
					throw new BackgroundTaskAbort(R.string.errorDownload);
				}
			} catch(JSONException e) {
				if(Log.isEnabled())
					Log.e(TAG, "Exception while parsing address JSON: " + e.toString());
				throw new BackgroundTaskAbort(R.string.errorDownload);
			}
		} else {
			if(Log.isEnabled())
				Log.e(TAG, "Cannot find address JSON");
			throw new BackgroundTaskAbort(R.string.errorGoogleMapsIncompleteAddressBug);
		}
	}

	private void downloadGeocodedAddress(Address address) throws BackgroundTaskAbort {

		HashMap<String, String> mapping = new HashMap<String, String>();
		mapping.put("street_number", "number");
		mapping.put("route", "street");
		mapping.put("locality", "city");
		mapping.put("postal_code", "postalCode");
		mapping.put("administrative_area_level_1", "province");
		mapping.put("country", "country");

		String geoURI = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + address.latitude + "," + address.longitude
				+ "&sensor=false";

		String geoHtml = "";
		try {
			if(Log.isEnabled())
				Log.d(TAG, "Geocoding  " + geoURI);

			httpGet.setURI(new URI(geoURI));
			HttpResponse response = client.execute(httpGet, httpContext);

			if(Log.isEnabled())
				Log.d(TAG, "Geocoding address. Status: " + response.getStatusLine().getStatusCode());

			if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
				return;
			}

			geoHtml = EntityUtils.toString(response.getEntity());

			if(Log.isEnabled())
				Log.d(TAG, "Response: " + Utils.htmlSnippet(geoHtml));

		} catch(IOException | URISyntaxException e) {
			if(Log.isEnabled())
				Log.d(TAG, "Exception while geocoding address: " + e.toString());
		}

		try {
			String oldNumber = "";
			String newNumber = "";

			JSONObject geoJSON = new JSONObject(geoHtml);
			JSONArray results = geoJSON.getJSONArray("results");
			if(results.length() > 0) {
				JSONArray components = results.getJSONObject(0).getJSONArray("address_components");

				for(int i = 0; i < components.length(); i++) {
					JSONObject component = components.getJSONObject(i);
					JSONArray types = component.getJSONArray("types");

					for(int j = 0; j < types.length(); j++) {
						String t = types.getString(j);
						String field = mapping.get(t);
						if(field != null) {
							String value = Utils.decodeHtml(component.getString("short_name"));
							/*
							 * special case for street number: remove part after
							 * dash
							 */
							if(field.equals("number")) {
								oldNumber = value;
								value = value.replaceFirst("-.*", "");
								newNumber = value;
							}
							address.getClass().getDeclaredField(field).set(address, value);
						}
					}
				}

				String display = Utils.decodeHtml(results.getJSONObject(0).optString("formatted_address", address.displayAddress));
				address.displayAddress = display.replaceFirst(oldNumber, newNumber);
			}

		} catch(JSONException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while parsing geocoding JSON: " + e.toString());
		} catch(IllegalAccessException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while updating address: " + e.toString());
		} catch(NoSuchFieldException e) {
			if(Log.isEnabled())
				Log.e(TAG, "Exception while updating address: " + e.toString());
		}
	}

	public class JSONMapRedirectHandler extends DefaultRedirectHandler {

		@Override
		public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
			URI defaultRedir = super.getLocationURI(response, context);
			String query = defaultRedir.getQuery();
			if(query != null) {
				query = query + "&output=json";
			}

			try {
				URI redir = new URI(defaultRedir.getScheme(), defaultRedir.getAuthority(), defaultRedir.getPath(), query,
						defaultRedir.getFragment());
				return redir;
			} catch(URISyntaxException e) {
				return defaultRedir;
			}
		}
	}

}
