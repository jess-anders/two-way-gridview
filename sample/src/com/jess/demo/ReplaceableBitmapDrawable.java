/*
 * ReplaceableBitmapDrawable
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


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;

public class ReplaceableBitmapDrawable extends Drawable {
	private static final String TAG = "ReplaceableBitmapDrawable";
	private static final boolean DEBUG = false;

	private Bitmap mBitmap;
	private boolean mLoaded;
	private boolean mApplyGravity;
	private int mGravity;
	private final Rect mDstRect = new Rect();

	public ReplaceableBitmapDrawable(Bitmap b) {
		mBitmap = b;
	}

	@Override
	public void draw(Canvas canvas) {
		copyBounds(mDstRect);
		if (mBitmap != null) {
			if (mApplyGravity) {
				Gravity.apply(mGravity, super.getIntrinsicWidth(), super.getIntrinsicHeight(),
						getBounds(), mDstRect);
				mApplyGravity = false;
			}
			canvas.drawBitmap(mBitmap, null, mDstRect, null);

		}
	}


	public void setGravity(int gravity) {
		mGravity = gravity;
		mApplyGravity = true;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
	}

	@Override
	public int getIntrinsicWidth() {
		if (mBitmap != null) {
			if (DEBUG) Log.i(TAG, "getIntrinsicWidth(): " + mBitmap.getWidth()+ " " + this);
			return mBitmap.getWidth();
		} else {
			return 0;
		}
	}

	@Override
	public int getIntrinsicHeight() {
		if (mBitmap != null) {
			if (DEBUG) Log.i(TAG, "getIntrinsicHeight(): " + mBitmap.getHeight() + " " + this);
			return mBitmap.getHeight();
		} else {
			return 0;
		}
	}

	@Override
	public int getMinimumWidth() {
		return 0;
	}

	@Override
	public int getMinimumHeight() {
		return 0;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		mLoaded = true;
		mBitmap = bitmap;
		if (DEBUG) Log.i("ReplaceableBitmapDrawable", "setBitmap() " + this);
		invalidateSelf();
	}

	public boolean isLoaded() {
		return mLoaded;
	}
}
