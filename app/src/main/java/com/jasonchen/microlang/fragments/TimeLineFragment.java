package com.jasonchen.microlang.fragments;

import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Layout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.AccountActivity;
import com.jasonchen.microlang.activitys.MainActivity;
import com.jasonchen.microlang.activitys.WriteCommentActivity;
import com.jasonchen.microlang.activitys.WriteRepostActivity;
import com.jasonchen.microlang.adapter.TimeLineAdapter;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.GroupBean;
import com.jasonchen.microlang.beans.GroupListBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.BilateralTimeLineDao;
import com.jasonchen.microlang.dao.FriendGroupDao;
import com.jasonchen.microlang.dao.FriendGroupTimeLineDao;
import com.jasonchen.microlang.dao.MainFriendsTimeLineDao;
import com.jasonchen.microlang.database.FriendsTimeLineDBTask;
import com.jasonchen.microlang.database.GroupDBTask;
import com.jasonchen.microlang.database.HomeOtherGroupTimeLineDBTask;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.List;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/16
 */
public class TimeLineFragment extends TimeLineBaseFragment {

    private static final String ARGUMENTS_ACCOUNT_EXTRA = TimeLineFragment.class
            .getName() + ":account_extra";
    private static final String ARGUMENTS_USER_EXTRA = TimeLineFragment.class
            .getName() + ":userBean_extra";
    private static final String ARGUMENTS_TOKEN_EXTRA = TimeLineFragment.class
            .getName() + ":token_extra";

    private static final int FIRST_LOAD_TIMELINE_FROM_INTERNET = 0;
    private static final int FIRST_LOAD_TIMELINE_FROM_DB = 1;
    private static final int REFRESH_LISTVIEW = 2;
    private static final int LOAD_OLD_MESSAGE = 3;
    private static final int NETWORK_ERROR = 4;
    private static final int REFRESH_MSG = 5;

    private List<String> groupNames;
    private int groupIndex = 0;

    private View view;
    private AccountBean accountBean;
    private UserBean userBean;
    private String token;
    private MyHandler handler;
    private SoundPool soundPool;
    private GroupListBean groupListBean;

    private MessageListBean list;
    private List<MessageBean> msgBean;
    private TimeLineAdapter adapter;

