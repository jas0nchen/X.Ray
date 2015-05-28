package com.jasonchen.microlang.activitys;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.view.FloatingActionButton;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/10
 */
public class BrowserActivity extends SwipeBackActivity implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout container;
    private WebView webView;
    private String url;
    private Uri uri;

    public static Intent newIntent(Uri uri){
        Intent intent = new Intent(GlobalContext.getInstance().getActivity(), BrowserActivity.class);
        intent.putExtra("url", uri.toString());
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_browser;
        super.onCreate(savedInstanceState);

        initView();
    }

    private void initView() {
        String action = getIntent().getAction();
        if (Intent.ACTION_VIEW.equalsIgnoreCase(action)) {
            url = getIntent().getData().toString();
        } else {
            url = getIntent().getStringExtra("url");
        }
        uri = Uri.parse(url);
        if (webView != null) {
            webView.destroy();
        }

        container = ViewUtility.findViewById(this, R.id.container);
        webView = ViewUtility.findViewById(this, R.id.webView);
        webView.setWebViewClient(new WeiboWebViewClient());
        getSupportActionBar().setTitle(getString(R.string.app_name));

        container.setColorSchemeColors(getResources().getColor(SettingUtility.getThemeColor()));
        container.setOnRefreshListener(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(webView != null){
            webView.clearCache(true);
            webView.destroy();
        }
    }

    public void refresh() {
        webView.clearView();
        webView.loadUrl(url);
        startRefreshAnimation();
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    private class WeiboWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Utility.isWeiboAccountIdLink(url)) {
                String result = Utility.getIdFromWeiboAccountLink(url);
                /*Intent intent = new Intent(BrowserWebActivity.this,
                        NewUserDetailActivity.class);
                intent.putExtra("id", result);*/
                //Intent intent = UserDetailActivity.newIntent(result, null, BrowserWebActivity.this);
                //startActivity(intent);
                //finish();
                /*GlobalContext
                        .getInstance()
                        .getCurrentRunningActivity()
                        .overridePendingTransition(R.anim.push_left_in,
                                R.anim.stay);*/
                return true;
            } else if (Utility.isWeiboMid(url)) {
                /*String mid = Utility.getMidFromUrl(url);
                RedirectLinkToWeiboIdTask task = new RedirectLinkToWeiboIdTask(
                        BrowserWebActivity.this, url, mid);
                task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);*/
                return true;
            } else {
                view.loadUrl(url);
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (this == null) {
                return;
            }
            if (!TextUtils.isEmpty(view.getTitle())) {
                getSupportActionBar().setTitle(view.getTitle());
            }
            startRefreshAnimation();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            final MaterialDialog showErrorDialog = new MaterialDialog(BrowserActivity.this);
            showErrorDialog.setTitle(getString(R.string.notice));
            showErrorDialog.setMessage(getString(R.string.sina_server_error))
                    .setPositiveButton(getString(R.string.confirm), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showErrorDialog.dismiss();
                            finish();
                            overridePendingTransition(R.anim.stay, R.anim.push_right_out);
                        }
                    });
            showErrorDialog.show();
            stopRefreshAnimation();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (this == null) {
                return;
            }
            if (!TextUtils.isEmpty(view.getTitle())) {
                getSupportActionBar().setTitle(view.getTitle());
            }
            stopRefreshAnimation();
        }
    }

    private void startRefreshAnimation(){
        container.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
        container.setRefreshing(true);
    }

    private void stopRefreshAnimation(){
        container.setRefreshing(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            webView.stopLoading();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_browser, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_copy) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("sinaweibo", url));
            Toast.makeText(this, getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_browser){
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
            startActivity(intent);
        }
        return false;
    }

    public WebView getWebView(){
        return webView;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(webView.canGoBack()){
                webView.goBack();
            }
        }
        return false;
    }
}

