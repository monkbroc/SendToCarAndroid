package com.jvanier.android.sendtocar.uploaders;

import android.os.AsyncTask;

import com.jvanier.android.sendtocar.models.Address;
import com.jvanier.android.sendtocar.models.CarProvider;


public class BaseUploader extends AsyncTask<Void, Void, Boolean> {
	private static final String TAG = "BaseUploader";
	
	public interface BaseUploaderHandler {
		public void onPreExecute(final BaseUploader self);
		public void onPostExecute(final BaseUploader self, Boolean result);
	}

	private BaseUploaderHandler handler;
	private Address address;
	private String account;
	private CarProvider provider;
	private String language;
	private String notes;
	
	private String errorMessage;
	private int errorStringId;

	public String getErrorMessage() {
		return errorMessage;
	}

	public int getErrorStringId() {
		return errorStringId;
	}

	public BaseUploader(BaseUploaderHandler handler) {
		this.handler = handler;
	}

	public void cancelUpload() {
		cancel(false);
		// httpPost.abort();
		// httpGet.abort();
	}
	
	public void sendDestination(Address address, String account, CarProvider provider, String language, String notes) {
		this.address = address;
		this.account = account;
		this.provider = provider;
		this.language = language;
		this.notes = notes;
		// TODO Auto-generated method stub
		
	}

	public Address getAddress() {
		return address;
	}

	public String getAccount() {
		return account;
	}

	public CarProvider getProvider() {
		return provider;
	}

	public String getLanguage() {
		return language;
	}

	public String getNotes() {
		return notes;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if(handler != null) {
			handler.onPreExecute(this);
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if(handler != null) {
			handler.onPostExecute(this, result);
		}
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		// TODO Auto-generated method stub
		return null;
	}

}
