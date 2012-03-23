package com.jvanier.android.sendtocar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

public class InformationActivity extends Activity {
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