package com.jvanier.android.sendtocar.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.jvanier.android.sendtocar.R;

class TutorialFragmentAdapter extends FragmentPagerAdapter {

	protected final int screenDrawables[] = {R.drawable.screenshot1, R.drawable.screenshot2, R.drawable.screenshot3, R.drawable.screenshot4 };
	protected final int screenMessage[] = { R.string.infoStart, R.string.infoShare, R.string.infoSend, R.string.infoEnter };

    private int mCount = screenDrawables.length;

    public TutorialFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return TutorialFragment.newInstance(screenDrawables[position], screenMessage[position]);
    }

    @Override
    public int getCount() {
        return mCount;
    }
}