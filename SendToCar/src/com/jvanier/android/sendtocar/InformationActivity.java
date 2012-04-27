package com.jvanier.android.sendtocar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

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