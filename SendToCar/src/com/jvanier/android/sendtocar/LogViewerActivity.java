package com.jvanier.android.sendtocar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class LogViewerActivity extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logviewer);
		
		setupButtons();
		refreshLog();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		
		refreshLog();
	}

	private void setupButtons() {
        final Button refreshButton = (Button) findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		refreshLog();
        	}
        });
        
        final Button clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		clearLog();
        	}
        });

        
        final Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		sendLog();
        	}
        });
	}

	private void clearLog() {
		DebugLogFile.clearLog(this);
		refreshLog();
	}

	private void refreshLog() {

		String log = DebugLogFile.readLog(this);
		if(log == null)
		{
			log = "<h1>No log</h1>";
		}
		
		log = "<html><body>" + log + "</body></html>";
		
		WebView lv = (WebView)findViewById(R.id.logView); 
		lv.loadData(log, "text/html", "UTF-8");

	}
	
	private void sendLog() {
		String log = DebugLogFile.readLog(this);
		
	    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
	    emailIntent.setType("text/html");
	    String aEmailList[] = { "sendtocar.app@gmail.com" };
	    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
	    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Send To Car Debug Log");
	    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(log));

	    startActivity(emailIntent);
	}
	
}
