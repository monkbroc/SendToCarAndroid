package com.jvanier.android.sendtocar.downloaders;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
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
			
			if(carList.carsEmpty() || carList.timeToReDownload() || carList.localeChanged(country, language)) {
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


	private class DownloadCarListTask extends AsyncTask<String, Void, Boolean> {
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
		protected Boolean doInBackground(String... mapHosts) {
			log.d("Starting car download");
			Boolean ret = Boolean.FALSE;
			
			try {
				setupHttp();
				
				for(String host : mapHosts) {
					if(host == null) {
						continue;
					}
						
					String carsJson = downloadCars(host);
					if(isCancelled()) return Boolean.FALSE;
					if(carsJson.length() == 0) {
						continue;
					}
					parseCarsData(carsJson, host);
					if(isCancelled()) return Boolean.FALSE;
					addMapQuestCars();
					if(isCancelled()) return Boolean.FALSE;
					
					ret = Boolean.TRUE;
				}
			} catch(BackgroundTaskAbort e) {
				return Boolean.FALSE;
			}
			
			return ret;
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
				
				switch(response.getStatusLine().getStatusCode()) {
					case HttpURLConnection.HTTP_OK:
					break;
					case HttpURLConnection.HTTP_NOT_FOUND:
					log.d("Car list not found. Skipping server " + host);
					return "";
					default:
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
						
						c.show_phone = !provider.optBoolean("suppress_extra_phone", false);
						c.show_notes= !provider.optBoolean("suppress_notes", false);
						
						carListTemp.add(c);
					}
					
				}
					
				log.d("Cars JSON parsed OK.");
				
			} catch(JSONException e) {
				log.d("<span style=\"color: red;\">Exception while parsing cars JSON: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(); 
			}
		}
		
		private void addMapQuestCars() {

			ArrayList<CarProvider> cl = carListTemp.getList();
			
			String[] fordIds = { "car_ford", "car_ford_lincoln", "car_ford_mercury" };
			String[] fordMakes = { "Ford", "Lincoln", "Mercury" };

			for(int i = 0; i < fordIds.length; i++) {
				CarProvider ford = new CarProvider();
				ford.id = fordIds[i];
				ford.host = SendToCarActivity.MAPQUEST;
				ford.make = fordMakes[i];
				ford.account = "Mobile Phone Number:";
				ford.type = 1;
				ford.system = "SYNC";
				ford.show_phone = false;
				ford.show_notes = false;

				if(!cl.contains(ford)) {
					carListTemp.add(ford);
				}
			}

			String[] onstarIds = { "car_gm_buick", "car_gm_cadillac", "car_gm_chevrolet", "car_gm_gmc", "car_gm_hummer", "car_gm_pontiac", "car_gm_saab", "car_gm_saturn", "car_onstar", "pnd_onstar"};
			String[] onstarMakes = { "Buick", "Cadillac", "Chevrolet", "GMC", "Hummer", "Pontiac", "Saab", "Saturn", "OnStar", "OnStar FMV" };
			int[]    onstarTypes = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 };

			for(int i = 0; i < onstarIds.length; i++) {
				CarProvider onstar = new CarProvider();
				onstar.id = onstarIds[i];
				onstar.host = SendToCarActivity.ONSTAR;
				onstar.make = onstarMakes[i];
				onstar.account = "OnStar Username:";
				onstar.type = onstarTypes[i];
				onstar.system = "OnStar";
				onstar.show_phone = false;
				onstar.show_notes = false;
				

				if(!cl.contains(onstar)) {
					carListTemp.add(onstar);
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			if(result.booleanValue()) {
				carList = carListTemp;
				writeCars();
			}
		}
	}

}
