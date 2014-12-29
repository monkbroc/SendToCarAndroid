package com.jvanier.android.sendtocar.controllers;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;

import com.jvanier.android.sendtocar.R;


public class SendToCarActivity extends ActionBarActivity {

    // Fragment managing the navigation drawer.
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sendtocar_activity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        mNavigationDrawerFragment = (NavigationDrawerFragment) fragmentManager.findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        
        // Set up the main fragment
        fragmentManager.beginTransaction()
        .replace(R.id.container, new SendToCarFragment(getIntent()))
        .commit();        
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        	mNavigationDrawerFragment.handleMenuKey();
        	return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
