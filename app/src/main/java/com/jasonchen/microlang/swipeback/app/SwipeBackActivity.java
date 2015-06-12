package com.jasonchen.microlang.swipeback.app;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.jasonchen.microlang.activitys.AbstractAppActivity;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.swipeback.SwipeBackLayout;
import com.jasonchen.microlang.swipeback.Utils;
import com.jasonchen.microlang.utils.MythouCrashHandler;


public class SwipeBackActivity extends AbstractAppActivity implements
		SwipeBackActivityBase {
	private SwipeBackActivityHelper mHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new MythouCrashHandler());
		mHelper = new SwipeBackActivityHelper(this);
		mHelper.onActivityCreate();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mHelper.onPostCreate();
	}

	@Override
	public View findViewById(int id) {
		View v = super.findViewById(id);
		if (v == null && mHelper != null)
			return mHelper.findViewById(id);
		return v;
	}

	@Override
	public SwipeBackLayout getSwipeBackLayout() {
		return mHelper.getSwipeBackLayout();
	}

	public void setResultData(MessageBean bean) {
		getSwipeBackLayout().setResultData(bean);
	}

	@Override
	public void setSwipeBackEnable(boolean enable) {
		getSwipeBackLayout().setEnableGesture(enable);
	}

	@Override
	public void scrollToFinishActivity() {
		Utils.convertActivityToTranslucent(this);
		getSwipeBackLayout().scrollToFinishActivity();
	}
}
