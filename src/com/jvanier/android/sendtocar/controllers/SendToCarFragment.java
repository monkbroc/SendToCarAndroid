package com.jvanier.android.sendtocar.controllers;

import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jvanier.android.sendtocar.R;
import com.jvanier.android.sendtocar.models.CarProvider;
import com.jvanier.android.sendtocar.models.RecentVehicle;

public class SendToCarFragment extends Fragment {

	private TextView helpButton;
	private TextView issueButton;

	private String helpButtonTemplateText;
	private String issueButtonTemplateText;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.sendtocar_fragment, container, false);
        
        setupMakeButton(rootView);
        setupVehicleHelp(rootView);
        
        return rootView;
    }
	
	private void setupMakeButton(View rootView)
	{
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == MakeActivity.PICK_MAKE && resultCode == Activity.RESULT_OK) {
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

	private void loadMake(CarProvider provider) {
		// TODO Auto-generated method stub
		
	}

	private void loadRecentVehicle(RecentVehicle vehicle) {
		// TODO Auto-generated method stub
		
	}
}
