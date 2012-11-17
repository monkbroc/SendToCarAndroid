
/* TODO:
 * - (Maybe) Integrate with Yelp
 * - Add explanation about "Address is approximate in help"
 */

package com.jvanier.android.sendtocar;

import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SendToCarActivity extends Activity {
	public static String MAPQUEST = "www.mapquest.com";
	public static String ONSTAR = "www.onstar.com";
	
	private ProgressDialog progressDialog;
	private DownloadAddressTask taskDownload;
	private SendToCarTask taskSend;

	private HttpHost mapHost;
	
	private HttpContext httpContext;
	private CookieStore cookieStore;
	private DefaultHttpClient client;
	
	private boolean manualEdit;
	private Address address;
	private boolean ignoreFirstSpinnerChange;
	
	private DebugLog log;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sendtocar);
				
		log = ((SendToCarApp) getApplication()).getLog();
		
		tagVisibilityAndText(false, "");
		manualAddressVisibility(false);
		setupSpinner();

		loadCars();
		
		setupHttp();

		loadMapFromIntent(getIntent());
		
		registerButtons();

	}

	private void loadCars() {
		CarListLoader loader = new CarListLoader(this, log);
		loader.readCars();
	}

	private void tagVisibilityAndText(boolean visible, String text) {
		TextView tagLabel = (TextView) findViewById(R.id.tagLabel);
		tagLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(visible)
		{
			tagLabel.setText(text);
		}
		
		EditText tagText = (EditText) findViewById(R.id.tagText);
		tagText.setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	private void notesVisibility(boolean visible) {
		View v = findViewById(R.id.notes);
		v.setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	private void manualAddressVisibility(boolean manualEdit) {
		this.manualEdit = manualEdit;
		
		View addressLabel = findViewById(R.id.addressLabel);
		addressLabel.setVisibility(!manualEdit ? View.VISIBLE : View.GONE);

		View addressText = findViewById(R.id.addressText);
		addressText.setVisibility(!manualEdit ? View.VISIBLE : View.GONE);
		
		View manualAddress = findViewById(R.id.manualAddress);
		manualAddress.setVisibility(manualEdit ? View.VISIBLE : View.GONE);
		
	}
	

	private void setupSpinner()
	{
		ArrayList<CarProvider> carsList = new ArrayList<CarProvider>();
		CarProvider fake = new CarProvider();
		fake.make = SendToCarActivity.this.getString(R.string.choose);
		fake.type = 0;
		carsList.add(fake);

		Spinner spinner = (Spinner) findViewById(R.id.makeSpinner);

		ArrayAdapter<CarProvider> adapter = new ArrayAdapter<CarProvider>(this,
				android.R.layout.simple_spinner_item, carsList.toArray(new CarProvider[carsList.size()]));

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}

	private void setupHttp() {
		httpContext = new BasicHttpContext();
		cookieStore = new BasicCookieStore();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		client = new DefaultHttpClient();

		RedirectHandler jsonMapRedirectHandler = new JSONMapRedirectHandler();
		client.setRedirectHandler(jsonMapRedirectHandler);

	}
	
	private void registerButtons() {
		Button sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setOnClickListener(new OnSendButtonClick());
		
		Button cancelButton = (Button) findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
		    @Override
			public void onClick(View v) {
		      finish();
		    }
		  });
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
	    if (manualEdit) {
	        menu.getItem(0).setEnabled(false);
	    }
	    return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
        case R.id.idManual:
        	manualAddressVisibility(true);
        	populatePhone();
        	
        	break;
        case R.id.idHelp:
            	startActivity(new Intent(SendToCarActivity.this, InformationActivity.class));
            	break;
	    }
	    return true;
	}

	private CarProvider getCar() {
		try {
			Spinner spinner = (Spinner) findViewById(R.id.makeSpinner);
			CarProvider car = (CarProvider)spinner.getSelectedItem();
			return car;
		}
		catch(ClassCastException e) {
			/* Cast (CarProvider)spinner.getSelectedItem() failed because populateMakes was never called.
			 * This is unusually and can be ignored. The user will press back and try again */
			return null;
		}
	}

	private void loadMapFromIntent(Intent i)
	{
		try {
			//log.d("Intent. Action: " + i.getAction() + ", Text: " + i.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString());
			if(i.getAction().equals(Intent.ACTION_SEND)) {
				String url = findURL(i.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString());
			
				log.d("URL: <a href=\""+ url + "\">url</a>");
			
				taskDownload = new DownloadAddressTask();
				taskDownload.execute(new String[] { url });
			}
			else
			{
		    	showMessageBoxAndFinish(getString(R.string.errorIntent));
			}
		} catch(NullPointerException e) {
			// not started from Google Maps, just open the manual address mode
			address = new Address();
			
			manualAddressVisibility(true);
			updateUI();
		}
	}
	
	private void showMessageBoxAndFinish(String message) {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setTitle(R.string.errorTitle);
        alertbox.setMessage(message);
        alertbox.setOnCancelListener(new OnCancelListener() {
        	@Override
        	public void onCancel(DialogInterface dialog) {
        		finish();
        	}
        });
        alertbox.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        	@Override
            public void onClick(DialogInterface arg0, int arg1) {
            	finish();
            }
        });

        // set a negative/no button and create a listener
        alertbox.setNegativeButton(R.string.showHelp, new DialogInterface.OnClickListener() {
        	@Override
            public void onClick(DialogInterface arg0, int arg1) {
            	finish();
            	startActivity(new Intent(SendToCarActivity.this, InformationActivity.class));
            }
        });

        // display box
        alertbox.show();
	}

	private String findURL(String string) {
		// match a URL, but try not to grab the punctuation at the end
		Pattern urlPatt = Pattern.compile("(?i)\\bhttps?://[a-z0-9_.\\-,@?^=%&:/~+#]*[a-z0-9_\\-@^=%&:/~+#]");
		Matcher matcher = urlPatt.matcher(string);
			
		if(matcher.find())
		{
			return matcher.group();
		}
		else
		{
			return null;
		}
	}
	
	private class BackgroundTaskAbort extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1829432201341253115L;
		public int errorId;
		public boolean errorOnStar;
		public String message;
		
		public BackgroundTaskAbort(int errorId)
		{
			this.errorId = errorId;
			this.errorOnStar = false;
		}
		public BackgroundTaskAbort(int errorId, boolean errorOnStar)
		{
			this.errorId = errorId;
			this.errorOnStar = errorOnStar;
		}
		
		public BackgroundTaskAbort(String message)
		{
			this.errorId = 0;
			this.errorOnStar = false;
			this.message = message;
		}
	}

	private class Address {
		public String title = "";
		public String latitude = "";
		public String longitude = "";
		public String street = "";
		public String number = "";
		public String city = "";
		public String province = "";
		public String postalCode = "";
		public String country = "";
		public String international_phone = "";
		public String phone = "";
		
		public String displayAddress = "";

		public Address() {
		}

		public String toString() {
			return displayAddress;
		}
	}
	
	private class DownloadAddressTask extends AsyncTask<String, Void, Void> {

		private BackgroundTaskAbort exception;
		private HttpGet httpGet;
		private boolean approximateAddress;
		
		public DownloadAddressTask()
		{
		}

		@Override
		protected void onPreExecute() {					
			exception = null;
			httpGet = new HttpGet();
			
			// Show progress dialog
			Context context = SendToCarActivity.this;
			progressDialog = ProgressDialog.show(context,
					null, context.getString(R.string.loadingAddress));
			progressDialog.setCancelable(true);
			
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					DownloadAddressTask.this.cancel(false);
					SendToCarActivity.this.finish();
					
					httpGet.abort();
				}
			});
		}
		
		@Override
		protected Void doInBackground(String... urls) {
			try {
				String url = getUrl(urls);
				if(isCancelled()) return null;
				String mapHtml = downloadMap(url);
				if(isCancelled()) return null;
				verifyGoogleMapHost();
				if(isCancelled()) return null;
				parseAddressData(mapHtml);
				if(isCancelled()) return null;
				
			} catch (BackgroundTaskAbort e) {
				exception = e;
			}
			return null;
		}

		private String getUrl(String[] urls) throws BackgroundTaskAbort {
			if(urls.length > 0 && urls[0] != null)
			{
				return urls[0];
			}
			throw new BackgroundTaskAbort(R.string.errorNoUrl);
		}

		private String downloadMap(String url) throws BackgroundTaskAbort {
			String mapHtml = "";
			try
			{
				log.d("Downloading " + url);
				
				httpGet.setURI(new URI(url));
				HttpResponse response = client.execute(httpGet, httpContext);
				
				log.d("Downloaded address. Status: " + response.getStatusLine().getStatusCode());

				if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
				{
					throw new BackgroundTaskAbort(R.string.errorDownload);
				}
				
				mapHtml = EntityUtils.toString(response.getEntity());

				log.d("Response: <pre>" + log.htmlSnippet(mapHtml) + "</pre>");
				
				/* Get the name of the Google server that sent the map after any redirects */
				mapHost = (HttpHost)  httpContext.getAttribute( 
						ExecutionContext.HTTP_TARGET_HOST);
				//HttpUriRequest currentReq = (HttpUriRequest) httpContext.getAttribute( 
				//		ExecutionContext.HTTP_REQUEST);
				//String currentUrl = mapHost.toURI() + currentReq.getURI();   
				
				log.d("Google Maps host: " + mapHost.getHostName());

			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while downloading address: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorDownload);
			}
			
			return mapHtml;
		}

		private void verifyGoogleMapHost() throws BackgroundTaskAbort {
			if(mapHost != null)
			{
				String hostname = mapHost.getHostName();
				for(String h : GoogleMapsHosts.hosts)
				{
					if(hostname.equals(h))
					{
						return;
					}
				}
			}
			throw new BackgroundTaskAbort(R.string.errorNotGoogleMaps);
		}

		private void parseAddressData(String mapHtml) throws BackgroundTaskAbort {
			approximateAddress = false;

			// find the JSON section called "markers"
			String startStr = "{sxcar:true,markers:";
			int startPos = mapHtml.indexOf(startStr);
			if(startPos == -1)
			{
				// if addresses in this country are not supported, do a geocode lookup for an approximate address
				startStr = "{sxcar:false,markers:";
				startPos = mapHtml.indexOf(startStr);
			}
			
			if(startPos >= 0) {
				String match = mapHtml.substring(startPos + startStr.length());
				
				// replace ASCII character escapes \xAB with Unicode escapes \u00AB since those are converted
				// automatically by the JSONObject parser to UTF-8 characters
				String rawData = match.replaceAll("\\\\x", "\\\\u00");
								
				try {
					JSONArray markers = new JSONArray(rawData);
					if(markers.length() >= 1)
					{
						JSONObject mapData = markers.getJSONObject(0);

						address = new Address();
						address.title = decodeHtml(mapData.optString("sxti", ""));
						address.street = decodeHtml(mapData.optString("sxst", ""));
						address.number = decodeHtml(mapData.optString("sxsn", ""));
						address.city = decodeHtml(mapData.optString("sxct", ""));
						address.province = decodeHtml(mapData.optString("sxpr", ""));
						address.postalCode = decodeHtml(mapData.optString("sxpo", ""));
						address.country = decodeHtml(mapData.optString("sxcn", ""));
						address.international_phone = decodeHtml(mapData.optString("sxph", ""));
						address.phone = "";
						
						if(mapData.has("infoWindow"))
						{
							JSONObject info = mapData.getJSONObject("infoWindow");
							if(info.has("addressLines"))
							{
								JSONArray lines = info.getJSONArray("addressLines");
								StringBuilder sb = new StringBuilder();
								for(int i = 0; i < lines.length(); i++)
								{
									if(i != 0)
									{
										sb.append("\n");
									}
									sb.append(lines.getString(i));
								}
								address.displayAddress = decodeHtml(sb.toString());
								if(address.title.length() == 0)
								{
									// use first line of address as title
									address.title = lines.optString(0, "");
								}
							}
							
							if(info.has("phones"))
							{
								JSONArray phones = info.getJSONArray("phones");
								if(phones.length() > 0)
								{
									address.phone = decodeHtml(phones.getString(0).replaceAll("\\D", ""));
								}
							}
						}
						
						if(mapData.has("latlng"))
						{
							JSONObject latlng = mapData.getJSONObject("latlng");
							address.latitude = latlng.optString("lat");
							address.longitude = latlng.optString("lng");
							
							/* Get reverse geocoded address from Google Maps API if the address is completely empty */
							if(address.number.length() == 0 &&
									address.city.length() == 0 &&
									address.province.length() == 0 &&
									address.country.length() == 0) {
								approximateAddress = true;
								DownloadGeocodedAddress(address);
							}
								
						}
					}
					
					log.d("Address JSON parsed OK.");
				} catch(JSONException e) {
					log.d("<span style=\"color: red;\">Exception while parsing address JSON: " + e.toString() + "</span>");
					address = null;
					throw new BackgroundTaskAbort(R.string.errorDownload); 
				}
			}
			else
			{
				log.d("<span style=\"color: red;\">Cannot find address JSON</span>");
				address = null;
				throw new BackgroundTaskAbort(R.string.errorDownload); 
			}
		}

		private void DownloadGeocodedAddress(Address address) throws BackgroundTaskAbort {
			
			HashMap<String, String> mapping = new HashMap<String, String>();
			mapping.put("street_number", "number");
			mapping.put("route", "street");
			mapping.put("locality", "city");
			mapping.put("postal_code", "postalCode");
			mapping.put("administrative_area_level_1", "province");
			mapping.put("country", "country");

			String geoURI = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
					address.latitude + "," + address.longitude + "&sensor=false"; 

			String geoHtml = "";
			try
			{
				log.d("Geocoding  " + geoURI);

				httpGet.setURI(new URI(geoURI));
				HttpResponse response = client.execute(httpGet, httpContext);

				log.d("Geocoding address. Status: " + response.getStatusLine().getStatusCode());

				if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
				{
					return;
				}

				geoHtml = EntityUtils.toString(response.getEntity());

				log.d("Response: <pre>" + log.htmlSnippet(geoHtml) + "</pre>");


			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while geocoding address: " + e.toString() + "</span>");
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
								String value = decodeHtml(component.getString("short_name"));
								/* special case for street number: remove part after dash */
								if(field.equals("number")) {
									oldNumber = value;
									value = value.replaceFirst("-.*", "");
									newNumber = value;
								}
								address.getClass().getDeclaredField(field).set(address, value);
							}
						}
						
					}
					
					String display = decodeHtml(results.getJSONObject(0).optString("formatted_address", address.displayAddress));
					address.displayAddress = display.replaceFirst(oldNumber, newNumber);
				}
				
			} catch(JSONException e) {
				log.d("<span style=\"color: red;\">Exception while parsing geocoding JSON: " + e.toString() + "</span>");
			} catch(IllegalAccessException e) {
				log.d("<span style=\"color: red;\">Exception while updating address: " + e.toString() + "</span>");
			} catch(NoSuchFieldException e) {
				log.d("<span style=\"color: red;\">Exception while updating address: " + e.toString() + "</span>");
			}
		}


		@Override
		protected void onPostExecute(Void result) {
			if(progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}

			if(!isCancelled()) {
				if(exception != null)
				{
					String message;
					if(exception.message != null) {
						message = exception.message;
					} else {
						message = getString(exception.errorId);
					}
					showMessageBoxAndFinish(message);
				}
				else
				{
					updateUI();
					
					if(approximateAddress) {
						Context context = getApplicationContext();
						Toast toast = Toast.makeText(context, R.string.approximateAddress, Toast.LENGTH_LONG);
						toast.show();
					}
				}
			}
		}
	}

	private void updateUI() {
		populateMakes();
		selectMakeFromPreferences();
		populateAddress();
		
	}
	

	@SuppressLint("UseSparseArrays")
	private void populateMakes()
	{
		CarList carsData = CarListLoader.carList;
		
		// TODO: show busy dialog while loading cars
		if(carsData != null)
		{
			ArrayList<CarProvider> carsList = new ArrayList<CarProvider>();
			
			CarProvider fake = new CarProvider();
			fake.make = SendToCarActivity.this.getString(R.string.choose);
			fake.type = 0;
			carsList.add(fake);
			
			carsList.addAll(carsData.getList());

			Spinner spinner = (Spinner) findViewById(R.id.makeSpinner);

			ArrayAdapter<CarProvider> adapter = new ArrayAdapter<CarProvider>(this,
					android.R.layout.simple_spinner_item, carsList.toArray(new CarProvider[carsList.size()]));

			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			
			// register change handler

			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				
			    @Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

		            CarProvider car = getCar();
		            
		            /* Replace the account label */
		            TextView accountLabel = (TextView) findViewById(R.id.accountLabel);
					if(car.type == 0)
					{
						accountLabel.setText(R.string.account);
					}
					else
					{
						accountLabel.setText(car.account);
					}
					
					tagVisibilityAndText(car.use_destination_tag, car.destination_tag);
					notesVisibility(car.show_notes);
		            
		            /* Clear account text and Use as Default */
					if(!ignoreFirstSpinnerChange)
					{
						EditText accountText = (EditText) findViewById(R.id.accountText);
						accountText.setText("");
					}
						
					ignoreFirstSpinnerChange = false;
				}

			    @Override
			    public void onNothingSelected(AdapterView<?> parentView) {
			        // do nothing
			    }

			});
		}
	}

	protected void selectMakeFromPreferences() {
       SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
	   String make = settings.getString("make", "");
	   String account = settings.getString("account", "");

	   if(make.length() > 0 && account.length() > 0) {
		   Spinner spinner = (Spinner) findViewById(R.id.makeSpinner);
		   @SuppressWarnings("unchecked")
		   ArrayAdapter<CarProvider> adapter = (ArrayAdapter<CarProvider>) spinner.getAdapter();
		   int pos;
		   for(pos = 0; pos < adapter.getCount(); pos++)
		   {
			   CarProvider car = adapter.getItem(pos);
			   if(car.type != 0 && car.id.equals(make))
			   {
				   spinner.setSelection(pos);
				   break;
			   }
		   }

		   if(pos != adapter.getCount())
		   {
			   EditText accountText = (EditText) findViewById(R.id.accountText);
			   accountText.setText(account);

			   ignoreFirstSpinnerChange = true;
		   }
	   }
	}

	@SuppressLint("UseSparseArrays")
	protected void populateAddress() {
		if(address != null)
		{
			HashMap<Integer, String> ids = new HashMap<Integer, String>();
			
			ids.put(R.id.destinationText, address.title);
			ids.put(R.id.tagText, address.title);
			ids.put(R.id.addressText, address.displayAddress);

			ids.put(R.id.manualNumberText, address.number);
            ids.put(R.id.manualStreetText, address.street);
            ids.put(R.id.manualCityText, address.city);
            ids.put(R.id.manualProvinceText, address.province);
            ids.put(R.id.manualPostalCodeText, address.postalCode);
            ids.put(R.id.manualCountryText, address.country);
            
            for(Integer id : ids.keySet()) {
            	TextView v = (TextView) findViewById(id);
            	v.setText(ids.get(id));
            }
        }
	}
	
	protected void populatePhone() {
		CarProvider car = getCar();
		
		if(car != null) {
			TextView v = (TextView) findViewById(R.id.manualPhoneText);
			v.setText(car.international_phone ? address.international_phone : address.phone);
		}
	}

	private class OnSendButtonClick extends Object implements OnClickListener {

		private class ValidationException extends Exception {
			/**
			 * 
			 */
			private static final long serialVersionUID = -5210917939912172597L;
			public int errorId;
			
			public ValidationException(int errorId)
			{
				this.errorId = errorId;
			}
		}

		@Override
		public void onClick(View v) {
			try
			{
				CarProvider car = getCar();
				if(car == null || car.type == 0)
				{
					throw new ValidationException(R.string.validationMake);
				}
				
				EditText accountText = (EditText) findViewById(R.id.accountText);
				String account = accountText.getText().toString();
				if(account.length() == 0)
				{
					throw new ValidationException(R.string.validationAccount);
				}
				
				EditText destinationText = (EditText) findViewById(R.id.destinationText);
				String destination = destinationText.getText().toString();
				if(destination.length() == 0)
				{
					throw new ValidationException(R.string.validationDestination);
				}
				
				EditText tagText = (EditText) findViewById(R.id.tagText);
				String tag = tagText.getText().toString();
				if(car.use_destination_tag && tag.length() == 0)
				{
					throw new ValidationException(R.string.validationTag);
				}
				saveMakeToPreferences();
				
				if(manualEdit) {
					updateAdressFromFields();
				}
				

				EditText notesText = (EditText) findViewById(R.id.notesText);
				String notes = notesText.getText().toString();
				
				taskSend = new SendToCarTask();
				taskSend.setProvider(car);
				taskSend.setAccount(account);
				taskSend.setDestination(destination);
				taskSend.setTag(tag);
				if(car.show_notes) {
					taskSend.setNotes(notes);
				}
				
				taskSend.execute(new Void[0]);

			} catch(ValidationException e) {
				Context context = getApplicationContext();
				Toast toast = Toast.makeText(context, e.errorId, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
	
	@SuppressLint("UseSparseArrays")
	private void updateAdressFromFields() {
		if(address != null)
		{
			HashMap<Integer, String> ids = new HashMap<Integer, String>();
			
			ids.put(R.id.manualNumberText, "number");
            ids.put(R.id.manualStreetText, "street");
            ids.put(R.id.manualCityText, "city");
            ids.put(R.id.manualProvinceText, "province");
            ids.put(R.id.manualPostalCodeText, "postalCode");
            ids.put(R.id.manualCountryText, "country");
            ids.put(R.id.manualPhoneText, "phone");
            ids.put(R.id.manualPhoneText, "international_phone");
            
            for(Integer id : ids.keySet()) {
            	try {
	            	TextView v = (TextView) findViewById(id);
	            	String field = ids.get(id);
	            	String value = v.getText().toString();
					address.getClass().getDeclaredField(field).set(address, value);
            	} catch(NoSuchFieldException e) {
            		// ignore
            	} catch(IllegalAccessException e) {
            		// ignore
            	}
            }
            
            /* set latitude and longitude to empty to force geocoding before sending */
        	address.latitude = "";
        	address.longitude = "";
        }
	}

	private void saveMakeToPreferences() {
	   // erase defaults if invalid values were saved
	   String make = "";
	   String account = "";

	   CarProvider car = getCar();
	   if(car != null && car.type != 0)
	   {
		   make = car.id;

		   EditText accountText = (EditText) findViewById(R.id.accountText);
		   account = accountText.getText().toString();
	   }

       SharedPreferences.Editor settings = getPreferences(Context.MODE_PRIVATE).edit();

	   settings.putString("make", make);
	   settings.putString("account", account);
	   settings.commit();
	}

	private class SendToCarTask extends AsyncTask<Void, Void, Boolean> {

		private BackgroundTaskAbort exception;
		private HttpPost httpPost;
		private HttpGet httpGet;

		private CarProvider car;
		private String account;
		private String destination;
		private String tag;
		private String notes;
		private boolean latLongOnly;

		public SendToCarTask()
		{
		}


		public void setProvider(CarProvider car) {
			this.car = car;
		}

		public void setAccount(String account) {
			this.account = account;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public void setNotes(String notes) {
			this.notes = notes;		
		}
		
		public void setLatLongOnly(boolean latLongOnly) {
			this.latLongOnly = latLongOnly;
		}
		
		@Override
		protected void onPreExecute() {
			exception = null;
			httpPost = new HttpPost();
			httpGet = new HttpGet();
			
			if(car == null || account == null || destination == null)
			{
				this.cancel(false);
			}
			else
			{
				// Show progress dialog
				Context context = SendToCarActivity.this;
				int msgId = (car.type == 2) ?  R.string.sendingToGPS : R.string.sendingToCar;
				progressDialog = ProgressDialog.show(context,
						null, context.getString(msgId));
				progressDialog.setCancelable(true);
				
				progressDialog.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						SendToCarTask.this.cancel(false);
						httpPost.abort();
						httpGet.abort();
					}
				});
			}

		}

		@Override
		protected Boolean doInBackground(Void... noarg) {
			try {
				if(car.host.equals(MAPQUEST)) {
					if(isCancelled()) return null;
					String post = preparePostDataMapquest();
					if(isCancelled()) return null;
					String sendToCarHtml = sendToCarMapquest(post);
					if(isCancelled()) return null;
					parseSendToCarMapquest(sendToCarHtml);	
				} else if(car.host.equals(ONSTAR)) {
					if(isCancelled()) return null;
					String post = preparePostDataOnStar();
					if(isCancelled()) return null;
					String sendToCarHtml = sendToCarOnStar(post);
					if(isCancelled()) return null;
					parseSendToCarOnStar(sendToCarHtml);	
				} else {
					if(isCancelled()) return null;
					String post = preparePostData();
					if(isCancelled()) return null;
					String sendToCarHtml = sendToCar(post);
					if(isCancelled()) return null;
					parseSendToCar(sendToCarHtml);
				}
			} catch (BackgroundTaskAbort e) {
				exception = e;
				return Boolean.FALSE;
			}
			return Boolean.TRUE;
		}
		
		private String preparePostData() throws BackgroundTaskAbort {
			String post = null;

			try
			{
				ArrayList<String> postData = new ArrayList<String>();

				postData.add("account");
				postData.add(account);

				postData.add("source");
				postData.add(car.host);

				postData.add("atx");
				postData.add(car.id);

				postData.add("name");
				postData.add(destination);
				
				if(address.latitude.length() == 0 || address.longitude.length() == 0) {
					updateAddressLatLong(address);
				}
				
				// full address data
				final String[] codesFull = {"lat", "lng", "street", "streetnum", "city", "province", "postalcode", "country", "phone", "notes"};
				final String[] valuesFull = {address.latitude, address.longitude, address.street, address.number, address.city, address.province, address.postalCode, address.country, car.international_phone ? address.international_phone : address.phone, notes};
				// using only latitude/longitude
				final String[] codesLL = {"lat", "lng"};
				final String[] valuesLL = {address.latitude, address.longitude};

				final String[] codes;
				final String[] values;
				if(latLongOnly) {
					codes = codesLL;
					values = valuesLL;
				} else {
					codes = codesFull;
					values = valuesFull;
				}
				
				for(int i = 0; i < codes.length; i++)
				{
					if(values[i] != null && values[i].length() > 0)
					{
						postData.add(codes[i]);
						postData.add(values[i]);
					}
				}
				
				if(car.use_destination_tag)
				{
					postData.add("destinationtag");
					postData.add(tag);
				}

				postData.add("sxauth");
				postData.add(getCookieId());

				// URL encode
				ListIterator<String> it = postData.listIterator();
				while(it.hasNext())
				{
					String s = it.next();
					it.set(Uri.encode(s));
				}

				post = TextUtils.join("|", postData);
				
				log.d("Sending to car. Post data <pre>" + post + "</pre>");

			} catch(NullPointerException e) {
				log.d("<span style=\"color: red;\">Null pointer exception while preparing post data: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			}

			return post;
		}


		private String getCookieId() {
			String cookie_id = null;
			
			if(car != null) {
				
				List<Cookie> cookies = cookieStore.getCookies();
				
				for(int tries = 0; tries < 2; tries++) {
					
					Iterator<Cookie> it = cookies.iterator();
					while(it.hasNext()) {
						Cookie c = it.next();
						if(c.getName().equals("PREF") && car.host.endsWith(c.getDomain())) {
							cookie_id = parseCookie(c);
							return cookie_id;
						}
					}

					if(tries == 0) {
						/* try to load the main Google Maps page to get a cookie */
						donwloadCookie();
					}
				}
			}

			return cookie_id;
		}

		private String parseCookie(Cookie c) {
			String cookie_id = null;
			String[] subcookies = c.getValue().split(":");
			for(int i = 0; i < subcookies.length; i++)
			{
				String[] sub = subcookies[i].split("=");
				if(sub.length >= 2 && sub[0].equals("ID")) {
					cookie_id = sub[1];
					log.d("Cookie: " + c.toString());
					break;
				}
			}
			return cookie_id;
		}


		private void donwloadCookie() {
			try {
				URI mainPage = new URI("http", car.host, "/", "output=json", null);
				log.d("Downloading main Google Maps page to fill the cookie jar: " + mainPage.toString());

				HttpGet httpGet = new HttpGet();
				httpGet.setURI(mainPage);

				HttpResponse response = client.execute(httpGet, httpContext);

				log.d("Downloaded cookie. Status: " + response.getStatusLine().getStatusCode());
			} catch (Exception e) {
				// ignore
			}
		}
		
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
				log.d("Updating latitude/longitude  " + geoURI);
				httpGet.setURI(new URI(geoURI));
				HttpResponse response = client.execute(httpGet, httpContext);

				log.d("Updating lat/long. Status: " + response.getStatusLine().getStatusCode());

				if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
				{
					return;
				}

				geoHtml = EntityUtils.toString(response.getEntity());

				log.d("Response: <pre>" + log.htmlSnippet(geoHtml) + "</pre>");


			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while geocoding address: " + e.toString() + "</span>");
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
				log.d("<span style=\"color: red;\">Exception while parsing geocoding JSON: " + e.toString() + "</span>");
			}
		}

		private String sendToCar(String post) throws BackgroundTaskAbort {
			String sendToCarHtml = "";
			try
			{
				URI postUri = new URI("http", car.host, "/maps/sendto", "stx=c", null);

				httpPost.setURI(postUri);
				httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
				httpPost.setEntity(new ByteArrayEntity(post.getBytes()));
				
				log.d("Uploading to " + postUri.toString());

				if(isCancelled() || httpPost.isAborted()) return null;
				
				HttpResponse response = client.execute(httpPost, httpContext);
				
				log.d("Uploaded to car. Status: " + response.getStatusLine().getStatusCode());
				
				if(isCancelled()) return null;

				if(response == null || response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK )
				{
					throw new BackgroundTaskAbort(R.string.errorSendToCar);
				}
				
				sendToCarHtml = EntityUtils.toString(response.getEntity());
				log.d("Response: <pre>" + log.htmlSnippet(sendToCarHtml) + "</pre>");
			} catch(InterruptedIOException e) {
				log.d("Upload to car aborted");
				return null;
			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while sending to car: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			}
			
			return sendToCarHtml;
		}

		private void parseSendToCar(String sendToCarHtml) throws BackgroundTaskAbort {
			try {
				// replace ASCII character escapes \xAB with Unicode escapes \u00AB since those are converted
				// automatically by the JSONObject parser to UTF-8 characters
				String html = sendToCarHtml.toString().replaceAll("\\\\x", "\\\\u00");

				JSONObject response = new JSONObject(html);
				
				if(isCancelled()) return;

				int status = response.getInt("status");

				log.d("Response JSON parsed OK. Status: " + ((status == 1) ? "Success" : "Failed"));

				if(status == 1) {
					// success
					return;
				}
				
				/* status 4 means redirect, used by Toyota in Europe
				 * other field in this case: redirect_url
				 */
				if(status == 4) {
					String url = response.optString("redirect_url");
					if(url.length() > 0) {
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(url));
		        		try
		        		{
		        			startActivity(i);
		        		}
		        		catch(ActivityNotFoundException e)
		        		{
		        			Context context = getApplicationContext();
		        			Toast toast = Toast.makeText(context, R.string.errorStartGoogleMaps, Toast.LENGTH_SHORT);
		        			toast.show();
		        		}
						finish();
						throw new BackgroundTaskAbort(R.string.redirect); 
					}
				}
				
				int errorCode = response.getInt("stcc_status");
				int errorMsg;
				boolean errorOnStar = false;
				
				switch(errorCode) {
				case 430:
				case 440:
					errorMsg = R.string.statusInvalidAccount;
					break;
					
				case 450:
					errorOnStar = true;
					errorMsg = R.string.statusInvalidTag;
					break;
				case 460:
					errorMsg = R.string.statusTagExists;
					break;
					
				case 470:
					errorMsg = R.string.statusDestinationNotSent;
					break;

				case 500:
				default:
					errorMsg = R.string.errorSendToCar;
					
					// retry with lat/long only
					if(!latLongOnly) {
						setLatLongOnly(true);
						if(doInBackground(new Void[0]).booleanValue()) {
							return;
						}
					}
					break;
				}
				
				log.d("<span style=\"color: red;\">Error code: " + errorCode + ", String: " + getString(errorMsg) + "</span>");
				
				throw new BackgroundTaskAbort(errorMsg, errorOnStar);
			} catch(JSONException e) {
				log.d("<span style=\"color: red;\">Exception while parsing resposne JSON: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar); 
			}
		}
		
		private String preparePostDataMapquest() throws BackgroundTaskAbort {
			try
			{
				JSONObject location = new JSONObject();
				location.put("name", destination);
				
				
				String streetaddress;
				if((address.number == null || address.number.length() == 0) &&
						(address.street == null || address.street.length() == 0)) {
					streetaddress = null;
				} else if(address.number == null || address.number.length() == 0) {
					streetaddress = address.street;
				} else {
					streetaddress = address.number + ' ' + address.street;
				}
				
				// full address data
				final String[] codesMQ = {"street", "city", "state", "postalCode", "country"};
				final String[] valuesMQ = { streetaddress, address.city, address.province, address.postalCode, address.country};
				
				final String[] codes = codesMQ;
				final String[] values = valuesMQ;

				for(int i = 0; i < codes.length; i++)
				{
					if(values[i] != null && values[i].length() > 0)
					{
						location.put(codes[i], values[i]);
					}
				}
				
				if(address.latitude.length() == 0 || address.longitude.length() == 0) {
					updateAddressLatLong(address);
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
				
				log.d("Sending to MapQuest. Post data <pre>" + post + "</pre>");
				
				return post;
				
			} catch(JSONException e) {
				log.d("<span style=\"color: red;\">JSON exception while preparing MapQuest post data: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			} catch(NullPointerException e) {
				log.d("<span style=\"color: red;\">Null pointer exception while preparing MapQuest post data: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			}
		}
		
		private String sendToCarMapquest(String post) throws BackgroundTaskAbort {
			String sendToCarHtml = "";
			try
			{
				URI postUri = new URI("https", car.host, "/FordSyncServlet/submit", null, null);
				
				httpPost.setURI(postUri);
				httpPost.addHeader("Content-Type", "application/json; charset=UTF-8");
				httpPost.setEntity(new ByteArrayEntity(post.getBytes()));
				
				log.d("Uploading to " + postUri.toString());

				if(isCancelled() || httpPost.isAborted()) return null;
				
				HttpResponse response = client.execute(httpPost, httpContext);
				
				log.d("Uploaded to Mapquest. Status: " + response.getStatusLine().getStatusCode());
				
				if(isCancelled()) return null;

				if(response == null || response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK )
				{
					throw new BackgroundTaskAbort(R.string.errorSendToCar);
				}
				
				sendToCarHtml = EntityUtils.toString(response.getEntity());
				log.d("Response: <pre>" + log.htmlSnippet(sendToCarHtml) + "</pre>");
			} catch(InterruptedIOException e) {
				log.d("Upload to Mapquest aborted");
				return null;
			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while sending to Mapquest: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			}
			
			return sendToCarHtml;
		}
		
		private void parseSendToCarMapquest(String sendToCarHtml) throws BackgroundTaskAbort {
			JSONObject response;
			
			try {
				// replace ASCII character escapes \xAB with Unicode escapes \u00AB since those are converted
				// automatically by the JSONObject parser to UTF-8 characters
				String html = sendToCarHtml.toString().replaceAll("\\\\x", "\\\\u00");

				response = new JSONObject(html);
				
				if(isCancelled()) return;

				String result = response.getString("result");

				log.d("Mapquest response JSON parsed OK. Status: " + result);

				if(result.equals("OK")) {
					// success
					return;
				}
				
			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while parsing Mapquest resposne JSON: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar); 
			}

			String errorMsg = response.optString("message");
			log.d("<span style=\"color: red;\">Could not send to Mapquest. Error: " + errorMsg + "</span>");
			throw new BackgroundTaskAbort(errorMsg);

		}
		
		private String preparePostDataOnStar() throws BackgroundTaskAbort {
			try
			{
				StringBuilder payload = new StringBuilder();
				payload.append("s_name=" + URLEncoder.encode(destination, "UTF-8"));

				String streetaddress;
				if((address.number == null || address.number.length() == 0) &&
						(address.street == null || address.street.length() == 0)) {
					streetaddress = null;
				} else if(address.number == null || address.number.length() == 0) {
					streetaddress = address.street;
				} else {
					streetaddress = address.number + ' ' + address.street;
				}

				if(address.latitude.length() == 0 || address.longitude.length() == 0) {
					updateAddressLatLong(address);
				}
				
				Time now = new Time();
				now.setToNow();
				String timestamp = now.toMillis(true) + "";

                // full address data
				final String[] codesMQ = {"s_lat", "s_long", "s_street", "s_city", "s_state_province", "s_postalcode", "s_country", "s_locale", "s_nounce", "s_entity_id", "SignatureMethod", "Signature" };
				final String[] valuesMQ = { address.latitude, address.longitude, streetaddress, address.city, address.province, address.postalCode, address.country, "en_US", timestamp, " " + timestamp, "https://mapquest.com.onstar.com", "RSA-SHA256", "" };
				
				final String[] codes = codesMQ;
				final String[] values = valuesMQ;

				for(int i = 0; i < codes.length; i++)
				{
					if(values[i] != null && values[i].length() > 0)
					{
						payload.append("&" + codes[i] + "=" + URLEncoder.encode(values[i], "UTF-8"));
					}
				}
				
				String post = payload.toString();
				
				log.d("Sending to OnStar. Post data <pre>" + post + "</pre>");
				
				return post;
				
			} catch(UnsupportedEncodingException e) {
				log.d("<span style=\"color: red;\">Unsupported encoding exception while preparing MapQuest post data: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			} catch(NullPointerException e) {
				log.d("<span style=\"color: red;\">Null pointer exception while preparing MapQuest post data: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar);
			}
		}
		
		private String sendToCarOnStar(String post) throws BackgroundTaskAbort {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.onstar.com/web/portal/odm?" + post));
    		try
    		{
    			startActivity(browserIntent);
				finish();
				throw new BackgroundTaskAbort(R.string.redirect); 
    		}
    		catch(ActivityNotFoundException e)
    		{
    			Context context = getApplicationContext();
    			Toast toast = Toast.makeText(context, R.string.errorStartGoogleMaps, Toast.LENGTH_SHORT);
    			toast.show();
    		}

			return "";
		}

		private void parseSendToCarOnStar(String sendToCarHtml) throws BackgroundTaskAbort {
			/* Nothing to do */
		}
		
		
		@Override
		protected void onPostExecute(Boolean result) {
			if(progressDialog != null) {
				try {
					progressDialog.dismiss();
				} catch(Exception e) {
					/* Dismissing dialog might fail if view is already gone */
				}
				progressDialog = null;
			}
						
			if(!isCancelled()) {
				if(exception != null)
				{
					if(exception.errorOnStar) {
						AlertDialog.Builder alertbox = new AlertDialog.Builder(SendToCarActivity.this);
						alertbox.setTitle(R.string.errorTitle);
						alertbox.setMessage(R.string.statusOnStarMaxDestinations);
						alertbox.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
							}
						});

						alertbox.setNeutralButton(R.string.showOnStarWebsite, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.onstar.com/web/portal/odm"));
				        		try
				        		{
				        			startActivity(browserIntent);
				        		}
				        		catch(ActivityNotFoundException e)
				        		{
				        			Context context = getApplicationContext();
				        			Toast toast = Toast.makeText(context, R.string.errorStartGoogleMaps, Toast.LENGTH_SHORT);
				        			toast.show();
				        		}
							}
						});

						// set a negative/no button and create a listener
						alertbox.setNegativeButton(R.string.showHelp, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								startActivity(new Intent(SendToCarActivity.this, InformationActivity.class));
							}
						});

						// display box
						alertbox.show();
					} else {
						Context context = getApplicationContext();
						String message;
						if(exception.message != null) {
							message = exception.message;
						} else {
							message = getString(exception.errorId);
						}
						Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
						toast.show();
					}

					// Don't close activity if upload fails in case the user wants to retry
				}
				else
				{
					int msgId = (car.type == 2) ?  R.string.successGPS : R.string.successCar;
					Context context = getApplicationContext();
					String msgStr = context.getString(msgId);

					/* Show additional message for Ford since users seem to find it difficult to download the destination to the car */
					if(car.host.equals(MAPQUEST)) {
						msgStr += "\n" + context.getString(R.string.fordDownload);
					}
					Toast toast = Toast.makeText(context, msgStr, Toast.LENGTH_LONG);
					toast.show();

					finish();
				}
			}
		}
	}

	public String  decodeHtml(String s) {
		/* Adapted from the android.net.Uri.decode() function */

		/*
	    Compared to java.net.URLEncoderDecoder.decode(), this method decodes a
	    chunk at a time instead of one character at a time, and it doesn't
	    throw exceptions. It also only allocates memory when necessary--if
	    there's nothing to decode, this method won't do much.
	    */
	    if (s == null) {
	        return null;
	    }
	    // Lazily-initialized buffers.
	    StringBuilder decoded = null;
	    Pattern escape = Pattern.compile("&#([0-9]+);");
	    
	    int oldLength = s.length();
	    // This loop alternates between copying over normal characters and
	    // decoding in chunks. This results in fewer method calls and
	    // allocations than decoding one character at a time.
	    int current = 0;
	    Matcher matcher = escape.matcher(s);

	    while (matcher.find()) {
	        // Copy over normal characters until match
	    	int nextEscape = matcher.start();

	        // Prepare buffers.
	        if (decoded == null) {
	            // Looks like we're going to need the buffers...
	            // We know the new string will be shorter. Using the old length
	            // may overshoot a bit, but it will save us from resizing the
	            // buffer.
	            decoded = new StringBuilder(oldLength);
	        }
	        
	        // Append characters leading up to the escape.
	        if (nextEscape > current) {
	            decoded.append(s, current, nextEscape);
	        } else {
	            // assert current == nextEscape
	        }
            current = matcher.end();
	        
	        // Decode and append escape sequence. Escape sequences look like
	        // "&#N;" where &# and ; are literal and N is a decimal number (several digits)
	        try {
	        	// Combine the hex digits into one byte and write.
	        	char c = (char) Integer.parseInt(matcher.group(1));
	        	decoded.append(c);
	        } catch (NumberFormatException e) {
	            throw new AssertionError(e);
	        }
	    }

        if (decoded == null) {
            // We didn't actually decode anything.
            return s;
        } else {
            // Append the remainder and return the decoded string.
            decoded.append(s, current, oldLength);
            return decoded.toString();
        }
	}
	
	 public static String join(Collection<?> s, String delimiter) {
	     StringBuilder builder = new StringBuilder();
	     Iterator<?> iter = s.iterator();
	     while (iter.hasNext()) {
	    	 Object o = iter.next();
	    	 if(o != null) {
	    		 builder.append(o);
	    	 }
	         if (!iter.hasNext()) {
	           break;                  
	         }
	         builder.append(delimiter);
	     }
	     return builder.toString();
	 }

	public class JSONMapRedirectHandler extends DefaultRedirectHandler {

		@Override
		public URI getLocationURI(HttpResponse response, HttpContext context)
				throws ProtocolException {
			URI defaultRedir = super.getLocationURI(response, context);
			String query = defaultRedir.getQuery();
			if(query != null)
			{
				query = query + "&output=json";
			}
			
			try
			{
				URI redir = new URI(defaultRedir.getScheme(), defaultRedir.getAuthority(),
						defaultRedir.getPath(), query, defaultRedir.getFragment());
				return redir;
			}
			catch(URISyntaxException e)
			{
				return defaultRedir;
			}
		}
	}
}
