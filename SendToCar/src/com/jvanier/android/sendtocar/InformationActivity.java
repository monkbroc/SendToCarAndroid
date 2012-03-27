package com.jvanier.android.sendtocar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class InformationActivity extends Activity {
	private int firstRunScreen;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.information);
		WebView wv = (WebView)findViewById(R.id.helpView);
		wv.setBackgroundColor(Color.BLACK);
		
		// get translated asset if it exists
		String content = getAssetString("information-" + Locale.getDefault().getLanguage() + ".html");
		if(content == null)
		{
			content = getAssetString("information.html");
		}
		wv.loadDataWithBaseURL("file:///android_asset", content, "text/html", "UTF-8", "");

		setupStartMaps();
		
		setupTutorial();
		
		firstRunDialog(false);
	}
	
	private void setupTutorial() {
		findViewById(R.id.tutorialButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				firstRunDialog(true);
			}
		});
	}

	private void firstRunDialog(boolean force) {
	    boolean firstrun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstrun", true);
	    if (firstrun || force) {
	    	firstRunScreen = 0;
	    	final int screenDrawables[] = {R.drawable.screenshot1, R.drawable.screenshot2, R.drawable.screenshot3, R.drawable.screenshot4 };
	    	final int screenStrings[] = { R.string.infoStart, R.string.infoShare, R.string.infoSend, R.string.infoEnter };
	    	
	    	final Dialog d = new Dialog(this, android.R.style.Theme);
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
	
	public void setupStartMaps()
	{
        final Button button = (Button) findViewById(R.id.mapsButton);
        button.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {

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

	}

	protected String getAssetString(String assetName) {
		AssetManager assetManager = getAssets();
		InputStream stream = null;
		String s = null;

		try {
			stream = assetManager.open(assetName);
			s = readTextFile(stream);
		} catch (IOException e) {
			// handle
		}
		finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {}
			}
		}
		return s;
	}

	protected String readTextFile(InputStream inputStream) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				outputStream.write(buf,0,len);
			}
			outputStream.close();
			inputStream.close();
		} catch (IOException e) {

		}
		return outputStream.toString();
	}
}