package com.jvanier.android.sendtocar.controllers;

import java.text.MessageFormat;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
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
import com.jvanier.android.sendtocar.downloaders.GoogleMapsAddressLoader;
import com.jvanier.android.sendtocar.models.Address;
import com.jvanier.android.sendtocar.models.CarProvider;
import com.jvanier.android.sendtocar.models.RecentVehicle;


/* TODO:
 * 
 * - Action when car list is updated
 * 
 */
public class SendToCarFragment extends Fragment {
	private static final String TAG = "SendToCarFragment";
	private TextView helpButton;
	private TextView issueButton;

	private String helpButtonTemplateText;
	private String issueButtonTemplateText;
	private Intent intent;
	private EditText destinationText;
	private EditText addressText;
	private EditText notesText;
	private Address loadedAddress;

	public SendToCarFragment(Intent intent) {
		this.intent = intent;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.sendtocar_fragment, container, false);

		setupMakeButton(rootView);
		setupVehicleHelp(rootView);
		setupAddressFields(rootView);

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
	}

	private void setupVehicleHelp(View rootView) {
		helpButton = (TextView) rootView.findViewById(R.id.vehicleHelp);
		helpButtonTemplateText = helpButton.getText().toString();

		issueButton = (TextView) rootView.findViewById(R.id.vehicleIssue);
		issueButtonTemplateText = issueButton.getText().toString();

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
	
	private class GoogleMapsAddressLoaderWithUI extends GoogleMapsAddressLoader {

		private ProgressDialog progressDialog;
		private Address address;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// Show progress dialog
			Context context = getActivity();
			progressDialog = ProgressDialog.show(context,
					null, context.getString(R.string.loadingAddress));
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
			
			if(progressDialog != null) {
				progressDialog.dismiss();
				progressDialog = null;
			}
			
			if(result.success) {
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
					// Download address details from Google Maps
					new GoogleMapsAddressLoaderWithUI().execute(new String[] { url });
				}
			} else {
				showMessageBoxAndFinish(getString(R.string.errorIntent));
			}
		} catch (NullPointerException e) {
			// not started from Google Maps, just allow the user to manually enter the address
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
		// TODO Auto-generated method stub
		
	}

	private void updateUIWithAddress(Address address) {
		loadedAddress = address;
		
		destinationText.setText(address.title);
		addressText.setText(address.displayAddress);
		notesText.setText("");
	}
}
