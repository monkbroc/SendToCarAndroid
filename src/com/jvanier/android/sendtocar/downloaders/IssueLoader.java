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
import android.util.Log;

import com.jvanier.android.sendtocar.common.BackgroundTaskAbort;
import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.models.Issue;

/* Users of this class can override onPostExecute(Issue issue) to receive the results */
public class IssueLoader extends AsyncTask<Void, Void, Issue> {
	
	private static final String TAG = "IssueLoader";

	private String makeId;
	private Issue issue;
	
	public IssueLoader(String makeId) {
		this.makeId = makeId;
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
			Log.d(TAG, "Downloading issue for " + makeId);


			HttpGet httpGet = new HttpGet();
			httpGet.setURI(issueUri);

			DefaultHttpClient client = new DefaultHttpClient();
			HttpResponse response = client.execute(httpGet);

			Log.d(TAG, "Downloaded issue. Status: " + response.getStatusLine().getStatusCode());

			switch (response.getStatusLine().getStatusCode()) {
			case HttpURLConnection.HTTP_OK:
				break;
			default:
				throw new BackgroundTaskAbort();
			}

			jsonData = EntityUtils.toString(response.getEntity());
			Log.d(TAG, "Response: " + jsonData);

		} catch (Exception e) {
			Log.e(TAG, "Exception while downloading issue: " + e.toString());
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
			
			Log.d(TAG, "Issue JSON parsed OK.");

		} catch (JSONException e) {
			Log.e(TAG, "Exception while parsing issue JSON: " + e.toString());
			throw new BackgroundTaskAbort();
		}
	}
}