    public static TimeLineFragment newInstance(AccountBean accountBean, UserBean userBean, String token) {
        TimeLineFragment fragment = new TimeLineFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGUMENTS_ACCOUNT_EXTRA, accountBean);
        bundle.putParcelable(ARGUMENTS_USER_EXTRA, userBean);
        bundle.putString(ARGUMENTS_TOKEN_EXTRA, token);
        fragment.setArguments(bundle);
        return fragment;
    }

    public TimeLineFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userBean = getArguments().getParcelable(ARGUMENTS_USER_EXTRA);
            accountBean = getArguments().getParcelable(ARGUMENTS_ACCOUNT_EXTRA);
            token = getArguments().getString(ARGUMENTS_TOKEN_EXTRA);
        }
        setHasOptionsMenu(true);
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
        list = new MessageListBean();
        msgBean = new ArrayList<MessageBean>();
        groupNames = new ArrayList<String>();
        groupNames.add("全部");
        groupNames.add("好友圈");
        handler = new MyHandler();
        soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 10);
        soundPool.load(getActivity(), R.raw.pop, 1);
        adapter = new TimeLineAdapter(TimeLineFragment.this, getListView(),
                msgBean);
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
        getStatusFromNetWorkOrDB();
        asyncGetGroupInfo();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main_group, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.group:
                showGroup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showGroup() {
        groupListBean = GroupDBTask.get(GlobalContext.getInstance().getCurrentAccountId());
        if(groupListBean != null && groupListBean.getLists().size() > 0) {
            for (int i = 0; i < groupListBean.getLists().size(); i++) {
                groupNames.add(groupListBean.getLists().get(i).getName());
            }
        }
        PopupMenu popupMenu = new PopupMenu(getActivity(), getActivity().findViewById(R.id.group));
        popupMenu.inflate(R.menu.menu_group_item);
        Menu menu = popupMenu.getMenu();
        int i = 0;
        if(groupListBean != null && groupListBean.getLists().size() > 0) {
            for (GroupBean bean : groupListBean.getLists()) {
                menu.add(Menu.NONE, Menu.FIRST + i, i, bean.getName());
                i++;
            }
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.all) {
                    if (groupIndex != 0) {
                        FriendsTimeLineDBTask.asyncUpdateRecentGroupId(GlobalContext.getInstance().getCurrentAccountId(), "0");
                        getStatusFromNetWorkOrDB();
                        ((MainActivity) getActivity()).getSupportActionBar().setTitle(GlobalContext.getInstance().getCurrentAccountName());
                    }
                    groupIndex = 0;
                } else if (item.getItemId() == R.id.friend) {
                    FriendsTimeLineDBTask.asyncUpdateRecentGroupId(GlobalContext.getInstance().getCurrentAccountId(), "1");
                    getFriendsTimeline();
                    ((MainActivity) getActivity()).getSupportActionBar().setTitle("好友圈");
                    groupIndex = 1;
                } else {
                    for (int i = 0; i < groupNames.size(); i++) {
                        if (item.getItemId() == (Menu.FIRST + i)) {
                            FriendsTimeLineDBTask.asyncUpdateRecentGroupId(GlobalContext.getInstance().getCurrentAccountId(), groupListBean.getLists().get(i).getIdstr());
                            ((MainActivity) getActivity()).getSupportActionBar().setTitle(groupListBean.getLists().get(i).getName());
                            getOtherTimeline(groupListBean.getLists().get(i).getIdstr());
                            groupIndex = 2 + i;
                            return true;
                        }
                    }
                }
                return true;
            }
        });
        popupMenu.show();
    }

    private void getOtherTimeline(String groupId) {
        listView.setVisibility(View.GONE);
        getRefreshLayout().setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));
        getRefreshLayout().setRefreshing(true);
        MessageListBean totalmsg = HomeOtherGroupTimeLineDBTask.get(accountBean.getUid(), groupId, 100);
        if(totalmsg.getSize() == 0){
            getOtherTimelineFromInternet(groupId);
        }else{
            readOtherTimelineFromDB(groupId);
        }
    }

    private void getOtherTimelineFromInternet(final String groupId) {
        new Thread() {
            public void run() {
                FriendGroupTimeLineDao dao = new FriendGroupTimeLineDao(accountBean.getAccess_token(), groupId);
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
                    AppLogger.e(newlist.toString());
                    HomeOtherGroupTimeLineDBTask.replace(newlist, accountBean.getInfo().getId(), groupId);
                    Message msg = Message.obtain();
                    msg.what = FIRST_LOAD_TIMELINE_FROM_INTERNET;
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
        }.start();
    }

    private void readOtherTimelineFromDB(final String groupId) {
        new Thread() {
            public void run() {
                MessageListBean totalmsg = HomeOtherGroupTimeLineDBTask.get(accountBean.getUid(), groupId, 100);
                Message msg = Message.obtain();
                msg.what = FIRST_LOAD_TIMELINE_FROM_DB;
                msg.obj = totalmsg;
                handler.sendMessage(msg);
            }
        }.start();
    }

    private void getFriendsTimeline() {
        listView.setVisibility(View.GONE);
        getRefreshLayout().setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));
        getRefreshLayout().setRefreshing(true);
        MessageListBean totalmsg = HomeOtherGroupTimeLineDBTask.get(accountBean.getUid(), "1", 100);
        if(totalmsg.getSize() == 0){
            getFriendsTimelineFromInternet();
        }else{
            readFriendsTimelineFromDB();
        }
    }

    private void readFriendsTimelineFromDB() {
        new Thread() {
            public void run() {
                MessageListBean totalmsg = HomeOtherGroupTimeLineDBTask
                        .get(accountBean.getUid(), "1", 100);
                Message msg = Message.obtain();
                msg.what = FIRST_LOAD_TIMELINE_FROM_DB;
                msg.obj = totalmsg;
                handler.sendMessage(msg);
            }
        }.start();
    }

    private void getFriendsTimelineFromInternet() {
        new Thread() {
            public void run() {
                BilateralTimeLineDao dao = new BilateralTimeLineDao(accountBean.getAccess_token());
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
                    AppLogger.e(newlist.toString());
                    HomeOtherGroupTimeLineDBTask.replace(newlist, accountBean.getInfo().getId(), "1");
                    Message msg = Message.obtain();
                    msg.what = FIRST_LOAD_TIMELINE_FROM_INTERNET;
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
        }.start();
    }

    private void getStatusFromNetWorkOrDB() {
        int total = FriendsTimeLineDBTask.getTotalHomeMsgCount(accountBean.getUid());
        getRefreshLayout().setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));
        getRefreshLayout().setRefreshing(true);

        if (SettingUtility.firstStart() || total == 0) {
            getListView().setVisibility(View.INVISIBLE);
            getNewStatusTask();
        } else {
            getListView().setVisibility(View.INVISIBLE);
            readStatusFromDBTask();
        }
    }

    private void getNewStatusTask() {
        new Thread() {
            public void run() {
                MainFriendsTimeLineDao dao = new MainFriendsTimeLineDao(
                        accountBean.getAccess_token(), "100");
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
                    FriendsTimeLineDBTask.asyncReplace(newlist, accountBean
                            .getInfo().getId(), "0");
                    Message msg = Message.obtain();
                    msg.what = FIRST_LOAD_TIMELINE_FROM_INTERNET;
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
                int total = FriendsTimeLineDBTask
                        .getTotalHomeMsgCount(accountBean.getUid());
                System.out.println("" + total);
                MessageListBean newlist = FriendsTimeLineDBTask
                        .getHomeLineMsgList(accountBean.getInfo().getId(),
                                total);
                Message msg = Message.obtain();
                msg.what = FIRST_LOAD_TIMELINE_FROM_DB;
                msg.obj = newlist;
                handler.sendMessage(msg);
            }

            ;
        }.start();
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FIRST_LOAD_TIMELINE_FROM_INTERNET:
                    list = (MessageListBean) msg.obj;
                    msgBean.clear();
                    msgBean.addAll(list.getItemList());
                    int firstLoadNumberFromInternet = list.getSize();
                    if (SettingUtility.isInvertRead()) {
                        getListView().setSelection(firstLoadNumberFromInternet);
                    }
                    if (list.getSize() > 0 && SettingUtility.getEnableSound()) {
                        soundPool.play(1, 1, 1, 0, 0, 1);
                    }
                    adapter = new TimeLineAdapter(TimeLineFragment.this, getListView(),
                            msgBean);
                    if(msgBean.size() == 0){
                        getListView().getFooterView().hide();
                    }else{
                        getListView().getFooterView().show();
                    }
                    getListView().setVisibility(View.VISIBLE);
                    getListView().setAdapter(adapter);
                    getRefreshLayout().setRefreshing(false);
                    break;
                case FIRST_LOAD_TIMELINE_FROM_DB:
                    list = (MessageListBean) msg.obj;
                    msgBean.clear();
                    msgBean.addAll(list.getItemList());
                    adapter = new TimeLineAdapter(TimeLineFragment.this, getListView(),
                            msgBean);
                    if(msgBean.size() == 0){
                        getListView().getFooterView().hide();
                    }else{
                        getListView().getFooterView().show();
                    }
                    getListView().setVisibility(View.VISIBLE);
                    getListView().setAdapter(adapter);
                    if (SettingUtility.getEnableAutoRefresh()) {
                        onRefresh();
                    } else {
                        refreshLayout.setRefreshing(false);
                    }
                    break;
                case REFRESH_LISTVIEW:
                    MessageListBean newList = (MessageListBean) msg.obj;
                    list.addNewData(newList);
                    listView.setVisibility(View.VISIBLE);
                    int number = newList.getItemList().size();
                    msgBean.addAll(0, newList.getItemList());
                    newList = null;
                    adapter.notifyDataSetChanged();
                    if (SettingUtility.isInvertRead()) {
                        getListView().setSelection(number);
                    }
                    if (number == 0) {
                        Toast.makeText(GlobalContext.getInstance(),
                                getString(R.string.no_new_message),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        if (SettingUtility.getEnableSound()) {
                            soundPool.play(1, 1, 1, 0, 0, 1);
                        }
                        Toast.makeText(
                                GlobalContext.getInstance(),
                                String.format(
                                        getString(R.string.new_messages_count),
                                        number), Toast.LENGTH_SHORT).show();
                    }
                    getRefreshLayout().setRefreshing(false);
                    break;
                case LOAD_OLD_MESSAGE:
                    MessageListBean oldList = (MessageListBean) msg.obj;
                    list.addOldData(oldList);
                    msgBean.addAll(oldList.getItemList());
                    getListView().requestLayout();
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
                    }
                    if (oldnumber < 20) {
                        getListView().setPullLoadEnable(false);
                        getListView().getFooterView().hide();
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
        if(groupIndex == 0) {
            refreshMainTimeLine();
        }else if(groupIndex == 1){
            refreshFriendsTimeLine();
        }else{
            if(groupListBean != null && groupListBean.getLists().size() > 0){
                refreshOtherTimeLine(groupListBean.getLists().get(groupIndex - 2).getIdstr());
            }
        }

    }

    private void refreshOtherTimeLine(final String groupId) {
        new Thread() {
            public void run() {
                FriendGroupTimeLineDao dao = new FriendGroupTimeLineDao(accountBean.getAccess_token(), groupId);
                if (msgBean != null && msgBean.size() > 0) {
                    String sinceId = msgBean.get(0).getId();
                    dao.setSince_id(sinceId);
                }
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
                    MessageListBean list = new MessageListBean();
                    if (newlist.getItemList().size() >= 100) {
                        list.getItemList().addAll(
                                newlist.getItemList().subList(0, 99));
                    } else {
                        list.getItemList().addAll(
                                newlist.getItemList().subList(0,
                                        newlist.getItemList().size()));
                        int totalCount = 100 > adapter.getList().size() ? adapter.getList()
                                .size() : 100;
                        list.getItemList()
                                .addAll(newlist.getItemList().size(),
                                        adapter.getList().subList(
                                                0,
                                                totalCount
                                                        - newlist.getItemList()
                                                        .size()));
                    }
                    HomeOtherGroupTimeLineDBTask.replace(list, accountBean.getInfo().getId(), groupId);
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
        }.start();
    }

    private void refreshFriendsTimeLine() {
        new Thread() {
            public void run() {
                BilateralTimeLineDao dao = new BilateralTimeLineDao(accountBean.getAccess_token());
                if (msgBean != null && msgBean.size() > 0) {
                    String sinceId = msgBean.get(0).getId();
                    dao.setSince_id(sinceId);
                }
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
                    MessageListBean list = new MessageListBean();
                    if (newlist.getItemList().size() >= 100) {
                        list.getItemList().addAll(
                                newlist.getItemList().subList(0, 99));
                    } else {
                        list.getItemList().addAll(
                                newlist.getItemList().subList(0,
                                        newlist.getItemList().size()));
                        int totalCount = 100 > adapter.getList().size() ? adapter.getList()
                                .size() : 100;
                        list.getItemList()
                                .addAll(newlist.getItemList().size(),
                                        adapter.getList().subList(
                                                0,
                                                totalCount
                                                        - newlist.getItemList()
                                                        .size()));
                    }
                    HomeOtherGroupTimeLineDBTask.replace(list, accountBean.getInfo().getId(), "1");
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
        }.start();
    }

    private void refreshMainTimeLine() {
        new Thread() {
            public void run() {
                MainFriendsTimeLineDao dao = new MainFriendsTimeLineDao(
                        accountBean.getAccess_token(), "100");
                if (msgBean != null && msgBean.size() > 0) {
                    String sinceId = msgBean.get(0).getId();
                    dao.setSince_id(sinceId);
                }

                try {
                    MessageListBean newlist = dao.getGSONMsgList();
                    MessageListBean list = new MessageListBean();
                    if (newlist.getItemList().size() >= 100) {
                        list.getItemList().addAll(
                                newlist.getItemList().subList(0, 99));
                    } else {
                        list.getItemList().addAll(
                                newlist.getItemList().subList(0,
                                        newlist.getItemList().size()));
                        int totalCount = 100 > adapter.getList().size() ? adapter.getList()
                                .size() : 100;
                        list.getItemList()
                                .addAll(newlist.getItemList().size(),
                                        adapter.getList().subList(
                                                0,
                                                totalCount
                                                        - newlist.getItemList()
                                                        .size()));
                    }
                    FriendsTimeLineDBTask.asyncReplace(list, accountBean
                            .getInfo().getId(), "0");
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
        if (groupIndex == 0) {
            asyncLoadOldMessage();
        }else if(groupIndex == 1){
            asyncLoadOldFriendsTimeLine();
        }else{
            if(groupListBean != null && groupListBean.getLists().size() > 0){
                asyncLoadOldOtherTimeLine(groupListBean.getLists().get(groupIndex - 2).getIdstr());
            }
        }
    }

    private void asyncLoadOldOtherTimeLine(final String groupId) {
        new Thread() {
            public void run() {
                FriendGroupTimeLineDao dao = new FriendGroupTimeLineDao(accountBean.getAccess_token(), groupId);
                long maxId = 0;
                if (getList() != null) {
                    maxId = getList().get(getList().size() - 1).getIdLong() - 1;
                    dao.setMax_id(String.valueOf(maxId));
                }
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
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
        }.start();
    }

    private void asyncLoadOldFriendsTimeLine() {
        new Thread() {
            public void run() {
                BilateralTimeLineDao dao = new BilateralTimeLineDao(accountBean.getAccess_token());
                long maxId = 0;
                if (getList() != null) {
                    maxId = getList().get(getList().size() - 1).getIdLong() - 1;
                    dao.setMax_id(String.valueOf(maxId));
                }
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
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
        }.start();
    }

    private void asyncLoadOldMessage() {
        new Thread() {
            public void run() {
                MainFriendsTimeLineDao dao = new MainFriendsTimeLineDao(
                        accountBean.getAccess_token(), "20");
                long maxId = 0;
                if (getList() != null) {
                    maxId = getList().get(getList().size() - 1).getIdLong() - 1;
                    dao.setMax_id(String.valueOf(maxId));
                }
                try {
                    MessageListBean newlist = dao.getGSONMsgList();
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

    private List<MessageBean> getList() {
        if (adapter != null && adapter.getList().size() > 0) {
            return adapter.getList();
        } else {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        final MessageBean msg = (MessageBean) data.getParcelableExtra("msg");
        if (msg != null) {
            for (int i = 0; i < getList().size(); i++) {
                if (msg.equals(getList().get(i))) {
                    MessageBean ori = getList().get(i);
                    if (ori.getComments_count() != msg.getComments_count()
                            || ori.getReposts_count() != msg.getReposts_count()) {
                        ori.setReposts_count(msg.getReposts_count());
                        ori.setComments_count(msg.getComments_count());
                        FriendsTimeLineDBTask
                                .asyncUpdateCount(msg.getId(),
                                        msg.getComments_count(),
                                        msg.getReposts_count());
                        adapter.notifyDataSetChanged();
                    }
                    break;
                }
            }
        }
    }

    public void showFab() {
        fab.showFloatingActionButton();
    }

    public void hideFab() {
        fab.hideFloatingActionButton();
    }

    private void asyncGetGroupInfo() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                FriendGroupDao dao = new FriendGroupDao(GlobalContext.getInstance().getSpecialToken());
                try {
                    GroupListBean list = dao.getGroup();
                    if(list != null) {
                        GroupDBTask.update(list, GlobalContext.getInstance().getCurrentAccountId());
                        AppLogger.e(list.toString());
                    }
                } catch (WeiboException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}

