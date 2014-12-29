package com.jvanier.android.sendtocar.controllers;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.common.Utils;
import com.jvanier.android.sendtocar.controllers.commands.ShowHelp;
import com.jvanier.android.sendtocar.controllers.commands.ShowHelpForMake;
import com.jvanier.android.sendtocar.controllers.commands.ShowIssueForMake;
import com.jvanier.android.sendtocar.downloaders.CarListManager;
import com.jvanier.android.sendtocar.downloaders.GoogleMapsAddressLoader;
import com.jvanier.android.sendtocar.models.Address;
import com.jvanier.android.sendtocar.models.CarProvider;
import com.jvanier.android.sendtocar.models.RecentVehicle;
import com.jvanier.android.sendtocar.models.RecentVehicleList;

/* TODO:
 * 
 * - Action when car list is updated
 * - Add "Can't find your make" in MakeActivity
 */
public class SendToCarFragment extends Fragment {
	private static final String TAG = "SendToCarFragment";

	private static final String DEBUG_ON1 = "debug";
	private static final String DEBUG_ON2 = "debug on";
	private static final String DEBUG_OFF = "debug off";

	private TextView helpButton;
	private TextView issueButton;
	private EditText destinationText;
	private EditText addressText;
	private EditText notesText;
	private Button sendButton;
	private EditText accountText;

	private String helpButtonTemplateText;
	private String issueButtonTemplateText;

	private Intent intent;
	private Address loadedAddress;
	private CarProvider selectedMake;

	private boolean latestVehicleSelectedAlready;

	public SendToCarFragment(Intent intent) {
		this.intent = intent;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.sendtocar_fragment, container, false);

		setupMakeButton(rootView);
		setupVehicleHelp(rootView);
		setupAddressFields(rootView);
		setupSendButton(rootView);

		loadMapFromIntent();

		selectLatestVehicle();

