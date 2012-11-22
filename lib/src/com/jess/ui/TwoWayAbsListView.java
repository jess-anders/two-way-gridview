/*
 * A modified version of the Android AbsListView
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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jess.ui;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Debug;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Adapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Scroller;

/**
 * Base class that can be used to implement virtualized lists of items. A list does
 * not have a spatial definition here. For instance, subclases of this class can
 * display the content of the list in a grid, in a carousel, as stack, etc.
 *
 * @attr ref android.R.styleable#JessAbsListView_listSelector
 * @attr ref android.R.styleable#JessAbsListView_drawSelectorOnTop
 * @attr ref android.R.styleable#JessAbsListView_stackFromBottom
 * @attr ref android.R.styleable#JessAbsListView_scrollingCache
 * @attr ref android.R.styleable#JessAbsListView_textFilterEnabled
 * @attr ref android.R.styleable#JessAbsListView_transcriptMode
 * @attr ref android.R.styleable#JessAbsListView_cacheColorHint
 * @attr ref android.R.styleable#JessAbsListView_smoothScrollbar
 */
public abstract class TwoWayAbsListView extends TwoWayAdapterView<ListAdapter> implements
ViewTreeObserver.OnTouchModeChangeListener {
	private static final String TAG = "TwoWayAbsListView";
	private static final boolean DEBUG = false;


	/**
	 * Disables the transcript mode.
	 *
	 * @see #setTranscriptMode(int)
	 */
	public static final int TRANSCRIPT_MODE_DISABLED = 0;
	/**
	 * The list will automatically scroll to the bottom when a data set change
	 * notification is received and only if the last item is already visible
	 * on screen.
	 *
	 * @see #setTranscriptMode(int)
	 */
	public static final int TRANSCRIPT_MODE_NORMAL = 1;
	/**
	 * The list will automatically scroll to the bottom, no matter what items
	 * are currently visible.
	 *
	 * @see #setTranscriptMode(int)
	 */
	public static final int TRANSCRIPT_MODE_ALWAYS_SCROLL = 2;

	/**
	 * Indicates that we are not in the middle of a touch gesture
	 */
	static final int TOUCH_MODE_REST = -1;

	/**
	 * Indicates we just received the touch event and we are waiting to see if the it is a tap or a
	 * scroll gesture.
	 */
	static final int TOUCH_MODE_DOWN = 0;

	/**
	 * Indicates the touch has been recognized as a tap and we are now waiting to see if the touch
	 * is a longpress
	 */
	static final int TOUCH_MODE_TAP = 1;

	/**
	 * Indicates we have waited for everything we can wait for, but the user's finger is still down
	 */
	static final int TOUCH_MODE_DONE_WAITING = 2;

	/**
	 * Indicates the touch gesture is a scroll
	 */
	static final int TOUCH_MODE_SCROLL = 3;

	/**
	 * Indicates the view is in the process of being flung
	 */
	static final int TOUCH_MODE_FLING = 4;

	/**
	 * Regular layout - usually an unsolicited layout from the view system
	 */
	static final int LAYOUT_NORMAL = 0;

	/**
	 * Show the first item
	 */
	static final int LAYOUT_FORCE_TOP = 1;

	/**
	 * Force the selected item to be on somewhere on the screen
	 */
	static final int LAYOUT_SET_SELECTION = 2;

	/**
	 * Show the last item
	 */
	static final int LAYOUT_FORCE_BOTTOM = 3;

	/**
	 * Make a mSelectedItem appear in a specific location and build the rest of
	 * the views from there. The top is specified by mSpecificTop.
	 */
	static final int LAYOUT_SPECIFIC = 4;

	/**
	 * Layout to sync as a result of a data change. Restore mSyncPosition to have its top
	 * at mSpecificTop
	 */
	static final int LAYOUT_SYNC = 5;

	/**
	 * Layout as a result of using the navigation keys
	 */
	static final int LAYOUT_MOVE_SELECTION = 6;

	/**
	 * Sets the View to Scroll Vertically.
	 *
	 * @see #setScrollDirectionPortrait(int)
	 * @see #setScrollDirectionLandscape(int)
	 */
	static final int SCROLL_VERTICAL = 0;

	/**
	 * Sets the View to Scroll Horizontally.
	 *
	 * @see #setScrollDirectionPortrait(int)
	 * @see #setScrollDirectionLandscape(int)
	 */
	static final int SCROLL_HORIZONTAL = 1;


	/**
	 * Controls how the next layout will happen
	 */
	int mLayoutMode = LAYOUT_NORMAL;

	/**
	 * Should be used by subclasses to listen to changes in the dataset
	 */
	AdapterDataSetObserver mDataSetObserver;

	/**
	 * The adapter containing the data to be displayed by this view
	 */
	ListAdapter mAdapter;

	/**
	 * Indicates whether the list selector should be drawn on top of the children or behind
	 */
	boolean mDrawSelectorOnTop = false;

	/**
	 * The drawable used to draw the selector
	 */
	Drawable mSelector;

	/**
	 * Defines the selector's location and dimension at drawing time
	 */
	Rect mSelectorRect = new Rect();

	/**
	 * The data set used to store unused views that should be reused during the next layout
	 * to avoid creating new ones
	 */
	final RecycleBin mRecycler = new RecycleBin();

	/**
	 * The selection's left padding
	 */
	int mSelectionLeftPadding = 0;

	/**
	 * The selection's top padding
	 */
	int mSelectionTopPadding = 0;

	/**
	 * The selection's right padding
	 */
	int mSelectionRightPadding = 0;

	/**
	 * The selection's bottom padding
	 */
	int mSelectionBottomPadding = 0;

	/**
	 * This view's padding
	 */
	Rect mListPadding = new Rect();

	/**
	 * Subclasses must retain their measure spec from onMeasure() into this member
	 */
	int mWidthMeasureSpec = 0;

	/**
	 * The top scroll indicator
	 */
	View mScrollUp;

	/**
	 * The down scroll indicator
	 */
	View mScrollDown;

	/**
	 * The left scroll indicator
	 */
	View mScrollLeft;

	/**
	 * The right scroll indicator
	 */
	View mScrollRight;

	/**
	 * When the view is scrolling, this flag is set to true to indicate subclasses that
	 * the drawing cache was enabled on the children
	 */
	boolean mCachingStarted;

	/**
	 * The position of the view that received the down motion event
	 */
	int mMotionPosition;



	/**
	 * The X value associated with the the down motion event
	 */
	int mMotionX;

	/**
	 * The Y value associated with the the down motion event
	 */
	int mMotionY;

	/**
	 * One of TOUCH_MODE_REST, TOUCH_MODE_DOWN, TOUCH_MODE_TAP, TOUCH_MODE_SCROLL, or
	 * TOUCH_MODE_DONE_WAITING
	 */
	int mTouchMode = TOUCH_MODE_REST;

	/**
	 * Determines speed during touch scrolling
	 */
	private VelocityTracker mVelocityTracker;

	/**
	 * The offset in pixels form the top of the AdapterView to the top
	 * of the currently selected view. Used to save and restore state.
	 */
	int mSelectedTop = 0;

	/**
	 * Indicates whether the list is stacked from the bottom edge or
	 * the top edge.
	 */
	boolean mStackFromBottom;

	/**
	 * When set to true, the list automatically discards the children's
	 * bitmap cache after scrolling.
	 */
	boolean mScrollingCacheEnabled;

	/**
	 * Whether or not to enable the fast scroll feature on this list
	 */
	//boolean mFastScrollEnabled;

	/**
	 * Optional callback to notify client when scroll position has changed
	 */
	private OnScrollListener mOnScrollListener;

	/**
	 * Keeps track of our accessory window
	 */
	//PopupWindow mPopup;

	/**
	 * Used with type filter window
	 */
	EditText mTextFilter;

	/**
	 * Indicates whether to use pixels-based or position-based scrollbar
	 * properties.
	 */
	private boolean mSmoothScrollbarEnabled = true;

	/**
	 * Indicates that this view supports filtering
	 */
	//private boolean mTextFilterEnabled;

	/**
	 * Indicates that this view is currently displaying a filtered view of the data
	 */
	//private boolean mFiltered;

	/**
	 * Rectangle used for hit testing children
	 */
	private Rect mTouchFrame;

	/**
	 * The position to resurrect the selected position to.
	 */
	int mResurrectToPosition = INVALID_POSITION;

	private ContextMenuInfo mContextMenuInfo = null;

	/**
	 * Used to request a layout when we changed touch mode
	 */
	private static final int TOUCH_MODE_UNKNOWN = -1;
	private static final int TOUCH_MODE_ON = 0;
	private static final int TOUCH_MODE_OFF = 1;

	private int mLastTouchMode = TOUCH_MODE_UNKNOWN;

	private static final boolean PROFILE_SCROLLING = false;
	private boolean mScrollProfilingStarted = false;

	private static final boolean PROFILE_FLINGING = false;
	private boolean mFlingProfilingStarted = false;

	/**
	 * The last CheckForLongPress runnable we posted, if any
	 */
	private CheckForLongPress mPendingCheckForLongPress;

	/**
	 * The last CheckForTap runnable we posted, if any
	 */
	private Runnable mPendingCheckForTap;

	/**
	 * The last CheckForKeyLongPress runnable we posted, if any
	 */
	private CheckForKeyLongPress mPendingCheckForKeyLongPress;

	/**
	 * Acts upon click
	 */
	private TwoWayAbsListView.PerformClick mPerformClick;

	/**
	 * This view is in transcript mode -- it shows the bottom of the list when the data
	 * changes
	 */
	private int mTranscriptMode;

	/**
	 * Indicates that this list is always drawn on top of a solid, single-color, opaque
	 * background
	 */
	private int mCacheColorHint;

	/**
	 * The select child's view (from the adapter's getView) is enabled.
	 */
	private boolean mIsChildViewEnabled;

	/**
	 * The last scroll state reported to clients through {@link OnScrollListener}.
	 */
	private int mLastScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	/**
	 * Helper object that renders and controls the fast scroll thumb.
	 */
	//private FastScroller mFastScroller;

	//private boolean mGlobalLayoutListenerAddedFilter;

	private int mTouchSlop;
	private float mDensityScale;

	//private InputConnection mDefInputConnection;
	//private InputConnectionWrapper mPublicInputConnection;

	private Runnable mClearScrollingCache;
	private int mMinimumVelocity;
	private boolean mScrollVerticallyPortrait;
	private boolean mScrollVerticallyLandscape;

	protected boolean mScrollVertically;

	protected boolean mPortraitOrientation;

	protected TouchHandler mTouchHandler;

	final boolean[] mIsScrap = new boolean[1];

	// True when the popup should be hidden because of a call to
	// dispatchDisplayHint()
	//private boolean mPopupHidden;

	/**
	 * ID of the active pointer. This is used to retain consistency during
	 * drags/flings if multiple pointers are used.
	 */
	private int mActivePointerId = INVALID_POINTER;

	/**
	 * Sentinel value for no current active pointer.
	 * Used by {@link #mActivePointerId}.
	 */
	private static final int INVALID_POINTER = -1;

	/**
	 * Interface definition for a callback to be invoked when the list or grid
	 * has been scrolled.
	 */
	public interface OnScrollListener {

		/**
		 * The view is not scrolling. Note navigating the list using the trackball counts as
		 * being in the idle state since these transitions are not animated.
		 */
		public static int SCROLL_STATE_IDLE = 0;

		/**
		 * The user is scrolling using touch, and their finger is still on the screen
		 */
		public static int SCROLL_STATE_TOUCH_SCROLL = 1;

		/**
		 * The user had previously been scrolling using touch and had performed a fling. The
		 * animation is now coasting to a stop
		 */
		public static int SCROLL_STATE_FLING = 2;

		/**
		 * Callback method to be invoked while the list view or grid view is being scrolled. If the
		 * view is being scrolled, this method will be called before the next frame of the scroll is
		 * rendered. In particular, it will be called before any calls to
		 * {@link Adapter#getView(int, View, ViewGroup)}.
		 *
		 * @param view The view whose scroll state is being reported
		 *
		 * @param scrollState The current scroll state. One of {@link #SCROLL_STATE_IDLE},
		 * {@link #SCROLL_STATE_TOUCH_SCROLL} or {@link #SCROLL_STATE_IDLE}.
		 */
		public void onScrollStateChanged(TwoWayAbsListView view, int scrollState);

		/**
		 * Callback method to be invoked when the list or grid has been scrolled. This will be
		 * called after the scroll has completed
		 * @param view The view whose scroll state is being reported
		 * @param firstVisibleItem the index of the first visible cell (ignore if
		 *        visibleItemCount == 0)
		 * @param visibleItemCount the number of visible cells
		 * @param totalItemCount the number of items in the list adaptor
		 */
		public void onScroll(TwoWayAbsListView view, int firstVisibleItem, int visibleItemCount,
				int totalItemCount);
	}

	public TwoWayAbsListView(Context context) {
		super(context);
		initAbsListView();
		setupScrollInfo();
		//TypedArray a = context.obtainStyledAttributes(android.R.styleable.View);
		//initializeScrollbars(a);
		//a.recycle();
	}

	public TwoWayAbsListView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.absListViewStyle);
	}

	public TwoWayAbsListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAbsListView();

		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.TwoWayAbsListView, defStyle, 0);

		Drawable d = a.getDrawable(R.styleable.TwoWayAbsListView_listSelector);
		if (d != null) {
			setSelector(d);
		}

		mDrawSelectorOnTop = a.getBoolean(
				R.styleable.TwoWayAbsListView_drawSelectorOnTop, false);

		boolean stackFromBottom = a.getBoolean(R.styleable.TwoWayAbsListView_stackFromBottom, false);
		setStackFromBottom(stackFromBottom);

		boolean scrollingCacheEnabled = a.getBoolean(R.styleable.TwoWayAbsListView_scrollingCache, true);
		setScrollingCacheEnabled(scrollingCacheEnabled);

		//boolean useTextFilter = a.getBoolean(R.styleable.JessAbsListView_textFilterEnabled, false);
		//setTextFilterEnabled(useTextFilter);

		int transcriptMode = a.getInt(R.styleable.TwoWayAbsListView_transcriptMode,
				TRANSCRIPT_MODE_DISABLED);
		setTranscriptMode(transcriptMode);

		int color = a.getColor(R.styleable.TwoWayAbsListView_cacheColorHint, 0);
		setCacheColorHint(color);

		//boolean enableFastScroll = a.getBoolean(R.styleable.JessAbsListView_fastScrollEnabled, false);
		//setFastScrollEnabled(enableFastScroll);

		boolean smoothScrollbar = a.getBoolean(R.styleable.TwoWayAbsListView_smoothScrollbar, true);
		setSmoothScrollbarEnabled(smoothScrollbar);

		int scrollDirection = a.getInt(R.styleable.TwoWayAbsListView_scrollDirectionPortrait, SCROLL_VERTICAL);
		mScrollVerticallyPortrait = (scrollDirection == SCROLL_VERTICAL);

		scrollDirection = a.getInt(R.styleable.TwoWayAbsListView_scrollDirectionLandscape, SCROLL_VERTICAL);
		mScrollVerticallyLandscape = (scrollDirection == SCROLL_VERTICAL);

		a.recycle();
		setupScrollInfo();
	}

	private void initAbsListView() {
		// Setting focusable in touch mode will set the focusable property to true
		setClickable(true);
		setFocusableInTouchMode(true);
		setWillNotDraw(false);
		setAlwaysDrawnWithCacheEnabled(false);
		setScrollingCacheEnabled(true);

		final ViewConfiguration configuration = ViewConfiguration.get(mContext);
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mDensityScale = getContext().getResources().getDisplayMetrics().density;
		mPortraitOrientation = (getResources().getConfiguration().orientation !=
			Configuration.ORIENTATION_LANDSCAPE);
		mScrollVertically = true;


	}



	private void setupScrollInfo() {
		mScrollVertically = mPortraitOrientation ? mScrollVerticallyPortrait: mScrollVerticallyLandscape;
		if (mScrollVertically) {
			mTouchHandler = new VerticalTouchHandler();
			setVerticalScrollBarEnabled(true);
			setHorizontalScrollBarEnabled(false);
		} else {
			mTouchHandler = new HorizontalTouchHandler();
			setVerticalScrollBarEnabled(false);
			setHorizontalScrollBarEnabled(true);
		}

	}

	private boolean orientationChanged() {
		boolean temp = mPortraitOrientation;
		mPortraitOrientation = (getResources().getConfiguration().orientation !=
			Configuration.ORIENTATION_LANDSCAPE);

		boolean result = (temp != mPortraitOrientation);
		if (result) {
			setupScrollInfo();
			mRecycler.scrapActiveViews();
		}

		return result;
	}

	/**
	 * Enables fast scrolling by letting the user quickly scroll through lists by
	 * dragging the fast scroll thumb. The adapter attached to the list may want
	 * to implement {@link SectionIndexer} if it wishes to display alphabet preview and
	 * jump between sections of the list.
	 * @see SectionIndexer
	 * @see #isFastScrollEnabled()
	 * @param enabled whether or not to enable fast scrolling
	 */
	/*
    public void setFastScrollEnabled(boolean enabled) {
        mFastScrollEnabled = enabled;
        if (enabled) {
            if (mFastScroller == null) {
                mFastScroller = new FastScroller(getContext(), this);
            }
        } else {
            if (mFastScroller != null) {
                mFastScroller.stop();
                mFastScroller = null;
            }
        }
    }*/

	/**
	 * Returns the current state of the fast scroll feature.
	 * @see #setFastScrollEnabled(boolean)
	 * @return true if fast scroll is enabled, false otherwise
	 */
	/*
    @ViewDebug.ExportedProperty
    public boolean isFastScrollEnabled() {
        return mFastScrollEnabled;
    }

    protected boolean isVerticalScrollBarHidden() {
        return mFastScroller != null && mFastScroller.isVisible();
    }*/

	/**
	 * When smooth scrollbar is enabled, the position and size of the scrollbar thumb
	 * is computed based on the number of visible pixels in the visible items. This
	 * however assumes that all list items have the same height. If you use a list in
	 * which items have different heights, the scrollbar will change appearance as the
	 * user scrolls through the list. To avoid this issue, you need to disable this
	 * property.
	 *
	 * When smooth scrollbar is disabled, the position and size of the scrollbar thumb
	 * is based solely on the number of items in the adapter and the position of the
	 * visible items inside the adapter. This provides a stable scrollbar as the user
	 * navigates through a list of items with varying heights.
	 *
	 * @param enabled Whether or not to enable smooth scrollbar.
	 *
	 * @see #setSmoothScrollbarEnabled(boolean)
	 * @attr ref android.R.styleable#JessAbsListView_smoothScrollbar
	 */
	public void setSmoothScrollbarEnabled(boolean enabled) {
		mSmoothScrollbarEnabled = enabled;
	}

	/**
	 * Returns the current state of the fast scroll feature.
	 *
	 * @return True if smooth scrollbar is enabled is enabled, false otherwise.
	 *
	 * @see #setSmoothScrollbarEnabled(boolean)
	 */
	@ViewDebug.ExportedProperty
	public boolean isSmoothScrollbarEnabled() {
		return mSmoothScrollbarEnabled;
	}

	/**
	 * Set the listener that will receive notifications every time the list scrolls.
	 *
	 * @param l the scroll listener
	 */
	public void setOnScrollListener(OnScrollListener l) {
		mOnScrollListener = l;
		invokeOnItemScrollListener();
	}

	/**
	 * Notify our scroll listener (if there is one) of a change in scroll state
	 */
	void invokeOnItemScrollListener() {
		//if (mFastScroller != null) {
		//    mFastScroller.onScroll(this, mFirstPosition, getChildCount(), mItemCount);
		//}
		if (mOnScrollListener != null) {
			mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(), mItemCount);
		}
	}

	/**
	 * Indicates whether the children's drawing cache is used during a scroll.
	 * By default, the drawing cache is enabled but this will consume more memory.
	 *
	 * @return true if the scrolling cache is enabled, false otherwise
	 *
	 * @see #setScrollingCacheEnabled(boolean)
	 * @see View#setDrawingCacheEnabled(boolean)
	 */
	@ViewDebug.ExportedProperty
	public boolean isScrollingCacheEnabled() {
		return mScrollingCacheEnabled;
	}

	/**
	 * Enables or disables the children's drawing cache during a scroll.
	 * By default, the drawing cache is enabled but this will use more memory.
	 *
	 * When the scrolling cache is enabled, the caches are kept after the
	 * first scrolling. You can manually clear the cache by calling
	 * {@link android.view.ViewGroup#setChildrenDrawingCacheEnabled(boolean)}.
	 *
	 * @param enabled true to enable the scroll cache, false otherwise
	 *
	 * @see #isScrollingCacheEnabled()
	 * @see View#setDrawingCacheEnabled(boolean)
	 */
	public void setScrollingCacheEnabled(boolean enabled) {
		if (mScrollingCacheEnabled && !enabled) {
			mTouchHandler.clearScrollingCache();
		}
		mScrollingCacheEnabled = enabled;
	}

