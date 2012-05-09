package com.jvanier.android.sendtocar;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Iterator;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;


public class CarListLoader {

	private static final String FILENAME = "cars.ser";
	public static CarList carList;
	private DebugLog log;
	private Context context;
	private String country;
	private String language;
	
	public CarListLoader(Context context, DebugLog log) {
		this.context = context;
		this.log = log;
		
		country = Locale.getDefault().getCountry();
		language = Locale.getDefault().getLanguage();
	}

	public void readCars() {

		FileInputStream fis = null;
		ObjectInputStream in = null;
		try
		{
			fis = context.openFileInput(FILENAME);
			in = new ObjectInputStream(fis);
			carList = (CarList)in.readObject();
			in.close();
			
			if(carList.timeToReDownload() || carList.localeChanged(country, language)) {
				loadCars();
			}
		}
		catch(Exception e)
		{
			loadCars();
		}
	}

	public void writeCars() {

		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try
		{
			fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
			out = new ObjectOutputStream(fos);
			out.writeObject(carList);
			out.close();
		}
		catch(IOException e)
		{
			// do nothing
		}
	}
	
	public void loadCars() {
		
		String[] hosts = new String[2];
		hosts[0] = "maps.google.com";
		hosts[1] = GoogleMapsHosts.getHostByCountry(country);
		// don't download the US data twice
		if(hosts[0].equals(hosts[1])) {
			hosts[1] = null;
		}
		
		DownloadCarListTask t = new DownloadCarListTask(language);
		t.execute(hosts);
	}
	
	private class BackgroundTaskAbort extends Exception {
		private static final long serialVersionUID = 2800070333002771844L;
	}


	private class DownloadCarListTask extends AsyncTask<String, Void, Void> {
		private HttpContext httpContext;
		private CookieStore cookieStore;
		private DefaultHttpClient client;
		private HttpGet httpGet;
		private String language;
		
		CarList carListTemp;
		
		public DownloadCarListTask(String language)
		{
			this.language = language;
			carListTemp = new CarList(country, language);
		}
		
		@Override
		protected Void doInBackground(String... mapHosts) {
			log.d("Starting car download");
			
			try {
				setupHttp();
				
				for(String host : mapHosts) {
					if(host == null) {
						continue;
					}
						
					String carsJson = downloadCars(host);
					if(isCancelled()) return null;
					parseCarsData(carsJson, host);
					if(isCancelled()) return null;
				}
			} catch(BackgroundTaskAbort e) {
				return null;
			}
			
			return null;
		}
		

		private void setupHttp() {
			httpContext = new BasicHttpContext();
			cookieStore = new BasicCookieStore();
			httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

			client = new DefaultHttpClient();
			httpGet = new HttpGet();
		}
	
		private String downloadCars(String host) throws BackgroundTaskAbort {
			String carsJson = "";
			try
			{
				URI carsUri = new URI("http", host, "/maps/sendtodata", "hl=" + language, null);
				log.d("Downloading " + carsUri.toString());
				
				httpGet.setURI(carsUri);
				
				HttpResponse response = client.execute(httpGet, httpContext);
				
				log.d("Downloaded cars. Status: " + response.getStatusLine().getStatusCode());
				
				if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK )
				{
					throw new BackgroundTaskAbort();
				}
				
				carsJson = EntityUtils.toString(response.getEntity());
				log.d("Response: <pre>" + log.htmlSnippet(carsJson) + "</pre>");
				
			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while downloading cars: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort();
			}
			
			return carsJson;
		}
	
		private void parseCarsData(CharSequence carsJson, String host) throws BackgroundTaskAbort {
	
			try {
				JSONObject carsRaw = new JSONObject(carsJson.toString().replaceAll("\\\\x", "\\\\u00"));
				JSONObject providers = carsRaw.getJSONObject("providers");
				
				@SuppressWarnings("unchecked")
				Iterator<String> it = (Iterator<String>)providers.keys();
				while(it.hasNext())
				{
					String name = it.next();
					JSONObject provider = providers.getJSONObject(name);
					
					/* Only take providers with a unique account number
					 * Other providers require special desktop software to send the address
					 */
					
					if(provider.has("account"))
					{
						CarProvider c = new CarProvider();
						c.id = name;
						c.host = host;
						c.make = provider.getString("make");
						c.type = provider.getInt("type");
						c.account = provider.getString("account");
						c.system = provider.getString("system");
						
						c.international_phone = provider.optBoolean("international_phone", false);
						c.use_destination_tag = provider.optBoolean("use_destination_tag", false);
						c.destination_tag = provider.optString("destination_tag", "");
						
						carListTemp.add(c);
					}
					
				}
					
				log.d("Cars JSON parsed OK.");
				
			} catch(JSONException e) {
				log.d("<span style=\"color: red;\">Exception while parsing cars JSON: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(); 
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			carList = carListTemp;
			writeCars();
		}
	}

}
