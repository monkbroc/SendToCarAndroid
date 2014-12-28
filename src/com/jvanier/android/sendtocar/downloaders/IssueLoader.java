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

import com.jvanier.android.sendtocar.common.Constants;
import com.jvanier.android.sendtocar.models.Issue;

public class IssueLoader extends AsyncTask<Void, Void, Boolean> {
	
	public interface IssueLoaderHandler {
		public abstract void onCompletion(boolean success, Issue issue);
	}

	private static final String TAG = "IssueLoader";

	private String makeId;
	private IssueLoaderHandler handler;
	private Issue issue;
	
	public IssueLoader(String makeId, IssueLoaderHandler handler) {
		this.makeId = makeId;
		this.handler = handler;
	}
	

	private class BackgroundTaskAbort extends Exception {
		private static final long serialVersionUID = 2338278677502562127L;
	}

	@Override
	protected Boolean doInBackground(Void... ignored) {
		try {
			String jsonData = downloadJsonData();
			parseJsonDataToIssue(jsonData);
			return Boolean.TRUE;
		} catch(BackgroundTaskAbort e) {
			return Boolean.FALSE;
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

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if(handler != null) {
			handler.onCompletion(result.booleanValue(), issue);
		}
	}

}
