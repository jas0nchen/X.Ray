package com.jasonchen.microlang.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.SearchActivity;
import com.jasonchen.microlang.adapter.FollowersAdapter;
import com.jasonchen.microlang.adapter.TimeLineAdapter;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.beans.UserListBean;
import com.jasonchen.microlang.dao.SearchStatusDao;
import com.jasonchen.microlang.dao.SearchUserDao;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 2015/04/16
 */
public class SearchUserFragment extends TimeLineBaseFragment implements SearchActivity.Searcher{

    private static final String ARGUMENTS_TOKEN_EXTRA = SearchUserFragment.class
            .getName() + ":token_extra";

    private static final int REFRESH_LISTVIEW = 2;
    private static final int LOAD_OLD_MESSAGE = 3;
    private static final int NETWORK_ERROR = 4;

    private View view;
    private String token;
    private MyHandler handler;

    private List<UserBean> userBean;
    private FollowersAdapter adapter;
    private int pager = 0;
    private String q;

    public static SearchUserFragment newInstance(String token) {
        SearchUserFragment fragment = new SearchUserFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARGUMENTS_TOKEN_EXTRA, token);
        fragment.setArguments(bundle);
        return fragment;
    }

    public SearchUserFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            token = getArguments().getString(ARGUMENTS_TOKEN_EXTRA);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userBean = new ArrayList<UserBean>();
        handler = new MyHandler();
        fab.setVisibility(View.GONE);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(false);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                false);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(true);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                true);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(true);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                true);
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

            }
        });

        listView.getFooterView().hide();
        adapter = new FollowersAdapter(getActivity(), userBean, listView);
        listView.setAdapter(adapter);
    }

    @Override
    public void search(String q) {
        this.q = q;
        try {
            userBean.clear();
            adapter.notifyDataSetChanged();
            listView.setVisibility(View.GONE);
            refreshLayout.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
            refreshLayout.setRefreshing(true);
            onRefresh();
        } catch (NullPointerException e) {

        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_LISTVIEW:
                    UserListBean newList = (UserListBean) msg.obj;
                    AppLogger.d(newList.toString());
                    userBean.addAll(0, newList.getUsers());
                    adapter.setList(userBean);
                    adapter.notifyDataSetChanged();
                    listView.setVisibility(View.VISIBLE);
                    listView.getFooterView().show();
                    getRefreshLayout().setRefreshing(false);
                    break;
                case LOAD_OLD_MESSAGE:
                    UserListBean oldList = (UserListBean) msg.obj;
                    userBean.addAll(oldList.getUsers());
                    adapter.setList(userBean);
                    adapter.notifyDataSetChanged();
                    int oldnumber = oldList.getUsers().size();
                    if (oldnumber == 0){
                        Toast.makeText(GlobalContext.getInstance(),
                                getString(R.string.load_complete),
                                Toast.LENGTH_SHORT).show();
                        listView.getFooterView().hide();
                        listView.setPullLoadEnable(false);
                    }
                    getListView().stopLoadMore();
                    break;
                case NETWORK_ERROR:
                    String errorStr = (String) msg.obj;
                    Toast.makeText(GlobalContext.getInstance(), errorStr, Toast.LENGTH_SHORT)
                            .show();
                    getRefreshLayout().setRefreshing(false);
                    getListView().stopLoadMore();
                    break;
            }
        }
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        new Thread() {
            public void run() {
                SearchUserDao dao = new SearchUserDao(token, q);
                try {
                    UserListBean newlist = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = REFRESH_LISTVIEW;
                    msg.obj = newlist;
                    handler.sendMessage(msg);
                } catch (WeiboException e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    msg.obj = e.getError();
                    handler.sendMessage(msg);
                }
            }

            ;
        }.start();

    }

    @Override
    public void onLoadMore() {
        super.onLoadMore();
        asyncLoadOldMessage();
    }

    private void asyncLoadOldMessage() {
        new Thread() {
            public void run() {
                SearchUserDao dao = new SearchUserDao(token, q);
                pager ++;
                dao.setPage(String.valueOf(pager));
                try {
                    UserListBean oldList = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = LOAD_OLD_MESSAGE;
                    msg.obj = oldList;
                    handler.sendMessage(msg);
                } catch (WeiboException e) {
                    e.printStackTrace();
                    Message msg = Message.obtain();
                    msg.what = NETWORK_ERROR;
                    msg.obj = e.getError();
                    handler.sendMessage(msg);
                }
            }

            ;
        }.start();
    }
}
