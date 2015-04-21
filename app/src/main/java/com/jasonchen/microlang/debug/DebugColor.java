package com.jasonchen.microlang.debug;

import android.graphics.Color;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.Utility;

/**
 * jasonchen
 * 2015/04/10
 */
public class DebugColor {

	// public static int DOWNLOAD_START = Color.BLUE;
	// public static int DOWNLOAD_FAILED = Color.RED;
	// public static int DOWNLOAD_CANCEL = Color.BLACK;
	// public static int PICTURE_ERROR = Color.YELLOW;
	// public static int LISTVIEW_FLING = Color.GREEN;
	public static int DOWNLOAD_START = GlobalContext
			.getInstance().getResources().getColor(R.color.gainsboro);
	public static int CHOOSE_CANCEL = !Utility.isDebugMode() ? GlobalContext
			.getInstance().getResources().getColor(R.color.gainsboro)
			: Color.BLACK;
	public static int DOWNLOAD_FAILED = !Utility.isDebugMode() ? GlobalContext
			.getInstance().getResources().getColor(R.color.gainsboro) : Color.RED;
	public static int DOWNLOAD_CANCEL = !Utility.isDebugMode() ? GlobalContext
			.getInstance().getResources().getColor(R.color.gainsboro)
			: Color.GREEN;
	public static int READ_FAILED = !Utility.isDebugMode() ? GlobalContext
			.getInstance().getResources().getColor(R.color.gainsboro)
			: Color.BLUE;
	public static int READ_CANCEL = !Utility.isDebugMode() ? GlobalContext
			.getInstance().getResources().getColor(R.color.gainsboro)
			: Color.YELLOW;
	public static int LISTVIEW_FLING = GlobalContext
			.getInstance().getResources().getColor(R.color.gainsboro);
}
