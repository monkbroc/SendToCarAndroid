package com.jvanier.android.sendtocar.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.jvanier.android.sendtocar.R;

public class SendToCarFragment extends Fragment {
	private int mShortAnimationDuration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.sendtocar_fragment, container, false);

		// Retrieve and cache the system's default "short" animation time.
		mShortAnimationDuration = getResources().getInteger(
				android.R.integer.config_shortAnimTime);
		
        CardView cardView = (CardView) rootView.findViewById(R.id.vehicleCard);
        
        createCurrentVehicleView(inflater, cardView);
        
        return rootView;
    }
	
	private void createCurrentVehicleView(final LayoutInflater inflater, final CardView cardView) {
		cardView.removeAllViews();
		View currentVehicleView = inflater.inflate(R.layout.current_vehicle, cardView, true);
		
		View changeVehicleView = currentVehicleView.findViewById(R.id.changeVehicle);
        
        final SendToCarFragment fragment = this;
        changeVehicleView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fragment.createVehicleListView(inflater, cardView);
			}
		});
	}

	private void createVehicleListView(final LayoutInflater inflater, final CardView cardView) {
		View vehicleListView = inflater.inflate(R.layout.vehicle_list, cardView, false);

		View vehicleEditView = vehicleListView.findViewById(R.id.editButton);

        final SendToCarFragment fragment = this;
        vehicleEditView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fragment.createVehicleEditListView(inflater, cardView);
			}
		});
		animateReplacementOfChildView(cardView, vehicleListView);
	}
	
	private void createVehicleEditListView(final LayoutInflater inflater, final CardView cardView) {
		View vehicleEditListView = inflater.inflate(R.layout.vehicle_edit_list, cardView, false);
		
        Spinner makeSpinner = (Spinner) vehicleEditListView.findViewById(R.id.makeSpinner);
        setupMakeSpinner(makeSpinner);
        
		animateReplacementOfChildView(cardView, vehicleEditListView);

		/*
        final PlaceholderFragment fragment = this;
        newVehicleView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fragment.createVehicleListView(inflater, cardView);
			}
		});
		*/
	}
	
	private void setupMakeSpinner(Spinner spinner)
	{
		/*
		ArrayList<CarProvider> carsList = new ArrayList<CarProvider>();
		CarProvider fake = new CarProvider();
		fake.make = getString(R.string.choose_make);
		fake.type = 0;
		carsList.add(fake);
		

		ArrayAdapter<CarProvider> adapter = new ArrayAdapter<CarProvider>(this,
				android.R.layout.simple_spinner_item, carsList.toArray(new CarProvider[carsList.size()]));
				*/

		String[] carsList = new String[3];
		carsList[0] = getString(R.string.choose_make);
		carsList[1] = "Ford";
		carsList[2] = "Toyota";

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				R.layout.make_spinner_item, carsList);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
	}
	
	private void animateReplacementOfChildView(ViewGroup container, View newView) {
		ReplaceChildViewAnimation anim = new ReplaceChildViewAnimation(container, newView);
		anim.setDuration(mShortAnimationDuration).start();
	}
}
