package com.jasonchen.microlang.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.FanListActivity;
import com.jasonchen.microlang.activitys.FollowerListActivity;
import com.jasonchen.microlang.adapter.TimeLineAdapter;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.TopicResultListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.SearchTopicDao;
import com.jasonchen.microlang.dao.StatusesTimeLineDao;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.swiperefresh.SwipeRefreshLayout;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.HackyMovementMethod;
import com.jasonchen.microlang.utils.TimeLineUtility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.view.AvatarBigImageView;
import com.jasonchen.microlang.view.HackyTextView;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 2015/04/16
 */
public class TopicFragment extends AbstractAppFragment implements SwipeRefreshLayout.OnRefreshListener, LoadListView.IXListViewListener {

    private static final int FIRST_LOAD = 0;
    private static final int REFRESH = 1;
    private static final int LOAD_MORE = 2;
    private static final int ERROR = 3;

    private View view;
    private SwipeRefreshLayout refreshLayout;
    private LoadListView listView;
    private TimeLineAdapter adapter;

    private List<MessageBean> list;

    private String topic;
    private int pager = 1;

    private MyHandler handler;

    public TopicFragment() {

    }

    public static TopicFragment newInstance(String topic) {
        TopicFragment fragment = new TopicFragment();
        Bundle bundle = new Bundle();
        bundle.putString("topic", topic);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_user, container, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout = ViewUtility.findViewById(view, R.id.refreshLayout);
        listView = ViewUtility.findViewById(view, R.id.listView);

        refreshLayout.setColorSchemeColors(getResources().getColor(SettingUtility.getThemeColor()));
        refreshLayout.setOnRefreshListener(this);
        listView.setXListViewListener(this);
        listView.setPullLoadEnable(true);
        listView.setDivider(null);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

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
                        if (adapter != null) {
                            adapter.setIsFling(true);
                        }
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(true);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                true);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        if (adapter != null) {
                            adapter.setIsFling(true);
                        }
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

        handler = new MyHandler();
        list = new ArrayList<MessageBean>();
        topic = getArguments().getString("topic");
        initListView();
        getWeiboList();

    }

    private void getWeiboList() {
        refreshLayout.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
        refreshLayout.setRefreshing(true);
        listView.getFooterView().hide();
        asyncDownLoadTopic();
    }

    private void asyncDownLoadTopic() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                SearchTopicDao dao = new SearchTopicDao(GlobalContext.getInstance().getSpecialToken(), topic);
                dao.setCount(String.valueOf(20));
                try {
                    TopicResultListBean result = dao.getGSONMsgList();
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

    private void initListView() {
        adapter = new TimeLineAdapter(this, listView, list);
        listView.setAdapter(adapter);
    }

    protected LoadListView getListView() {
        return listView;
    }

    @Override
    public void onRefresh() {
        asyncDownLoadTopic();
    }

    @Override
    public void onLoadMore() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                SearchTopicDao dao = new SearchTopicDao(GlobalContext.getInstance().getSpecialToken(), topic);
                dao.setPage(String.valueOf(pager++));
                dao.setCount(String.valueOf(20));
                try {
                    TopicResultListBean result = dao.getGSONMsgList();
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
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FIRST_LOAD:
                    TopicResultListBean newbean = (TopicResultListBean) msg.obj;
                    list.clear();
                    list.addAll(newbean.getItemList());
                    adapter.notifyDataSetChanged();
                    refreshLayout.setRefreshing(false);
                    listView.getFooterView().show();
                    break;
                case LOAD_MORE:
                    TopicResultListBean oldbean = (TopicResultListBean) msg.obj;
                    list.addAll(list.size(), oldbean.getItemList());
                    adapter.notifyDataSetChanged();
                    listView.stopLoadMore();
                    if(oldbean.getItemList().size() == 0){
                        listView.getFooterView().hide();
                        listView.setPullLoadEnable(false);
                    }
                    break;
                case ERROR:
                    refreshLayout.setRefreshing(false);
                    listView.stopLoadMore();
                    WeiboException e = (WeiboException) msg.obj;
                    Toast.makeText(GlobalContext.getInstance(), e.getError(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
