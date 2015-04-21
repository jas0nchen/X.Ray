package com.jasonchen.microlang.activitys;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.adapter.TimeLineAdapter;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.beans.UserListBean;
import com.jasonchen.microlang.dao.FriendshipsDao;
import com.jasonchen.microlang.dao.ShowUserDao;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.fragments.TopicFragment;
import com.jasonchen.microlang.fragments.UserFragment;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;

/**
 * jasonchen
 * 2015/04/10
 */
public class TopicActivity extends SwipeBackActivity implements SwipeRefreshLayout.OnRefreshListener, LoadListView.IXListViewListener {

    private String q;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_topic;
        super.onCreate(savedInstanceState);

        initView();
    }

    private void initView() {
        Uri data = getIntent().getData();
        String d = data.toString();
        int index = d.indexOf("#");
        q = d.substring(index + 1, d.length() - 1);
        getSupportActionBar().setTitle(getString(R.string.topic_list));
        getSupportActionBar().setSubtitle(q);

        buildContent();
    }

    private void buildContent() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getSupportFragmentManager().findFragmentByTag(TopicFragment.class.getName()) == null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, TopicFragment.newInstance(q),
                                    TopicFragment.class.getName())
                            .commitAllowingStateLoss();
                    getSupportFragmentManager().executePendingTransactions();
                    findViewById(R.id.container).setBackgroundDrawable(null);
                }
            }
        });
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
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onLoadMore() {

    }


}

