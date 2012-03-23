
/* TODO:
 * - Add SingleTop mode and (maybe) change of intent bug when Sharing through Map, then
 *   clicking Home and sharing another location (same Activity reopens with old Intent)
 * - Expand help: troubleshooting, market link, credits, GPL
 * - Test all error cases
 * - Test in other locales/languages/countries/vehicle makes
 */

package com.jvanier.android.sendtocar;

import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SendToCarActivity extends Activity {
	private ProgressDialog progressDialog;
	private DownloadAddressTask taskDownload;
	private SendToCarTask taskSend;

	private HttpHost mapHost;
	
	private HttpContext httpContext;
	private CookieStore cookieStore;
	private DefaultHttpClient client;
	
	private ArrayList<CarProvider> carsData;
	private Address address;
	private boolean ignoreFirstSpinnerChange;
	
	private DebugLog log;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sendtocar);
		
		//log = new DebugLogFile(this);
		log = new DebugLogDummy();
		
		tagVisibilityAndText(false, "");
		
		setupHttp();

		loadMapFromIntent(getIntent());
		
		registerButtons();
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

	private void setupHttp() {
		httpContext = new BasicHttpContext();
		cookieStore = new BasicCookieStore();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		client = new DefaultHttpClient();
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
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.idHelp:
            	startActivity(new Intent(SendToCarActivity.this, InformationActivity.class));
            	break;
	    }
	    return true;
	}

	private void loadMapFromIntent(Intent i)
	{
		if(i == null)
		{
			log.d("<span style=\"color: red;\">No intent</span>");
		}
		else
		{
			log.d("Intent. Action: " + i.getAction() + ", Text: " + i.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString());
		}
		
		if(i != null && i.getAction().equals(Intent.ACTION_SEND) && i.getExtras() != null)
		{
			String url = findURL(i.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString());
			
			log.d("URL: <a href=\""+ url + "\">url</a>");
			
			taskDownload = new DownloadAddressTask();
			taskDownload.execute(new String[] { url });
		}
		else
		{
	    	showMessageBoxAndFinish(R.string.errorIntent);
		}
	}
	
	private void showMessageBoxAndFinish(int messageId) {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setTitle(R.string.errorTitle);
        alertbox.setMessage(messageId);
        alertbox.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            	finish();
            }
        });

        // set a negative/no button and create a listener
        alertbox.setNegativeButton(R.string.showHelp, new DialogInterface.OnClickListener() {
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
		Pattern urlPatt = Pattern.compile("(?i)\\bhttps?://[a-z0-9.\\-,@?^=%&:/~+#]*[a-z0-9\\-@^=%&:/~+#]");
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
		public int errorId;
		
		public BackgroundTaskAbort(int errorId)
		{
			this.errorId = errorId;
		}

	}

	private class Address {
		public String title;
		public String latitude;
		public String longitude;
		public String street;
		public String number;
		public String city;
		public String province;
		public String postalCode;
		public String country;
		public String international_phone;
		public String phone;
		
		public String displayAddress;

		public Address() {
		}

		public String toString() {
			return displayAddress;
		}
	}
	
	private class CarProvider implements Comparable<CarProvider> {
		public String id;
		public int type;
		public String make;
		public String account;
		public String destination_tag;
		public boolean use_destination_tag;
		public String system;
		public boolean international_phone;
		
		public CarProvider() {
		}
		
		public String toString() {
			return make + ((type == 2) ? " (GPS)" : "");
		}
		
	    @Override
	    public int compareTo(CarProvider other) {
	    	int typediff = (this.type - other.type);
	        return (typediff == 0) ? this.make.compareTo(other.make) : typediff;
	    }
	}

	
	private class DownloadAddressTask extends AsyncTask<String, Void, Void> {

		private BackgroundTaskAbort exception;
		private HttpGet httpGet;
		
		public DownloadAddressTask()
		{
		}

		@Override
		protected void onPreExecute() {
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
			
			exception = null;
			httpGet = new HttpGet();
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
				
				String carsJson = downloadCars();
				if(isCancelled()) return null;
				parseCarsData(carsJson);
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

				log.d("Response: <pre>" + htmlSnippet(mapHtml) + "</pre>");
				
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
			// match a URL, but try not to grab the punctuation at the end
			Pattern dataPatt = Pattern.compile("window\\.gHomeVPage=(.*);\\}\\)\\(\\);gHomeVPage\\.panel");
			Matcher matcher = dataPatt.matcher(mapHtml);
			
			if(matcher.find())
			{
				// replace ASCII character escapes \xAB with Unicode escapes \u00AB since those are converted
				// automatically by the JSONObject parser to UTF-8 characters
				String rawData = matcher.group(1).replaceAll("\\\\x", "\\\\u00");
								
				try {
					JSONObject parsedData = new JSONObject(rawData);
					
					JSONArray markers = parsedData.getJSONObject("overlays").getJSONArray("markers");
					if(markers.length() >= 1)
					{
						JSONObject mapData = markers.getJSONObject(0);

						if(!mapData.has("sxst"))
						{
							throw new BackgroundTaskAbort(R.string.errorCountry); 
						}

						address = new Address();
						address.title = mapData.optString("sxti", "");
						address.street = mapData.optString("sxst", "");
						address.number = mapData.optString("sxsn", "");
						address.city = mapData.optString("sxct", "");
						address.province = mapData.optString("sxpr", "");
						address.postalCode = mapData.optString("sxpo", "");
						address.country = mapData.optString("sxcn", "");
						address.international_phone = mapData.optString("sxph", "");
						address.phone = "";
						
						if(mapData.has("latlng"))
						{
							JSONObject latlng = mapData.getJSONObject("latlng");
							address.latitude = latlng.optString("lat");
							address.longitude = latlng.optString("lng");
						}

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
								address.displayAddress = sb.toString();
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
									address.phone = phones.getString(0).replaceAll("\\D", "");
								}
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
		}

		private String downloadCars() throws BackgroundTaskAbort {
			String carsJson = "";
			try
			{
				String lang = Locale.getDefault().getLanguage();
				URI carsUri = new URI(mapHost.getSchemeName(), mapHost.getHostName(), "/maps/sendtodata", "hl=" + lang, null);

				log.d("Downloading " + carsUri.toString());
				
				httpGet.setURI(carsUri);
				
				HttpResponse response = client.execute(httpGet, httpContext);
				
				log.d("Downloaded cars. Status: " + response.getStatusLine().getStatusCode());

				if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK )
				{
					throw new BackgroundTaskAbort(R.string.errorNotAvailable);
				}
				
				carsJson = EntityUtils.toString(response.getEntity());
				log.d("Response: <pre>" + htmlSnippet(carsJson) + "</pre>");
				
			} catch(Exception e) {
				log.d("<span style=\"color: red;\">Exception while downloading cars: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorDownload);
			}
			
			return carsJson;
		}

		private void parseCarsData(CharSequence carsJson) throws BackgroundTaskAbort {
	
			try {
				JSONObject carsRaw = new JSONObject(carsJson.toString().replaceAll("\\\\x", "\\\\u00"));
				JSONObject providers = carsRaw.getJSONObject("providers");
				
				carsData = new ArrayList<CarProvider>();
				
				Context context = SendToCarActivity.this;
				CarProvider fake = new CarProvider();
				fake.make = context.getString(R.string.choose);
				fake.type = 0;

				carsData.add(fake);
				
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
						c.make = provider.getString("make");
						c.type = provider.getInt("type");
						c.account = provider.getString("account");
						c.system = provider.getString("system");
						
						c.international_phone = provider.optBoolean("international_phone", false);
						c.use_destination_tag = provider.optBoolean("use_destination_tag", false);
						c.destination_tag = provider.optString("destination_tag", "");
						
						carsData.add(c);
					}
					
				}

				Collections.sort(carsData);
				
				log.d("Cars JSON parsed OK.");
				
			} catch(JSONException e) {
				log.d("<span style=\"color: red;\">Exception while parsing cars JSON: " + e.toString() + "</span>");
				carsData = null;
				throw new BackgroundTaskAbort(R.string.errorDownload); 
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
					showMessageBoxAndFinish(exception.errorId);
				}
				else
				{
					updateUI();
				}
			}
		}
	}

	private void updateUI() {
		populateMakes();
		selectMakeFromPreferences();
		populateAddress();
	}
	


	private void populateMakes()
	{
		if(carsData != null)
		{
			Spinner spinner = (Spinner) findViewById(R.id.makeSpinner);

			ArrayAdapter<CarProvider> adapter = new ArrayAdapter<CarProvider>(this,
					android.R.layout.simple_spinner_item, carsData.toArray(new CarProvider[0]));

			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			
			// register change handler

			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				
			    @Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

		            CarProvider car = (CarProvider)parentView.getSelectedItem();
		            
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
		            
		            /* Clear account text and Use as Default */
					if(!ignoreFirstSpinnerChange)
					{
						EditText accountText = (EditText) findViewById(R.id.accountText);
						accountText.setText("");

						CheckBox saveAsDefault = (CheckBox) findViewById(R.id.useDefaultCheck);
						saveAsDefault.setChecked(false);
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

			   CheckBox saveAsDefault = (CheckBox) findViewById(R.id.useDefaultCheck);
			   saveAsDefault.setChecked(true);
			   
			   ignoreFirstSpinnerChange = true;
		   }
	   }
	}

	protected void populateAddress() {
		if(address != null)
		{
			EditText destinationText = (EditText) findViewById(R.id.destinationText);
			EditText tagText = (EditText) findViewById(R.id.tagText);
			TextView addressText = (TextView) findViewById(R.id.addressText);

			destinationText.setText(address.title);
			tagText.setText(address.title);
			addressText.setText(address.displayAddress);
		}
	}

	private class OnSendButtonClick extends Object implements OnClickListener {

		private class ValidationException extends Exception {
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
				Spinner spinner = (Spinner) findViewById(R.id.makeSpinner);
				CarProvider car = (CarProvider)spinner.getSelectedItem();
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
				
				taskSend = new SendToCarTask();
				taskSend.setProvider(car);
				taskSend.setAccount(account);
				taskSend.setDestination(destination);
				taskSend.setTag(tag);
				
				taskSend.execute(new Void[0]);

			} catch(ValidationException e) {
				Context context = getApplicationContext();
				Toast toast = Toast.makeText(context, e.errorId, Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}

	private void saveMakeToPreferences() {
	   CheckBox saveAsDefault = (CheckBox) findViewById(R.id.useDefaultCheck);

	   // erase defaults if invalid values were saved
	   String make = "";
	   String account = "";
	   if(saveAsDefault.isChecked())
	   {
		   Spinner spinner = (Spinner) findViewById(R.id.makeSpinner);
		   CarProvider car = (CarProvider)spinner.getSelectedItem();
		   if(car != null && car.type != 0)
		   {
			   make = car.id;

			   EditText accountText = (EditText) findViewById(R.id.accountText);
			   account = accountText.getText().toString();
		   }
	   }

       SharedPreferences.Editor settings = getPreferences(Context.MODE_PRIVATE).edit();

	   settings.putString("make", make);
	   settings.putString("account", account);
	   settings.commit();
	}

	private class SendToCarTask extends AsyncTask<Void, Void, Void> {

		private BackgroundTaskAbort exception;
		private HttpPost httpPost;

		private CarProvider car;
		private String account;
		private String destination;
		private String tag;

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
		
		@Override
		protected void onPreExecute() {
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
					}
				});
			}

			exception = null;
			httpPost = new HttpPost();
		}

		@Override
		protected Void doInBackground(Void... noarg) {
			try {
				if(isCancelled()) return null;
				String post = preparePostData();
				if(isCancelled()) return null;
				String sendToCarHtml = sendToCar(post);
				if(isCancelled()) return null;
				parseSendToCar(sendToCarHtml);
			} catch (BackgroundTaskAbort e) {
				exception = e;
			}
			return null;
		}

		private String preparePostData() throws BackgroundTaskAbort {
			String post = null;

			try
			{
				ArrayList<String> postData = new ArrayList<String>();

				postData.add("account");
				postData.add(account);

				postData.add("source");
				postData.add(mapHost.getHostName());

				postData.add("atx");
				postData.add(car.id);

				postData.add("name");
				postData.add(destination);

				// address data
				String[] codes = {"lat", "lng", "street", "streetnum", "city", "province", "postalcode", "country", "phone"};
				String[] values = {address.latitude, address.longitude, address.street, address.number, address.city, address.province, address.postalCode, address.country, car.international_phone ? address.international_phone : address.phone};
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

			List<Cookie> cookies = cookieStore.getCookies();
			Iterator<Cookie> it = cookies.iterator();
			while(it.hasNext()) {
				Cookie c = it.next();
				if(c.getName().equals("PREF")) {
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
				}
			}

			return cookie_id;
		}

		private String sendToCar(String post) throws BackgroundTaskAbort {
			String sendToCarHtml = "";
			try
			{
				URI postUri = new URI(mapHost.getSchemeName(), mapHost.getHostName(), "/maps/sendto", "authuser=0&stx=c", null);
				
				log.d("Uploading to " + postUri.toString());

				httpPost.setURI(postUri);
				httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
				httpPost.setEntity(new ByteArrayEntity(post.getBytes()));
				
				if(isCancelled() || httpPost.isAborted()) return null;
				
				HttpResponse response = client.execute(httpPost, httpContext);
				
				log.d("Uploaded to car. Status: " + response.getStatusLine().getStatusCode());
				
				if(isCancelled()) return null;

				if(response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK )
				{
					throw new BackgroundTaskAbort(R.string.errorSendToCar);
				}
				
				sendToCarHtml = EntityUtils.toString(response.getEntity());
				log.d("Response: <pre>" + htmlSnippet(sendToCarHtml) + "</pre>");
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
				JSONObject response = new JSONObject(sendToCarHtml.toString());
				
				if(isCancelled()) return;

				int status = response.getInt("status");

				log.d("Response JSON parsed OK. Status: " + ((status == 1) ? "Success" : "Failed"));
				
				if(status == 1)
				{
					// success
					return;
				}
				
				int errorCode = response.getInt("stcc_status");
				int errorMsg;
				
				switch(errorCode) {
				case 430:
				case 440:
					errorMsg = R.string.statusInvalidAccount;
					break;
					
				case 450:
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
					break;
				}
				
				log.d("<span style=\"color: red;\">Error code: " + errorCode + ", String: " + getString(errorMsg) + "</span>");
				
				throw new BackgroundTaskAbort(errorMsg);
			} catch(JSONException e) {
				log.d("<span style=\"color: red;\">Exception while parsing resposne JSON: " + e.toString() + "</span>");
				throw new BackgroundTaskAbort(R.string.errorSendToCar); 
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
					Context context = getApplicationContext();
					Toast toast = Toast.makeText(context, exception.errorId, Toast.LENGTH_LONG);
					toast.show();

					// Don't close activity if upload fails in case the user wants to retry
				}
				else
				{
					Context context = getApplicationContext();
					int msgId = (car.type == 2) ?  R.string.successGPS : R.string.successCar;
					Toast toast = Toast.makeText(context, msgId, Toast.LENGTH_LONG);
					toast.show();

					finish();
				}
			}
		}
	}

	public String htmlSnippet(String s) {
		return TextUtils.htmlEncode(s.substring(0, Math.min(s.length(), 1000)));
	}
}
