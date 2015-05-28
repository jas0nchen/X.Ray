package com.jasonchen.microlang.activitys;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import com.jasonchen.microlang.exception.WeiboException;

/**
 * jasonchen
 * 2015/04/10
 */
public class AbstractAppActivity extends ActionBarActivity {

    protected int mLayout = 0;
    protected int theme = 0;
    protected Toolbar mToolbar;
    @Override
    protected void onResume() {
        super.onResume();
        GlobalContext.getInstance().setCurrentRunningActivity(this);
        if(!(GlobalContext.getInstance().getCurrentRunningActivity() instanceof OAuthActivity)){
            if (!Utility.isTokenValid(GlobalContext.getInstance().getAccountBean())) {
                Utility.showExpiredTokenDialogOrNotification();
            }
        }
        configTheme();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (GlobalContext.getInstance().getCurrentRunningActivity() == this) {
            GlobalContext.getInstance().setCurrentRunningActivity(null);
        }
    }

    private void configTheme() {
        if(theme == SettingUtility.getTheme()){
            setTheme(theme);
        }else{
            reload();
            return;
        }
    }

    public void reload(){
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            theme = SettingUtility.getTheme();
        } else {
            theme = savedInstanceState.getInt("theme");
        }
        configTheme();
        super.onCreate(savedInstanceState);
        GlobalContext.getInstance().setActivity(this);
        GlobalContext.getInstance().setCurrentRunningActivity(this);
        setContentView(mLayout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            LinearLayout root = (LinearLayout) findViewById(R.id.root);
            View view = new View(this);
            LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Utility.getStatusBarHeight());
            int color = SettingUtility.getThemeColor();
            view.setBackgroundColor(getResources().getColor(color));
            view.setLayoutParams(lParams);
            root.addView(view, 0);
        }
        configTheme();

        mToolbar = ViewUtility.findViewById(this, R.id.toolbar);

        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishWithAnimation();
                }
            });
            if (Build.VERSION.SDK_INT >= 21) {
                mToolbar.setElevation(getToolbarElevation());
            }
        }

    }

    public float getToolbarElevation() {
        if (Build.VERSION.SDK_INT >= 21) {
            return 12.8f;
        } else {
            return -1;
        }
    }

    public TimeLineBitmapDownloader getBitmapDownloader() {
        return TimeLineBitmapDownloader.getInstance();
    }

    protected void dealWithException(WeiboException e) {
        Toast.makeText(this, e.getError(), Toast.LENGTH_SHORT).show();
    }

    protected void openActivityWithAnimation(){
        overridePendingTransition(R.anim.push_left_in, R.anim.stay);
    }

    protected void finishWithAnimation(){
        finish();
        overridePendingTransition(R.anim.stay, R.anim.push_right_out);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(!(GlobalContext.getInstance().getCurrentRunningActivity() instanceof BrowserActivity)){
                finish();
                overridePendingTransition(R.anim.stay, R.anim.push_right_out);
            }
        }
        return false;
    }
}
