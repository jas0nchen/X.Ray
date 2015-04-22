package com.jasonchen.microlang.preference;

import android.os.Bundle;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;

/**
 * jasonchen
 * 2015/04/10
 */
public class AboutActivity extends SwipeBackActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_about;
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.container, new AboutFragment()).commit();
        }

        getSupportActionBar().setTitle(getString(R.string.about));


    }

}
