package com.jvanier.android.sendtocar.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jvanier.android.sendtocar.R;

public class MakeActivity extends ActionBarActivity {

    private CardView recentVehiclesCard;
	private TextView recentVehiclesLabel;
	private CardView makesCard;
	private ViewGroup recentVehiclesContainer;
	private ViewGroup recentVehiclesCardContainer;
	private ViewGroup makesCardContainer;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.make_activity);
        
        setupRecentVehicles();
        setupMakes();
    }
    
	private void setupRecentVehicles() {
    	recentVehiclesLabel = (TextView) findViewById(R.id.recentVehiclesLabel);
    	recentVehiclesCard = (CardView) findViewById(R.id.recentVehiclesCard);
    	recentVehiclesCardContainer = (ViewGroup) findViewById(R.id.recentVehiclesCardContainer);
    	
    	String makes[] = { "Ford", "BMW" };
    	String accounts[] = { "2484810771", "test@example.com" };
    	
    	for(int i = 0; i < makes.length; i++) {
    		View recentVehicleItem = getLayoutInflater().inflate(R.layout.recent_vehicle_item, recentVehiclesCardContainer, false);
    		TextView makeView = (TextView) recentVehicleItem.findViewById(android.R.id.text1);
    		TextView accountView = (TextView) recentVehicleItem.findViewById(android.R.id.text2);
    		
    		makeView.setText(makes[i]);
    		accountView.setText(accounts[i]);
    		
    		if(i != 0) {
    			recentVehiclesCardContainer.addView(getLayoutInflater().inflate(R.layout.card_divider, recentVehiclesCardContainer, false));
    		}
    		recentVehiclesCardContainer.addView(recentVehicleItem);
    	}
    }

	private void setupMakes() {
    	makesCard = (CardView) findViewById(R.id.makesCard);
    	makesCardContainer = (ViewGroup) findViewById(R.id.makesCardContainer);
    	
    	String makes[] = { "Ford", "BMW", "Toyota", "Nissan" };
    	for(int i = 0; i < makes.length; i++) {
    		View makeItem = getLayoutInflater().inflate(R.layout.make_item, makesCardContainer, false);
    		TextView makeView = (TextView) makeItem.findViewById(android.R.id.text1);
    		
    		makeView.setText(makes[i]);
    		
    		if(i != 0) {
    			makesCardContainer.addView(getLayoutInflater().inflate(R.layout.card_divider, makesCardContainer, false));
    		}
    		makesCardContainer.addView(makeItem);
    	}
	}

}