//	/**
//	 * Enables or disables the type filter window. If enabled, typing when
//	 * this view has focus will filter the children to match the users input.
//	 * Note that the {@link Adapter} used by this view must implement the
//	 * {@link Filterable} interface.
//	 *
//	 * @param textFilterEnabled true to enable type filtering, false otherwise
//	 *
//	 * @see Filterable
//	 */
//	//public void setTextFilterEnabled(boolean textFilterEnabled) {
//	//    mTextFilterEnabled = textFilterEnabled;
//	//}
//
//	/**
//	 * Indicates whether type filtering is enabled for this view
//	 *
//	 * @return true if type filtering is enabled, false otherwise
//	 *
//	 * @see #setTextFilterEnabled(boolean)
//	 * @see Filterable
//	 */
//	//@ViewDebug.ExportedProperty
//	//public boolean isTextFilterEnabled() {
//	//    return mTextFilterEnabled;
//	//}

	@Override
	public void getFocusedRect(Rect r) {
		View view = getSelectedView();
		if (view != null && view.getParent() == this) {
			// the focused rectangle of the selected view offset into the
			// coordinate space of this view.
			view.getFocusedRect(r);
			offsetDescendantRectToMyCoords(view, r);
		} else {
			// otherwise, just the norm
			super.getFocusedRect(r);
		}
	}

	private void useDefaultSelector() {
		setSelector(getResources().getDrawable(
				android.R.drawable.list_selector_background));
	}

	/**
	 * Indicates whether the content of this view is pinned to, or stacked from,
	 * the bottom edge.
	 *
	 * @return true if the content is stacked from the bottom edge, false otherwise
	 */
	@ViewDebug.ExportedProperty
	public boolean isStackFromBottom() {
		return mStackFromBottom;
	}

	/**
	 * When stack from bottom is set to true, the list fills its content starting from
	 * the bottom of the view.
	 *
	 * @param stackFromBottom true to pin the view's content to the bottom edge,
	 *        false to pin the view's content to the top edge
	 */
	public void setStackFromBottom(boolean stackFromBottom) {
		if (mStackFromBottom != stackFromBottom) {
			mStackFromBottom = stackFromBottom;
			requestLayoutIfNecessary();
		}
	}

	void requestLayoutIfNecessary() {
		if (getChildCount() > 0) {
			resetList();
			requestLayout();
			invalidate();
		}
	}

	static class SavedState extends BaseSavedState {
		long selectedId;
		long firstId;
		int viewTop;
		int position;
		int height;
		//String filter;

		/**
		 * Constructor called from {@link TwoWayAbsListView#onSaveInstanceState()}
		 */
		SavedState(Parcelable superState) {
			super(superState);
		}

		/**
		 * Constructor called from {@link #CREATOR}
		 */
		private SavedState(Parcel in) {
			super(in);
			selectedId = in.readLong();
			firstId = in.readLong();
			viewTop = in.readInt();
			position = in.readInt();
			height = in.readInt();
			//filter = in.readString();
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeLong(selectedId);
			out.writeLong(firstId);
			out.writeInt(viewTop);
			out.writeInt(position);
			out.writeInt(height);
			//out.writeString(filter);
		}

		@Override
		public String toString() {
			return "TwoWayAbsListView.SavedState{"
			+ Integer.toHexString(System.identityHashCode(this))
			+ " selectedId=" + selectedId
			+ " firstId=" + firstId
			+ " viewTop=" + viewTop
			+ " position=" + position
			+ " height=" + height + "}";
			//+ " filter=" + filter + "}";
		}

		public static final Parcelable.Creator<SavedState> CREATOR
		= new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	@Override
	public Parcelable onSaveInstanceState() {
		/*
		 * This doesn't really make sense as the place to dismiss the
		 * popups, but there don't seem to be any other useful hooks
		 * that happen early enough to keep from getting complaints
		 * about having leaked the window.
		 */
		//dismissPopup();

		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);

		boolean haveChildren = getChildCount() > 0;
		long selectedId = getSelectedItemId();
		ss.selectedId = selectedId;
		ss.height = getHeight();

		if (selectedId >= 0) {
			// Remember the selection
			ss.viewTop = mSelectedTop;
			ss.position = getSelectedItemPosition();
			ss.firstId = INVALID_POSITION;
		} else {
			if (haveChildren) {
				// Remember the position of the first child
				View v = getChildAt(0);
				if(mScrollVertically) {
					ss.viewTop = v.getTop();
				} else {
					ss.viewTop = v.getLeft();
				}
				ss.position = mFirstPosition;
				ss.firstId = mAdapter.getItemId(mFirstPosition);
			} else {
				ss.viewTop = 0;
				ss.firstId = INVALID_POSITION;
				ss.position = 0;
			}
		}
		/*
        ss.filter = null;
        if (mFiltered) {
            final EditText textFilter = mTextFilter;
            if (textFilter != null) {
                Editable filterText = textFilter.getText();
                if (filterText != null) {
                    ss.filter = filterText.toString();
                }
            }
        }*/

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;

		super.onRestoreInstanceState(ss.getSuperState());
		mDataChanged = true;

		mSyncSize = ss.height;

		if (ss.selectedId >= 0) {
			mNeedSync = true;
			mSyncRowId = ss.selectedId;
			mSyncPosition = ss.position;
			mSpecificTop = ss.viewTop;
			mSyncMode = SYNC_SELECTED_POSITION;
		} else if (ss.firstId >= 0) {
			setSelectedPositionInt(INVALID_POSITION);
			// Do this before setting mNeedSync since setNextSelectedPosition looks at mNeedSync
			setNextSelectedPositionInt(INVALID_POSITION);
			mNeedSync = true;
			mSyncRowId = ss.firstId;
			mSyncPosition = ss.position;
			mSpecificTop = ss.viewTop;
			mSyncMode = SYNC_FIRST_POSITION;
		}

		//setFilterText(ss.filter);

		requestLayout();
	}

	//    private boolean acceptFilter() {
	//        return mTextFilterEnabled && getAdapter() instanceof Filterable &&
	//                ((Filterable) getAdapter()).getFilter() != null;
	//    }
	//
	//    /**
	//     * Sets the initial value for the text filter.
	//     * @param filterText The text to use for the filter.
	//     *
	//     * @see #setTextFilterEnabled
	//     */
	//    public void setFilterText(String filterText) {
	//        // TODO: Should we check for acceptFilter()?
	//        if (mTextFilterEnabled && !TextUtils.isEmpty(filterText)) {
	//            createTextFilter(false);
	//            // This is going to call our listener onTextChanged, but we might not
	//            // be ready to bring up a window yet
	//            mTextFilter.setText(filterText);
	//            mTextFilter.setSelection(filterText.length());
	//            if (mAdapter instanceof Filterable) {
	//                // if mPopup is non-null, then onTextChanged will do the filtering
	//                if (mPopup == null) {
	//                    Filter f = ((Filterable) mAdapter).getFilter();
	//                    f.filter(filterText);
	//                }
	//                // Set filtered to true so we will display the filter window when our main
	//                // window is ready
	//                mFiltered = true;
	//                mDataSetObserver.clearSavedState();
	//            }
	//        }
	//    }
	//
	//    /**
	//     * Returns the list's text filter, if available.
	//     * @return the list's text filter or null if filtering isn't enabled
	//     */
	//    public CharSequence getTextFilter() {
	//        if (mTextFilterEnabled && mTextFilter != null) {
	//            return mTextFilter.getText();
	//        }
	//        return null;
	//    }

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		if (gainFocus && mSelectedPosition < 0 && !isInTouchMode()) {
			resurrectSelection();
		}
	}

	@Override
	public void requestLayout() {
		if (!mBlockLayoutRequests && !mInLayout) {
			super.requestLayout();
		}
	}

	/**
	 * The list is empty. Clear everything out.
	 */
	void resetList() {
		removeAllViewsInLayout();
		mFirstPosition = 0;
		mDataChanged = false;
		mNeedSync = false;
		mOldSelectedPosition = INVALID_POSITION;
		mOldSelectedRowId = INVALID_ROW_ID;
		setSelectedPositionInt(INVALID_POSITION);
		setNextSelectedPositionInt(INVALID_POSITION);
		mSelectedTop = 0;
		mSelectorRect.setEmpty();
		invalidate();
	}

	@Override
	protected int computeVerticalScrollExtent() {
		final int count = getChildCount();
		if (count > 0 && mScrollVertically) {
			if (mSmoothScrollbarEnabled) {
				int extent = count * 100;

				View view = getChildAt(0);
				final int top = view.getTop();
				int height = view.getHeight();
				if (height > 0) {
					extent += (top * 100) / height;
				}

				view = getChildAt(count - 1);
				final int bottom = view.getBottom();
				height = view.getHeight();
				if (height > 0) {
					extent -= ((bottom - getHeight()) * 100) / height;
				}

				return extent;
			} else {
				return 1;
			}
		}
		return 0;
	}

	@Override
	protected int computeVerticalScrollOffset() {
		final int firstPosition = mFirstPosition;
		final int childCount = getChildCount();
		if (firstPosition >= 0 && childCount > 0 && mScrollVertically) {
			if (mSmoothScrollbarEnabled) {
				final View view = getChildAt(0);
				final int top = view.getTop();
				int height = view.getHeight();
				if (height > 0) {
					return Math.max(firstPosition * 100 - (top * 100) / height +
							(int)((float)getScrollY() / getHeight() * mItemCount * 100), 0);
				}
			} else {
				int index;
				final int count = mItemCount;
				if (firstPosition == 0) {
					index = 0;
				} else if (firstPosition + childCount == count) {
					index = count;
				} else {
					index = firstPosition + childCount / 2;
				}
				return (int) (firstPosition + childCount * (index / (float) count));
			}
		}
		return 0;
	}

	@Override
	protected int computeVerticalScrollRange() {
		int result;
		if (!mScrollVertically) {
			result = 0;
		} else if (mSmoothScrollbarEnabled) {
			result = Math.max(mItemCount * 100, 0);
		} else {
			result = mItemCount;
		}
		return result;
	}

	@Override
	protected int computeHorizontalScrollExtent() {
		final int count = getChildCount();
		if (count > 0 && !mScrollVertically) {
			if (mSmoothScrollbarEnabled) {
				int extent = count * 100;

				View view = getChildAt(0);
				final int left = view.getLeft();
				int width = view.getWidth();
				if (width > 0) {
					extent += (left * 100) / width;
				}

				view = getChildAt(count - 1);
				final int right = view.getRight();
				width = view.getWidth();
				if (width > 0) {
					extent -= ((right - getWidth()) * 100) / width;
				}

				return extent;
			} else {
				return 1;
			}
		}
		return 0;
	}

	@Override
	protected int computeHorizontalScrollOffset() {
		final int firstPosition = mFirstPosition;
		final int childCount = getChildCount();
		if (firstPosition >= 0 && childCount > 0 && !mScrollVertically) {
			if (mSmoothScrollbarEnabled) {
				final View view = getChildAt(0);
				final int left = view.getLeft();
				int width = view.getWidth();
				if (width > 0) {
					return Math.max(firstPosition * 100 - (left * 100) / width +
							(int)((float)getScrollX() / getWidth() * mItemCount * 100), 0);
				}
			} else {
				int index;
				final int count = mItemCount;
				if (firstPosition == 0) {
					index = 0;
				} else if (firstPosition + childCount == count) {
					index = count;
				} else {
					index = firstPosition + childCount / 2;
				}
				return (int) (firstPosition + childCount * (index / (float) count));
			}
		}
		return 0;
	}

	@Override
	protected int computeHorizontalScrollRange() {
		int result;
		if (mScrollVertically) {
			result = 0;
		} else if (mSmoothScrollbarEnabled) {
			result = Math.max(mItemCount * 100, 0);
		} else {
			result = mItemCount;
		}
		return result;
	}

	@Override
	protected float getTopFadingEdgeStrength() {
		final int count = getChildCount();
		final float fadeEdge = super.getTopFadingEdgeStrength();
		if (count == 0 || !mScrollVertically) {
			return fadeEdge;
		} else {
			if (mFirstPosition > 0) {
				return 1.0f;
			}

			final int top = getChildAt(0).getTop();
			final float fadeLength = getVerticalFadingEdgeLength();
			int paddintTop = getPaddingTop();
			return top < paddintTop ? -(top - paddintTop) / fadeLength : fadeEdge;
		}
	}

	@Override
	protected float getBottomFadingEdgeStrength() {
		final int count = getChildCount();
		final float fadeEdge = super.getBottomFadingEdgeStrength();
		if (count == 0 || !mScrollVertically) {
			return fadeEdge;
		} else {
			if (mFirstPosition + count - 1 < mItemCount - 1) {
				return 1.0f;
			}

			final int bottom = getChildAt(count - 1).getBottom();
			final int height = getHeight();
			final float fadeLength = getVerticalFadingEdgeLength();
			int paddingBottom = getPaddingBottom();
			return bottom > height - paddingBottom ?
					(bottom - height + paddingBottom) / fadeLength : fadeEdge;
		}
	}

	@Override
	protected float getLeftFadingEdgeStrength() {
		final int count = getChildCount();
		final float fadeEdge = super.getLeftFadingEdgeStrength();
		if (count == 0 || mScrollVertically) {
			return fadeEdge;
		} else {
			if (mFirstPosition > 0) {
				return 1.0f;
			}

			final int left = getChildAt(0).getLeft();
			final float fadeLength = getHorizontalFadingEdgeLength();
			int paddingLeft = getPaddingLeft();
			return left < paddingLeft ? -(left - paddingLeft) / fadeLength : fadeEdge;
		}
	}

	@Override
	protected float getRightFadingEdgeStrength() {
		final int count = getChildCount();
		final float fadeEdge = super.getRightFadingEdgeStrength();
		if (count == 0 || mScrollVertically) {
			return fadeEdge;
		} else {
			if (mFirstPosition + count - 1 < mItemCount - 1) {
				return 1.0f;
			}

			final int right = getChildAt(count - 1).getRight();
			final int width = getWidth();
			final float fadeLength = getHorizontalFadingEdgeLength();
			int paddingRight = getPaddingRight();
			return right > width - paddingRight ?
					(right - width + paddingRight) / fadeLength : fadeEdge;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		orientationChanged();

		if (mSelector == null) {
			useDefaultSelector();
		}
		final Rect listPadding = mListPadding;
		listPadding.left = mSelectionLeftPadding + getPaddingLeft();
		listPadding.top = mSelectionTopPadding + getPaddingTop();
		listPadding.right = mSelectionRightPadding + getPaddingRight();
		listPadding.bottom = mSelectionBottomPadding + getPaddingBottom();
	}

	/**
	 * Subclasses should NOT override this method but
	 *  {@link #layoutChildren()} instead.
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (orientationChanged()) {
			setupScrollInfo();
		}
		super.onLayout(changed, l, t, r, b);
		mInLayout = true;
		if (changed) {
			int childCount = getChildCount();
			for (int i = 0; i < childCount; i++) {
				getChildAt(i).forceLayout();
			}
			mRecycler.markChildrenDirty();
		}

		layoutChildren();
		mInLayout = false;
	}

	/*
    protected boolean setFrame(int left, int top, int right, int bottom) {
        final boolean changed = super.setFrame(left, top, right, bottom);

        if (changed) {
            // Reposition the popup when the frame has changed. This includes
            // translating the widget, not just changing its dimension. The
            // filter popup needs to follow the widget.
            final boolean visible = getWindowVisibility() == View.VISIBLE;
            if (mFiltered && visible && mPopup != null && mPopup.isShowing()) {
                positionPopup();
            }
        }

        return changed;
    }*/

	/**
	 * Subclasses must override this method to layout their children.
	 */
	protected void layoutChildren() {
	}

	void updateScrollIndicators() {
		if (mScrollUp != null && mScrollVertically) {
			boolean canScrollUp;
			// 0th element is not visible
			canScrollUp = mFirstPosition > 0;

			// ... Or top of 0th element is not visible
			if (!canScrollUp) {
				if (getChildCount() > 0) {
					View child = getChildAt(0);
					canScrollUp = child.getTop() < mListPadding.top;
				}
			}

			mScrollUp.setVisibility(canScrollUp ? View.VISIBLE : View.INVISIBLE);
		}

		if (mScrollDown != null && mScrollVertically) {
			boolean canScrollDown;
			int count = getChildCount();

			// Last item is not visible
			canScrollDown = (mFirstPosition + count) < mItemCount;

			// ... Or bottom of the last element is not visible
			if (!canScrollDown && count > 0) {
				View child = getChildAt(count - 1);
				canScrollDown = child.getBottom() > getBottom() - mListPadding.bottom;
			}

			mScrollDown.setVisibility(canScrollDown ? View.VISIBLE : View.INVISIBLE);
		}

		if (mScrollLeft != null && !mScrollVertically) {
			boolean canScrollLeft;
			// 0th element is not visible
			canScrollLeft = mFirstPosition > 0;

			// ... Or top of 0th element is not visible
			if (!canScrollLeft) {
				if (getChildCount() > 0) {
					View child = getChildAt(0);
					canScrollLeft = child.getLeft() < mListPadding.left;
				}
			}

			mScrollLeft.setVisibility(canScrollLeft ? View.VISIBLE : View.INVISIBLE);
		}

		if (mScrollRight != null && !mScrollVertically) {
			boolean canScrollRight;
			int count = getChildCount();

			// Last item is not visible
			canScrollRight = (mFirstPosition + count) < mItemCount;

			// ... Or bottom of the last element is not visible
			if (!canScrollRight && count > 0) {
				View child = getChildAt(count - 1);
				canScrollRight = child.getRight() > getRight() - mListPadding.right;
			}

			mScrollRight.setVisibility(canScrollRight ? View.VISIBLE : View.INVISIBLE);
		}
	}

	@Override
	@ViewDebug.ExportedProperty
	public View getSelectedView() {
		if (mItemCount > 0 && mSelectedPosition >= 0) {
			return getChildAt(mSelectedPosition - mFirstPosition);
		} else {
			return null;
		}
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingTop()
	 * @see #getSelector()
	 *
	 * @return The top list padding.
	 */
	public int getListPaddingTop() {
		return mListPadding.top;
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingBottom()
	 * @see #getSelector()
	 *
	 * @return The bottom list padding.
	 */
	public int getListPaddingBottom() {
		return mListPadding.bottom;
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingLeft()
	 * @see #getSelector()
	 *
	 * @return The left list padding.
	 */
	public int getListPaddingLeft() {
		return mListPadding.left;
	}

	/**
	 * List padding is the maximum of the normal view's padding and the padding of the selector.
	 *
	 * @see android.view.View#getPaddingRight()
	 * @see #getSelector()
	 *
	 * @return The right list padding.
	 */
	public int getListPaddingRight() {
		return mListPadding.right;
	}

	/**
	 * Get a view and have it show the data associated with the specified
	 * position. This is called when we have already discovered that the view is
	 * not available for reuse in the recycle bin. The only choices left are
	 * converting an old view or making a new one.
	 *
	 * @param position The position to display
	 * @param isScrap Array of at least 1 boolean, the first entry will become true if
	 *                the returned view was taken from the scrap heap, false if otherwise.
	 * 
	 * @return A view displaying the data associated with the specified position
	 */
	View obtainView(int position, boolean[] isScrap) {
		isScrap[0] = false;
		View scrapView;

		scrapView = mRecycler.getScrapView(position);

		View child;
		if (scrapView != null) {
			if (ViewDebug.TRACE_RECYCLER) {
				ViewDebug.trace(scrapView, ViewDebug.RecyclerTraceType.RECYCLE_FROM_SCRAP_HEAP,
						position, -1);
			}

			child = mAdapter.getView(position, scrapView, this);

			if (ViewDebug.TRACE_RECYCLER) {
				ViewDebug.trace(child, ViewDebug.RecyclerTraceType.BIND_VIEW,
						position, getChildCount());
			}

			if (child != scrapView) {
				mRecycler.addScrapView(scrapView);
				if (mCacheColorHint != 0) {
					child.setDrawingCacheBackgroundColor(mCacheColorHint);
				}
				if (ViewDebug.TRACE_RECYCLER) {
					ViewDebug.trace(scrapView, ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
							position, -1);
				}
			} else {
				isScrap[0] = true;
				child.onFinishTemporaryDetach();
			}
		} else {
			child = mAdapter.getView(position, null, this);
			if (mCacheColorHint != 0) {
				child.setDrawingCacheBackgroundColor(mCacheColorHint);
			}
			if (ViewDebug.TRACE_RECYCLER) {
				ViewDebug.trace(child, ViewDebug.RecyclerTraceType.NEW_VIEW,
						position, getChildCount());
			}
		}

		return child;
	}

	void positionSelector(View sel) {
		final Rect selectorRect = mSelectorRect;
		selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
		positionSelector(selectorRect.left, selectorRect.top, selectorRect.right,
				selectorRect.bottom);

		final boolean isChildViewEnabled = mIsChildViewEnabled;
		if (sel.isEnabled() != isChildViewEnabled) {
			mIsChildViewEnabled = !isChildViewEnabled;
			refreshDrawableState();
		}
	}

	private void positionSelector(int l, int t, int r, int b) {
		mSelectorRect.set(l - mSelectionLeftPadding, t - mSelectionTopPadding, r
				+ mSelectionRightPadding, b + mSelectionBottomPadding);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		int saveCount = 0;
		//TODO????
		/*
        final boolean clipToPadding = (mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK;
        if (clipToPadding) {
            saveCount = canvas.save();
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            canvas.clipRect(scrollX + getPaddingLeft(), scrollY + getPaddingTop(),
                    scrollX + getRight() - getLeft() - getPaddingRight(),
                    scrollY + getBottom() - getTop() - getPaddingBottom());
            mGroupFlags &= ~CLIP_TO_PADDING_MASK;
        }*/

		final boolean drawSelectorOnTop = mDrawSelectorOnTop;
		if (!drawSelectorOnTop) {
			drawSelector(canvas);
		}

		super.dispatchDraw(canvas);

		if (drawSelectorOnTop) {
			drawSelector(canvas);
		}
		/*
        if (clipToPadding) {
            canvas.restoreToCount(saveCount);
            mGroupFlags |= CLIP_TO_PADDING_MASK;
        }*/
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (getChildCount() > 0) {
			mDataChanged = true;
			rememberSyncState();
		}

		//if (mFastScroller != null) {
		//    mFastScroller.onSizeChanged(w, h, oldw, oldh);
		//}
	}

	/**
	 * @return True if the current touch mode requires that we draw the selector in the pressed
	 *         state.
	 */
	boolean touchModeDrawsInPressedState() {
		// FIXME use isPressed for this
		switch (mTouchMode) {
		case TOUCH_MODE_TAP:
		case TOUCH_MODE_DONE_WAITING:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Indicates whether this view is in a state where the selector should be drawn. This will
	 * happen if we have focus but are not in touch mode, or we are in the middle of displaying
	 * the pressed state for an item.
	 *
	 * @return True if the selector should be shown
	 */
	boolean shouldShowSelector() {
		return (hasFocus() && !isInTouchMode()) || touchModeDrawsInPressedState();
	}

	private void drawSelector(Canvas canvas) {
		if (shouldShowSelector() && mSelectorRect != null && !mSelectorRect.isEmpty()) {
			final Drawable selector = mSelector;
			selector.setBounds(mSelectorRect);
			selector.draw(canvas);
		}
	}

	/**
	 * Controls whether the selection highlight drawable should be drawn on top of the item or
	 * behind it.
	 *
	 * @param onTop If true, the selector will be drawn on the item it is highlighting. The default
	 *        is false.
	 *
	 * @attr ref android.R.styleable#JessAbsListView_drawSelectorOnTop
	 */
	public void setDrawSelectorOnTop(boolean onTop) {
		mDrawSelectorOnTop = onTop;
	}

	/**
	 * Set a Drawable that should be used to highlight the currently selected item.
	 *
	 * @param resID A Drawable resource to use as the selection highlight.
	 *
	 * @attr ref android.R.styleable#JessAbsListView_listSelector
	 */
	public void setSelector(int resID) {
		setSelector(getResources().getDrawable(resID));
	}

	public void setSelector(Drawable sel) {
		if (mSelector != null) {
			mSelector.setCallback(null);
			unscheduleDrawable(mSelector);
		}
		mSelector = sel;
		Rect padding = new Rect();
		sel.getPadding(padding);
		mSelectionLeftPadding = padding.left;
		mSelectionTopPadding = padding.top;
		mSelectionRightPadding = padding.right;
		mSelectionBottomPadding = padding.bottom;
		sel.setCallback(this);
		sel.setState(getDrawableState());
	}

	/**
	 * Returns the selector {@link android.graphics.drawable.Drawable} that is used to draw the
	 * selection in the list.
	 *
	 * @return the drawable used to display the selector
	 */
	public Drawable getSelector() {
		return mSelector;
	}

	/**
	 * Sets the selector state to "pressed" and posts a CheckForKeyLongPress to see if
	 * this is a long press.
	 */
	void keyPressed() {
		if (!isEnabled() || !isClickable()) {
			return;
		}

		Drawable selector = mSelector;
		Rect selectorRect = mSelectorRect;
		if (selector != null && (isFocused() || touchModeDrawsInPressedState())
				&& selectorRect != null && !selectorRect.isEmpty()) {

			final View v = getChildAt(mSelectedPosition - mFirstPosition);

			if (v != null) {
				if (v.hasFocusable()) return;
				v.setPressed(true);
			}
			setPressed(true);

			final boolean longClickable = isLongClickable();
			Drawable d = selector.getCurrent();
			if (d != null && d instanceof TransitionDrawable) {
				if (longClickable) {
					((TransitionDrawable) d).startTransition(
							ViewConfiguration.getLongPressTimeout());
				} else {
					((TransitionDrawable) d).resetTransition();
				}
			}
			if (longClickable && !mDataChanged) {
				if (mPendingCheckForKeyLongPress == null) {
					mPendingCheckForKeyLongPress = new CheckForKeyLongPress();
				}
				mPendingCheckForKeyLongPress.rememberWindowAttachCount();
				postDelayed(mPendingCheckForKeyLongPress, ViewConfiguration.getLongPressTimeout());
			}
		}
	}

	public void setScrollIndicators(View up, View down, View left, View right) {
		mScrollUp = up;
		mScrollDown = down;
		mScrollLeft = left;
		mScrollRight = right;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (mSelector != null) {
			mSelector.setState(getDrawableState());
		}
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		// If the child view is enabled then do the default behavior.
		if (mIsChildViewEnabled) {
			// Common case
			return super.onCreateDrawableState(extraSpace);
		}

		// The selector uses this View's drawable state. The selected child view
		// is disabled, so we need to remove the enabled state from the drawable
		// states.
		final int enabledState = ENABLED_STATE_SET[0];

		// If we don't have any extra space, it will return one of the static state arrays,
		// and clearing the enabled state on those arrays is a bad thing!  If we specify
		// we need extra space, it will create+copy into a new array that safely mutable.
		int[] state = super.onCreateDrawableState(extraSpace + 1);
		int enabledPos = -1;
		for (int i = state.length - 1; i >= 0; i--) {
			if (state[i] == enabledState) {
				enabledPos = i;
				break;
			}
		}

		// Remove the enabled state
		if (enabledPos >= 0) {
			System.arraycopy(state, enabledPos + 1, state, enabledPos,
					state.length - enabledPos - 1);
		}

		return state;
	}

	@Override
	public boolean verifyDrawable(Drawable dr) {
		return mSelector == dr || super.verifyDrawable(dr);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		final ViewTreeObserver treeObserver = getViewTreeObserver();
		if (treeObserver != null) {
			treeObserver.addOnTouchModeChangeListener(this);
			/*
            if (mTextFilterEnabled && mPopup != null && !mGlobalLayoutListenerAddedFilter) {
                treeObserver.addOnGlobalLayoutListener(this);
            }*/
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		// Dismiss the popup in case onSaveInstanceState() was not invoked
		//dismissPopup();

		// Detach any view left in the scrap heap
		mRecycler.clear();

		final ViewTreeObserver treeObserver = getViewTreeObserver();
		if (treeObserver != null) {
			treeObserver.removeOnTouchModeChangeListener(this);
			/*
            if (mTextFilterEnabled && mPopup != null) {
                treeObserver.removeGlobalOnLayoutListener(this);
                mGlobalLayoutListenerAddedFilter = false;
            }*/
		}
	}



	/**
	 * Creates the ContextMenuInfo returned from {@link #getContextMenuInfo()}. This
	 * methods knows the view, position and ID of the item that received the
	 * long press.
	 *
	 * @param view The view that received the long press.
	 * @param position The position of the item that received the long press.
	 * @param id The ID of the item that received the long press.
	 * @return The extra information that should be returned by
	 *         {@link #getContextMenuInfo()}.
	 */
	ContextMenuInfo createContextMenuInfo(View view, int position, long id) {
		return new AdapterContextMenuInfo(view, position, id);
	}

	/**
	 * A base class for Runnables that will check that their view is still attached to
	 * the original window as when the Runnable was created.
	 *
	 */
	private class WindowRunnnable {
		private int mOriginalAttachCount;

		public void rememberWindowAttachCount() {
			mOriginalAttachCount = getWindowAttachCount();
		}

		public boolean sameWindow() {
			return hasWindowFocus() && getWindowAttachCount() == mOriginalAttachCount;
		}
	}

	private class PerformClick extends WindowRunnnable implements Runnable {
		View mChild;
		int mClickMotionPosition;

		public void run() {
			// The data has changed since we posted this action in the event queue,
			// bail out before bad things happen
			if (mDataChanged) return;

			final ListAdapter adapter = mAdapter;
			final int motionPosition = mClickMotionPosition;
			if (adapter != null && mItemCount > 0 &&
					motionPosition != INVALID_POSITION &&
					motionPosition < adapter.getCount() && sameWindow()) {
				performItemClick(mChild, motionPosition, adapter.getItemId(motionPosition));
			}
		}
	}

	private class CheckForLongPress extends WindowRunnnable implements Runnable {
		public void run() {
			final int motionPosition = mMotionPosition;
			final View child = getChildAt(motionPosition - mFirstPosition);
			if (child != null) {
				final int longPressPosition = mMotionPosition;
				final long longPressId = mAdapter.getItemId(mMotionPosition);

				boolean handled = false;
				if (sameWindow() && !mDataChanged) {
					handled = performLongPress(child, longPressPosition, longPressId);
				}
				if (handled) {
					mTouchMode = TOUCH_MODE_REST;
					setPressed(false);
					child.setPressed(false);
				} else {
					mTouchMode = TOUCH_MODE_DONE_WAITING;
				}

			}
		}
	}

	private class CheckForKeyLongPress extends WindowRunnnable implements Runnable {
		public void run() {
			if (isPressed() && mSelectedPosition >= 0) {
				int index = mSelectedPosition - mFirstPosition;
				View v = getChildAt(index);

				if (!mDataChanged) {
					boolean handled = false;
					if (sameWindow()) {
						handled = performLongPress(v, mSelectedPosition, mSelectedRowId);
					}
					if (handled) {
						setPressed(false);
						v.setPressed(false);
					}
				} else {
					setPressed(false);
					if (v != null) v.setPressed(false);
				}
			}
		}
	}

	private boolean performLongPress(final View child,
			final int longPressPosition, final long longPressId) {
		boolean handled = false;

		if (mOnItemLongClickListener != null) {
			handled = mOnItemLongClickListener.onItemLongClick(TwoWayAbsListView.this, child,
					longPressPosition, longPressId);
		}
		if (!handled) {
			mContextMenuInfo = createContextMenuInfo(child, longPressPosition, longPressId);
			handled = super.showContextMenuForChild(TwoWayAbsListView.this);
		}
		if (handled) {
			performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
		}
		return handled;
	}

	@Override
	protected ContextMenuInfo getContextMenuInfo() {
		return mContextMenuInfo;
	}

	@Override
	public boolean showContextMenuForChild(View originalView) {
		final int longPressPosition = getPositionForView(originalView);
		if (longPressPosition >= 0) {
			final long longPressId = mAdapter.getItemId(longPressPosition);
			boolean handled = false;

			if (mOnItemLongClickListener != null) {
				handled = mOnItemLongClickListener.onItemLongClick(TwoWayAbsListView.this, originalView,
						longPressPosition, longPressId);
			}
			if (!handled) {
				mContextMenuInfo = createContextMenuInfo(
						getChildAt(longPressPosition - mFirstPosition),
						longPressPosition, longPressId);
				handled = super.showContextMenuForChild(originalView);
			}

			return handled;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			if (!isEnabled()) {
				return true;
			}
			if (isClickable() && isPressed() &&
					mSelectedPosition >= 0 && mAdapter != null &&
					mSelectedPosition < mAdapter.getCount()) {

				final View view = getChildAt(mSelectedPosition - mFirstPosition);
				if (view != null) {
					performItemClick(view, mSelectedPosition, mSelectedRowId);
					view.setPressed(false);
				}
				setPressed(false);
				return true;
			}
			break;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected void dispatchSetPressed(boolean pressed) {
		// Don't dispatch setPressed to our children. We call setPressed on ourselves to
		// get the selector in the right state, but we don't want to press each child.
	}

	/**
	 * Maps a point to a position in the list.
	 *
	 * @param x X in local coordinate
	 * @param y Y in local coordinate
	 * @return The position of the item which contains the specified point, or
	 *         {@link #INVALID_POSITION} if the point does not intersect an item.
	 */
	public int pointToPosition(int x, int y) {
		Rect frame = mTouchFrame;
		if (frame == null) {
			mTouchFrame = new Rect();
			frame = mTouchFrame;
		}

		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = getChildAt(i);
			if (child.getVisibility() == View.VISIBLE) {
				child.getHitRect(frame);
				if (frame.contains(x, y)) {
					return mFirstPosition + i;
				}
			}
		}
		return INVALID_POSITION;
	}


	/**
	 * Maps a point to a the rowId of the item which intersects that point.
	 *
	 * @param x X in local coordinate
	 * @param y Y in local coordinate
	 * @return The rowId of the item which contains the specified point, or {@link #INVALID_ROW_ID}
	 *         if the point does not intersect an item.
	 */
	public long pointToRowId(int x, int y) {
		int position = pointToPosition(x, y);
		if (position >= 0) {
			return mAdapter.getItemId(position);
		}
		return INVALID_ROW_ID;
	}

	final class CheckForTap implements Runnable {
		public void run() {
			if (mTouchMode == TOUCH_MODE_DOWN) {
				mTouchMode = TOUCH_MODE_TAP;
				final View child = getChildAt(mMotionPosition - mFirstPosition);
				if (child != null && !child.hasFocusable()) {
					mLayoutMode = LAYOUT_NORMAL;

					if (!mDataChanged) {
						layoutChildren();
						child.setPressed(true);
						positionSelector(child);
						setPressed(true);

						final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
						final boolean longClickable = isLongClickable();

						if (mSelector != null) {
							Drawable d = mSelector.getCurrent();
							if (d != null && d instanceof TransitionDrawable) {
								if (longClickable) {
									((TransitionDrawable) d).startTransition(longPressTimeout);
								} else {
									((TransitionDrawable) d).resetTransition();
								}
							}
						}

						if (longClickable) {
							if (mPendingCheckForLongPress == null) {
								mPendingCheckForLongPress = new CheckForLongPress();
							}
							mPendingCheckForLongPress.rememberWindowAttachCount();
							postDelayed(mPendingCheckForLongPress, longPressTimeout);
						} else {
							mTouchMode = TOUCH_MODE_DONE_WAITING;
						}
					} else {
						mTouchMode = TOUCH_MODE_DONE_WAITING;
					}
				}
			}
		}
	}


	public boolean startScrollIfNeeded(int delta) {
		return mTouchHandler.startScrollIfNeeded(delta);
	}


	public void onTouchModeChanged(boolean isInTouchMode) {
		mTouchHandler.onTouchModeChanged(isInTouchMode);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return mTouchHandler.onTouchEvent(ev);
	}

	/*
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mFastScroller != null) {
            mFastScroller.draw(canvas);
        }
    }*/

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return mTouchHandler.onInterceptTouchEvent(ev);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addTouchables(ArrayList<View> views) {
		final int count = getChildCount();
		final int firstPosition = mFirstPosition;
		final ListAdapter adapter = mAdapter;

		if (adapter == null) {
			return;
		}

		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (adapter.isEnabled(firstPosition + i)) {
				views.add(child);
			}
			child.addTouchables(views);
		}
	}


	/**
	 * Fires an "on scroll state changed" event to the registered
	 * {@link android.widget.AbsListView.OnScrollListener}, if any. The state change
	 * is fired only if the specified state is different from the previously known state.
	 *
	 * @param newState The new scroll state.
	 */
	void reportScrollStateChange(int newState) {
		if (newState != mLastScrollState) {
			if (mOnScrollListener != null) {
				mOnScrollListener.onScrollStateChanged(this, newState);
				mLastScrollState = newState;
			}
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		mTouchHandler.onWindowFocusChanged(hasWindowFocus);
	}


	/**
	 * Smoothly scroll to the specified adapter position. The view will
	 * scroll such that the indicated position is displayed.
	 * @param position Scroll to this adapter position.
	 */
	public void smoothScrollToPosition(int position) {
		mTouchHandler.smoothScrollToPosition(position);
	}

	/**
	 * Smoothly scroll to the specified adapter position. The view will
	 * scroll such that the indicated position is displayed, but it will
	 * stop early if scrolling further would scroll boundPosition out of
	 * view.
	 * @param position Scroll to this adapter position.
	 * @param boundPosition Do not scroll if it would move this adapter
	 *          position out of view.
	 */
	public void smoothScrollToPosition(int position, int boundPosition) {
		mTouchHandler.smoothScrollToPosition(position, boundPosition);
	}

	/**
	 * Smoothly scroll by distance pixels over duration milliseconds.
	 * @param distance Distance to scroll in pixels.
	 * @param duration Duration of the scroll animation in milliseconds.
	 */
	public void smoothScrollBy(int distance, int duration) {
		mTouchHandler.smoothScrollBy(distance, duration);
	}


	/**
	 * Returns the number of header views in the list. Header views are special views
	 * at the top of the list that should not be recycled during a layout.
	 *
	 * @return The number of header views, 0 in the default implementation.
	 */
	int getHeaderViewsCount() {
		return 0;
	}

	/**
	 * Returns the number of footer views in the list. Footer views are special views
	 * at the bottom of the list that should not be recycled during a layout.
	 *
	 * @return The number of footer views, 0 in the default implementation.
	 */
	int getFooterViewsCount() {
		return 0;
	}

	/**
	 * Fills the gap left open by a touch-scroll. During a touch scroll, children that
	 * remain on screen are shifted and the other ones are discarded. The role of this
	 * method is to fill the gap thus created by performing a partial layout in the
	 * empty space.
	 *
	 * @param down true if the scroll is going down, false if it is going up
	 */
	abstract void fillGap(boolean down);

	void hideSelector() {
		if (mSelectedPosition != INVALID_POSITION) {
			if (mLayoutMode != LAYOUT_SPECIFIC) {
				mResurrectToPosition = mSelectedPosition;
			}
			if (mNextSelectedPosition >= 0 && mNextSelectedPosition != mSelectedPosition) {
				mResurrectToPosition = mNextSelectedPosition;
			}
			setSelectedPositionInt(INVALID_POSITION);
			setNextSelectedPositionInt(INVALID_POSITION);
			mSelectedTop = 0;
			mSelectorRect.setEmpty();
		}
	}

	/**
	 * @return A position to select. First we try mSelectedPosition. If that has been clobbered by
	 * entering touch mode, we then try mResurrectToPosition. Values are pinned to the range
	 * of items available in the adapter
	 */
	int reconcileSelectedPosition() {
		int position = mSelectedPosition;
		if (position < 0) {
			position = mResurrectToPosition;
		}
		position = Math.max(0, position);
		position = Math.min(position, mItemCount - 1);
		return position;
	}

	/**
	 * Find the row closest to y. This row will be used as the motion row when scrolling
	 *
	 * @param y Where the user touched
	 * @return The position of the first (or only) item in the row containing y
	 */
	abstract int findMotionRowY(int y);

	/**
	 * Find the row closest to y. This row will be used as the motion row when scrolling.
	 * 
	 * @param y Where the user touched
	 * @return The position of the first (or only) item in the row closest to y
	 */
	int findClosestMotionRowY(int y) {
		final int childCount = getChildCount();
		if (childCount == 0) {
			return INVALID_POSITION;
		}

		final int motionRow = findMotionRowY(y);
		return motionRow != INVALID_POSITION ? motionRow : mFirstPosition + childCount - 1;
	}

	/**
	 * Find the row closest to x. This row will be used as the motion row when scrolling
	 *
	 * @param x Where the user touched
	 * @return The position of the first (or only) item in the row containing y
	 */
	abstract int findMotionRowX(int x);

	/**
	 * Find the row closest to y. This row will be used as the motion row when scrolling.
	 * 
	 * @param x Where the user touched
	 * @return The position of the first (or only) item in the row closest to y
	 */
	int findClosestMotionRow(int x) {
		final int childCount = getChildCount();
		if (childCount == 0) {
			return INVALID_POSITION;
		}

		final int motionRow = findMotionRowX(x);
		return motionRow != INVALID_POSITION ? motionRow : mFirstPosition + childCount - 1;
	}

	/**
	 * Causes all the views to be rebuilt and redrawn.
	 */
	public void invalidateViews() {
		mDataChanged = true;
		rememberSyncState();
		requestLayout();
		invalidate();
	}

	/**
	 * Makes the item at the supplied position selected.
	 *
	 * @param position the position of the new selection
	 */
	abstract void setSelectionInt(int position);

	/**
	 * Attempt to bring the selection back if the user is switching from touch
	 * to trackball mode
	 * @return Whether selection was set to something.
	 */
	boolean resurrectSelection() {
		return mTouchHandler.resurrectSelection();
	}

	@Override
	protected void handleDataChanged() {
		int count = mItemCount;
		if (count > 0) {

			int newPos;

			int selectablePos;

			// Find the row we are supposed to sync to
			if (mNeedSync) {
				// Update this first, since setNextSelectedPositionInt inspects it
				mNeedSync = false;

				if (mTranscriptMode == TRANSCRIPT_MODE_ALWAYS_SCROLL ||
						(mTranscriptMode == TRANSCRIPT_MODE_NORMAL &&
								mFirstPosition + getChildCount() >= mOldItemCount)) {
					mLayoutMode = LAYOUT_FORCE_BOTTOM;
					return;
				}

				switch (mSyncMode) {
				case SYNC_SELECTED_POSITION:
					if (isInTouchMode()) {
						// We saved our state when not in touch mode. (We know this because
						// mSyncMode is SYNC_SELECTED_POSITION.) Now we are trying to
						// restore in touch mode. Just leave mSyncPosition as it is (possibly
						// adjusting if the available range changed) and return.
						mLayoutMode = LAYOUT_SYNC;
						mSyncPosition = Math.min(Math.max(0, mSyncPosition), count - 1);

						return;
					} else {
						// See if we can find a position in the new data with the same
						// id as the old selection. This will change mSyncPosition.
						newPos = findSyncPosition();
						if (newPos >= 0) {
							// Found it. Now verify that new selection is still selectable
							selectablePos = lookForSelectablePosition(newPos, true);
							if (selectablePos == newPos) {
								// Same row id is selected
								mSyncPosition = newPos;
								int size = mIsVertical ? getHeight() : getWidth();
								if (mSyncSize == size) {
									// If we are at the same height as when we saved state, try
									// to restore the scroll position too.
									mLayoutMode = LAYOUT_SYNC;
								} else {
									// We are not the same height as when the selection was saved, so
									// don't try to restore the exact position
									mLayoutMode = LAYOUT_SET_SELECTION;
								}

								// Restore selection
								setNextSelectedPositionInt(newPos);
								return;
							}
						}
					}
					break;
				case SYNC_FIRST_POSITION:
					// Leave mSyncPosition as it is -- just pin to available range
					mLayoutMode = LAYOUT_SYNC;
					mSyncPosition = Math.min(Math.max(0, mSyncPosition), count - 1);

					return;
				}
			}

			if (!isInTouchMode()) {
				// We couldn't find matching data -- try to use the same position
				newPos = getSelectedItemPosition();

				// Pin position to the available range
				if (newPos >= count) {
					newPos = count - 1;
				}
				if (newPos < 0) {
					newPos = 0;
				}

				// Make sure we select something selectable -- first look down
				selectablePos = lookForSelectablePosition(newPos, true);

				if (selectablePos >= 0) {
					setNextSelectedPositionInt(selectablePos);
					return;
				} else {
					// Looking down didn't work -- try looking up
					selectablePos = lookForSelectablePosition(newPos, false);
					if (selectablePos >= 0) {
						setNextSelectedPositionInt(selectablePos);
						return;
					}
				}
			} else {

				// We already know where we want to resurrect the selection
				if (mResurrectToPosition >= 0) {
					return;
				}
			}

		}

		// Nothing is selected. Give up and reset everything.
		mLayoutMode = mStackFromBottom ? LAYOUT_FORCE_BOTTOM : LAYOUT_FORCE_TOP;
		mSelectedPosition = INVALID_POSITION;
		mSelectedRowId = INVALID_ROW_ID;
		mNextSelectedPosition = INVALID_POSITION;
		mNextSelectedRowId = INVALID_ROW_ID;
		mNeedSync = false;
		checkSelectionChanged();
	}


	//    @Override
	//    protected void onDisplayHint(int hint) {
	//        super.onDisplayHint(hint);
	//        switch (hint) {
	//            case INVISIBLE:
	//                if (mPopup != null && mPopup.isShowing()) {
	//                    dismissPopup();
	//                }
	//                break;
	//            case VISIBLE:
	//                if (mFiltered && mPopup != null && !mPopup.isShowing()) {
	//                    showPopup();
	//                }
	//                break;
	//        }
	//        mPopupHidden = hint == INVISIBLE;
	//    }
	//
	//    /**
	//     * Removes the filter window
	//     */
	//    private void dismissPopup() {
	//        if (mPopup != null) {
	//            mPopup.dismiss();
	//        }
	//    }
	//
	//    /**
	//     * Shows the filter window
	//     */
	//    private void showPopup() {
	//        // Make sure we have a window before showing the popup
	//        if (getWindowVisibility() == View.VISIBLE) {
	//            createTextFilter(true);
	//            positionPopup();
	//            // Make sure we get focus if we are showing the popup
	//            checkFocus();
	//        }
	//    }
	//
	//    private void positionPopup() {
	//        int screenHeight = getResources().getDisplayMetrics().heightPixels;
	//        final int[] xy = new int[2];
	//        getLocationOnScreen(xy);
	//        // TODO: The 20 below should come from the theme
	//        // TODO: And the gravity should be defined in the theme as well
	//        final int bottomGap = screenHeight - xy[1] - getHeight() + (int) (mDensityScale * 20);
	//        if (!mPopup.isShowing()) {
	//            mPopup.showAtLocation(this, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
	//                    xy[0], bottomGap);
	//        } else {
	//            mPopup.update(xy[0], bottomGap, -1, -1);
	//        }
	//    }

	/**
	 * What is the distance between the source and destination rectangles given the direction of
	 * focus navigation between them? The direction basically helps figure out more quickly what is
	 * self evident by the relationship between the rects...
	 *
	 * @param source the source rectangle
	 * @param dest the destination rectangle
	 * @param direction the direction
	 * @return the distance between the rectangles
	 */
	static int getDistance(Rect source, Rect dest, int direction) {
		int sX, sY; // source x, y
		int dX, dY; // dest x, y
		switch (direction) {
		case View.FOCUS_RIGHT:
			sX = source.right;
			sY = source.top + source.height() / 2;
			dX = dest.left;
			dY = dest.top + dest.height() / 2;
			break;
		case View.FOCUS_DOWN:
			sX = source.left + source.width() / 2;
			sY = source.bottom;
			dX = dest.left + dest.width() / 2;
			dY = dest.top;
			break;
		case View.FOCUS_LEFT:
			sX = source.left;
			sY = source.top + source.height() / 2;
			dX = dest.right;
			dY = dest.top + dest.height() / 2;
			break;
		case View.FOCUS_UP:
			sX = source.left + source.width() / 2;
			sY = source.top;
			dX = dest.left + dest.width() / 2;
			dY = dest.bottom;
			break;
		case View.FOCUS_FORWARD:
		case View.FOCUS_BACKWARD:
			sX = source.right + source.width() / 2;
			sY = source.top + source.height() / 2;
			dX = dest.left + dest.width() / 2;
			dY = dest.top + dest.height() / 2;
			break;
		default:
			throw new IllegalArgumentException("direction must be one of "
				+ "{FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, "
				+ "FOCUS_FORWARD, FOCUS_BACKWARD}.");
		}
		int deltaX = dX - sX;
		int deltaY = dY - sY;
		return deltaY * deltaY + deltaX * deltaX;
	}

	//    @Override
	//    protected boolean isInFilterMode() {
	//        return mFiltered;
	//    }
	//
	//    /**
	//     * Sends a key to the text filter window
	//     *
	//     * @param keyCode The keycode for the event
	//     * @param event The actual key event
	//     *
	//     * @return True if the text filter handled the event, false otherwise.
	//     */
	//    boolean sendToTextFilter(int keyCode, int count, KeyEvent event) {
	//        if (!acceptFilter()) {
	//            return false;
	//        }
	//
	//        boolean handled = false;
	//        boolean okToSend = true;
	//        switch (keyCode) {
	//        case KeyEvent.KEYCODE_DPAD_UP:
	//        case KeyEvent.KEYCODE_DPAD_DOWN:
	//        case KeyEvent.KEYCODE_DPAD_LEFT:
	//        case KeyEvent.KEYCODE_DPAD_RIGHT:
	//        case KeyEvent.KEYCODE_DPAD_CENTER:
	//        case KeyEvent.KEYCODE_ENTER:
	//            okToSend = false;
	//            break;
	//        case KeyEvent.KEYCODE_BACK:
	//            if (mFiltered && mPopup != null && mPopup.isShowing()) {
	//                if (event.getAction() == KeyEvent.ACTION_DOWN
	//                        && event.getRepeatCount() == 0) {
	//                    getKeyDispatcherState().startTracking(event, this);
	//                    handled = true;
	//                } else if (event.getAction() == KeyEvent.ACTION_UP
	//                        && event.isTracking() && !event.isCanceled()) {
	//                    handled = true;
	//                    mTextFilter.setText("");
	//                }
	//            }
	//            okToSend = false;
	//            break;
	//        case KeyEvent.KEYCODE_SPACE:
	//            // Only send spaces once we are filtered
	//            okToSend = mFiltered;
	//            break;
	//        }
	//
	//        if (okToSend) {
	//            createTextFilter(true);
	//
	//            KeyEvent forwardEvent = event;
	//            if (forwardEvent.getRepeatCount() > 0) {
	//                forwardEvent = KeyEvent.changeTimeRepeat(event, event.getEventTime(), 0);
	//            }
	//
	//            int action = event.getAction();
	//            switch (action) {
	//                case KeyEvent.ACTION_DOWN:
	//                    handled = mTextFilter.onKeyDown(keyCode, forwardEvent);
	//                    break;
	//
	//                case KeyEvent.ACTION_UP:
	//                    handled = mTextFilter.onKeyUp(keyCode, forwardEvent);
	//                    break;
	//
	//                case KeyEvent.ACTION_MULTIPLE:
	//                    handled = mTextFilter.onKeyMultiple(keyCode, count, event);
	//                    break;
	//            }
	//        }
	//        return handled;
	//    }
	//
	//    /**
	//     * Return an InputConnection for editing of the filter text.
	//     */
	//    @Override
	//    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
	//        if (isTextFilterEnabled()) {
	//            // XXX we need to have the text filter created, so we can get an
	//            // InputConnection to proxy to.  Unfortunately this means we pretty
	//            // much need to make it as soon as a list view gets focus.
	//            createTextFilter(false);
	//            if (mPublicInputConnection == null) {
	//                mDefInputConnection = new BaseInputConnection(this, false);
	//                mPublicInputConnection = new InputConnectionWrapper(
	//                        mTextFilter.onCreateInputConnection(outAttrs), true) {
	//                    @Override
	//                    public boolean reportFullscreenMode(boolean enabled) {
	//                        // Use our own input connection, since it is
	//                        // the "real" one the IME is talking with.
	//                        return mDefInputConnection.reportFullscreenMode(enabled);
	//                    }
	//
	//                    @Override
	//                    public boolean performEditorAction(int editorAction) {
	//                        // The editor is off in its own window; we need to be
	//                        // the one that does this.
	//                        if (editorAction == EditorInfo.IME_ACTION_DONE) {
	//                            InputMethodManager imm = (InputMethodManager)
	//                                    getContext().getSystemService(
	//                                            Context.INPUT_METHOD_SERVICE);
	//                            if (imm != null) {
	//                                imm.hideSoftInputFromWindow(getWindowToken(), 0);
	//                            }
	//                            return true;
	//                        }
	//                        return false;
	//                    }
	//
	//                    @Override
	//                    public boolean sendKeyEvent(KeyEvent event) {
	//                        // Use our own input connection, since the filter
	//                        // text view may not be shown in a window so has
	//                        // no ViewRoot to dispatch events with.
	//                        return mDefInputConnection.sendKeyEvent(event);
	//                    }
	//                };
	//            }
	//            outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT
	//                    | EditorInfo.TYPE_TEXT_VARIATION_FILTER;
	//            outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE;
	//            return mPublicInputConnection;
	//        }
	//        return null;
	//    }
	//
	//    /**
	//     * For filtering we proxy an input connection to an internal text editor,
	//     * and this allows the proxying to happen.
	//     */
	//
	//    @Override
	//    public boolean checkInputConnectionProxy(View view) {
	//        return view == mTextFilter;
	//    }
	//
	//    /**
	//     * Creates the window for the text filter and populates it with an EditText field;
	//     *
	//     * @param animateEntrance true if the window should appear with an animation
	//     */
	//    /*
	//    private void createTextFilter(boolean animateEntrance) {
	//        if (mPopup == null) {
	//            Context c = getContext();
	//            PopupWindow p = new PopupWindow(c);
	//            LayoutInflater layoutInflater = (LayoutInflater)
	//                    c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	//            mTextFilter = (EditText) layoutInflater.inflate(
	//                    android.R.layout.typing_filter, null);
	//            // For some reason setting this as the "real" input type changes
	//            // the text view in some way that it doesn't work, and I don't
	//            // want to figure out why this is.
	//            mTextFilter.setRawInputType(EditorInfo.TYPE_CLASS_TEXT
	//                    | EditorInfo.TYPE_TEXT_VARIATION_FILTER);
	//            mTextFilter.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
	//            mTextFilter.addTextChangedListener(this);
	//            p.setFocusable(false);
	//            p.setTouchable(false);
	//            p.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
	//            p.setContentView(mTextFilter);
	//            p.setWidth(LayoutParams.WRAP_CONTENT);
	//            p.setHeight(LayoutParams.WRAP_CONTENT);
	//            p.setBackgroundDrawable(null);
	//            mPopup = p;
	//            getViewTreeObserver().addOnGlobalLayoutListener(this);
	//            mGlobalLayoutListenerAddedFilter = true;
	//        }
	//        if (animateEntrance) {
	//            mPopup.setAnimationStyle(R.style.Animation_TypingFilter);
	//        } else {
	//            mPopup.setAnimationStyle(R.style.Animation_TypingFilterRestore);
	//        }
	//    }*/
	//
	//    /**
	//     * Clear the text filter.
	//     */
	//    /*
	//    public void clearTextFilter() {
	//        if (mFiltered) {
	//            mTextFilter.setText("");
	//            mFiltered = false;
	//            if (mPopup != null && mPopup.isShowing()) {
	//                dismissPopup();
	//            }
	//        }
	//    }*/
	//
	//    /**
	//     * Returns if the ListView currently has a text filter.
	//     */
	//    public boolean hasTextFilter() {
	//        return mFiltered;
	//    }
	//
	//    public void onGlobalLayout() {
	//        if (isShown()) {
	//            // Show the popup if we are filtered
	//            if (mFiltered && mPopup != null && !mPopup.isShowing() && !mPopupHidden) {
	//                showPopup();
	//            }
	//        } else {
	//            // Hide the popup when we are no longer visible
	//            if (mPopup != null && mPopup.isShowing()) {
	//                dismissPopup();
	//            }
	//        }
	//
	//    }
	//
	//    /**
	//     * For our text watcher that is associated with the text filter.  Does
	//     * nothing.
	//     */
	//    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	//    }
	//
	//    /**
	//     * For our text watcher that is associated with the text filter. Performs
	//     * the actual filtering as the text changes, and takes care of hiding and
	//     * showing the popup displaying the currently entered filter text.
	//     */
	//    public void onTextChanged(CharSequence s, int start, int before, int count) {
	//        if (mPopup != null && isTextFilterEnabled()) {
	//            int length = s.length();
	//            boolean showing = mPopup.isShowing();
	//            if (!showing && length > 0) {
	//                // Show the filter popup if necessary
	//                showPopup();
	//                mFiltered = true;
	//            } else if (showing && length == 0) {
	//                // Remove the filter popup if the user has cleared all text
	//                dismissPopup();
	//                mFiltered = false;
	//            }
	//            if (mAdapter instanceof Filterable) {
	//                Filter f = ((Filterable) mAdapter).getFilter();
	//                // Filter should not be null when we reach this part
	//                if (f != null) {
	//                    f.filter(s, this);
	//                } else {
	//                    throw new IllegalStateException("You cannot call onTextChanged with a non "
	//                            + "filterable adapter");
	//                }
	//            }
	//        }
	//    }
	//
	//    /**
	//     * For our text watcher that is associated with the text filter.  Does
	//     * nothing.
	//     */
	//    public void afterTextChanged(Editable s) {
	//    }
	//
	//    public void onFilterComplete(int count) {
	//        if (mSelectedPosition < 0 && count > 0) {
	//            mResurrectToPosition = INVALID_POSITION;
	//            resurrectSelection();
	//        }
	//    }

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new TwoWayAbsListView.LayoutParams(getContext(), attrs);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof TwoWayAbsListView.LayoutParams;
	}

	/**
	 * Puts the list or grid into transcript mode. In this mode the list or grid will always scroll
	 * to the bottom to show new items.
	 *
	 * @param mode the transcript mode to set
	 *
	 * @see #TRANSCRIPT_MODE_DISABLED
	 * @see #TRANSCRIPT_MODE_NORMAL
	 * @see #TRANSCRIPT_MODE_ALWAYS_SCROLL
	 */
	public void setTranscriptMode(int mode) {
		mTranscriptMode = mode;
	}

	/**
	 * Returns the current transcript mode.
	 *
	 * @return {@link #TRANSCRIPT_MODE_DISABLED}, {@link #TRANSCRIPT_MODE_NORMAL} or
	 *         {@link #TRANSCRIPT_MODE_ALWAYS_SCROLL}
	 */
	public int getTranscriptMode() {
		return mTranscriptMode;
	}

	/**
	 * Sets the direction that the view schould scroll when in portrait orientation
	 *
	 * @param direction the view should scroll
	 *
	 * @see #SCROLL_VERTICAL
	 * @see #SCROLL_HORIZONTAL
	 */
	public void setScrollDirectionPortrait(int direction) {
		boolean tempDirection = mScrollVerticallyPortrait;
		mScrollVerticallyPortrait = (direction == SCROLL_VERTICAL);
		if (tempDirection != mScrollVerticallyPortrait) {
			setupScrollInfo();
			//TODO or requestLayoutIfNecessary()?
			resetList();
			mRecycler.clear();
		}
	}

	/**
	 * Returns the current portrait scroll direction.
	 *
	 * @return {@link #SCROLL_VERTICAL} or {@link #SCROLL_HORIZONTAL}
	 */
	public int getScrollDirectionPortrait() {
		return mScrollVerticallyPortrait ? SCROLL_VERTICAL : SCROLL_HORIZONTAL;
	}

	/**
	 * Sets the direction that the view schould scroll when in landscape orientation
	 *
	 * @param direction the view should scroll
	 *
	 * @see #SCROLL_VERTICAL
	 * @see #SCROLL_HORIZONTAL
	 */
	public void setScrollDirectionLandscape(int direction) {
		boolean tempDirection = mScrollVerticallyLandscape;
		mScrollVerticallyLandscape = (direction == SCROLL_VERTICAL);
		if (tempDirection != mScrollVerticallyLandscape) {
			setupScrollInfo();
			//TODO or requestLayoutIfNecessary()?
			resetList();
			mRecycler.clear();
		}
	}

	/**
	 * Returns the current landscape scroll direction.
	 *
	 * @return {@link #SCROLL_VERTICAL} or {@link #SCROLL_HORIZONTAL}
	 */
	public int getScrollDirectionLandscape() {
		return mScrollVerticallyLandscape ? SCROLL_VERTICAL : SCROLL_HORIZONTAL;
	}

	@Override
	public int getSolidColor() {
		return mCacheColorHint;
	}

	/**
	 * When set to a non-zero value, the cache color hint indicates that this list is always drawn
	 * on top of a solid, single-color, opaque background
	 *
	 * @param color The background color
	 */
	public void setCacheColorHint(int color) {
		if (color != mCacheColorHint) {
			mCacheColorHint = color;
			int count = getChildCount();
			for (int i = 0; i < count; i++) {
				getChildAt(i).setDrawingCacheBackgroundColor(color);
			}
			mRecycler.setCacheColorHint(color);
		}
	}

	/**
	 * When set to a non-zero value, the cache color hint indicates that this list is always drawn
	 * on top of a solid, single-color, opaque background
	 *
	 * @return The cache color hint
	 */
	public int getCacheColorHint() {
		return mCacheColorHint;
	}

	/**
	 * Move all views (excluding headers and footers) held by this TwoWayAbsListView into the supplied
	 * List. This includes views displayed on the screen as well as views stored in TwoWayAbsListView's
	 * internal view recycler.
	 *
	 * @param views A list into which to put the reclaimed views
	 */
	public void reclaimViews(List<View> views) {
		int childCount = getChildCount();
		RecyclerListener listener = mRecycler.mRecyclerListener;

		// Reclaim views on screen
		for (int i = 0; i < childCount; i++) {
			View child = getChildAt(i);
			TwoWayAbsListView.LayoutParams lp = (TwoWayAbsListView.LayoutParams) child.getLayoutParams();
			// Don't reclaim header or footer views, or views that should be ignored
			if (lp != null && mRecycler.shouldRecycleViewType(lp.viewType)) {
				views.add(child);
				if (listener != null) {
					// Pretend they went through the scrap heap
					listener.onMovedToScrapHeap(child);
				}
			}
		}
		mRecycler.reclaimScrapViews(views);
		removeAllViewsInLayout();
	}

	/**
	 * @hide
	 */
	protected boolean checkConsistency(int consistency) {
		boolean result = true;

		final boolean checkLayout = true;

		if (checkLayout) {
			// The active recycler must be empty
			final View[] activeViews = mRecycler.mActiveViews;
			int count = activeViews.length;
			for (int i = 0; i < count; i++) {
				if (activeViews[i] != null) {
					result = false;
					Log.d("Consistency",
							"AbsListView " + this + " has a view in its active recycler: " +
							activeViews[i]);
				}
			}

			// All views in the recycler must NOT be on screen and must NOT have a parent
			final ArrayList<View> scrap = mRecycler.mCurrentScrap;
			if (!checkScrap(scrap)) result = false;
			final ArrayList<View>[] scraps = mRecycler.mScrapViews;
			count = scraps.length;
			for (int i = 0; i < count; i++) {
				if (!checkScrap(scraps[i])) result = false;
			}
		}

		return result;
	}

	private boolean checkScrap(ArrayList<View> scrap) {
		if (scrap == null) return true;
		boolean result = true;

		final int count = scrap.size();
		for (int i = 0; i < count; i++) {
			final View view = scrap.get(i);
			if (view.getParent() != null) {
				result = false;
				Log.d("Consistency", "TwoWayAbsListView " + this +
						" has a view in its scrap heap still attached to a parent: " + view);
			}
			if (indexOfChild(view) >= 0) {
				result = false;
				Log.d("Consistency", "TwoWayAbsListView " + this +
						" has a view in its scrap heap that is also a direct child: " + view);
			}
		}

		return result;
	}

	/**
	 * Sets the recycler listener to be notified whenever a View is set aside in
	 * the recycler for later reuse. This listener can be used to free resources
	 * associated to the View.
	 *
	 * @param listener The recycler listener to be notified of views set aside
	 *        in the recycler.
	 *
	 * @see com.jess.ui.TwoWayAbsListView.RecycleBin
	 * @see com.jess.ui.TwoWayAbsListView.RecyclerListener
	 */
	public void setRecyclerListener(RecyclerListener listener) {
		mRecycler.mRecyclerListener = listener;
	}

	/**
	 * TwoWayAbsListView extends LayoutParams to provide a place to hold the view type.
	 */
	public static class LayoutParams extends ViewGroup.LayoutParams {
		/**
		 * View type for this view, as returned by
		 * {@link android.widget.Adapter#getItemViewType(int) }
		 */
		@ViewDebug.ExportedProperty(mapping = {
				@ViewDebug.IntToString(from = ITEM_VIEW_TYPE_IGNORE, to = "ITEM_VIEW_TYPE_IGNORE"),
				@ViewDebug.IntToString(from = ITEM_VIEW_TYPE_HEADER_OR_FOOTER, to = "ITEM_VIEW_TYPE_HEADER_OR_FOOTER")
		})
		int viewType;

		/**
		 * When this boolean is set, the view has been added to the TwoWayAbsListView
		 * at least once. It is used to know whether headers/footers have already
		 * been added to the list view and whether they should be treated as
		 * recycled views or not.
		 */
		@ViewDebug.ExportedProperty
		boolean recycledHeaderFooter;

		/**
		 * When an TwoWayAbsListView is measured with an AT_MOST measure spec, it needs
		 * to obtain children views to measure itself. When doing so, the children
		 * are not attached to the window, but put in the recycler which assumes
		 * they've been attached before. Setting this flag will force the reused
		 * view to be attached to the window rather than just attached to the
		 * parent.
		 */
		@ViewDebug.ExportedProperty
		boolean forceAdd;

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);
		}

		public LayoutParams(int w, int h) {
			super(w, h);
		}

		public LayoutParams(int w, int h, int viewType) {
			super(w, h);
			this.viewType = viewType;
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
	}

	/**
	 * A RecyclerListener is used to receive a notification whenever a View is placed
	 * inside the RecycleBin's scrap heap. This listener is used to free resources
	 * associated to Views placed in the RecycleBin.
	 *
	 * @see com.jess.ui.TwoWayAbsListView.RecycleBin
	 * @see com.jess.ui.TwoWayAbsListView#setRecyclerListener(com.jess.ui.TwoWayAbsListView.RecyclerListener)
	 */
	public static interface RecyclerListener {
		/**
		 * Indicates that the specified View was moved into the recycler's scrap heap.
		 * The view is not displayed on screen any more and any expensive resource
		 * associated with the view should be discarded.
		 *
		 * @param view
		 */
		void onMovedToScrapHeap(View view);
	}

	/**
	 * The RecycleBin facilitates reuse of views across layouts. The RecycleBin has two levels of
	 * storage: ActiveViews and ScrapViews. ActiveViews are those views which were onscreen at the
	 * start of a layout. By construction, they are displaying current information. At the end of
	 * layout, all views in ActiveViews are demoted to ScrapViews. ScrapViews are old views that
	 * could potentially be used by the adapter to avoid allocating views unnecessarily.
	 *
	 * @see com.jess.ui.TwoWayAbsListView#setRecyclerListener(com.jess.ui.TwoWayAbsListView.RecyclerListener)
	 * @see com.jess.ui.TwoWayAbsListView.RecyclerListener
	 */
	class RecycleBin {
		private RecyclerListener mRecyclerListener;

		/**
		 * The position of the first view stored in mActiveViews.
		 */
		private int mFirstActivePosition;

		/**
		 * Views that were on screen at the start of layout. This array is populated at the start of
		 * layout, and at the end of layout all view in mActiveViews are moved to mScrapViews.
		 * Views in mActiveViews represent a contiguous range of Views, with position of the first
		 * view store in mFirstActivePosition.
		 */
		private View[] mActiveViews = new View[0];

		/**
		 * Unsorted views that can be used by the adapter as a convert view.
		 */
		private ArrayList<View>[] mScrapViews;

		private int mViewTypeCount;

		private ArrayList<View> mCurrentScrap;

		public void setViewTypeCount(int viewTypeCount) {
			if (viewTypeCount < 1) {
				throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
			}
			//noinspection unchecked
			ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
			for (int i = 0; i < viewTypeCount; i++) {
				scrapViews[i] = new ArrayList<View>();
			}
			mViewTypeCount = viewTypeCount;
			mCurrentScrap = scrapViews[0];
			mScrapViews = scrapViews;
		}

		public void markChildrenDirty() {
			if (mViewTypeCount == 1) {
				final ArrayList<View> scrap = mCurrentScrap;
				final int scrapCount = scrap.size();
				for (int i = 0; i < scrapCount; i++) {
					scrap.get(i).forceLayout();
				}
			} else {
				final int typeCount = mViewTypeCount;
				for (int i = 0; i < typeCount; i++) {
					final ArrayList<View> scrap = mScrapViews[i];
					final int scrapCount = scrap.size();
					for (int j = 0; j < scrapCount; j++) {
						scrap.get(j).forceLayout();
					}
				}
			}
		}

		public boolean shouldRecycleViewType(int viewType) {
			return viewType >= 0;
		}

		/**
		 * Clears the scrap heap.
		 */
		void clear() {
			if (mViewTypeCount == 1) {
				final ArrayList<View> scrap = mCurrentScrap;
				final int scrapCount = scrap.size();
				for (int i = 0; i < scrapCount; i++) {
					removeDetachedView(scrap.remove(scrapCount - 1 - i), false);
				}
			} else {
				final int typeCount = mViewTypeCount;
				for (int i = 0; i < typeCount; i++) {
					final ArrayList<View> scrap = mScrapViews[i];
					final int scrapCount = scrap.size();
					for (int j = 0; j < scrapCount; j++) {
						removeDetachedView(scrap.remove(scrapCount - 1 - j), false);
					}
				}
			}
		}

		/**
		 * Fill ActiveViews with all of the children of the TwoWayAbsListView.
		 *
		 * @param childCount The minimum number of views mActiveViews should hold
		 * @param firstActivePosition The position of the first view that will be stored in
		 *        mActiveViews
		 */
		void fillActiveViews(int childCount, int firstActivePosition) {
			if (mActiveViews.length < childCount) {
				mActiveViews = new View[childCount];
			}
			mFirstActivePosition = firstActivePosition;

			final View[] activeViews = mActiveViews;
			for (int i = 0; i < childCount; i++) {
				View child = getChildAt(i);
				TwoWayAbsListView.LayoutParams lp = (TwoWayAbsListView.LayoutParams) child.getLayoutParams();
				// Don't put header or footer views into the scrap heap
				if (lp != null && lp.viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
					// Note:  We do place TwoWayAdapterView.ITEM_VIEW_TYPE_IGNORE in active views.
					//        However, we will NOT place them into scrap views.
					activeViews[i] = child;
				}
			}
		}

		/**
		 * Get the view corresponding to the specified position. The view will be removed from
		 * mActiveViews if it is found.
		 *
		 * @param position The position to look up in mActiveViews
		 * @return The view if it is found, null otherwise
		 */
		View getActiveView(int position) {
			int index = position - mFirstActivePosition;
			final View[] activeViews = mActiveViews;
			if (index >=0 && index < activeViews.length) {
				final View match = activeViews[index];
				activeViews[index] = null;
				return match;
			}
			return null;
		}

		/**
		 * @return A view from the ScrapViews collection. These are unordered.
		 */
		View getScrapView(int position) {
			ArrayList<View> scrapViews;
			if (mViewTypeCount == 1) {
				scrapViews = mCurrentScrap;
				int size = scrapViews.size();
				if (size > 0) {
					return scrapViews.remove(size - 1);
				} else {
					return null;
				}
			} else {
				int whichScrap = mAdapter.getItemViewType(position);
				if (whichScrap >= 0 && whichScrap < mScrapViews.length) {
					scrapViews = mScrapViews[whichScrap];
					int size = scrapViews.size();
					if (size > 0) {
						return scrapViews.remove(size - 1);
					}
				}
			}
			return null;
		}

		/**
		 * Put a view into the ScapViews list. These views are unordered.
		 *
		 * @param scrap The view to add
		 */
		void addScrapView(View scrap) {
			TwoWayAbsListView.LayoutParams lp = (TwoWayAbsListView.LayoutParams) scrap.getLayoutParams();
			if (lp == null) {
				return;
			}

			// Don't put header or footer views or views that should be ignored
			// into the scrap heap
			int viewType = lp.viewType;
			if (!shouldRecycleViewType(viewType)) {
				if (viewType != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
					removeDetachedView(scrap, false);
				}
				return;
			}

			if (mViewTypeCount == 1) {
				scrap.onStartTemporaryDetach();
				mCurrentScrap.add(scrap);
			} else {
				scrap.onStartTemporaryDetach();
				mScrapViews[viewType].add(scrap);
			}

			if (mRecyclerListener != null) {
				mRecyclerListener.onMovedToScrapHeap(scrap);
			}
		}

		/**
		 * Move all views remaining in mActiveViews to mScrapViews.
		 */
		void scrapActiveViews() {
			final View[] activeViews = mActiveViews;
			final boolean hasListener = mRecyclerListener != null;
			final boolean multipleScraps = mViewTypeCount > 1;

			ArrayList<View> scrapViews = mCurrentScrap;
			final int count = activeViews.length;
			for (int i = count - 1; i >= 0; i--) {
				final View victim = activeViews[i];
				if (victim != null) {
					int whichScrap = ((TwoWayAbsListView.LayoutParams) victim.getLayoutParams()).viewType;

					activeViews[i] = null;

					if (!shouldRecycleViewType(whichScrap)) {
						// Do not move views that should be ignored
						if (whichScrap != ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
							removeDetachedView(victim, false);
						}
						continue;
					}

					if (multipleScraps) {
						scrapViews = mScrapViews[whichScrap];
					}
					victim.onStartTemporaryDetach();
					scrapViews.add(victim);

					if (hasListener) {
						mRecyclerListener.onMovedToScrapHeap(victim);
					}

					if (ViewDebug.TRACE_RECYCLER) {
						ViewDebug.trace(victim,
								ViewDebug.RecyclerTraceType.MOVE_FROM_ACTIVE_TO_SCRAP_HEAP,
								mFirstActivePosition + i, -1);
					}
				}
			}

			pruneScrapViews();
		}

		/**
		 * Makes sure that the size of mScrapViews does not exceed the size of mActiveViews.
		 * (This can happen if an adapter does not recycle its views).
		 */
		private void pruneScrapViews() {
			final int maxViews = mActiveViews.length;
			final int viewTypeCount = mViewTypeCount;
			final ArrayList<View>[] scrapViews = mScrapViews;
			for (int i = 0; i < viewTypeCount; ++i) {
				final ArrayList<View> scrapPile = scrapViews[i];
				int size = scrapPile.size();
				final int extras = size - maxViews;
				size--;
				for (int j = 0; j < extras; j++) {
					removeDetachedView(scrapPile.remove(size--), false);
				}
			}
		}

		/**
		 * Puts all views in the scrap heap into the supplied list.
		 */
		void reclaimScrapViews(List<View> views) {
			if (mViewTypeCount == 1) {
				views.addAll(mCurrentScrap);
			} else {
				final int viewTypeCount = mViewTypeCount;
				final ArrayList<View>[] scrapViews = mScrapViews;
				for (int i = 0; i < viewTypeCount; ++i) {
					final ArrayList<View> scrapPile = scrapViews[i];
					views.addAll(scrapPile);
				}
			}
		}

		/**
		 * Updates the cache color hint of all known views.
		 *
		 * @param color The new cache color hint.
		 */
		void setCacheColorHint(int color) {
			if (mViewTypeCount == 1) {
				final ArrayList<View> scrap = mCurrentScrap;
				final int scrapCount = scrap.size();
				for (int i = 0; i < scrapCount; i++) {
					scrap.get(i).setDrawingCacheBackgroundColor(color);
				}
			} else {
				final int typeCount = mViewTypeCount;
				for (int i = 0; i < typeCount; i++) {
					final ArrayList<View> scrap = mScrapViews[i];
					final int scrapCount = scrap.size();
					for (int j = 0; j < scrapCount; j++) {
						scrap.get(i).setDrawingCacheBackgroundColor(color);
					}
				}
			}
			// Just in case this is called during a layout pass
			final View[] activeViews = mActiveViews;
			final int count = activeViews.length;
			for (int i = 0; i < count; ++i) {
				final View victim = activeViews[i];
				if (victim != null) {
					victim.setDrawingCacheBackgroundColor(color);
				}
			}
		}
	}





	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//  Touch Handler
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	abstract class TouchHandler {
		/**
		 * Handles scrolling between positions within the list.
		 */
		protected PositionScroller mPositionScroller;

		/**
		 * Handles one frame of a fling
		 */
		protected FlingRunnable mFlingRunnable;

		/**
		 * How far the finger moved before we started scrolling
		 */
		int mMotionCorrection;

		public void onWindowFocusChanged(boolean hasWindowFocus) {

			final int touchMode = isInTouchMode() ? TOUCH_MODE_ON : TOUCH_MODE_OFF;

			if (!hasWindowFocus) {
				setChildrenDrawingCacheEnabled(false);
				if (mFlingRunnable != null) {
					removeCallbacks(mFlingRunnable);
					// let the fling runnable report it's new state which
					// should be idle
					mFlingRunnable.endFling();
					//TODO: this doesn't seem the right way to do this.
					if (getScrollY() != 0) {
						scrollTo(getScrollX(), 0);
						//mScrollY = 0;
						invalidate();
					}
				}
				// Always hide the type filter
				//dismissPopup();

				if (touchMode == TOUCH_MODE_OFF) {
					// Remember the last selected element
					mResurrectToPosition = mSelectedPosition;
				}
			} else {
				//if (mFiltered && !mPopupHidden) {
				// Show the type filter only if a filter is in effect
				//    showPopup();
				//}

				// If we changed touch mode since the last time we had focus
				if (touchMode != mLastTouchMode && mLastTouchMode != TOUCH_MODE_UNKNOWN) {
					// If we come back in trackball mode, we bring the selection back
					if (touchMode == TOUCH_MODE_OFF) {
						// This will trigger a layout
						resurrectSelection();

						// If we come back in touch mode, then we want to hide the selector
					} else {
						hideSelector();
						mLayoutMode = LAYOUT_NORMAL;
						layoutChildren();
					}
				}
			}

			mLastTouchMode = touchMode;
		}


		public boolean startScrollIfNeeded(int delta) {
			// Check if we have moved far enough that it looks more like a
			// scroll than a tap
			final int distance = Math.abs(delta);
			if (distance > mTouchSlop) {
				createScrollingCache();
				mTouchMode = TOUCH_MODE_SCROLL;
				mMotionCorrection = delta;
				final Handler handler = getHandler();
				// Handler should not be null unless the TwoWayAbsListView is not attached to a
				// window, which would make it very hard to scroll it... but the monkeys
				// say it's possible.
				if (handler != null) {
					handler.removeCallbacks(mPendingCheckForLongPress);
				}
				setPressed(false);
				View motionView = getChildAt(mMotionPosition - mFirstPosition);
				if (motionView != null) {
					motionView.setPressed(false);
				}
				reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
				// Time to start stealing events! Once we've stolen them, don't let anyone
				// steal from us
				requestDisallowInterceptTouchEvent(true);
				return true;
			}

			return false;
		}


		public void onTouchModeChanged(boolean isInTouchMode) {
			if (isInTouchMode) {
				// Get rid of the selection when we enter touch mode
				hideSelector();
				// Layout, but only if we already have done so previously.
				// (Otherwise may clobber a LAYOUT_SYNC layout that was requested to restore
				// state.)
				if (getHeight() > 0 && getChildCount() > 0) {
					// We do not lose focus initiating a touch (since TwoWayAbsListView is focusable in
					// touch mode). Force an initial layout to get rid of the selection.
					layoutChildren();
				}
			}
		}

		/**
		 * Fires an "on scroll state changed" event to the registered
		 * {@link com.jess.ui.TwoWayAbsListView.OnScrollListener}, if any. The state change
		 * is fired only if the specified state is different from the previously known state.
		 *
		 * @param newState The new scroll state.
		 */
		void reportScrollStateChange(int newState) {
			if (newState != mLastScrollState) {
				if (mOnScrollListener != null) {
					mOnScrollListener.onScrollStateChanged(TwoWayAbsListView.this, newState);
					mLastScrollState = newState;
				}
			}
		}

		/**
		 * Smoothly scroll to the specified adapter position. The view will
		 * scroll such that the indicated position is displayed.
		 * @param position Scroll to this adapter position.
		 */
		public void smoothScrollToPosition(int position) {
			if (mPositionScroller == null) {
				mPositionScroller = getPositionScroller();
			}
			mPositionScroller.start(position);
		}

		/**
		 * Smoothly scroll to the specified adapter position. The view will
		 * scroll such that the indicated position is displayed, but it will
		 * stop early if scrolling further would scroll boundPosition out of
		 * view.
		 * @param position Scroll to this adapter position.
		 * @param boundPosition Do not scroll if it would move this adapter
		 *          position out of view.
		 */
		public void smoothScrollToPosition(int position, int boundPosition) {
			if (mPositionScroller == null) {
				mPositionScroller = getPositionScroller();
			}
			mPositionScroller.start(position, boundPosition);
		}

		/**
		 * Smoothly scroll by distance pixels over duration milliseconds.
		 * @param distance Distance to scroll in pixels.
		 * @param duration Duration of the scroll animation in milliseconds.
		 */
		public void smoothScrollBy(int distance, int duration) {
			if (mFlingRunnable == null) {
				mFlingRunnable = getFlingRunnable();
			} else {
				mFlingRunnable.endFling();
			}
			mFlingRunnable.startScroll(distance, duration);
		}

		protected void createScrollingCache() {
			if (mScrollingCacheEnabled && !mCachingStarted) {
				setChildrenDrawnWithCacheEnabled(true);
				setChildrenDrawingCacheEnabled(true);
				mCachingStarted = true;
			}
		}

		protected void clearScrollingCache() {
			if (mClearScrollingCache == null) {
				mClearScrollingCache = new Runnable() {
					public void run() {
						if (mCachingStarted) {
							mCachingStarted = false;
							setChildrenDrawnWithCacheEnabled(false);
							if ((TwoWayAbsListView.this.getPersistentDrawingCache() & PERSISTENT_SCROLLING_CACHE) == 0) {
								setChildrenDrawingCacheEnabled(false);
							}
							if (!isAlwaysDrawnWithCacheEnabled()) {
								invalidate();
							}
						}
					}
				};
			}
			post(mClearScrollingCache);
		}

		/**
		 * Track a motion scroll
		 *
		 * @param delta Amount to offset mMotionView. This is the accumulated delta since the motion
		 *        began. Positive numbers mean the user's finger is moving down or right on the screen.
		 * @param incrementalDelta Change in delta from the previous event.
		 * @return true if we're already at the beginning/end of the list and have nothing to do.
		 */
		abstract boolean trackMotionScroll(int delta, int incrementalDelta);

		/**
		 * Attempt to bring the selection back if the user is switching from touch
		 * to trackball mode
		 * @return Whether selection was set to something.
		 */
		abstract boolean resurrectSelection();


		public abstract boolean onTouchEvent(MotionEvent ev);


		public abstract boolean onInterceptTouchEvent(MotionEvent ev);

		protected abstract PositionScroller getPositionScroller();

		protected abstract FlingRunnable getFlingRunnable();

		/**
		 * Responsible for fling behavior. Use {@link #start(int)} to
		 * initiate a fling. Each frame of the fling is handled in {@link #run()}.
		 * A FlingRunnable will keep re-posting itself until the fling is done.
		 *
		 */
		protected abstract class FlingRunnable implements Runnable {
			/**
			 * Tracks the decay of a fling scroll
			 */
			protected final Scroller mScroller;

			FlingRunnable() {
				mScroller = new Scroller(getContext());
			}

			abstract void start(int initialVelocity);

			abstract void startScroll(int distance, int duration);

			protected void endFling() {
				mTouchMode = TOUCH_MODE_REST;

				reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
				clearScrollingCache();

				removeCallbacks(this);

				if (mPositionScroller != null) {
					removeCallbacks(mPositionScroller);
				}
			}

			public abstract void run();
		}

		abstract class PositionScroller implements Runnable {
			protected static final int SCROLL_DURATION = 400;

			protected static final int MOVE_DOWN_POS = 1;
			protected static final int MOVE_UP_POS = 2;
			protected static final int MOVE_DOWN_BOUND = 3;
			protected static final int MOVE_UP_BOUND = 4;

			protected boolean mVertical;
			protected int mMode;
			protected int mTargetPos;
			protected int mBoundPos;
			protected int mLastSeenPos;
			protected int mScrollDuration;
			protected final int mExtraScroll;

			PositionScroller() {
				mExtraScroll = ViewConfiguration.get(mContext).getScaledFadingEdgeLength();
			}

			void start(int position) {
				final int firstPos = mFirstPosition;
				final int lastPos = firstPos + getChildCount() - 1;

				int viewTravelCount = 0;
				if (position <= firstPos) {
					viewTravelCount = firstPos - position + 1;
					mMode = MOVE_UP_POS;
				} else if (position >= lastPos) {
					viewTravelCount = position - lastPos + 1;
					mMode = MOVE_DOWN_POS;
				} else {
					// Already on screen, nothing to do
					return;
				}

				if (viewTravelCount > 0) {
					mScrollDuration = SCROLL_DURATION / viewTravelCount;
				} else {
					mScrollDuration = SCROLL_DURATION;
				}
				mTargetPos = position;
				mBoundPos = INVALID_POSITION;
				mLastSeenPos = INVALID_POSITION;

				post(this);
			}

			void start(int position, int boundPosition) {
				if (boundPosition == INVALID_POSITION) {
					start(position);
					return;
				}

				final int firstPos = mFirstPosition;
				final int lastPos = firstPos + getChildCount() - 1;

				int viewTravelCount = 0;
				if (position <= firstPos) {
					final int boundPosFromLast = lastPos - boundPosition;
					if (boundPosFromLast < 1) {
						// Moving would shift our bound position off the screen. Abort.
						return;
					}

					final int posTravel = firstPos - position + 1;
					final int boundTravel = boundPosFromLast - 1;
					if (boundTravel < posTravel) {
						viewTravelCount = boundTravel;
						mMode = MOVE_UP_BOUND;
					} else {
						viewTravelCount = posTravel;
						mMode = MOVE_UP_POS;
					}
				} else if (position >= lastPos) {
					final int boundPosFromFirst = boundPosition - firstPos;
					if (boundPosFromFirst < 1) {
						// Moving would shift our bound position off the screen. Abort.
						return;
					}

					final int posTravel = position - lastPos + 1;
					final int boundTravel = boundPosFromFirst - 1;
					if (boundTravel < posTravel) {
						viewTravelCount = boundTravel;
						mMode = MOVE_DOWN_BOUND;
					} else {
						viewTravelCount = posTravel;
						mMode = MOVE_DOWN_POS;
					}
				} else {
					// Already on screen, nothing to do
					return;
				}

				if (viewTravelCount > 0) {
					mScrollDuration = SCROLL_DURATION / viewTravelCount;
				} else {
					mScrollDuration = SCROLL_DURATION;
				}
				mTargetPos = position;
				mBoundPos = boundPosition;
				mLastSeenPos = INVALID_POSITION;

				post(this);
			}

			void stop() {
				removeCallbacks(this);
			}

			public abstract void run();
		}


	}






	////////////////////////////////////////////////////////////////////////////////////////
	//  Vertical Touch Handler
	////////////////////////////////////////////////////////////////////////////////////////

	class VerticalTouchHandler extends TouchHandler {
		/**
		 * The offset to the top of the mMotionPosition view when the down motion event was received
		 */
		int mMotionViewOriginalTop;

		/**
		 * Y value from on the previous motion event (if any)
		 */
		int mLastY;

		/**
		 * The desired offset to the top of the mMotionPosition view after a scroll
		 */
		int mMotionViewNewTop;

		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			if (!isEnabled()) {
				// A disabled view that is clickable still consumes the touch
				// events, it just doesn't respond to them.
				return isClickable() || isLongClickable();
			}

			/*
            if (mFastScroller != null) {
                boolean intercepted = mFastScroller.onTouchEvent(ev);
                if (intercepted) {
                    return true;
                }
            }*/

			final int action = ev.getAction();

			View v;
			int deltaY;

			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);

			switch (action) {
			case MotionEvent.ACTION_DOWN: {
				final int x = (int) ev.getX();
				final int y = (int) ev.getY();
				int motionPosition = pointToPosition(x, y);
				if (!mDataChanged) {
					if ((mTouchMode != TOUCH_MODE_FLING) && (motionPosition >= 0)
							&& (getAdapter().isEnabled(motionPosition))) {
						// User clicked on an actual view (and was not stopping a fling). It might be a
						// click or a scroll. Assume it is a click until proven otherwise
						mTouchMode = TOUCH_MODE_DOWN;
						// FIXME Debounce
						if (mPendingCheckForTap == null) {
							mPendingCheckForTap = new CheckForTap();
						}
						postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
					} else {
						if (ev.getEdgeFlags() != 0 && motionPosition < 0) {
							// If we couldn't find a view to click on, but the down event was touching
							// the edge, we will bail out and try again. This allows the edge correcting
							// code in ViewRoot to try to find a nearby view to select
							return false;
						}

						if (mTouchMode == TOUCH_MODE_FLING) {
							// Stopped a fling. It is a scroll.
							createScrollingCache();
							mTouchMode = TOUCH_MODE_SCROLL;
							mMotionCorrection = 0;
							motionPosition = findMotionRowY(y);
							reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
						}
					}
				}

				if (motionPosition >= 0) {
					// Remember where the motion event started
					v = getChildAt(motionPosition - mFirstPosition);
					mMotionViewOriginalTop = v.getTop();
				}
				mMotionX = x;
				mMotionY = y;
				mMotionPosition = motionPosition;
				mLastY = Integer.MIN_VALUE;
				break;
			}

			case MotionEvent.ACTION_MOVE: {
				final int y = (int) ev.getY();
				deltaY = y - mMotionY;
				switch (mTouchMode) {
				case TOUCH_MODE_DOWN:
				case TOUCH_MODE_TAP:
				case TOUCH_MODE_DONE_WAITING:
					// Check if we have moved far enough that it looks more like a
					// scroll than a tap
					startScrollIfNeeded(deltaY);
					break;
				case TOUCH_MODE_SCROLL:
					if (PROFILE_SCROLLING) {
						if (!mScrollProfilingStarted) {
							Debug.startMethodTracing("JessAbsListViewScroll");
							mScrollProfilingStarted = true;
						}
					}

					if (y != mLastY) {
						deltaY -= mMotionCorrection;
						int incrementalDeltaY = mLastY != Integer.MIN_VALUE ? y - mLastY : deltaY;

						// No need to do all this work if we're not going to move anyway
						boolean atEdge = false;
						if (incrementalDeltaY != 0) {
							atEdge = trackMotionScroll(deltaY, incrementalDeltaY);
						}

						// Check to see if we have bumped into the scroll limit
						if (atEdge && getChildCount() > 0) {
							// Treat this like we're starting a new scroll from the current
							// position. This will let the user start scrolling back into
							// content immediately rather than needing to scroll back to the
							// point where they hit the limit first.
							int motionPosition = findMotionRowY(y);
							if (motionPosition >= 0) {
								final View motionView = getChildAt(motionPosition - mFirstPosition);
								mMotionViewOriginalTop = motionView.getTop();
							}
							mMotionY = y;
							mMotionPosition = motionPosition;
							invalidate();
						}
						mLastY = y;
					}
					break;
				}

				break;
			}

			case MotionEvent.ACTION_UP: {
				switch (mTouchMode) {
				case TOUCH_MODE_DOWN:
				case TOUCH_MODE_TAP:
				case TOUCH_MODE_DONE_WAITING:
					final int motionPosition = mMotionPosition;
					final View child = getChildAt(motionPosition - mFirstPosition);
					if (child != null && !child.hasFocusable()) {
						if (mTouchMode != TOUCH_MODE_DOWN) {
							child.setPressed(false);
						}

						if (mPerformClick == null) {
							mPerformClick = new PerformClick();
						}

						final TwoWayAbsListView.PerformClick performClick = mPerformClick;
						performClick.mChild = child;
						performClick.mClickMotionPosition = motionPosition;
						performClick.rememberWindowAttachCount();

						mResurrectToPosition = motionPosition;

						if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
							final Handler handler = getHandler();
							if (handler != null) {
								handler.removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ?
										mPendingCheckForTap : mPendingCheckForLongPress);
							}
							mLayoutMode = LAYOUT_NORMAL;
							if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
								mTouchMode = TOUCH_MODE_TAP;
								setSelectedPositionInt(mMotionPosition);
								layoutChildren();
								child.setPressed(true);
								positionSelector(child);
								setPressed(true);
								if (mSelector != null) {
									Drawable d = mSelector.getCurrent();
									if (d != null && d instanceof TransitionDrawable) {
										((TransitionDrawable) d).resetTransition();
									}
								}
								postDelayed(new Runnable() {
									public void run() {
										child.setPressed(false);
										setPressed(false);
										if (!mDataChanged) {
											post(performClick);
										}
										mTouchMode = TOUCH_MODE_REST;
									}
								}, ViewConfiguration.getPressedStateDuration());
							} else {
								mTouchMode = TOUCH_MODE_REST;
							}
							return true;
						} else if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
							post(performClick);
						}
					}
					mTouchMode = TOUCH_MODE_REST;
					break;
				case TOUCH_MODE_SCROLL:
					final int childCount = getChildCount();
					if (childCount > 0) {
						if (mFirstPosition == 0 && getChildAt(0).getTop() >= mListPadding.top &&
								mFirstPosition + childCount < mItemCount &&
								getChildAt(childCount - 1).getBottom() <=
									getHeight() - mListPadding.bottom) {
							mTouchMode = TOUCH_MODE_REST;
							reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
						} else {
							final VelocityTracker velocityTracker = mVelocityTracker;
							velocityTracker.computeCurrentVelocity(1000);
							final int initialVelocity = (int) velocityTracker.getYVelocity();

							if (Math.abs(initialVelocity) > mMinimumVelocity) {
								if (mFlingRunnable == null) {
									mFlingRunnable = new VerticalFlingRunnable();
								}
								reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);

								mFlingRunnable.start(-initialVelocity);
							} else {
								mTouchMode = TOUCH_MODE_REST;
								reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
							}
						}
					} else {
						mTouchMode = TOUCH_MODE_REST;
						reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
					}
					break;
				}

				setPressed(false);

				// Need to redraw since we probably aren't drawing the selector anymore
				invalidate();

				final Handler handler = getHandler();
				if (handler != null) {
					handler.removeCallbacks(mPendingCheckForLongPress);
				}

				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}

				mActivePointerId = INVALID_POINTER;

				if (PROFILE_SCROLLING) {
					if (mScrollProfilingStarted) {
						Debug.stopMethodTracing();
						mScrollProfilingStarted = false;
					}
				}
				break;
			}

			case MotionEvent.ACTION_CANCEL: {
				mTouchMode = TOUCH_MODE_REST;
				setPressed(false);
				View motionView = TwoWayAbsListView.this.getChildAt(mMotionPosition - mFirstPosition);
				if (motionView != null) {
					motionView.setPressed(false);
				}
				clearScrollingCache();

				final Handler handler = getHandler();
				if (handler != null) {
					handler.removeCallbacks(mPendingCheckForLongPress);
				}

				if (mVelocityTracker != null) {
					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}

				mActivePointerId = INVALID_POINTER;
				break;
			}

			}

			return true;
		}



		@Override
		public boolean onInterceptTouchEvent(MotionEvent ev) {
			int action = ev.getAction();
			View v;

			/*
            if (mFastScroller != null) {
                boolean intercepted = mFastScroller.onInterceptTouchEvent(ev);
                if (intercepted) {
                    return true;
                }
            }*/

			switch (action) {
				case MotionEvent.ACTION_DOWN: {
					int touchMode = mTouchMode;
	
					final int x = (int) ev.getX();
					final int y = (int) ev.getY();
					
					int motionPosition = findMotionRowY(y);
					if (touchMode != TOUCH_MODE_FLING && motionPosition >= 0) {
						// User clicked on an actual view (and was not stopping a fling).
						// Remember where the motion event started
						v = getChildAt(motionPosition - mFirstPosition);
						mMotionViewOriginalTop = v.getTop();
						mMotionX = x;
						mMotionY = y;
						mMotionPosition = motionPosition;
						mTouchMode = TOUCH_MODE_DOWN;
						clearScrollingCache();
					}
					mLastY = Integer.MIN_VALUE;
					if (touchMode == TOUCH_MODE_FLING) {
						return true;
					}
					break;
				}
	
				case MotionEvent.ACTION_MOVE: {
					switch (mTouchMode) {
					case TOUCH_MODE_DOWN:
						final int y = (int) ev.getY();
						if (startScrollIfNeeded(y - mMotionY)) {
							return true;
						}
						break;
					}
					break;
				}
	
				case MotionEvent.ACTION_UP: {
					mTouchMode = TOUCH_MODE_REST;
					mActivePointerId = INVALID_POINTER;
					reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
					break;
				}
			}

			return false;
		}

		/**
		 * Track a motion scroll
		 *
		 * @param deltaY Amount to offset mMotionView. This is the accumulated delta since the motion
		 *        began. Positive numbers mean the user's finger is moving down the screen.
		 * @param incrementalDeltaY Change in deltaY from the previous event.
		 * @return true if we're already at the beginning/end of the list and have nothing to do.
		 */
		@Override
		boolean trackMotionScroll(int deltaY, int incrementalDeltaY) {
			if (DEBUG) Log.i(TAG, "trackMotionScroll() - deltaY: " + deltaY + " incrDeltaY: " + incrementalDeltaY);
			final int childCount = getChildCount();
			if (childCount == 0) {
				return true;
			}

			final int firstTop = getChildAt(0).getTop();
			final int lastBottom = getChildAt(childCount - 1).getBottom();

			final Rect listPadding = mListPadding;

			// FIXME account for grid vertical spacing too?
			final int spaceAbove = listPadding.top - firstTop;
			final int end = getHeight() - listPadding.bottom;
			final int spaceBelow = lastBottom - end;

			final int height = getHeight() - getPaddingBottom() - getPaddingTop();
			if (deltaY < 0) {
				deltaY = Math.max(-(height - 1), deltaY);
			} else {
				deltaY = Math.min(height - 1, deltaY);
			}

			if (incrementalDeltaY < 0) {
				incrementalDeltaY = Math.max(-(height - 1), incrementalDeltaY);
			} else {
				incrementalDeltaY = Math.min(height - 1, incrementalDeltaY);
			}

			final int firstPosition = mFirstPosition;

			if (firstPosition == 0 && firstTop >= listPadding.top && deltaY >= 0) {
				// Don't need to move views down if the top of the first position
				// is already visible
				return true;
			}

			if (firstPosition + childCount == mItemCount && lastBottom <= end && deltaY <= 0) {
				// Don't need to move views up if the bottom of the last position
				// is already visible
				return true;
			}

			final boolean down = incrementalDeltaY < 0;

			final boolean inTouchMode = isInTouchMode();
			if (inTouchMode) {
				hideSelector();
			}

			final int headerViewsCount = getHeaderViewsCount();
			final int footerViewsStart = mItemCount - getFooterViewsCount();

			int start = 0;
			int count = 0;

			if (down) {
				final int top = listPadding.top - incrementalDeltaY;
				for (int i = 0; i < childCount; i++) {
					final View child = getChildAt(i);
					if (child.getBottom() >= top) {
						break;
					} else {
						count++;
						int position = firstPosition + i;
						if (position >= headerViewsCount && position < footerViewsStart) {
							mRecycler.addScrapView(child);

							if (ViewDebug.TRACE_RECYCLER) {
								ViewDebug.trace(child,
										ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
										firstPosition + i, -1);
							}
						}
					}
				}
			} else {
				final int bottom = getHeight() - listPadding.bottom - incrementalDeltaY;
				for (int i = childCount - 1; i >= 0; i--) {
					final View child = getChildAt(i);
					if (child.getTop() <= bottom) {
						break;
					} else {
						start = i;
						count++;
						int position = firstPosition + i;
						if (position >= headerViewsCount && position < footerViewsStart) {
							mRecycler.addScrapView(child);

							if (ViewDebug.TRACE_RECYCLER) {
								ViewDebug.trace(child,
										ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
										firstPosition + i, -1);
							}
						}
					}
				}
			}

			mMotionViewNewTop = mMotionViewOriginalTop + deltaY;

			mBlockLayoutRequests = true;

			if (count > 0) {
				detachViewsFromParent(start, count);
			}
			offsetChildrenTopAndBottom(incrementalDeltaY);

			if (down) {
				mFirstPosition += count;
			}

			invalidate();

			final int absIncrementalDeltaY = Math.abs(incrementalDeltaY);
			if (spaceAbove < absIncrementalDeltaY || spaceBelow < absIncrementalDeltaY) {
				fillGap(down);
			}

			if (!inTouchMode && mSelectedPosition != INVALID_POSITION) {
				final int childIndex = mSelectedPosition - mFirstPosition;
				if (childIndex >= 0 && childIndex < getChildCount()) {
					positionSelector(getChildAt(childIndex));
				}
			}

			mBlockLayoutRequests = false;

			invokeOnItemScrollListener();
			//awakenScrollBars();

			return false;
		}

		/**
		 * Attempt to bring the selection back if the user is switching from touch
		 * to trackball mode
		 * @return Whether selection was set to something.
		 */
		@Override
		boolean resurrectSelection() {
			final int childCount = getChildCount();

			if (childCount <= 0) {
				return false;
			}

			int selectedTop = 0;
			int selectedPos;
			int childrenTop = mListPadding.top;
			int childrenBottom = getBottom() - getTop() - mListPadding.bottom;
			final int firstPosition = mFirstPosition;
			final int toPosition = mResurrectToPosition;
			boolean down = true;

			if (toPosition >= firstPosition && toPosition < firstPosition + childCount) {
				selectedPos = toPosition;

				final View selected = getChildAt(selectedPos - mFirstPosition);
				selectedTop = selected.getTop();
				int selectedBottom = selected.getBottom();

				// We are scrolled, don't get in the fade
				if (selectedTop < childrenTop) {
					selectedTop = childrenTop + getVerticalFadingEdgeLength();
				} else if (selectedBottom > childrenBottom) {
					selectedTop = childrenBottom - selected.getMeasuredHeight()
					- getVerticalFadingEdgeLength();
				}
			} else {
				if (toPosition < firstPosition) {
					// Default to selecting whatever is first
					selectedPos = firstPosition;
					for (int i = 0; i < childCount; i++) {
						final View v = getChildAt(i);
						final int top = v.getTop();

						if (i == 0) {
							// Remember the position of the first item
							selectedTop = top;
							// See if we are scrolled at all
							if (firstPosition > 0 || top < childrenTop) {
								// If we are scrolled, don't select anything that is
								// in the fade region
								childrenTop += getVerticalFadingEdgeLength();
							}
						}
						if (top >= childrenTop) {
							// Found a view whose top is fully visisble
							selectedPos = firstPosition + i;
							selectedTop = top;
							break;
						}
					}
				} else {
					final int itemCount = mItemCount;
					down = false;
					selectedPos = firstPosition + childCount - 1;

					for (int i = childCount - 1; i >= 0; i--) {
						final View v = getChildAt(i);
						final int top = v.getTop();
						final int bottom = v.getBottom();

						if (i == childCount - 1) {
							selectedTop = top;
							if (firstPosition + childCount < itemCount || bottom > childrenBottom) {
								childrenBottom -= getVerticalFadingEdgeLength();
							}
						}

						if (bottom <= childrenBottom) {
							selectedPos = firstPosition + i;
							selectedTop = top;
							break;
						}
					}
				}
			}

			mResurrectToPosition = INVALID_POSITION;
			removeCallbacks(mFlingRunnable);
			mTouchMode = TOUCH_MODE_REST;
			clearScrollingCache();
			mSpecificTop = selectedTop;
			selectedPos = lookForSelectablePosition(selectedPos, down);
			if (selectedPos >= firstPosition && selectedPos <= getLastVisiblePosition()) {
				mLayoutMode = LAYOUT_SPECIFIC;
				setSelectionInt(selectedPos);
				invokeOnItemScrollListener();
			} else {
				selectedPos = INVALID_POSITION;
			}
			reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);

			return selectedPos >= 0;
		}



		@Override
		protected PositionScroller getPositionScroller() {
			return new VerticalPositionScroller();
		}

		@Override
		protected FlingRunnable getFlingRunnable() {
			return new VerticalFlingRunnable();
		}


		/**
		 * Responsible for fling behavior. Use {@link #start(int)} to
		 * initiate a fling. Each frame of the fling is handled in {@link #run()}.
		 * A FlingRunnable will keep re-posting itself until the fling is done.
		 *
		 */
		private class VerticalFlingRunnable extends FlingRunnable {
			/**
			 * Y value reported by mScroller on the previous fling
			 */
			protected int mLastFlingY;

			@Override
			void start(int initialVelocity) {
				int initialY = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
				mLastFlingY = initialY;
				mScroller.fling(0, initialY, 0, initialVelocity,
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
				mTouchMode = TOUCH_MODE_FLING;
				post(this);

				if (PROFILE_FLINGING) {
					if (!mFlingProfilingStarted) {
						Debug.startMethodTracing("AbsListViewFling");
						mFlingProfilingStarted = true;
					}
				}
			}

			@Override
			void startScroll(int distance, int duration) {
				int initialY = distance < 0 ? Integer.MAX_VALUE : 0;
				mLastFlingY = initialY;
				mScroller.startScroll(0, initialY, 0, distance, duration);
				mTouchMode = TOUCH_MODE_FLING;
				post(this);
			}

			@Override
			public void run() {
				switch (mTouchMode) {
				default:
					return;

				case TOUCH_MODE_FLING: {
					if (mItemCount == 0 || getChildCount() == 0) {
						endFling();
						return;
					}

					final Scroller scroller = mScroller;
					boolean more = scroller.computeScrollOffset();
					final int y = scroller.getCurrY();

					// Flip sign to convert finger direction to list items direction
					// (e.g. finger moving down means list is moving towards the top)
					int delta = mLastFlingY - y;

					// Pretend that each frame of a fling scroll is a touch scroll
					if (delta > 0) {
						// List is moving towards the top. Use first view as mMotionPosition
						mMotionPosition = mFirstPosition;
						final View firstView = getChildAt(0);
						mMotionViewOriginalTop = firstView.getTop();

						// Don't fling more than 1 screen
						delta = Math.min(getHeight() - getPaddingBottom() - getPaddingTop() - 1, delta);
					} else {
						// List is moving towards the bottom. Use last view as mMotionPosition
						int offsetToLast = getChildCount() - 1;
						mMotionPosition = mFirstPosition + offsetToLast;

						final View lastView = getChildAt(offsetToLast);
						mMotionViewOriginalTop = lastView.getTop();

						// Don't fling more than 1 screen
						delta = Math.max(-(getHeight() - getPaddingBottom() - getPaddingTop() - 1), delta);
					}

					final boolean atEnd = trackMotionScroll(delta, delta);

					if (more && !atEnd) {
						invalidate();
						mLastFlingY = y;
						post(this);
					} else {
						endFling();

						if (PROFILE_FLINGING) {
							if (mFlingProfilingStarted) {
								Debug.stopMethodTracing();
								mFlingProfilingStarted = false;
							}
						}
					}
					break;
				}
				}

			}
		}


		class VerticalPositionScroller extends PositionScroller {
			@Override
			public void run() {
				final int listHeight = getHeight();
				final int firstPos = mFirstPosition;

				switch (mMode) {
				case MOVE_DOWN_POS: {
					final int lastViewIndex = getChildCount() - 1;
					final int lastPos = firstPos + lastViewIndex;

					if (lastViewIndex < 0) {
						return;
					}

					if (lastPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View lastView = getChildAt(lastViewIndex);
					final int lastViewHeight = lastView.getHeight();
					final int lastViewTop = lastView.getTop();
					final int lastViewPixelsShowing = listHeight - lastViewTop;
					final int extraScroll = lastPos < mItemCount - 1 ? mExtraScroll : mListPadding.bottom;

					smoothScrollBy(lastViewHeight - lastViewPixelsShowing + extraScroll,
							mScrollDuration);

					mLastSeenPos = lastPos;
					if (lastPos < mTargetPos) {
						post(this);
					}
					break;
				}

				case MOVE_DOWN_BOUND: {
					final int nextViewIndex = 1;
					final int childCount = getChildCount();

					if (firstPos == mBoundPos || childCount <= nextViewIndex
							|| firstPos + childCount >= mItemCount) {
						return;
					}
					final int nextPos = firstPos + nextViewIndex;

					if (nextPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View nextView = getChildAt(nextViewIndex);
					final int nextViewHeight = nextView.getHeight();
					final int nextViewTop = nextView.getTop();
					final int extraScroll = mExtraScroll;
					if (nextPos < mBoundPos) {
						smoothScrollBy(Math.max(0, nextViewHeight + nextViewTop - extraScroll),
								mScrollDuration);

						mLastSeenPos = nextPos;

						post(this);
					} else  {
						if (nextViewTop > extraScroll) {
							smoothScrollBy(nextViewTop - extraScroll, mScrollDuration);
						}
					}
					break;
				}

				case MOVE_UP_POS: {
					if (firstPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View firstView = getChildAt(0);
					if (firstView == null) {
						return;
					}
					final int firstViewTop = firstView.getTop();
					final int extraScroll = firstPos > 0 ? mExtraScroll : mListPadding.top;

					smoothScrollBy(firstViewTop - extraScroll, mScrollDuration);

					mLastSeenPos = firstPos;

					if (firstPos > mTargetPos) {
						post(this);
					}
					break;
				}

				case MOVE_UP_BOUND: {
					final int lastViewIndex = getChildCount() - 2;
					if (lastViewIndex < 0) {
						return;
					}
					final int lastPos = firstPos + lastViewIndex;

					if (lastPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View lastView = getChildAt(lastViewIndex);
					final int lastViewHeight = lastView.getHeight();
					final int lastViewTop = lastView.getTop();
					final int lastViewPixelsShowing = listHeight - lastViewTop;
					mLastSeenPos = lastPos;
					if (lastPos > mBoundPos) {
						smoothScrollBy(-(lastViewPixelsShowing - mExtraScroll), mScrollDuration);
						post(this);
					} else {
						final int bottom = listHeight - mExtraScroll;
						final int lastViewBottom = lastViewTop + lastViewHeight;
						if (bottom > lastViewBottom) {
							smoothScrollBy(-(bottom - lastViewBottom), mScrollDuration);
						}
					}
					break;
				}

				default:
					break;
				}
			}
		}
	}






	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//  Horizontal Touch Handler
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	class HorizontalTouchHandler extends TouchHandler {
		/**
		 * The offset to the top of the mMotionPosition view when the down motion event was received
		 */
		int mMotionViewOriginalLeft;

		/**
		 * X value from on the previous motion event (if any)
		 */
		int mLastX;

		/**
		 * The desired offset to the top of the mMotionPosition view after a scroll
		 */
		int mMotionViewNewLeft;



		@Override
		protected FlingRunnable getFlingRunnable() {
			return new HorizontalFlingRunnable();
		}


		@Override
		protected PositionScroller getPositionScroller() {
			return new HorizontalPositionScroller();
		}


		@Override
		public boolean onInterceptTouchEvent(MotionEvent ev) {
			int action = ev.getAction();
			View v;

			/*
            if (mFastScroller != null) {
                boolean intercepted = mFastScroller.onInterceptTouchEvent(ev);
                if (intercepted) {
                    return true;
                }
            }*/

			switch (action) {
				case MotionEvent.ACTION_DOWN: {
					int touchMode = mTouchMode;
	
					final int x = (int) ev.getX();
					final int y = (int) ev.getY();
					
					int motionPosition = findMotionRowX(x);
					if (touchMode != TOUCH_MODE_FLING && motionPosition >= 0) {
						// User clicked on an actual view (and was not stopping a fling).
						// Remember where the motion event started
						v = getChildAt(motionPosition - mFirstPosition);
						mMotionViewOriginalLeft = v.getLeft();
						mMotionX = x;
						mMotionY = y;
						mMotionPosition = motionPosition;
						mTouchMode = TOUCH_MODE_DOWN;
						clearScrollingCache();
					}
					mLastX = Integer.MIN_VALUE;
					if (touchMode == TOUCH_MODE_FLING) {
						return true;
					}
					break;
				}
	
				case MotionEvent.ACTION_MOVE: {
					switch (mTouchMode) {
					case TOUCH_MODE_DOWN:
						final int x = (int) ev.getX();
						if (startScrollIfNeeded(x - mMotionX)) {
							return true;
						}
						break;
					}
					break;
				}
	
				case MotionEvent.ACTION_UP: {
					mTouchMode = TOUCH_MODE_REST;
					mActivePointerId = INVALID_POINTER;
					reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
					break;
				}

			}

			return false;
		}


		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			if (!isEnabled()) {
				// A disabled view that is clickable still consumes the touch
				// events, it just doesn't respond to them.
				return isClickable() || isLongClickable();
			}

			/*
            if (mFastScroller != null) {
                boolean intercepted = mFastScroller.onTouchEvent(ev);
                if (intercepted) {
                    return true;
                }
            }*/

			final int action = ev.getAction();

			View v;
			int deltaX;

			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);

			switch (action) {
				case MotionEvent.ACTION_DOWN: {
					final int x = (int) ev.getX();
					final int y = (int) ev.getY();
					int motionPosition = pointToPosition(x, y);
					if (!mDataChanged) {
						if ((mTouchMode != TOUCH_MODE_FLING) && (motionPosition >= 0)
								&& (getAdapter().isEnabled(motionPosition))) {
							// User clicked on an actual view (and was not stopping a fling). It might be a
							// click or a scroll. Assume it is a click until proven otherwise
							mTouchMode = TOUCH_MODE_DOWN;
							// FIXME Debounce
							if (mPendingCheckForTap == null) {
								mPendingCheckForTap = new CheckForTap();
							}
							postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
						} else {
							if (ev.getEdgeFlags() != 0 && motionPosition < 0) {
								// If we couldn't find a view to click on, but the down event was touching
								// the edge, we will bail out and try again. This allows the edge correcting
								// code in ViewRoot to try to find a nearby view to select
								return false;
							}
	
							if (mTouchMode == TOUCH_MODE_FLING) {
								// Stopped a fling. It is a scroll.
								createScrollingCache();
								mTouchMode = TOUCH_MODE_SCROLL;
								mMotionCorrection = 0;
								motionPosition = findMotionRowX(x);
								reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
							}
						}
					}
	
					if (motionPosition >= 0) {
						// Remember where the motion event started
						v = getChildAt(motionPosition - mFirstPosition);
						mMotionViewOriginalLeft = v.getLeft();
					}
					mMotionX = x;
					mMotionY = y;
					mMotionPosition = motionPosition;
					mLastX = Integer.MIN_VALUE;
					break;
				}
	
				case MotionEvent.ACTION_MOVE: {
					final int x = (int) ev.getX();
					deltaX = x - mMotionX;
					switch (mTouchMode) {
					case TOUCH_MODE_DOWN:
					case TOUCH_MODE_TAP:
					case TOUCH_MODE_DONE_WAITING:
						// Check if we have moved far enough that it looks more like a
						// scroll than a tap
						startScrollIfNeeded(deltaX);
						break;
					case TOUCH_MODE_SCROLL:
						if (PROFILE_SCROLLING) {
							if (!mScrollProfilingStarted) {
								Debug.startMethodTracing("JessAbsListViewScroll");
								mScrollProfilingStarted = true;
							}
						}
	
						if (x != mLastX) {
							deltaX -= mMotionCorrection;
							int incrementalDeltaX = mLastX != Integer.MIN_VALUE ? x - mLastX : deltaX;
	
							// No need to do all this work if we're not going to move anyway
							boolean atEdge = false;
							if (incrementalDeltaX != 0) {
								atEdge = trackMotionScroll(deltaX, incrementalDeltaX);
							}
	
							// Check to see if we have bumped into the scroll limit
							if (atEdge && getChildCount() > 0) {
								// Treat this like we're starting a new scroll from the current
								// position. This will let the user start scrolling back into
								// content immediately rather than needing to scroll back to the
								// point where they hit the limit first.
								int motionPosition = findMotionRowX(x);
								if (motionPosition >= 0) {
									final View motionView = getChildAt(motionPosition - mFirstPosition);
									mMotionViewOriginalLeft = motionView.getLeft();
								}
								mMotionX = x;
								mMotionPosition = motionPosition;
								invalidate();
							}
							mLastX = x;
						}
						break;
					}
	
					break;
				}
	
				case MotionEvent.ACTION_UP: {
					switch (mTouchMode) {
					case TOUCH_MODE_DOWN:
					case TOUCH_MODE_TAP:
					case TOUCH_MODE_DONE_WAITING:
						final int motionPosition = mMotionPosition;
						final View child = getChildAt(motionPosition - mFirstPosition);
						if (child != null && !child.hasFocusable()) {
							if (mTouchMode != TOUCH_MODE_DOWN) {
								child.setPressed(false);
							}
	
							if (mPerformClick == null) {
								mPerformClick = new PerformClick();
							}
	
							final TwoWayAbsListView.PerformClick performClick = mPerformClick;
							performClick.mChild = child;
							performClick.mClickMotionPosition = motionPosition;
							performClick.rememberWindowAttachCount();
	
							mResurrectToPosition = motionPosition;
	
							if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
								final Handler handler = getHandler();
								if (handler != null) {
									handler.removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ?
											mPendingCheckForTap : mPendingCheckForLongPress);
								}
								mLayoutMode = LAYOUT_NORMAL;
								if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
									mTouchMode = TOUCH_MODE_TAP;
									setSelectedPositionInt(mMotionPosition);
									layoutChildren();
									child.setPressed(true);
									positionSelector(child);
									setPressed(true);
									if (mSelector != null) {
										Drawable d = mSelector.getCurrent();
										if (d != null && d instanceof TransitionDrawable) {
											((TransitionDrawable) d).resetTransition();
										}
									}
									postDelayed(new Runnable() {
										public void run() {
											child.setPressed(false);
											setPressed(false);
											if (!mDataChanged) {
												post(performClick);
											}
											mTouchMode = TOUCH_MODE_REST;
										}
									}, ViewConfiguration.getPressedStateDuration());
								} else {
									mTouchMode = TOUCH_MODE_REST;
								}
								return true;
							} else if (!mDataChanged && mAdapter.isEnabled(motionPosition)) {
								post(performClick);
							}
						}
						mTouchMode = TOUCH_MODE_REST;
						break;
					case TOUCH_MODE_SCROLL:
						final int childCount = getChildCount();
						if (childCount > 0) {
							if (mFirstPosition == 0 && getChildAt(0).getLeft() >= mListPadding.left &&
									mFirstPosition + childCount < mItemCount &&
									getChildAt(childCount - 1).getRight() <=
										getWidth() - mListPadding.right) {
								mTouchMode = TOUCH_MODE_REST;
								reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
							} else {
								final VelocityTracker velocityTracker = mVelocityTracker;
								velocityTracker.computeCurrentVelocity(1000);
								final int initialVelocity = (int) velocityTracker.getXVelocity();
	
								if (Math.abs(initialVelocity) > mMinimumVelocity) {
									if (mFlingRunnable == null) {
										mFlingRunnable = new HorizontalFlingRunnable();
									}
									reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
	
									mFlingRunnable.start(-initialVelocity);
								} else {
									mTouchMode = TOUCH_MODE_REST;
									reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
								}
							}
						} else {
							mTouchMode = TOUCH_MODE_REST;
							reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
						}
						break;
					}
	
					setPressed(false);
	
					// Need to redraw since we probably aren't drawing the selector anymore
					invalidate();
	
					final Handler handler = getHandler();
					if (handler != null) {
						handler.removeCallbacks(mPendingCheckForLongPress);
					}
	
					if (mVelocityTracker != null) {
						mVelocityTracker.recycle();
						mVelocityTracker = null;
					}
	
					mActivePointerId = INVALID_POINTER;
	
					if (PROFILE_SCROLLING) {
						if (mScrollProfilingStarted) {
							Debug.stopMethodTracing();
							mScrollProfilingStarted = false;
						}
					}
					break;
				}
	
				case MotionEvent.ACTION_CANCEL: {
					mTouchMode = TOUCH_MODE_REST;
					setPressed(false);
					View motionView = TwoWayAbsListView.this.getChildAt(mMotionPosition - mFirstPosition);
					if (motionView != null) {
						motionView.setPressed(false);
					}
					clearScrollingCache();
	
					final Handler handler = getHandler();
					if (handler != null) {
						handler.removeCallbacks(mPendingCheckForLongPress);
					}
	
					if (mVelocityTracker != null) {
						mVelocityTracker.recycle();
						mVelocityTracker = null;
					}
	
					mActivePointerId = INVALID_POINTER;
					break;
				}
			}

			return true;
		}


		@Override
		boolean resurrectSelection() {
			final int childCount = getChildCount();

			if (childCount <= 0) {
				return false;
			}

			int selectedLeft = 0;
			int selectedPos;
			int childrenLeft = mListPadding.top;
			int childrenRight = getRight() - getLeft() - mListPadding.right;
			final int firstPosition = mFirstPosition;
			final int toPosition = mResurrectToPosition;
			boolean down = true;

			if (toPosition >= firstPosition && toPosition < firstPosition + childCount) {
				selectedPos = toPosition;

				final View selected = getChildAt(selectedPos - mFirstPosition);
				selectedLeft = selected.getLeft();
				int selectedRight = selected.getRight();

				// We are scrolled, don't get in the fade
				if (selectedLeft < childrenLeft) {
					selectedLeft = childrenLeft + getHorizontalFadingEdgeLength();
				} else if (selectedRight > childrenRight) {
					selectedLeft = childrenRight - selected.getMeasuredWidth()
					- getHorizontalFadingEdgeLength();
				}
			} else {
				if (toPosition < firstPosition) {
					// Default to selecting whatever is first
					selectedPos = firstPosition;
					for (int i = 0; i < childCount; i++) {
						final View v = getChildAt(i);
						final int left = v.getLeft();

						if (i == 0) {
							// Remember the position of the first item
							selectedLeft = left;
							// See if we are scrolled at all
							if (firstPosition > 0 || left < childrenLeft) {
								// If we are scrolled, don't select anything that is
								// in the fade region
								childrenLeft += getHorizontalFadingEdgeLength();
							}
						}
						if (left >= childrenLeft) {
							// Found a view whose top is fully visisble
							selectedPos = firstPosition + i;
							selectedLeft = left;
							break;
						}
					}
				} else {
					final int itemCount = mItemCount;
					down = false;
					selectedPos = firstPosition + childCount - 1;

					for (int i = childCount - 1; i >= 0; i--) {
						final View v = getChildAt(i);
						final int left = v.getLeft();
						final int right = v.getRight();

						if (i == childCount - 1) {
							selectedLeft = left;
							if (firstPosition + childCount < itemCount || right > childrenRight) {
								childrenRight -= getHorizontalFadingEdgeLength();
							}
						}

						if (right <= childrenRight) {
							selectedPos = firstPosition + i;
							selectedLeft = left;
							break;
						}
					}
				}
			}

			mResurrectToPosition = INVALID_POSITION;
			removeCallbacks(mFlingRunnable);
			mTouchMode = TOUCH_MODE_REST;
			clearScrollingCache();
			mSpecificTop = selectedLeft;
			selectedPos = lookForSelectablePosition(selectedPos, down);
			if (selectedPos >= firstPosition && selectedPos <= getLastVisiblePosition()) {
				mLayoutMode = LAYOUT_SPECIFIC;
				setSelectionInt(selectedPos);
				invokeOnItemScrollListener();
			} else {
				selectedPos = INVALID_POSITION;
			}
			reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);

			return selectedPos >= 0;
		}


		@Override
		boolean trackMotionScroll(int delta, int incrementalDelta) {
			if (DEBUG) Log.i(TAG, "trackMotionScroll() - deltaX: " + delta + " incrDeltaX: " + incrementalDelta);
			final int childCount = getChildCount();
			if (childCount == 0) {
				return true;
			}

			final int firstLeft = getChildAt(0).getLeft();
			final int lastRight = getChildAt(childCount - 1).getRight();

			final Rect listPadding = mListPadding;

			// FIXME account for grid horizontal spacing too?
			final int spaceAbove = listPadding.left - firstLeft;
			final int end = getWidth() - listPadding.right;
			final int spaceBelow = lastRight - end;

			final int width = getWidth() - getPaddingRight() - getPaddingLeft();
			if (delta < 0) {
				delta = Math.max(-(width - 1), delta);
			} else {
				delta = Math.min(width - 1, delta);
			}

			if (incrementalDelta < 0) {
				incrementalDelta = Math.max(-(width - 1), incrementalDelta);
			} else {
				incrementalDelta = Math.min(width - 1, incrementalDelta);
			}

			final int firstPosition = mFirstPosition;

			if (firstPosition == 0 && firstLeft >= listPadding.left && delta >= 0) {
				// Don't need to move views right if the top of the first position
				// is already visible
				if (DEBUG) Log.i(TAG, "trackScrollMotion returning true");
				return true;
			}

			if (firstPosition + childCount == mItemCount && lastRight <= end && delta <= 0) {
				// Don't need to move views left if the bottom of the last position
				// is already visible
				if (DEBUG) Log.i(TAG, "trackScrollMotion returning true");
				return true;
			}

			final boolean down = incrementalDelta < 0;

			final boolean inTouchMode = isInTouchMode();
			if (inTouchMode) {
				hideSelector();
			}

			final int headerViewsCount = getHeaderViewsCount();
			final int footerViewsStart = mItemCount - getFooterViewsCount();

			int start = 0;
			int count = 0;

			if (down) {
				final int left = listPadding.left - incrementalDelta;
				for (int i = 0; i < childCount; i++) {
					final View child = getChildAt(i);
					if (child.getRight() >= left) {
						break;
					} else {
						count++;
						int position = firstPosition + i;
						if (position >= headerViewsCount && position < footerViewsStart) {
							mRecycler.addScrapView(child);

							if (ViewDebug.TRACE_RECYCLER) {
								ViewDebug.trace(child,
										ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
										firstPosition + i, -1);
							}
						}
					}
				}
			} else {
				final int right = getWidth() - listPadding.right - incrementalDelta;
				for (int i = childCount - 1; i >= 0; i--) {
					final View child = getChildAt(i);
					if (child.getLeft() <= right) {
						break;
					} else {
						start = i;
						count++;
						int position = firstPosition + i;
						if (position >= headerViewsCount && position < footerViewsStart) {
							mRecycler.addScrapView(child);

							if (ViewDebug.TRACE_RECYCLER) {
								ViewDebug.trace(child,
										ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
										firstPosition + i, -1);
							}
						}
					}
				}
			}

			mMotionViewNewLeft = mMotionViewOriginalLeft + delta;

			mBlockLayoutRequests = true;

			if (count > 0) {
				detachViewsFromParent(start, count);
			}
			offsetChildrenLeftAndRight(incrementalDelta);

			if (down) {
				mFirstPosition += count;
			}

			invalidate();

			final int absIncrementalDelta = Math.abs(incrementalDelta);
			if (spaceAbove < absIncrementalDelta|| spaceBelow < absIncrementalDelta) {
				fillGap(down);
			}

			if (!inTouchMode && mSelectedPosition != INVALID_POSITION) {
				final int childIndex = mSelectedPosition - mFirstPosition;
				if (childIndex >= 0 && childIndex < getChildCount()) {
					positionSelector(getChildAt(childIndex));
				}
			}

			mBlockLayoutRequests = false;

			invokeOnItemScrollListener();
			//awakenScrollBars();
			if (DEBUG) Log.i(TAG, "trackScrollMotion returning false - mFirstPosition: " + mFirstPosition);
			return false;
		}


		/**
		 * Responsible for fling behavior. Use {@link #start(int)} to
		 * initiate a fling. Each frame of the fling is handled in {@link #run()}.
		 * A FlingRunnable will keep re-posting itself until the fling is done.
		 *
		 */
		private class HorizontalFlingRunnable extends FlingRunnable {
			/**
			 * X value reported by mScroller on the previous fling
			 */
			protected int mLastFlingX;

			@Override
			void start(int initialVelocity) {
				int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
				mLastFlingX = initialX;
				mScroller.fling(initialX, 0, initialVelocity, 0,
						0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
				mTouchMode = TOUCH_MODE_FLING;
				post(this);

				if (PROFILE_FLINGING) {
					if (!mFlingProfilingStarted) {
						Debug.startMethodTracing("AbsListViewFling");
						mFlingProfilingStarted = true;
					}
				}
			}

			@Override
			void startScroll(int distance, int duration) {
				int initialX = distance < 0 ? Integer.MAX_VALUE : 0;
				mLastFlingX = initialX;
				mScroller.startScroll(initialX, 0, distance, 0, duration);
				mTouchMode = TOUCH_MODE_FLING;
				post(this);
			}

			@Override
			public void run() {
				switch (mTouchMode) {
				default:
					return;

				case TOUCH_MODE_FLING: {
					if (mItemCount == 0 || getChildCount() == 0) {
						endFling();
						return;
					}

					final Scroller scroller = mScroller;
					boolean more = scroller.computeScrollOffset();
					final int x = scroller.getCurrX();

					// Flip sign to convert finger direction to list items direction
					// (e.g. finger moving down means list is moving towards the top)
					int delta = mLastFlingX - x;

					// Pretend that each frame of a fling scroll is a touch scroll
					if (delta > 0) {
						// List is moving towards the top. Use first view as mMotionPosition
						mMotionPosition = mFirstPosition;
						final View firstView = getChildAt(0);
						mMotionViewOriginalLeft = firstView.getLeft();

						// Don't fling more than 1 screen
						delta = Math.min(getWidth() - getPaddingRight() - getPaddingLeft() - 1, delta);
					} else {
						// List is moving towards the bottom. Use last view as mMotionPosition
						int offsetToLast = getChildCount() - 1;
						mMotionPosition = mFirstPosition + offsetToLast;

						final View lastView = getChildAt(offsetToLast);
						mMotionViewOriginalLeft = lastView.getLeft();

						// Don't fling more than 1 screen
						delta = Math.max(-(getWidth() - getPaddingRight() - getPaddingLeft() - 1), delta);
					}

					final boolean atEnd = trackMotionScroll(delta, delta);

					if (more && !atEnd) {
						invalidate();
						mLastFlingX = x;
						post(this);
					} else {
						endFling();

						if (PROFILE_FLINGING) {
							if (mFlingProfilingStarted) {
								Debug.stopMethodTracing();
								mFlingProfilingStarted = false;
							}
						}
					}
					break;
				}
				}

			}
		}


		class HorizontalPositionScroller extends PositionScroller {
			@Override
			public void run() {
				final int listWidth = getWidth();
				final int firstPos = mFirstPosition;

				switch (mMode) {
				case MOVE_DOWN_POS: {
					final int lastViewIndex = getChildCount() - 1;
					final int lastPos = firstPos + lastViewIndex;

					if (lastViewIndex < 0) {
						return;
					}

					if (lastPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View lastView = getChildAt(lastViewIndex);
					final int lastViewWidth = lastView.getWidth();
					final int lastViewLeft = lastView.getLeft();
					final int lastViewPixelsShowing = listWidth - lastViewLeft;
					final int extraScroll = lastPos < mItemCount - 1 ? mExtraScroll : mListPadding.right;

					smoothScrollBy(lastViewWidth - lastViewPixelsShowing + extraScroll,
							mScrollDuration);

					mLastSeenPos = lastPos;
					if (lastPos < mTargetPos) {
						post(this);
					}
					break;
				}

				case MOVE_DOWN_BOUND: {
					final int nextViewIndex = 1;
					final int childCount = getChildCount();

					if (firstPos == mBoundPos || childCount <= nextViewIndex
							|| firstPos + childCount >= mItemCount) {
						return;
					}
					final int nextPos = firstPos + nextViewIndex;

					if (nextPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View nextView = getChildAt(nextViewIndex);
					final int nextViewWidth = nextView.getWidth();
					final int nextViewLeft = nextView.getLeft();
					final int extraScroll = mExtraScroll;
					if (nextPos < mBoundPos) {
						smoothScrollBy(Math.max(0, nextViewWidth + nextViewLeft - extraScroll),
								mScrollDuration);

						mLastSeenPos = nextPos;

						post(this);
					} else  {
						if (nextViewLeft > extraScroll) {
							smoothScrollBy(nextViewLeft - extraScroll, mScrollDuration);
						}
					}
					break;
				}

				case MOVE_UP_POS: {
					if (firstPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View firstView = getChildAt(0);
					if (firstView == null) {
						return;
					}
					final int firstViewLeft = firstView.getLeft();
					final int extraScroll = firstPos > 0 ? mExtraScroll : mListPadding.left;

					smoothScrollBy(firstViewLeft - extraScroll, mScrollDuration);

					mLastSeenPos = firstPos;

					if (firstPos > mTargetPos) {
						post(this);
					}
					break;
				}

				case MOVE_UP_BOUND: {
					final int lastViewIndex = getChildCount() - 2;
					if (lastViewIndex < 0) {
						return;
					}
					final int lastPos = firstPos + lastViewIndex;

					if (lastPos == mLastSeenPos) {
						// No new views, let things keep going.
						post(this);
						return;
					}

					final View lastView = getChildAt(lastViewIndex);
					final int lastViewWidth = lastView.getWidth();
					final int lastViewLeft = lastView.getLeft();
					final int lastViewPixelsShowing = listWidth - lastViewLeft;
					mLastSeenPos = lastPos;
					if (lastPos > mBoundPos) {
						smoothScrollBy(-(lastViewPixelsShowing - mExtraScroll), mScrollDuration);
						post(this);
					} else {
						final int right = listWidth - mExtraScroll;
						final int lastViewRight = lastViewLeft + lastViewWidth;
						if (right > lastViewRight) {
							smoothScrollBy(-(right - lastViewRight), mScrollDuration);
						}
					}
					break;
				}

				default:
					break;
				}
			}
		}

	}


}



