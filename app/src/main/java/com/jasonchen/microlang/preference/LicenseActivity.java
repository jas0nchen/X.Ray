package com.jasonchen.microlang.preference;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebView;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;

/**
 * jasonchen
 * 2015/04/21
 */
public class LicenseActivity extends SwipeBackActivity {

    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_lisence;
        super.onCreate(savedInstanceState);
        webView = ViewUtility.findViewById(this, R.id.webView);
        webView.loadUrl("file:///android_asset/licenses.html");
    }
 }
