package com.jasonchen.microlang.activitys;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.adapter.FollowersAdapter;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.beans.UserListBean;
import com.jasonchen.microlang.dao.FanListDao;
import com.jasonchen.microlang.dao.OAuthDao;
import com.jasonchen.microlang.database.AccountDBTask;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.swiperefresh.LoadListView;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/10
 */
public class FanListActivity extends SwipeBackActivity implements SwipeRefreshLayout.OnRefreshListener, LoadListView.IXListViewListener {

    private static final int FIRST_LOAD = 0;
    private static final int REFRESH = 1;
    private static final int LOAD_MORE = 2;
    private static final int ERROR = 3;

    private SwipeRefreshLayout container;
    private LoadListView listView;
    private FollowersAdapter adapter;
    private MyHandler handler;
    private String id;
    private List<UserBean> listBean;

    private int cursor = 0;

    public static Intent newIntent(Context context, String uid) {
        Intent intent = new Intent(context, FanListActivity.class);
        intent.putExtra("id", uid);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_fanlist;
        super.onCreate(savedInstanceState);

        initView();
    }

    private void initView() {
        container = ViewUtility.findViewById(this, R.id.container);
        listView = ViewUtility.findViewById(this, R.id.listView);
        handler = new MyHandler();
        getSupportActionBar().setTitle(getString(R.string.fan_list));

        container.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        container.setOnRefreshListener(this);
        listView.setPullLoadEnable(true);
        listView.setXListViewListener(this);
        listView.setDivider(null);

        id = getIntent().getStringExtra("id");
        listBean = new ArrayList<UserBean>();
        adapter = new FollowersAdapter(FanListActivity.this, listBean, listView);
        listView.setAdapter(adapter);

        listView.getFooterView().hide();
        getFanlist();
    }

    private void getFanlist() {
        container.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
        container.setRefreshing(true);
        new Thread() {
            @Override
            public void run() {
                super.run();
                FanListDao dao = new FanListDao(GlobalContext.getInstance().getSpecialToken(), id);
                dao.setCursor("0");
                try {
                    UserListBean result = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = FIRST_LOAD;
                    msg.obj = result;
                    handler.sendMessage(msg);
                } catch (WeiboException e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = ERROR;
                    msg.obj = e;
                    handler.sendMessage(msg);
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRefresh() {
        getFanlist();
    }

    @Override
    public void onLoadMore() {
        if (cursor != 0) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    FanListDao dao = new FanListDao(GlobalContext.getInstance().getSpecialToken(), id);
                    dao.setCursor(String.valueOf(cursor));
                    try {
                        UserListBean result = dao.getGSONMsgList();
                        Message msg = Message.obtain();
                        msg.what = LOAD_MORE;
                        msg.obj = result;
                        handler.sendMessage(msg);
                    } catch (WeiboException e) {
                        e.printStackTrace();
                        Message msg = Message.obtain();
                        msg.what = ERROR;
                        msg.obj = e;
                        handler.sendMessage(msg);
                    }
                }
            }.start();
        } else {
            listView.getFooterView().hide();
            listView.setPullLoadEnable(false);
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FIRST_LOAD:
                    UserListBean bean = (UserListBean) msg.obj;
                    listBean = bean.getUsers();
                    AppLogger.e(listBean.toString());
                    cursor = bean.getNext_cursor();
                    container.setRefreshing(false);
                    listView.getFooterView().show();
                    adapter.setList(listBean);
                    adapter.notifyDataSetChanged();
                    break;
                case REFRESH:
                    UserListBean newbean = (UserListBean) msg.obj;
                    listBean = newbean.getUsers();
                    cursor = newbean.getNext_cursor();
                    container.setRefreshing(false);
                    listView.getFooterView().show();
                    adapter.setList(listBean);
                    adapter.notifyDataSetChanged();
                    break;
                case LOAD_MORE:
                    UserListBean oldbean = (UserListBean) msg.obj;
                    cursor = oldbean.getNext_cursor();
                    listBean.addAll(listBean.size(), oldbean.getUsers());
                    if (oldbean.getUsers().size() == 0) {
                        listView.getFooterView().hide();
                        listView.setPullLoadEnable(false);
                    }
                    adapter.setList(listBean);
                    adapter.notifyDataSetChanged();
                    listView.stopLoadMore();
                    break;
                case ERROR:
                    container.setRefreshing(false);
                    listView.stopLoadMore();
                    WeiboException e = (WeiboException) msg.obj;
                    Toast.makeText(FanListActivity.this, e.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
