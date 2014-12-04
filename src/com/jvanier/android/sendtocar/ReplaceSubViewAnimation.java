package com.jvanier.android.sendtocar;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.Animation.AnimationListener;

public class ReplaceSubViewAnimation {
    private final ViewGroup container;
    private final View previousView;
    private final View newView;
	private int duration;

    public ReplaceSubViewAnimation(ViewGroup container, View previousView, View newView) {
        this.container = container;
        this.previousView = previousView;
        this.newView = newView;
    }
    
    public ReplaceSubViewAnimation setDuration(int duration) {
	    this.duration = duration;
	    return this;
    }
    
    public void start() {
        newView.setAlpha(0.0f);

		ViewTreeObserver vto = container.getViewTreeObserver(); 
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
		    @SuppressWarnings("deprecation")
			@Override 
		    public void onGlobalLayout() { 
		    	container.getViewTreeObserver().removeGlobalOnLayoutListener(this); 
		    			        
		        newView.animate()
		            .alpha(1f)
		            .setDuration(duration)
		            .setListener(null);
		        
		        previousView.animate()
		            .alpha(0f)
		            .setDuration(duration)
		            .setListener(null);
		        
		    	int previousHeight = previousView.getMeasuredHeight();
		        int newHeight = newView.getMeasuredHeight();
		        
		        ContainerHeightAnimation heightAnim = new ContainerHeightAnimation(container, previousHeight, newHeight);
		        heightAnim.setDuration(duration);
		        heightAnim.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {}
					
					@Override
					public void onAnimationEnd(Animation animation) {
						container.removeView(previousView);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
				container.startAnimation(heightAnim);
		    } 
		});
    }
    
    class ContainerHeightAnimation extends Animation {
    	private final View view;
        private final int startHeight;
        private final int targetHeight;
        
        public ContainerHeightAnimation(View view, int startHeight, int targetHeight) {
        	this.view = view;
        	this.startHeight = startHeight;
        	this.targetHeight = targetHeight;
        }
        
	    @Override
	    protected void applyTransformation(float interpolatedTime, Transformation t) {
	        int newHeight;
	        if(interpolatedTime == 1) {
	        	newHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
	        } else {
	        	newHeight = (int)(startHeight + (targetHeight - startHeight) * interpolatedTime);
	        }
	        view.getLayoutParams().height = newHeight;
	        view.requestLayout();
	    }
	
	    @Override
	    public void initialize(int width, int height, int parentWidth,
	            int parentHeight) {
	        super.initialize(width, height, parentWidth, parentHeight);
	    }
	
	    @Override
	    public boolean willChangeBounds() {
	        return true;
	    }
    }
}
