package com.jasonchen.microlang.activitys;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.OAuthDao;
import com.jasonchen.microlang.database.AccountDBTask;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.StreamUtility;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.http.URLHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/10
 */
public class OAuthActivity extends SwipeBackActivity implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout container;
    private WebView webView;
    private String code;
    private MyHandler handler;
    private static final int START_OAUTH_TASK = 0;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_oauth;
        super.onCreate(savedInstanceState);

        initView();
    }

    private void initView() {
        container = ViewUtility.findViewById(this, R.id.container);
        webView = ViewUtility.findViewById(this, R.id.webView);
        webView.setWebViewClient(new WeiboWebViewClient());
        handler = new MyHandler(this);
        getSupportActionBar().setTitle(getString(R.string.oauth));

        container.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        container.setOnRefreshListener(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.clearCache(true);
        webView = null;
        handler = null;
    }

    public void refresh() {
        webView.clearView();
        webView.loadUrl("about:blank");
        webView.loadUrl(getWeiboOAuthUrl());
        container.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
        container.setRefreshing(true);
    }

    private void completeRefresh() {
        container.setRefreshing(false);
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    static class MyHandler extends Handler {

        WeakReference<OAuthActivity> mActivity;

        MyHandler(OAuthActivity activity) {
            mActivity = new WeakReference<OAuthActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final OAuthActivity activity = mActivity.get();
            super.handleMessage(msg);
            if (START_OAUTH_TASK == msg.what) {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) msg.obj;
                String access_token = map.get("access_token");
                String expires_time = map.get("expires_in");
                new OAuthTask(activity).execute(access_token, expires_time);
            }
        }

    }

    private String getWeiboOAuthUrl() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("client_id", URLHelper.HACK_APP_KEY);
        parameters.put("response_type", "code");
        parameters.put("redirect_uri", URLHelper.HACK_DIRECT_URL);
        parameters.put("display", "mobile");
        return URLHelper.URL_OAUTH2_ACCESS_AUTHORIZE + "?"
                + Utility.encodeUrl(parameters)
                + "&scope=all";
    }

    private String getWeiboTokenUrl() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("client_id", URLHelper.HACK_APP_KEY);
        parameters.put("client_secret", URLHelper.HACK_APP_SECRET);
        parameters.put("grant_type", "authorization_code");
        parameters.put("redirect_uri", URLHelper.HACK_DIRECT_URL);
        parameters.put("code", code);
        return URLHelper.ACESS_TOKEN + "?" + Utility.encodeUrl(parameters);
    }

    private class WeiboWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.startsWith(URLHelper.HACK_DIRECT_URL)) {
                handleRedirectUrl(view, url);
                view.stopLoading();
                return;
            }
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            final MaterialDialog showErrorDialog = new MaterialDialog(OAuthActivity.this);
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
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (!url.equals("about:blank")) {
                completeRefresh();
            }
        }
    }


    private void handleRedirectUrl(WebView view, String url) {
        Bundle values = Utility.parseUrl(url);
        String error = values.getString("error");
        String error_code = values.getString("error_code");
        code = values.getString("code");
        final Bundle newValues = new Bundle();

        final Intent intent = new Intent();
        intent.putExtras(newValues);

        if (error == null && error_code == null && code != null) {
            new Thread() {
                public void run() {
                    try {
                        URL url = new URL(getWeiboTokenUrl());
                        HttpURLConnection conn;
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(4000);
                        conn.setRequestMethod("POST");
                        int responce_code = conn.getResponseCode();
                        if (responce_code == 200) {
                            InputStream is = conn.getInputStream();
                            String result = StreamUtility.readFromStream(is);
                            JSONObject obj = new JSONObject(result);
                            String access_token = obj.getString("access_token");
                            String expires_time = obj.getString("expires_in");
                            newValues.putString("access_token", access_token);
                            newValues.putString("expires_in", expires_time);
                            System.out.println("token:" + access_token
                                    + ",expires:" + expires_time);
                            setResult(RESULT_OK, intent);
                            Map<String, String> map = new HashMap<String, String>();
                            map.put("access_token", access_token);
                            map.put("expires_in", expires_time);
                            Message msg = Message.obtain();
                            msg.what = START_OAUTH_TASK;
                            msg.obj = map;
                            handler.sendMessage(msg);
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                ;
            }.start();

            System.out.println("code:" + code);
        } else {
            Toast.makeText(OAuthActivity.this,
                    getString(R.string.you_cancel_login), Toast.LENGTH_SHORT)
                    .show();
            finish();
            overridePendingTransition(R.anim.stay, R.anim.push_right_out);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            Toast.makeText(OAuthActivity.this,
                    getString(R.string.you_cancel_login), Toast.LENGTH_SHORT)
                    .show();
            finish();
            overridePendingTransition(R.anim.stay, R.anim.push_right_out);
        }
    }

    private static class OAuthTask extends
            MyAsyncTask<String, UserBean, DBResult> {

        private WeiboException e;
        private ProgressFragment progressFragment = ProgressFragment
                .newInstance();
        private WeakReference<OAuthActivity> oAuthActivityWeakReference;

        private OAuthTask(OAuthActivity activity) {
            oAuthActivityWeakReference = new WeakReference<OAuthActivity>(
                    activity);
        }

        @Override
        protected void onPreExecute() {
            progressFragment.setAsyncTask(this);
            OAuthActivity activity = oAuthActivityWeakReference.get();
            if (activity != null) {
                progressFragment.show(activity.getSupportFragmentManager(), "");
            }
        }

        @Override
        protected DBResult doInBackground(String... params) {
            String token = params[0];
            long expiresInSeconds = Long.valueOf(params[1]);

            try {
                UserBean user = new OAuthDao(token).getOAuthUserInfo();
                AccountBean account = new AccountBean();
                account.setAccess_token(token);
                account.setExpires_time(System.currentTimeMillis()
                        + expiresInSeconds * 1000);
                account.setInfo(user);
                AppLogger.e("token expires in "
                        + Utility.calcTokenExpiresInDays(account) + " days");
                return AccountDBTask.addOrUpdateAccount(account, false);
            } catch (WeiboException e) {
                AppLogger.e(e.getError());
                this.e = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onCancelled(DBResult dbResult) {
            super.onCancelled(dbResult);
            if (progressFragment != null) {
                progressFragment.dismissAllowingStateLoss();
            }

            OAuthActivity activity = oAuthActivityWeakReference.get();
            if (activity == null) {
                return;
            }

            if (e != null) {
                Toast.makeText(activity, e.getError(), Toast.LENGTH_SHORT)
                        .show();
            }
            activity.webView.loadUrl(activity.getWeiboOAuthUrl());
        }

        @Override
        protected void onPostExecute(DBResult dbResult) {
            if (progressFragment.isVisible()) {
                progressFragment.dismissAllowingStateLoss();
            }
            OAuthActivity activity = oAuthActivityWeakReference.get();
            if (activity == null) {
                return;
            }
            switch (dbResult) {
                case add_successfuly:
                    Toast.makeText(activity,
                            activity.getString(R.string.login_success),
                            Toast.LENGTH_SHORT).show();
                    break;
                case update_successfully:
                    Toast.makeText(activity,
                            activity.getString(R.string.update_account_success),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            activity.finish();
            activity.overridePendingTransition(R.anim.stay, R.anim.push_right_out);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            webView.stopLoading();
        }
    }

    public static class ProgressFragment extends DialogFragment {

        MyAsyncTask asyncTask = null;

        public static ProgressFragment newInstance() {
            ProgressFragment frag = new ProgressFragment();
            frag.setRetainInstance(true);
            Bundle args = new Bundle();
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getString(R.string.oauthing));
            dialog.setIndeterminate(false);
            dialog.setCancelable(true);
            return dialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (asyncTask != null) {
                asyncTask.cancel(true);
            }
            super.onCancel(dialog);
        }

        void setAsyncTask(MyAsyncTask task) {
            asyncTask = task;
        }
    }

    public static enum DBResult {
        add_successfuly, update_successfully
    }
}
