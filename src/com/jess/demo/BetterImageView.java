/*
 * BetterImageView
 *
 * Copyright 2012 Jess Anders
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
