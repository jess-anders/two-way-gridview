TwoWayGridView
==============

An Android GridView that can be configured to scroll horizontally or vertically.

I should have posted this over a year and a half ago, but never got around to it.  I needed a grid view that in portrait would scroll vertically, but in landscape, would scroll horizontally.  I thought I could try hacking up the Gallery, but that never works out well, and if GridView could really be configured to scroll any direction, it would just be so much easier.

So I built it one weekend.  Lots of left, right, top, bottom changes, but the end result is a really useful UI widget.

Feel free to use it in your apps, according to the Apache 2.0 license.  Also feel free to fork it and improve it.  You could fairly easily create a horizontal listview by extending TwoWayAbsListView

Usage
-----

The TwoWayGridView can be used as a drop-in replacement for the normal Android GridView.  It just has a few more configurable attributes:

* `scrollDirectionPortrait` (vertical | horizontal) The direction the grid will scroll when the device is in portrait orientation
* `scrollDirectionLandscape` (vertical | horizontal) The direction the grid will scroll when the device is in landscape orientation
* `numRows` (integer) Number of rows in grid view when in horizontal scrolling mode
* `verticalSpacing` (dimension) Height of vertical spacing between grid rows
* `rowHeight` (dimension) Height of each grid row

Here is an example from the demo layout where it is configured to scroll vertically in portrait and horizontally in landscape :

    <?xml version="1.0" encoding="utf-8"?>
    <com.jess.ui.TwoWayGridView
        xmlns:android="http://schemas.android.com/apk/res/android" 
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:background="#E8E8E8"
        android:id="@+id/gridview"
        android:layout_width="fill_parent" 
        android:layout_height="fill_parent"
        app:cacheColorHint="#E8E8E8"
        app:columnWidth="80dp"
        app:rowHeight="80dp"
        app:numColumns="auto_fit"
        app:numRows="auto_fit"
        app:verticalSpacing="16dp"
        app:horizontalSpacing="16dp"
        app:stretchMode="spacingWidthUniform"
        app:scrollDirectionPortrait="vertical"
        app:scrollDirectionLandscape="horizontal"
        app:gravity="center"/>