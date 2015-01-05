package com.jvanier.android.sendtocar.downloaders;

import java.net.HttpURLConnection;
import java.net.URI;
import java.text.MessageFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.SniHttpClient;
import com.jvanier.android.sendtocar.models.Issue;

public class IssueLoader extends AsyncTask<Void, Void, Issue> {
	public interface IssueLoaderHandler {
		public void onPreExecute(final IssueLoader loader);

		public void onPostExecute(final IssueLoader loader, Issue issue);
	}

	private static final String TAG = "IssueLoader";

	private String makeId;
	private Issue issue;

	private IssueLoaderHandler handler;

	public IssueLoader(String makeId, IssueLoaderHandler handler) {
		this.makeId = makeId;
		this.handler = handler;
	}

	public String getMakeId() {
		return makeId;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if(handler != null) {
			handler.onPreExecute(this);
		}
	}

	@Override
	protected void onPostExecute(Issue issue) {
		super.onPostExecute(issue);
		if(handler != null) {
			handler.onPostExecute(this, issue);
		}
	}

	@Override
	protected Issue doInBackground(Void... ignored) {
		try {
			String jsonData = downloadJsonData();
			parseJsonDataToIssue(jsonData);
			return issue;
		} catch(BackgroundTaskAbort e) {
			return null;
		}
	}

	private String downloadJsonData() throws BackgroundTaskAbort {
		String jsonData = "";
		try {
			URI issueUri = new URI(MessageFormat.format(Constants.ISSUE_URL, makeId));
			if(Log.isEnabled()) Log.d(TAG, "Downloading issue for " + makeId);

			HttpGet httpGet = new HttpGet();
			httpGet.setURI(issueUri);

			DefaultHttpClient client = new SniHttpClient();
			HttpResponse response = client.execute(httpGet);

			if(Log.isEnabled()) Log.d(TAG, "Downloaded issue. Status: " + response.getStatusLine().getStatusCode());

			switch(response.getStatusLine().getStatusCode()) {
			case HttpURLConnection.HTTP_OK:
				break;
			default:
				throw new BackgroundTaskAbort();
			}

			jsonData = EntityUtils.toString(response.getEntity());
			if(Log.isEnabled()) Log.d(TAG, "Response: " + jsonData);

		} catch(Exception e) {
			if(Log.isEnabled()) Log.e(TAG, "Exception while downloading issue: " + e.toString());
			throw new BackgroundTaskAbort();
		}

		return jsonData;
	}

	private void parseJsonDataToIssue(String jsonData) throws BackgroundTaskAbort {

		try {
			JSONObject issueRaw = new JSONObject(jsonData.toString());
			boolean hasIssue = issueRaw.optBoolean("has_issue", false);
			String message = issueRaw.optString("message");
			issue = new Issue(hasIssue, message);

			if(Log.isEnabled()) Log.d(TAG, "Issue JSON parsed OK.");

		} catch(JSONException e) {
			if(Log.isEnabled()) Log.e(TAG, "Exception while parsing issue JSON: " + e.toString());
			throw new BackgroundTaskAbort();
		}
	}
}
