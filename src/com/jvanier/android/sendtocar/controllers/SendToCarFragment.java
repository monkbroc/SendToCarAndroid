package com.jvanier.android.sendtocar.controllers;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.Log;
import com.jvanier.android.sendtocar.common.Mixpanel;
import com.jvanier.android.sendtocar.common.Utils;
import com.jvanier.android.sendtocar.controllers.commands.ShowHelp;
import com.jvanier.android.sendtocar.controllers.commands.ShowHelpForMake;
import com.jvanier.android.sendtocar.controllers.commands.ShowIssueForMake;
import com.jvanier.android.sendtocar.downloaders.CarListManager;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsAddressLoader;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsAddressLoader.GoogleMapsAddressLoaderHandler;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsAddressLoader.Result;
import com.jvanier.android.sendtocar.downloaders.IssueLoader;
import com.jvanier.android.sendtocar.downloaders.IssueLoader.IssueLoaderHandler;
import com.jvanier.android.sendtocar.models.Address;
import com.jvanier.android.sendtocar.models.CarProvider;
import com.jvanier.android.sendtocar.models.Issue;
import com.jvanier.android.sendtocar.models.RecentVehicle;
import com.jvanier.android.sendtocar.models.RecentVehicleList;
import com.jvanier.android.sendtocar.models.UserPreferences;
import com.jvanier.android.sendtocar.uploaders.BaseUploader;
import com.jvanier.android.sendtocar.uploaders.BaseUploader.BaseUploaderHandler;
import com.jvanier.android.sendtocar.uploaders.GoogleMapsUploader;
import com.jvanier.android.sendtocar.uploaders.HereComUploader;
import com.jvanier.android.sendtocar.uploaders.MapquestUploader;
import com.jvanier.android.sendtocar.uploaders.OnStarUploader;

/* TODO:
 *
 * - Grey send button when disabled and green when enabled
 *   http://stackoverflow.com/questions/14042866/state-list-drawable-and-disabled-state
 * - Translation
 * 
 */
public class SendToCarFragment extends Fragment {
	private static final String TAG = "SendToCarFragment";

	private static final String DEBUG_ON1 = "debug";
	private static final String DEBUG_ON2 = "debug on";
	private static final String DEBUG_OFF = "debug off";

	private static final String ADDRESS_ENTERED_MANUALLY = "AddressEnteredManually";
	private static final String ADDRESS_FROM_GOOGLE_MAPS = "AddressFromGoogleMaps";

	public static final int PICK_MAKE = 0;

	private Button makeButton;
	private TextView accountLabel;
	private EditText accountText;
	private TextView helpButton;
	private TextView issueButton;
	private EditText destinationText;
	private EditText addressText;
	private EditText notesText;
	private Button sendButton;

	private String helpButtonTemplateText;
	private String issueButtonTemplateText;

	private Intent intent;
	private Address loadedAddress;
	private CarProvider selectedMake;
	private String addressOrigin;

	private boolean latestVehicleSelectedAlready;

	private String oldMakeId;

	private Button cancelButton;

	private TextWatcher textWatcher;

	public SendToCarFragment(Intent intent) {
		this.intent = intent;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.sendtocar_fragment, container, false);

		setupTextWatcher();
		setupMakeButton(rootView);
		setupVehicleHelp(rootView);
		setupAddressFields(rootView);
		setupButtonBarButtons(rootView);

		loadMapFromIntent();
		showTutorialFirstTime();
		selectLatestVehicle();

