package com.jvanier.android.sendtocar.controllers;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.jvanier.android.sendtocar.R;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

public class TutorialActivity extends ActionBarActivity implements OnPageChangeListener {
	TutorialPagesFragmentAdapter mAdapter;
    ViewPager mPager;
    PageIndicator mIndicator;
	private Button mSkip;
	private Button mNext;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_activity);
        
        setupViewPager();
        setupButtons();
    }
    
    private void setupViewPager() {
        mAdapter = new TutorialPagesFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (CirclePageIndicator)findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setOnPageChangeListener(this);
    }
    
    private void setupButtons() {
        mSkip = (Button) findViewById(R.id.skipButton);
        mSkip.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
            	// close tutorial activity
            	finish();
			}
		});

        mNext = (Button) findViewById(R.id.nextButton);
        mNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int current = mPager.getCurrentItem();
				if(current < mAdapter.getCount() - 1) {
					// Go to next with smooth scrolling
					mPager.setCurrentItem(current + 1, true);
				} else {
	            	// close tutorial activity
	            	finish();
				}
			}
		});

    }
    

	@Override
	public void onPageSelected(int position) {
		if(position >= mAdapter.getCount() - 1) {
			mNext.setText(R.string.done);
			mSkip.setVisibility(View.GONE);
		} else {
			mNext.setText(R.string.next);
			mSkip.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		// Do nothing
	}
	
	@Override
    public void onPageScrollStateChanged(int state) {
		// Do nothing
	}
}
