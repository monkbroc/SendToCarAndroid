package com.jvanier.android.sendtocar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class ReplaceChildViewAnimation {
	private static final String TAG = "ReplaceChildViewAnimation";
    private final ViewGroup container;
    private final View previousView;
    private final View newView;
	private int duration;
	private AnimatorSet set;

    public ReplaceChildViewAnimation(ViewGroup container, View previousView, View newView) {
        this.container = container;
        this.previousView = previousView;
        this.newView = newView;
    }
    
    public ReplaceChildViewAnimation setDuration(int duration) {
	    this.duration = duration;
	    return this;
    }
    
    public void start() {
    	if(container == null || previousView == null || newView == null) {
    		Log.d(TAG, "ReplaceChildViewAnimation called with some null views. No animation performed.");
    		return;
    	}
        newView.setAlpha(0.0f);

		ViewTreeObserver vto = container.getViewTreeObserver(); 
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
		    @SuppressWarnings("deprecation")
			@Override 
		    public void onGlobalLayout() { 
		    	container.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		    	animate(previousView.getMeasuredHeight(), newView.getMeasuredHeight());
		    }
		});
    }
    
    private void animate(int previousHeight, int newHeight) {
    	
    	ObjectAnimator animation1 = ObjectAnimator.ofFloat(newView, "alpha", 0.0f, 1.0f);
    	animation1.setDuration(duration);
    	
    	ObjectAnimator animation2 = ObjectAnimator.ofFloat(previousView, "alpha", 1.0f, 0.0f);
    	animation2.setDuration(duration);

        ValueAnimator animation3 = ValueAnimator.ofInt(previousHeight, newHeight);
        animation3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                Log.d("Animation", "Height " + val);
                ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
                layoutParams.height = val;
                container.setLayoutParams(layoutParams);
            }
        });
    	animation3.setDuration(duration);
    	
    	set = new AnimatorSet();
        set.playTogether(animation1, animation2, animation3);

        set.addListener(new AnimatorListenerAdapter() {

          @Override
          public void onAnimationEnd(Animator animation) {
        	  container.removeView(previousView);
        	  container.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
          }
        });
        set.start();
    }
		    			        
        /*
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
    */
}
