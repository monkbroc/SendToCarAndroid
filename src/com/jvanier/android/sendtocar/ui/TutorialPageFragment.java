package com.jvanier.android.sendtocar.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jvanier.android.sendtocar.R;

public final class TutorialPageFragment extends Fragment {
    private static final String KEY_DRAWABLE = "TutorialFragment:drawable";
    private static final String KEY_MESSAGE = "TutorialFragment:message";

    public static TutorialPageFragment newInstance(int drawable, int message) {
    	TutorialPageFragment fragment = new TutorialPageFragment();
    	fragment.mDrawable = drawable;
    	fragment.mMessage = message;
        return fragment;
    }

    private int mDrawable = 0;
    private int mMessage = 0;
    
    private View mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mDrawable = savedInstanceState.getInt(KEY_DRAWABLE);
            mMessage = savedInstanceState.getInt(KEY_MESSAGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mView = inflater.inflate(R.layout.tutorial_page_fragment, container, false);
    	
    	TextView messageText = (TextView) mView.findViewById(R.id.messageText);
    	messageText.setText(mMessage);
    	
    	ImageView screenshotImage = (ImageView) mView.findViewById(R.id.screenshotImage);
    	screenshotImage.setImageResource(mDrawable);

        return mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_DRAWABLE, mDrawable);
        outState.putInt(KEY_MESSAGE, mMessage);
    }
}