		return rootView;
	}

	private void setupMakeButton(View rootView) {
		Button makeButton = (Button) rootView.findViewById(R.id.makeButton);
		makeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), MakeActivity.class);
				startActivityForResult(intent, MakeActivity.PICK_MAKE);
			}
		});

		accountText = (EditText) rootView.findViewById(R.id.accountText);
	}

	private void setupVehicleHelp(View rootView) {
		helpButton = (TextView) rootView.findViewById(R.id.vehicleHelp);
		helpButtonTemplateText = helpButton.getText().toString();
		helpButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectedMake != null) {
					new ShowHelpForMake(selectedMake.makeId);
				}
			}
		});

		issueButton = (TextView) rootView.findViewById(R.id.vehicleIssue);
		issueButtonTemplateText = issueButton.getText().toString();
		issueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectedMake != null) {
					new ShowIssueForMake(selectedMake.makeId);
				}
			}
		});

		updateVehicleHelpButtons(getResources().getText(R.string.vehicle_lowercase).toString());
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

	private void setupAddressFields(View rootView) {
		destinationText = (EditText) rootView.findViewById(R.id.destinationText);
		addressText = (EditText) rootView.findViewById(R.id.addressText);
		notesText = (EditText) rootView.findViewById(R.id.notesText);
	}

	private void setupSendButton(View rootView) {
		sendButton = (Button) rootView.findViewById(R.id.sendButton);
		updateSendButtonEnabled();
	}

	private void updateSendButtonEnabled() {
		boolean enabled = addressText.getText().length() > 0 && destinationText.getText().length() > 0 && selectedMake != null
				&& accountText.getText().length() > 0;
		sendButton.setEnabled(enabled);
	}

	private class GoogleMapsAddressLoaderWithUI extends GoogleMapsAddressLoader {

		private ProgressDialog progressDialog;
		private Address address;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// Show progress dialog
			Context context = getActivity();
			progressDialog = ProgressDialog.show(context, null, context.getString(R.string.loadingAddress));
			progressDialog.setCancelable(true);

			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					GoogleMapsAddressLoaderWithUI.this.cancelDownload();
					getActivity().finish();
				}
			});
		}

		@Override
		protected void onPostExecute(Result result) {
			super.onPostExecute(result);

			if (progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}

			if (result.success) {
				updateUIWithAddress(result.address);
			} else {
				String message = getString(result.messageId);
				showMessageBoxAndFinish(message);
			}
		}

	}

	private void loadMapFromIntent() {
		try {
			Log.d(TAG, "Intent. Action: " + intent.getAction() + ", Text: "
					+ intent.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString());
			if (intent.getAction().equals(Intent.ACTION_SEND)) {
				List<String> urls = Utils.findURLs(intent.getExtras().getCharSequence(Intent.EXTRA_TEXT).toString());

				String url = (urls.size() > 0) ? urls.get(urls.size() - 1) : null;

				Log.d(TAG, "URL: " + url);

				if (url == null) {
					// Show message about the Google Maps long press bug
					showMessageBoxAndFinish(getString(R.string.errorGoogleMapsLongPressBug));
				} else {
					if(checkNetworkReachabilityAndAlert(R.string.noInternetLoadingAddress)) {
						// Download address details from Google Maps
						new GoogleMapsAddressLoaderWithUI().execute(new String[] { url });
					} else {
						// No internet, give up
						getActivity().finish();
					}
				}
			} else {
				showMessageBoxAndFinish(getString(R.string.errorIntent));
			}
		} catch (NullPointerException e) {
			// not started from Google Maps, just allow the user to manually
			// enter the address
		}
	}

	private void showMessageBoxAndFinish(String message) {
		AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());
		alertbox.setTitle(R.string.errorTitle);
		alertbox.setMessage(message);
		alertbox.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				getActivity().finish();
			}
		});
		alertbox.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				getActivity().finish();
			}
		});

		// set a negative/no button and create a listener
		alertbox.setNegativeButton(R.string.showHelp, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				getActivity().finish();
				(new ShowHelp()).perfrom(getActivity());
			}
		});

		// display box
		alertbox.show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == MakeActivity.PICK_MAKE && resultCode == Activity.RESULT_OK) {
			String type = data.getStringExtra(MakeActivity.EXTRA_TYPE);

			switch (type) {
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

	private void loadMake(CarProvider provider) {
		// TODO Auto-generated method stub

	}

	private void loadRecentVehicle(RecentVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	private void selectLatestVehicle() {
		if (!latestVehicleSelectedAlready) {
			migrateLatestVehicleFromPreferences();

			RecentVehicle latestVehicle = RecentVehicleList.sharedInstance().latestVehicle();

			if (latestVehicle != null && latestVehicle.makeId != null && latestVehicle.account != null) {

				CarProvider p = CarListManager.sharedInstance().getCarList().get(latestVehicle.makeId);

				if (p != null) {
					Log.d(TAG, p.make + " loaded");

					updateMake(p, latestVehicle.account);
					updateSendButtonEnabled();
					latestVehicleSelectedAlready = true;
				}
			}
		}
	}

	private void migrateLatestVehicleFromPreferences() {
		SharedPreferences settings = getActivity().getPreferences(Context.MODE_PRIVATE);
		String makeId = settings.getString("make", null);
		String account = settings.getString("account", null);

		if (makeId != null && account != null) {
			CarProvider p = CarListManager.sharedInstance().getCarList().get(makeId);

			if (p != null) {
				RecentVehicle latestVehicle = new RecentVehicle();
				latestVehicle.makeId = makeId;
				latestVehicle.make = p.make;
				latestVehicle.account = account;
				RecentVehicleList.sharedInstance().addRecentVehicle(latestVehicle).saveToCache(getActivity());

				SharedPreferences.Editor settingsEditor = settings.edit();
				settingsEditor.remove("make");
				settingsEditor.remove("account");
				settingsEditor.commit();
			}
		}
	}

	private void sendDestination() {
		// Parse the account field for a special keyword to turn debug on/off
		if (updateDebugState()) {
			return;
		}

		if (validateAddressFields() && checkNetworkReachabilityAndAlert(R.string.noInternetSendingDestination)) {
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
			SharedPreferences.Editor settingsEditor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
			settingsEditor.putBoolean("debug", debug);
			settingsEditor.commit();

			AlertDialog.Builder alertbox = new AlertDialog.Builder(getActivity());
			alertbox.setTitle(R.string.debugMode);
			alertbox.setMessage(debug ? R.string.debugOn : R.string.debugOff);
			alertbox.setPositiveButton(R.string.ok, null);
			alertbox.show();
		}
	   
		return showAlertAndAbortSend;
	}

	private boolean validateAddressFields() {
		// TODO Auto-generated method stub
		return false;
	}

	private void updateAddressFromUIAndSend() {
		// TODO Auto-generated method stub
		
	}

	private boolean checkNetworkReachabilityAndAlert(int noInternetMessageId) {
		// TODO Auto-generated method stub
		return false;
	}

	private void updateMake(CarProvider p, String account) {
		// TODO Auto-generated method stub

	}

	private void updateUIWithAddress(Address address) {
		loadedAddress = address;

		destinationText.setText(address.title);
		addressText.setText(address.displayAddress);
		notesText.setText("");
	}
}
