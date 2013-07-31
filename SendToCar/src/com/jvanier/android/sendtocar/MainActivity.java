package com.jvanier.android.sendtocar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private interface Clicker {
		public void OnClick();
	}

	private DebugLog log;

	private ListView mainList;
	private ArrayList<Clicker> mainEvents;
	private int firstRunScreen;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		log = ((SendToCarApp) getApplication()).getLog();
		
        populateMainList();

		firstRunDialog(false);
		
		loadCars();
	}

	private void loadCars() {
		CarListLoader loader = new CarListLoader(this, log);
		loader.readCars();
	}
	
	private void populateMainList() {
		mainList = (ListView) findViewById(R.id.mainListView);

        // Insert each list element into an ArrayList
        ArrayList<HashMap<String, String>> listItems = new ArrayList<HashMap<String, String>>();
        mainEvents = new ArrayList<Clicker>();

        listItems.add(setupOpenMap());
        listItems.add(setupManualAddress());
        listItems.add(setupTutorial());
        listItems.add(setupHelp());
        listItems.add(setupRate());
        listItems.add(setupEmail());
 
        // Create a SimpleAdapter to show items
        SimpleAdapter adapter = new SimpleAdapter (this.getBaseContext(), listItems, R.layout.mainitem,
               new String[] {"img", "title", "description"}, new int[] {R.id.img, R.id.title, R.id.description});
        mainList.setAdapter(adapter);
 
        mainList.setOnItemClickListener(new OnItemClickListener() {
			@Override
         	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				mainEvents.get(position).OnClick();
        	}
         });
	}


	private HashMap<String, String> setupOpenMap() {
		HashMap<String, String> map;

        map = new HashMap<String, String>();
        map.put("title", getString(R.string.listMapTitle));
        map.put("description", getString(R.string.listMapDescription));
        map.put("img", String.valueOf(R.drawable.ic_menu_see_map));
        mainEvents.add(new Clicker() {
			@Override
			public void OnClick() {
        		Intent intent = new Intent("android.intent.action.MAIN");
        		intent.setComponent(ComponentName.unflattenFromString("com.google.android.apps.maps/com.google.android.maps.MapsActivity"));
        		intent.addCategory("android.intent.category.LAUNCHER");
        		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        		try
        		{
        			startActivity(intent);
        		}
        		catch(ActivityNotFoundException e)
        		{
        			Context context = getApplicationContext();
        			Toast toast = Toast.makeText(context, R.string.errorStartGoogleMaps, Toast.LENGTH_SHORT);
        			toast.show();
        		}
        	}
        });
        return map;
	}

	private HashMap<String, String> setupManualAddress() {
		HashMap<String, String> map;

        map = new HashMap<String, String>();
        map.put("title", getString(R.string.listManualTitle));
        map.put("description", getString(R.string.listManualDescription));
        map.put("img", String.valueOf(R.drawable.ic_menu_edit));
        
        mainEvents.add(new Clicker() {
			@Override
			public void OnClick() {
        		Intent intent = new Intent(MainActivity.this, SendToCarActivity.class);
        		try {
        			startActivity(intent);
        		}
        		catch(ActivityNotFoundException e)
        		{
        			Context context = getApplicationContext();
        			Toast toast = Toast.makeText(context, R.string.errorNoApp, Toast.LENGTH_SHORT);
        			toast.show();
        		}

        	}
        });
        
        return map;
	}

	private HashMap<String, String> setupTutorial() {
		HashMap<String, String> map;
		map = new HashMap<String, String>();
        map.put("title", getString(R.string.listTutorialTitle));
        map.put("description", getString(R.string.listTutorialDescription));
        map.put("img", String.valueOf(R.drawable.ic_menu_list));

        mainEvents.add(new Clicker() {
			@Override
			public void OnClick() {
				firstRunDialog(true);
        	}
        });
        
		return map;
	}

	private HashMap<String, String> setupHelp() {
		HashMap<String, String> map;
		map = new HashMap<String, String>();
        map.put("title", getString(R.string.listHelpTitle));
        map.put("description", getString(R.string.listHelpDescription));
        map.put("img", String.valueOf(R.drawable.ic_menu_help));
        

        mainEvents.add(new Clicker() {
			@Override
			public void OnClick() {
        		Intent intent = new Intent(MainActivity.this, InformationActivity.class);
        		try {
        			startActivity(intent);
        		}
        		catch(ActivityNotFoundException e)
        		{
        			Context context = getApplicationContext();
        			Toast toast = Toast.makeText(context, R.string.errorNoApp, Toast.LENGTH_SHORT);
        			toast.show();
        		}
			}
        });
        
		return map;
	}

	private HashMap<String, String> setupRate() {
		HashMap<String, String> map;
		map = new HashMap<String, String>();
        map.put("title", getString(R.string.listRateTitle));
        map.put("description", getString(R.string.listRateDescription));
        map.put("img", String.valueOf(R.drawable.ic_menu_star));
        

        mainEvents.add(new Clicker() {
			@Override
			public void OnClick() {
				Intent intent = new Intent(Intent.ACTION_VIEW); 
				intent.setData(Uri.parse("market://details?id=com.jvanier.android.sendtocar")); 
        		try {
        			startActivity(intent);
        		}
        		catch(ActivityNotFoundException e)
        		{
        			Context context = getApplicationContext();
        			Toast toast = Toast.makeText(context, R.string.errorNoApp, Toast.LENGTH_SHORT);
        			toast.show();
        		}
			}
        });
        
		return map;
	}

	private HashMap<String, String> setupEmail() {
		HashMap<String, String> map;
		map = new HashMap<String, String>();
        map.put("title", getString(R.string.listEmailTitle));
        map.put("description", getString(R.string.listEmailDescription));
        map.put("img", String.valueOf(R.drawable.ic_menu_email));
        

        mainEvents.add(new Clicker() {
			@Override
			public void OnClick() {
				Intent intent = new Intent(Intent.ACTION_VIEW); 
				intent.setData(Uri.parse("mailto:sendtocar.app@gmail.com")); 
        		try {
        			startActivity(intent);
        		}
        		catch(ActivityNotFoundException e)
        		{
        			Context context = getApplicationContext();
        			Toast toast = Toast.makeText(context, R.string.errorNoApp, Toast.LENGTH_SHORT);
        			toast.show();
        		}
        	}
        });
        
		return map;
	}
	

	private void firstRunDialog(boolean force) {
	    boolean firstrun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstrun", true);
	    if (firstrun || force) {
	    	firstRunScreen = 0;
	    	final int screenDrawables[] = {R.drawable.screenshot1, R.drawable.screenshot2, R.drawable.screenshot3, R.drawable.screenshot4 };
	    	final int screenStrings[] = { R.string.infoStart, R.string.infoShare, R.string.infoSend, R.string.infoEnter };

	    	final Dialog d = new Dialog(this, android.R.style.Theme_DeviceDefault);
	    	d.setContentView(R.layout.firstrun_dialog);
	    	d.setTitle(R.string.infoHowTo);
	    	d.findViewById(R.id.prevButton).setVisibility(View.INVISIBLE);
	    	
	    	d.findViewById(R.id.prevButton).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(firstRunScreen > 0)
					{
						firstRunScreen--;
						TextView t = (TextView)d.findViewById(R.id.messageText);
						t.setText(screenStrings[firstRunScreen]);
						t.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
								v.getResources().getDrawable(screenDrawables[firstRunScreen]));
					}
					
					boolean hide = (firstRunScreen == 0);
					d.findViewById(R.id.prevButton).setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
					((Button)d.findViewById(R.id.nextButton)).setText(R.string.infoNext);
				}
			});
	    	
	    	
	    	d.findViewById(R.id.nextButton).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(firstRunScreen <= 2)
					{
						firstRunScreen++;
						TextView t = (TextView)d.findViewById(R.id.messageText);
						t.setText(screenStrings[firstRunScreen]);
						t.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
								v.getResources().getDrawable(screenDrawables[firstRunScreen]));
					}
					else
					{
						d.dismiss();
					}
					
					d.findViewById(R.id.prevButton).setVisibility(View.VISIBLE);
					boolean finish = (firstRunScreen == 3);
					((Button)d.findViewById(R.id.nextButton)).setText(finish ? R.string.infoFinish : R.string.infoNext);
				}
			});
	    	
	        d.show();


	    	// Save the state
	    	getSharedPreferences("PREFERENCE", MODE_PRIVATE)
		    	.edit()
		    	.putBoolean("firstrun", false)
		    	.commit();
	    }

	}
}
