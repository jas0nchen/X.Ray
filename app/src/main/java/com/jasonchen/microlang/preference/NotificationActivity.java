package com.jasonchen.microlang.preference;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.SettingActivity;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;

/**
 * jasonchen
 * 2015/06/12
 */
public class NotificationActivity extends SwipeBackActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_about;
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.container, new NotificationFragment()).commit();
        }

        getSupportActionBar().setTitle(getString(R.string.pref_notification_title));
    }
}