		return rootView;
	}

	private void setupTextWatcher() {
		textWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// Do nothing
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// Do nothing
			}

			@Override
			public void afterTextChanged(Editable s) {
				updateSendButtonEnabled();
			}
		};
	}

	private void setupMakeButton(View rootView) {
		makeButton = (Button) rootView.findViewById(R.id.makeButton);
		makeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), MakeActivity.class);
				startActivityForResult(intent, PICK_MAKE);
			}
		});

		accountLabel = (TextView) rootView.findViewById(R.id.accountLabel);
		accountText = (EditText) rootView.findViewById(R.id.accountText);
		accountText.addTextChangedListener(textWatcher);
	}

	private void setupVehicleHelp(View rootView) {
		helpButton = (TextView) rootView.findViewById(R.id.vehicleHelp);
		helpButtonTemplateText = helpButton.getText().toString();
		helpButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(selectedMake != null) {
					new ShowHelpForMake(selectedMake.makeId).perfrom(getActivity());
				}
			}
		});

		issueButton = (TextView) rootView.findViewById(R.id.vehicleIssue);
		issueButtonTemplateText = issueButton.getText().toString();
		issueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(selectedMake != null) {
					new ShowIssueForMake(selectedMake.makeId).perfrom(getActivity());
				}
			}
		});

		updateVehicleHelpButtons(getResources().getText(R.string.vehicle_lowercase).toString());
	}

	private void setupAddressFields(View rootView) {
		destinationText = (EditText) rootView.findViewById(R.id.destinationText);
		addressText = (EditText) rootView.findViewById(R.id.addressText);
		notesText = (EditText) rootView.findViewById(R.id.notesText);

		destinationText.addTextChangedListener(textWatcher);
		addressText.addTextChangedListener(textWatcher);
	}

	private void setupButtonBarButtons(View rootView) {
		sendButton = (Button) rootView.findViewById(R.id.sendButton);
		sendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendDestination();
			}
		});
		updateSendButtonEnabled();

		cancelButton = (Button) rootView.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().finish();
			}
		});
	}

	private void updateSendButtonEnabled() {
		boolean enabled = addressText.getText().length() > 0 && destinationText.getText().length() > 0 && selectedMake != null
				&& accountText.getText().length() > 0;
		sendButton.setEnabled(enabled);
	}

	private class GoogleMapsAddressLoaderUIHandler implements GoogleMapsAddressLoaderHandler {

		private ProgressDialog progressDialog;

		@Override
		public void onPreExecute(final GoogleMapsAddressLoader loader) {
			// Show progress dialog
			Context context = getActivity();
			progressDialog = ProgressDialog.show(context, null, context.getString(R.string.loadingAddress));
			progressDialog.setCancelable(true);

			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					loader.cancelDownload();
					getActivity().finish();
				}
			});
		}

		@Override
		public void onPostExecute(final GoogleMapsAddressLoader loader, Result result) {
			if(progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}

			if(result.success) {
				updateUIWithAddress(result.address);
			} else {
				String message = getString(result.messageId);
				showMessageBoxAndFinish(message, true);
			}
		}
	}

	private void loadMapFromIntent() {
		addressOrigin = ADDRESS_ENTERED_MANUALLY;

		String action = intent.getAction();
		String text = intent.hasExtra(Intent.EXTRA_TEXT) ? intent.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString() : null;

		if(Log.isEnabled()) Log.d(TAG, "Intent. Action: " + action + ", Text: " + text);

		if(action.equals(Intent.ACTION_SEND)) {
			List<String> urls = Utils.findURLs(intent.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString());

			// Show the cancel button when loading an address from Google
			// Maps to go back to the Maps app
			showCancelButton(true);

			String url = (urls.size() > 0) ? urls.get(urls.size() - 1) : null;

			if(Log.isEnabled()) Log.d(TAG, "URL: " + url);

			if(url == null) {
				// Share -> Send To Car was selected from another app than
				// Google Maps
				showMessageBoxAndFinish(getString(R.string.errorIntent), false);
			} else {
				if(checkNetworkReachabilityAndAlert(R.string.noInternetLoadingAddress)) {
					// Download address details from Google Maps
					addressOrigin = ADDRESS_FROM_GOOGLE_MAPS;
					new GoogleMapsAddressLoader(new GoogleMapsAddressLoaderUIHandler()).execute(new String[] { url });
				} else {
					// No internet, give up
					getActivity().finish();
				}
			}
		} else {
			// not started from Google Maps, just allow the user to manually
			// enter the address
		}
	}

	private void showMessageBoxAndFinish(String message, final boolean finishOnOk) {
		AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());
		alertbox.setTitle(R.string.errorTitle);
		alertbox.setMessage(message);
		alertbox.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if(finishOnOk) {
					getActivity().finish();
				}
			}
		});
		alertbox.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if(finishOnOk) {
					getActivity().finish();
				}
			}
		});

		// set a negative/no button and create a listener
		alertbox.setNegativeButton(R.string.showHelp, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if(finishOnOk) {
					getActivity().finish();
				}
				// Show help for make
				if(selectedMake != null) {
					new ShowHelpForMake(selectedMake.makeId).perfrom(getActivity());
				} else {
					new ShowHelp().perfrom(getActivity());
				}
			}
		});

		// display box
		alertbox.show();
	}

	private void showTutorialFirstTime() {
		UserPreferences prefs = UserPreferences.sharedInstance();
		if(addressOrigin == ADDRESS_ENTERED_MANUALLY && !prefs.isTutorialShown()) {
			prefs.setTutorialShown(true).save(getActivity());
			Intent intent = new Intent(getActivity(), TutorialActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == PICK_MAKE && resultCode == Activity.RESULT_OK) {
			String type = data.getStringExtra(MakeActivity.EXTRA_TYPE);

			switch(type) {
			case MakeActivity.TYPE_RECENT_VEHICLE:
				RecentVehicle vehicle = (RecentVehicle) data.getSerializableExtra(MakeActivity.EXTRA_RECENT_VEHICLE);
				loadRecentVehicle(vehicle);
				break;
			case MakeActivity.TYPE_MAKE:
				CarProvider provider = (CarProvider) data.getSerializableExtra(MakeActivity.EXTRA_PROVIDER);
				loadMake(provider);
				break;
			}
		}
	}

	private void selectLatestVehicle() {
		if(!latestVehicleSelectedAlready) {
			RecentVehicleList.sharedInstance().migrateLatestVehicleFromPreferences(getActivity());

			RecentVehicle latestVehicle = RecentVehicleList.sharedInstance().latestVehicle();

			if(latestVehicle != null && latestVehicle.makeId != null && latestVehicle.account != null) {

				CarProvider p = CarListManager.sharedInstance().getCarList().get(latestVehicle.makeId);

				if(p != null) {
					if(Log.isEnabled()) Log.d(TAG, p.make + " loaded");

					updateMake(p, latestVehicle.account);
					updateSendButtonEnabled();
					latestVehicleSelectedAlready = true;
				}
			}
		}
	}

	private void sendDestination() {
		// Parse the account field for a special keyword to turn debug on/off
		if(updateDebugState()) {
			return;
		}

		if(checkNetworkReachabilityAndAlert(R.string.noInternetSendingDestination)) {
			updateAddressFromUIAndSend();
		}
	}

	private boolean updateDebugState() {
		String txt = accountText.getText().toString().toLowerCase(Locale.US);

		boolean showAlertAndAbortSend = false;
		boolean debug = false;
		if(txt.equals(DEBUG_ON1) || txt.equals(DEBUG_ON2)) {
			showAlertAndAbortSend = true;
			debug = true;
		} else if(txt.equals(DEBUG_OFF)) {
			showAlertAndAbortSend = true;
			debug = false;
		}

		if(showAlertAndAbortSend) {
			UserPreferences.sharedInstance().setDebug(debug).save(getActivity());
			if(debug) {
				Log.enableToFile(getActivity());
			} else {
				Log.disableAndDeleteFile(getActivity());
			}

			AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());
			alertbox.setTitle(R.string.debugMode);
			alertbox.setMessage(debug ? R.string.debugOn : R.string.debugOff);
			alertbox.setPositiveButton(R.string.ok, null);
			alertbox.show();
		}

		return showAlertAndAbortSend;
	}

	private void updateAddressFromUIAndSend() {
		saveProviderToRecentVehicleList();
		hideKeyboard();

		String updatedName = destinationText.getText().toString();
		String updatedAddress = addressText.getText().toString();

		if(loadedAddress != null && loadedAddress.title.equals(updatedName) && loadedAddress.displayAddress.equals(updatedAddress)) {
			if(Log.isEnabled()) Log.d(TAG, "Don't update address from fields");
		} else {
			// User modified address or entered one manually
			addressOrigin = ADDRESS_ENTERED_MANUALLY;
			Address address = new Address();
			address.title = updatedName;
			address.displayAddress = updatedAddress;
			loadedAddress = address;

			if(Log.isEnabled()) {
				Log.d(TAG, "Address updated from fields");
				Log.d(TAG, "Title: " + address.title);
				Log.d(TAG, "Address: " + address.displayAddress);
			}
		}

		JSONObject props = new JSONObject();
		try {
			props.put("AddressOrigin", addressOrigin);
			props.put("Make", selectedMake.makeId);
		} catch(JSONException e) {
		}
		Mixpanel.sharedInstance().track("Sending address", props);

		doAddressSend();
	}

	private void hideKeyboard() {
		View focusedView = getActivity().getCurrentFocus();
		if(focusedView != null) {
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
		}
	}

	private class UploaderUIHandler implements BaseUploaderHandler {

		private ProgressDialog progressDialog;

		@Override
		public void onPreExecute(final BaseUploader uploader) {
			// Show progress dialog
			Context context = getActivity();
			progressDialog = ProgressDialog.show(context, null, context.getString(R.string.sendingToCar));
			progressDialog.setCancelable(true);

			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					uploader.cancelUpload();
				}
			});
		}

		@Override
		public void onPostExecute(final BaseUploader uploader, Boolean result) {
			boolean success = result.booleanValue();

			if(progressDialog != null) {
				try {
					progressDialog.dismiss();
				} catch(Exception e) {
					/* Dismissing dialog might fail if view is already gone */
				}
				progressDialog = null;
			}

			Context context = getActivity();
			String message = "";
			if(success) {
				String msgStr = context.getString(R.string.successCar);

				/*
				 * Show additional message for Ford since users seem to find it
				 * difficult to download the destination to the car
				 */
				if(uploader.getProvider().provider == CarProvider.PROVIDER_MAPQUEST && !UserPreferences.sharedInstance().hideFordHint()) {
					msgStr += "\n\n" + context.getString(R.string.fordDownload);

					AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());
					alertbox.setTitle(R.string.successTitle);
					alertbox.setMessage(msgStr);
					AlertDialog.OnClickListener buttonListener = new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(which == AlertDialog.BUTTON_NEGATIVE) {
								UserPreferences.sharedInstance().setHideFordHint(true).save(getActivity());
							}
							// Close activity after successfully sending the
							// destination
							getActivity().finish();
						}
					};
					alertbox.setPositiveButton(R.string.ok, buttonListener);
					alertbox.setNegativeButton(R.string.dontShowAgain, buttonListener);
					alertbox.show();

				} else {
					Toast toast = Toast.makeText(context, msgStr, Toast.LENGTH_LONG);
					toast.show();

					// Close activity after successfully sending the destination
					getActivity().finish();
				}
			} else {
				message = uploader.getErrorMessage();
				if(message == null) {
					message = getString(uploader.getErrorStringId());
				}

				// Don't close activity if upload fails in case the user wants
				// to retry
				showMessageBoxAndFinish(message, false);
			}

			JSONObject props = new JSONObject();
			try {
				props.put("AddressOrigin", addressOrigin);
				props.put("Make", uploader.getProvider().makeId);
				if(!success) {
					props.put("Message", message);
				}
			} catch(JSONException e) {
			}
			Mixpanel.sharedInstance().track(success ? "Sending successful" : "Sending failed", props);
		}

	}

	private void doAddressSend() {
		BaseUploader uploader = null;
		UploaderUIHandler handler = new UploaderUIHandler();
		switch(selectedMake.provider) {
		case CarProvider.PROVIDER_MAPQUEST:
			if(Log.isEnabled()) Log.d(TAG, "Sending address to MapQuest");
			uploader = new MapquestUploader(getActivity(), handler);
			break;
		case CarProvider.PROVIDER_GOOGLE_MAPS:
			if(Log.isEnabled()) Log.d(TAG, "Sending address to Google Maps");
			uploader = new GoogleMapsUploader(getActivity(), handler);
			break;
		case CarProvider.PROVIDER_ONSTAR:
			if(Log.isEnabled()) Log.d(TAG, "Sending address to OnStar");
			uploader = new OnStarUploader(getActivity(), handler);
			break;
		case CarProvider.PROVIDER_HERE_COM:
			if(Log.isEnabled()) Log.d(TAG, "Sending address to Here.com");
			uploader = new HereComUploader(getActivity(), handler);
			break;
		}

		String account = accountText.getText().toString();
		String language = CarListManager.sharedInstance().getCarList().getLanguage();
		String notes = selectedMake.showNotes ? notesText.getText().toString() : "";
		uploader.sendDestination(loadedAddress, account, selectedMake, language, notes);
	}

	private void saveProviderToRecentVehicleList() {
		RecentVehicle latestVehicle = new RecentVehicle();
		latestVehicle.makeId = selectedMake.makeId;
		latestVehicle.make = selectedMake.make;
		latestVehicle.account = accountText.getText().toString();

		RecentVehicleList.sharedInstance().addRecentVehicle(latestVehicle).saveToCache(getActivity());
	}

	private boolean checkNetworkReachabilityAndAlert(int noInternetMessageId) {
		ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

		if(!isConnected) {
			AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());
			alertbox.setTitle(R.string.noInternet);
			alertbox.setMessage(noInternetMessageId);
			alertbox.setPositiveButton(R.string.ok, null);
			alertbox.show();
		}

		return isConnected;
	}

	private void updateUIWithAddress(Address address) {
		loadedAddress = address;

		destinationText.setText(address.title);
		addressText.setText(address.displayAddress);
		notesText.setText("");
	}

	private void loadMake(CarProvider provider) {
		if(provider != null) {
			if(Log.isEnabled()) Log.d(TAG, "Selected make " + provider.make);
			updateMake(provider, "");
		}
	}

	private void loadRecentVehicle(RecentVehicle vehicle) {
		if(vehicle != null) {
			if(Log.isEnabled()) Log.d(TAG, "Selected recent vehicle " + vehicle.toString());
			CarProvider provider = CarListManager.sharedInstance().getCarList().get(vehicle.makeId);
			if(provider != null) {
				updateMake(provider, vehicle.account);
			}
		}
	}

	private class IssueLoaderUIHandler implements IssueLoaderHandler {

		@Override
		public void onPreExecute(IssueLoader loader) {
			// Do nothing
		}

		@Override
		public void onPostExecute(IssueLoader loader, Issue issue) {
			if(issue != null && selectedMake != null && loader.getMakeId().equals(selectedMake.makeId)) {
				showVehicleIssueButton(issue.hasIssue);
			}
		}
	}

	private void updateMake(CarProvider provider, String account) {
		if(provider == null) {
			return;
		}

		selectedMake = provider;
		makeButton.setText(selectedMake.make);

		// set account name when selecting new make or recent vehicle, but not
		// when reloading
		if(account != null) {
			accountText.setText(account);
		}
		accountLabel.setText(selectedMake.account);
		updateVehicleHelpButtons(selectedMake.make);

		showVehicleHelpButton(true);

		// Load the know issue with the selected make
		if(oldMakeId != selectedMake.makeId) {
			showVehicleIssueButton(false);

			new IssueLoader(selectedMake.makeId, new IssueLoaderUIHandler()).execute((Void) null);

			showAndClearNotes(selectedMake.showNotes);

			oldMakeId = selectedMake.makeId;
		}
	}

	private void updateVehicleHelpButtons(String make) {
		helpButton.setText(MessageFormat.format(helpButtonTemplateText, make));
		issueButton.setText(MessageFormat.format(issueButtonTemplateText, make));
	}

	private void showVehicleHelpButton(boolean show) {
		helpButton.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	private void showVehicleIssueButton(boolean show) {
		issueButton.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	private void showAndClearNotes(boolean show) {
		notesText.setVisibility(show ? View.VISIBLE : View.GONE);
		if(show) {
			notesText.setText("");
		}
	}

	private void showCancelButton(boolean show) {
		cancelButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
	}
}
