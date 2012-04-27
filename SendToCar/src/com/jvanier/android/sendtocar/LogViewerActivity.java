package com.jvanier.android.sendtocar;

import android.app.Activity;
import android.os.Bundle;
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
	
}
