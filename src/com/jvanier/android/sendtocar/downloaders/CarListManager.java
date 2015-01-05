package com.jvanier.android.sendtocar.downloaders;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.SniHttpClient;
import com.jvanier.android.sendtocar.models.CarList;
import com.jvanier.android.sendtocar.models.CarProvider;

public class CarListManager {
	private static final String TAG = "CarListLoader";

	private static final String FILENAME = "cars.ser";
	private static final String DEFAULT_FILENAME = "cars_default.ser";

	private CarList carList;

	private static final CarListManager INSTANCE = new CarListManager();

	public static CarListManager sharedInstance() {
		return INSTANCE;
	}

	// Make singleton constructor private
	private CarListManager() {
	}

	public synchronized CarList getCarList() {
		return carList;
	}

	private synchronized void setCarList(CarList carList) {
		this.carList = carList;
	}

	public void updateCarList(Context context, String language) {
		// Read cache/default list on main thread to make sure there's at least some date for the UI
		if(readCarList(context, language) == false) {
			// Download new list if needed
			(new DownloadCarListTask(context, language)).execute();
		}
	}
	
	protected boolean readCarList(Context context, String language) {
		InputStream in = null;
		ObjectInputStream objIn = null;
		boolean success = false;
		
		final int READ_CACHE = 0;
		final int READ_DEFAULT = 1;
		for(int state = READ_CACHE; state <= READ_DEFAULT; state++) {
			try {
				if(state == READ_CACHE) {
					in = context.openFileInput(FILENAME);
				} else {
					in = context.getAssets().open(DEFAULT_FILENAME);
				}

				objIn = new ObjectInputStream(in);
				CarList readCarList = (CarList) objIn.readObject();
				objIn.close();

				if(Log.isEnabled()) Log.d(TAG, "Read car list with " + readCarList.size() + " providers from " + ((state == READ_CACHE) ? "cache" : "default"));
				setCarList(readCarList);

				if (state == READ_DEFAULT || readCarList.isEmpty() || readCarList.timeToReDownload()
						|| readCarList.languageChanged(language)) {
					success = false;
				} else {
					success = true;
				}
				// If cache was read successfully no need to read the default file
				break;
			} catch (Exception e) {
				success = false;
			}
		}

		return success;
	}

	private class DownloadCarListTask extends AsyncTask<Void, Void, Boolean> {
		private Context context;
		private String language;

		private CarList downloadedCarList;

		public DownloadCarListTask(Context context, String language) {
			this.context = context;
			this.language = language;
		}

		@Override
		protected Boolean doInBackground(Void... ignored) {
			if(Log.isEnabled()) Log.d(TAG, "Starting car download");

			try {
				downloadedCarList = new CarList(language);

				String carsJson = downloadCarList();

				if (carsJson.length() == 0)
					return Boolean.FALSE;
				parseCarsData(carsJson);

				setCarList(downloadedCarList);

				writeCarList(downloadedCarList);

				return Boolean.TRUE;
			} catch (BackgroundTaskAbort e) {
				return Boolean.FALSE;
			}
		}

		private String downloadCarList() throws BackgroundTaskAbort {
			String carsJson = "";
			try {
				URI carsUri = new URI(MessageFormat.format(Constants.CARS_URL, language));
				if(Log.isEnabled()) Log.d(TAG, "Downloading car list from URL " + carsUri.toString());


				HttpGet httpGet = new HttpGet();
				httpGet.setURI(carsUri);

				DefaultHttpClient client = new SniHttpClient();
				HttpResponse response = client.execute(httpGet);

				if(Log.isEnabled()) Log.d(TAG, "Downloaded cars. Status: " + response.getStatusLine().getStatusCode());

				switch (response.getStatusLine().getStatusCode()) {
				case HttpURLConnection.HTTP_OK:
					break;
				default:
					throw new BackgroundTaskAbort();
				}

				carsJson = EntityUtils.toString(response.getEntity());
				if(Log.isEnabled()) Log.d(TAG, "Response: " + carsJson);

			} catch (Exception e) {
				if(Log.isEnabled()) Log.e(TAG, "Exception while downloading cars: " + e.toString());
				throw new BackgroundTaskAbort();
			}

			return carsJson;
		}

		private void parseCarsData(String carsJson) throws BackgroundTaskAbort {

			try {
				JSONObject carsRaw = new JSONObject(carsJson);
				JSONObject providers = carsRaw.getJSONObject("providers");

				Iterator<String> providerIterator = (Iterator<String>) providers
						.keys();
				while (providerIterator.hasNext()) {
					String name = providerIterator.next();
					JSONObject provider = providers.getJSONObject(name);

					CarProvider c = new CarProvider();
					c.makeId = name;
					c.host = provider.getString("host");
					c.provider = provider.getInt("provider");
					c.make = provider.getString("make");
					c.type = provider.getInt("type");
					c.account = provider.getString("account");
					c.system = provider.getString("system");

					c.internationalPhone = provider.optBoolean("international_phone", false);
					c.showPhone = !provider.optBoolean("suppress_extra_phone", false);
					c.showNotes = !provider.optBoolean("suppress_notes", false);

					JSONArray supportedCountries = provider
							.optJSONArray("countries");
					if (supportedCountries != null) {
						for (int countryIndex = 0; countryIndex < supportedCountries.length(); countryIndex++) {
							String country = supportedCountries.getString(countryIndex);
							c.addSupportedCountry(country);
						}
					}

					downloadedCarList.addCarProvider(c);
				}

				if(Log.isEnabled()) Log.d(TAG, "Cars JSON parsed OK.");

			} catch (JSONException e) {
				if(Log.isEnabled()) Log.e(TAG, "Exception while parsing cars JSON: " + e.toString());
				throw new BackgroundTaskAbort();
			}
		}

		public void writeCarList(CarList carList) {
			try {
				FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
				ObjectOutputStream out = new ObjectOutputStream(fos);
				out.writeObject(carList);
				out.close();
				if(Log.isEnabled()) Log.d(TAG, "Wrote car list with " + carList.size() + " providers to cache");
			} catch (IOException e) {
				// do nothing
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
		}
	}
}
