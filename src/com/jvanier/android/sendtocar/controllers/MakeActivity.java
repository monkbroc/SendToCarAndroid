package com.jvanier.android.sendtocar.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jvanier.android.sendtocar.R;

public class MakeActivity extends ActionBarActivity {
	public static final int PICK_MAKE = 0;

	public static final String EXTRA_TYPE = "type";
	public static final String TYPE_RECENT_VEHICLE = "recentVehicle";
	public static final String TYPE_MAKE = "make";

	public static final String EXTRA_MAKE_ID = "makeId";
	public static final String EXTRA_RECENT_VEHICLE_POSITION = "recentVehiclePosition";

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

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		setupRecentVehicles();
		setupMakes();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class RecentVehicleClickListener implements OnClickListener {
		private int position;

		public RecentVehicleClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			MakeActivity.this.selectRecentVehicle(position);
		}
	}

	private class RecentVehicleDiscardClickListener implements OnClickListener {
		private int position;

		public RecentVehicleDiscardClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			MakeActivity.this.discardRecentVehicle(position);
		}
	}

	private void setupRecentVehicles() {
		recentVehiclesLabel = (TextView) findViewById(R.id.recentVehiclesLabel);
		recentVehiclesCard = (CardView) findViewById(R.id.recentVehiclesCard);
		recentVehiclesCardContainer = (ViewGroup) findViewById(R.id.recentVehiclesCardContainer);

		String makes[] = { "Ford", "BMW" };
		String accounts[] = { "2484810771", "test@example.com" };

		for (int i = 0; i < makes.length; i++) {
			View recentVehicleItem = getLayoutInflater().inflate(
					R.layout.recent_vehicle_item, recentVehiclesCardContainer,
					false);
			TextView makeView = (TextView) recentVehicleItem
					.findViewById(android.R.id.text1);
			TextView accountView = (TextView) recentVehicleItem
					.findViewById(android.R.id.text2);

			makeView.setText(makes[i]);
			accountView.setText(accounts[i]);

			recentVehicleItem.findViewById(R.id.recentVehicleButton)
					.setOnClickListener(new RecentVehicleClickListener(i));

			recentVehicleItem.findViewById(R.id.discardButton)
					.setOnClickListener(new RecentVehicleDiscardClickListener(i));

			if (i != 0) {
				recentVehiclesCardContainer.addView(getLayoutInflater()
						.inflate(R.layout.card_divider,
								recentVehiclesCardContainer, false));
			}
			recentVehiclesCardContainer.addView(recentVehicleItem);
		}
	}

	private class MakeClickListener implements OnClickListener {
		private String makeId;

		public MakeClickListener(String makeId) {
			this.makeId = makeId;
		}

		@Override
		public void onClick(View v) {
			MakeActivity.this.selectMake(makeId);
		}
	}

	private void setupMakes() {
		makesCard = (CardView) findViewById(R.id.makesCard);
		makesCardContainer = (ViewGroup) findViewById(R.id.makesCardContainer);

		String makes[] = { "Ford", "BMW", "Toyota", "Nissan" };
		for (int i = 0; i < makes.length; i++) {
			View makeItem = getLayoutInflater().inflate(R.layout.make_item,
					makesCardContainer, false);
			TextView makeView = (TextView) makeItem
					.findViewById(android.R.id.text1);
			String makeId = makes[i];
			makeView.setText(makeId);

			makeItem.setOnClickListener(new MakeClickListener(makeId));

			if (i != 0) {
				makesCardContainer.addView(getLayoutInflater().inflate(
						R.layout.card_divider, makesCardContainer, false));
			}
			makesCardContainer.addView(makeItem);
		}
	}

	public void selectRecentVehicle(int position) {
		Intent intent = new Intent();
		intent.putExtra(EXTRA_TYPE, TYPE_RECENT_VEHICLE);
		intent.putExtra(EXTRA_RECENT_VEHICLE_POSITION, position);
		setResult(RESULT_OK, intent);
		finish();
	}

	public void selectMake(String makeId) {
		Intent intent = new Intent();
		intent.putExtra(EXTRA_TYPE, TYPE_MAKE);
		intent.putExtra(EXTRA_MAKE_ID, makeId);
		setResult(RESULT_OK, intent);
		finish();
	}

	public void discardRecentVehicle(int position) {
		// TODO Auto-generated method stub
		
	}

}
