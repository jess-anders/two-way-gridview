package com.jess.demo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;


public class BetterImageView extends ImageView {
	private static final String TAG = "BetterImageView";
	private static final boolean DEBUG = false;

	public BetterImageView(Context context) {
		super(context);
	}

	public BetterImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BetterImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void invalidateDrawable(Drawable dr) {
		Drawable currentDrawable = getDrawable();
		if (DEBUG) Log.i(TAG, "invalidateDrawable: " + dr + " current drawable: " + currentDrawable);
		if (dr == currentDrawable) {
			/* we invalidate the whole view in this case because it's very
			 * hard to know where the drawable actually is. This is made
			 * complicated because of the offsets and transformations that
			 * can be applied. In theory we could get the drawable's bounds
			 * and run them through the transformation and offsets, but this
			 * is probably not worth the effort.
			 */
			Log.i(TAG, "invalidateDrawable - setting imageDrawable");
			//destroyDrawingCache();
			drawableStateChanged();
			forceLayout();
			setImageDrawable(currentDrawable);
			invalidate();
		} else {
			super.invalidateDrawable(dr);
		}
	}
}
