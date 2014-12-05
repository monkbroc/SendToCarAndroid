package com.jvanier.android.sendtocar;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

		private int mShortAnimationDuration;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

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
            
            final PlaceholderFragment fragment = this;
            changeVehicleView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					fragment.createVehicleListView(inflater, cardView);
				}
			});
		}

		private void createVehicleListView(final LayoutInflater inflater, final CardView cardView) {
			View vehicleListView = inflater.inflate(R.layout.vehicle_list, cardView, true);

			View vehicleEditView = vehicleListView.findViewById(R.id.editButton);

            final PlaceholderFragment fragment = this;
            vehicleEditView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					fragment.createVehicleEditListView(inflater, cardView);
				}
			});
			animateTransitionOfChildViews(cardView);
		}
		
		private void createVehicleEditListView(final LayoutInflater inflater, final CardView cardView) {
			View vehicleListView = inflater.inflate(R.layout.vehicle_edit_list, cardView, true);
			
            Spinner makeSpinner = (Spinner) vehicleListView.findViewById(R.id.makeSpinner);
            setupMakeSpinner(makeSpinner);
            
			animateTransitionOfChildViews(cardView);

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
		
		private void animateTransitionOfChildViews(ViewGroup container) {
            // Animate transition
			final View previousView = container.getChildAt(0);
			final View newView = container.getChildAt(1);
			ReplaceChildViewAnimation anim = new ReplaceChildViewAnimation(container, previousView, newView);
			anim.setDuration(mShortAnimationDuration).start();
		}

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }
}
