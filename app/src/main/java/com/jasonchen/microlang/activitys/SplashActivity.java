package com.jasonchen.microlang.activitys;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.utils.GlobalContext;


public class SplashActivity extends ActionBarActivity {

    private String defaultAccountId;
    private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        defaultAccountId = SettingUtility.getDefaultAccountId();
        handler = new Handler();

        if (!TextUtils.isEmpty(defaultAccountId)) {
            // if has default account,jump to the maintimeline activity.
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    jumpToMainActivity();
                    //jumpToAccountActivity();
                }
            }, 1000);
        } else {
            // if doesn't has default account, check update and jump to the
            // account acitvity.
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    jumpToAccountActivity();
                }
            }, 1000);

        }
    }

    protected void jumpToMainActivity() {
        Intent intent = MainActivity.newIntent(GlobalContext.getInstance().getAccountBean());
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.stay);
    }

    private void jumpToAccountActivity() {
        Intent intent = new Intent(SplashActivity.this, AccountActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.stay);
    }
}
