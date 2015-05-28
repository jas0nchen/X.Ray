package com.jasonchen.microlang.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.adapter.TimeLineAdapter;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.FavListBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.FavListDao;
import com.jasonchen.microlang.database.FavouriteDBTask;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 2015/04/10
 */
@SuppressLint({"NewApi", "ResourceAsColor"})
public class FavFragment extends FixedOnActivityResultBugFragment implements
        SwipeRefreshLayout.OnRefreshListener, LoadListView.IXListViewListener {

    private View view;

    private static final String ARGUMENTS_ACCOUNT_EXTRA = FavFragment.class
            .getName() + ":account_extra";
    private static final String ARGUMENTS_USER_EXTRA = FavFragment.class
            .getName() + ":userBean_extra";
    private static final String ARGUMENTS_TOKEN_EXTRA = FavFragment.class
            .getName() + ":token_extra";

    private static final int FIRST_LOAD_TIMELINE = 0;
    private static final int REFRESH_LISTVIEW = 1;
    private static final int NETWORK_ERROR = 2;
    private static final int LOAD_OLD_MESSAGE = 3;

    private SwipeRefreshLayout swipeRefreshLayout;
    private AccountBean accountBean;
    private UserBean userBean;
    private String token;
    private MessageListBean list;
    private List<MessageBean> msgBean;
    private LoadListView mListView;
    private Handler handler;
    private TimeLineAdapter adapter;

    private int totalPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_comment_to_me, container, false);
        }
        return view;
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView = ViewUtility.findViewById(view, R.id.list);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(SettingUtility.getThemeColor()));
        swipeRefreshLayout.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
        swipeRefreshLayout.setRefreshing(true);
        mListView.setPullLoadEnable(true);
        mListView.setXListViewListener(this);
        mListView.getFooterView().hide();
        totalPager = 1;
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case FIRST_LOAD_TIMELINE:
                        FavListBean firstData = (FavListBean) msg.obj;
                        msgBean = firstData.getItemList();
                        adapter = new TimeLineAdapter(FavFragment.this, mListView, msgBean);
                        mListView.setVisibility(View.VISIBLE);
                        mListView.setAdapter(adapter);
                        swipeRefreshLayout.setRefreshing(false);
                        mListView.getFooterView().show();
                        break;
                    case REFRESH_LISTVIEW:
                        List<MessageBean> newList = (List<MessageBean>) msg.obj;
                        int number = newList.size();
                        msgBean.clear();
                        msgBean.addAll(newList);
                        newList = null;
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case LOAD_OLD_MESSAGE:
                        FavListBean oldList = (FavListBean) msg.obj;
                        msgBean.addAll(oldList.getItemList());
                        mListView.requestLayout();
                        adapter.notifyDataSetChanged();
                        int oldnumber = oldList.getItemList().size();
                        oldList = null;
                        if (oldnumber > 0) {
                            Toast.makeText(
                                    GlobalContext.getInstance(),
                                    String.format(
                                            getString(R.string.old_messages_count),
                                            oldnumber), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GlobalContext.getInstance(),
                                    getString(R.string.older_message_empty),
                                    Toast.LENGTH_SHORT).show();
                            mListView.getFooterView().hide();
                            mListView.setPullLoadEnable(false);
                        }
                        mListView.stopLoadMore();
                        break;
                    case NETWORK_ERROR:
                        String errorStr = (String) msg.obj;
                        Toast.makeText(GlobalContext.getInstance(), errorStr, Toast.LENGTH_SHORT)
                                .show();
                        swipeRefreshLayout.setRefreshing(false);
                        mListView.stopLoadMore();
                        break;
                }
            }
        };
    }

    public BaseAdapter getAdapter() {
        return adapter;
    }

    public static FavFragment newInstance(AccountBean accountBean,
                                          UserBean userBean, String token) {
        FavFragment fragment = new FavFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGUMENTS_ACCOUNT_EXTRA, accountBean);
        bundle.putParcelable(ARGUMENTS_USER_EXTRA, userBean);
        bundle.putString(ARGUMENTS_TOKEN_EXTRA, token);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.getFooterView().hide();
        userBean = getArguments().getParcelable(ARGUMENTS_USER_EXTRA);
        accountBean = getArguments().getParcelable(ARGUMENTS_ACCOUNT_EXTRA);
        token = getArguments().getString(ARGUMENTS_TOKEN_EXTRA);
        msgBean = new ArrayList<MessageBean>();
        int total = FavouriteDBTask.getTotalNumber(accountBean.getUid());

        if (SettingUtility.firstOpenFav() || total == 0) {
            mListView.setVisibility(View.INVISIBLE);
            getNewStatusTask();
        } else {
            mListView.setVisibility(View.INVISIBLE);
            readStatusFromDBTask();
        }

        mListView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState) {
                    case OnScrollListener.SCROLL_STATE_IDLE:
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(false);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                false);

                        break;
                    case OnScrollListener.SCROLL_STATE_FLING:
                        adapter.setIsFling(true);
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(true);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                true);
                        break;
                    case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        adapter.setIsFling(true);
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
    }

    public void getNewStatusTask() {
        new Thread() {
            public void run() {
                FavListDao dao = new FavListDao(
                        accountBean.getAccess_token());
                try {
                    FavListBean newlist = dao.getGSONMsgList();
                    FavouriteDBTask.asyncReplace(newlist, 1, accountBean
                            .getInfo().getId());
                    Message msg = Message.obtain();
                    msg.what = FIRST_LOAD_TIMELINE;
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

    private void asyncLoadOldMessage() {
        new Thread() {
            public void run() {
                FavListDao dao = new FavListDao(
                        accountBean.getAccess_token());
                totalPager++;
                dao.setPage(String.valueOf(totalPager));
                try {
                    FavListBean newlist = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = LOAD_OLD_MESSAGE;
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

    private void readStatusFromDBTask() {
        new Thread() {
            public void run() {
                int total = FavouriteDBTask
                        .getTotalNumber(accountBean.getUid());
                FavListBean newlist = FavouriteDBTask
                        .getFavouriteMsgList(accountBean.getInfo().getId(),
                                total);
                Message msg = Message.obtain();
                msg.what = FIRST_LOAD_TIMELINE;
                msg.obj = newlist;
                handler.sendMessage(msg);
            }

            ;
        }.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onRefresh() {
        new Thread() {
            public void run() {

                FavListDao dao = new FavListDao(
                        accountBean.getAccess_token());

                try {
                    FavListBean favListBean = dao.getGSONMsgList();
                    List<MessageBean> newlist = favListBean.getItemList();
                    FavouriteDBTask.asyncReplace(favListBean, 1, accountBean
                            .getInfo().getId());
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
        asyncLoadOldMessage();
    }

    private List<MessageBean> getList() {
        return adapter.getList();
    }

}
