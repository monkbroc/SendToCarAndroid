package com.jvanier.android.sendtocar.downloaders;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import android.util.Log;

import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.models.CarList;
import com.jvanier.android.sendtocar.models.CarProvider;

public class CarListManager {
	private static final String TAG = "CarListLoader";

	private static final String FILENAME = "cars.ser";

	private CarList carList;

	private UpdateCarListTask task;

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
		if (task == null) {
			task = new UpdateCarListTask(context, language);
			task.execute();
		}
	}
	
	private void finishTask() {
		task = null;
	}
	
	

	private class BackgroundTaskAbort extends Exception {
		private static final long serialVersionUID = 2800070333002771844L;
	}

	private class UpdateCarListTask extends AsyncTask<Void, Void, Boolean> {
		private Context context;
		private String language;

		private CarList downloadedCarList;

		private DefaultHttpClient client;
		private HttpGet httpGet;

		public UpdateCarListTask(Context context, String language) {
			this.context = context;
			this.language = language;
		}

		@Override
		protected Boolean doInBackground(Void... ignored) {
			Log.d(TAG, "Starting car download");

			try {
				if (readCarList() == false) {
					if (isCancelled())
						return Boolean.FALSE;

					downloadedCarList = new CarList(language);

					setupHttp();
					String carsJson = downloadCarList();

					if (isCancelled() || carsJson.length() == 0)
						return Boolean.FALSE;
					parseCarsData(carsJson);

					setCarList(downloadedCarList);

					if (isCancelled())
						return Boolean.FALSE;

					writeCarList(downloadedCarList);
				}

				return Boolean.TRUE;
			} catch (BackgroundTaskAbort e) {
				return Boolean.FALSE;
			}
		}

		protected boolean readCarList() {
			FileInputStream fis = null;
			ObjectInputStream in = null;
			boolean success = false;
			try {
				fis = context.openFileInput(FILENAME);
				in = new ObjectInputStream(fis);
				CarList readCarList = (CarList) in.readObject();
				in.close();

				setCarList(readCarList);

				if (readCarList.carsEmpty() || readCarList.timeToReDownload()
						|| readCarList.languageChanged(language)) {
					success = false;
				} else {
					success = true;
				}
			} catch (Exception e) {
				success = false;
			}

			return success;
		}

		public void writeCarList(CarList carList) {

			FileOutputStream fos = null;
			ObjectOutputStream out = null;
			try {
				fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
				out = new ObjectOutputStream(fos);
				out.writeObject(carList);
				out.close();
			} catch (IOException e) {
				// do nothing
			}
		}

		private void setupHttp() {
			client = new DefaultHttpClient();
			httpGet = new HttpGet();
		}

		private String downloadCarList() throws BackgroundTaskAbort {
			String carsJson = "";
			try {
				URI carsUri = new URI(MessageFormat.format(Constants.CARS_URL, language));
				Log.d(TAG, "Downloading car list from URL " + carsUri.toString());

				httpGet.setURI(carsUri);

				HttpResponse response = client.execute(httpGet);

				Log.d(TAG, "Downloaded cars. Status: " + response.getStatusLine().getStatusCode());

				switch (response.getStatusLine().getStatusCode()) {
				case HttpURLConnection.HTTP_OK:
					break;
				default:
					throw new BackgroundTaskAbort();
				}

				carsJson = EntityUtils.toString(response.getEntity());
				Log.d(TAG, "Response: " + carsJson);

			} catch (Exception e) {
				Log.e(TAG, "Exception while downloading cars: " + e.toString());
				throw new BackgroundTaskAbort();
			}

			return carsJson;
		}

		private void parseCarsData(CharSequence carsJson)
				throws BackgroundTaskAbort {

			try {
				JSONObject carsRaw = new JSONObject(carsJson.toString());
				JSONObject providers = carsRaw.getJSONObject("providers");

				Iterator<String> providerIterator = (Iterator<String>) providers
						.keys();
				while (providerIterator.hasNext()) {
					String name = providerIterator.next();
					JSONObject provider = providers.getJSONObject(name);

					CarProvider c = new CarProvider();
					c.makeId = name;
					c.host = provider.getString("host");
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

				Log.d(TAG, "Cars JSON parsed OK.");

			} catch (JSONException e) {
				Log.e(TAG, "Exception while parsing cars JSON: " + e.toString());
				throw new BackgroundTaskAbort();
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			finishTask();
		}
	}
}
