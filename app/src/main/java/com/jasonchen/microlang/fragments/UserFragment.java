package com.jasonchen.microlang.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.FanListActivity;
import com.jasonchen.microlang.activitys.FollowerListActivity;
import com.jasonchen.microlang.activitys.WriteWeiboActivity;
import com.jasonchen.microlang.adapter.TimeLineAdapter;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.ShowUserDao;
import com.jasonchen.microlang.dao.StatusesTimeLineDao;
import com.jasonchen.microlang.database.AccountDBTask;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.swiperefresh.SwipeRefreshLayout;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.HackyMovementMethod;
import com.jasonchen.microlang.utils.TimeLineUtility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.utils.http.HttpUtility;
import com.jasonchen.microlang.view.AvatarBigImageView;
import com.jasonchen.microlang.view.FloatingActionButton;
import com.jasonchen.microlang.view.HackyTextView;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

/**
 * jasonchen
 * 2015/04/16
 */
public class UserFragment extends AbstractAppFragment implements SwipeRefreshLayout.OnRefreshListener, LoadListView.IXListViewListener, View.OnClickListener {

    private static final int REFRESH_COMPLETE = 0;
    private static final int REFRESH_LOADER_ID = 1;
    private static final int LOAD_OLD_STATUSES_COMPLETE = 2;
    private static final int REFRESH_MYINFO = 3;
    private static final int NETWORK_ERROR = 4;

    private View view;
    private SwipeRefreshLayout refreshLayout;
    private LoadListView listView;
    private TimeLineAdapter adapter;
    private View header;
    private AvatarBigImageView avatar;
    private TextView userName;
    private TextView gendel;
    private TextView location;
    private TextView verifyReason;
    private HackyTextView description;
    private HackyTextView url;
    private TextView follow;
    private TextView fan;
    private TextView status;

    private UserBean userBean;
    private List<MessageBean> list;

    private MyHandler handler;

    public UserFragment() {

    }

    public static UserFragment newInstance(UserBean userBean){
        UserFragment fragment = new UserFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("user", userBean);
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

        userBean = getArguments().getParcelable("user");
        list = new ArrayList<MessageBean>();
        handler = new MyHandler();
        initHeader();
        initListView();
        getWeiboList();

    }

    private void getWeiboList() {
        refreshLayout.setProgressViewOffset(false, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
        refreshLayout.setRefreshing(true);
        asyncDownloadMessage();
    }

    private void asyncDownloadMessage() {
            new Thread() {
                public void run() {
                    StatusesTimeLineDao dao = new StatusesTimeLineDao(GlobalContext
                            .getInstance().getSpecialToken(), userBean.getId());
                    dao.setCount(String.valueOf(20));
                    try {
                        MessageListBean bean = dao.getGSONMsgList();
                        Message msg = Message.obtain();
                        msg.what = REFRESH_COMPLETE;
                        msg.obj = bean;
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

    private void asyncLoadOldStatus() {
        if (list != null && list.size() > 0) {
            long maxId = list.get(list.size() - 1).getIdLong();
            final String max_id = String.valueOf(maxId - 1);
            new Thread() {
                public void run() {
                    StatusesTimeLineDao dao = new StatusesTimeLineDao(GlobalContext
                            .getInstance().getSpecialToken(), userBean.getId());
                    dao.setCount(String.valueOf(20));
                    dao.setMax_id(max_id);
                    try {
                        MessageListBean bean = dao.getGSONMsgList();
                        Message msg = Message.obtain();
                        msg.what = LOAD_OLD_STATUSES_COMPLETE;
                        msg.obj = bean;
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
        } else {
            listView.getFooterView().hide();
            Toast.makeText(GlobalContext.getInstance(), getString(R.string.empty), Toast.LENGTH_SHORT).show();
        }
    }

    private void initListView() {
        adapter = new TimeLineAdapter(this, listView, list);
        listView.setAdapter(adapter);
        listView.addHeaderView(header);
        listView.getFooterView().hide();
        listView.setOnScrollListener(new LoadListView.OnXScrollListener() {
            @Override
            public void onXScrolling(View view) {

            }

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(false);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                false);

                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                        adapter.setIsFling(true);
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(true);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                true);
                        break;
                    case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                        adapter.setIsFling(true);
                        TimeLineBitmapDownloader.getInstance()
                                .setPauseDownloadWork(true);
                        TimeLineBitmapDownloader.getInstance().setPauseReadWork(
                                true);
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });
    }

    private void initHeader() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        header = inflater.inflate(R.layout.userdetail_header_layout, listView, false);
        avatar = ViewUtility.findViewById(header, R.id.avatar);
        userName = ViewUtility.findViewById(header, R.id.username);
        gendel = ViewUtility.findViewById(header, R.id.gendel);
        location = ViewUtility.findViewById(header, R.id.location);
        description = ViewUtility.findViewById(header, R.id.description);
        verifyReason = ViewUtility.findViewById(header, R.id.verify_reason);
        url = ViewUtility.findViewById(header, R.id.url);
        follow = ViewUtility.findViewById(header, R.id.follow_list);
        fan = ViewUtility.findViewById(header, R.id.fan_list);
        status = ViewUtility.findViewById(header, R.id.weibo_list);

        buildContent();

        follow.setOnClickListener(this);
        status.setOnClickListener(this);
        fan.setOnClickListener(this);

    }

    private void buildContent() {
        TimeLineBitmapDownloader.getInstance().display(
                avatar.getImageView(),
                avatar.getImageView().getWidth(),
                avatar.getImageView().getHeight(),
                userBean.getAvatar_large(),
                FileLocationMethod.avatar_large, false);
        avatar.checkVerified(userBean);
        userName.setText(userBean.getScreen_name());
        if(!TextUtils.isEmpty(userBean.getGender())) {
            gendel.setText("f".equals(userBean.getGender()) ? getString(R.string.f) : getString(R.string.m));
        }else{
            gendel.setVisibility(View.GONE);
        }
        if(!TextUtils.isEmpty(userBean.getLocation())){
            location.setText(userBean.getLocation());
        }else{
            location.setVisibility(View.GONE);
        }
        if(!TextUtils.isEmpty(userBean.getDescription())){
            description.setVisibility(View.VISIBLE);
            description.setText(userBean.getDescription());
            TimeLineUtility.addLinks(description);
            description.setMovementMethod(HackyMovementMethod.getInstance());
        }
        if(!TextUtils.isEmpty(userBean.getVerified_reason())){
            verifyReason.setVisibility(View.VISIBLE);
            verifyReason.setText("认证：" + userBean.getVerified_reason());
        }
        if(!TextUtils.isEmpty(userBean.getUrl())){
            url.setVisibility(View.VISIBLE);
            url.setText(userBean.getUrl());
            TimeLineUtility.addLinks(url);
            url.setMovementMethod(HackyMovementMethod.getInstance());
        }

        follow.setText("关注 " + userBean.getFriends_count());
        fan.setText("粉丝 " + userBean.getFollowers_count());
        status.setText("微博 " + userBean.getStatus_count());
    }

    protected LoadListView getListView(){
        return listView;
    }

    @Override
    public void onRefresh() {
        if(GlobalContext.getInstance().getAccountBean().getUid().equals(userBean.getId())){
            refreshMyInfo();
        }else{
            refreshLayout.setRefreshing(false);
        }
    }

    private void refreshMyInfo() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                ShowUserDao dao = new ShowUserDao(GlobalContext.getInstance().getSpecialToken());
                dao.setUid(userBean.getId());
                try {
                    UserBean newuserBean = dao.getUserInfo();
                    Message msg = Message.obtain();
                    msg.what = REFRESH_MYINFO;
                    msg.obj = newuserBean;
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

    @Override
    public void onLoadMore() {
        asyncLoadOldStatus();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.follow_list:
                getActivity().startActivity(FollowerListActivity.newIntent(getActivity(), userBean.getId()));
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                break;
            case R.id.fan_list:
                getActivity().startActivity(FanListActivity.newIntent(getActivity(), userBean.getId()));
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                break;
        }
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_COMPLETE:
                    MessageListBean result = new MessageListBean();
                    result.replaceData((MessageListBean) msg.obj);
                    adapter.setList(result.getItemList());
                    list.clear();
                    list.addAll(result.getItemList());
                    adapter.notifyDataSetChanged();
                    listView.getFooterView().show();
                    refreshLayout.setRefreshing(false);
                    if (list.size() > 0) {
                        listView.getFooterView().show();
                    } else {
                        listView.getFooterView().hide();
                    }
                    break;
                case LOAD_OLD_STATUSES_COMPLETE:
                    MessageListBean oldResult = (MessageListBean) msg.obj;
                    int number = oldResult.getSize();
                    if (number > 0) {
                        list.addAll(list.size(), oldResult.getItemList());
                        adapter.setList(list);
                        adapter.notifyDataSetChanged();
                        listView.stopLoadMore();
                        Toast.makeText(GlobalContext.getInstance(), String.format(getString(R.string.old_messages_count), number), Toast.LENGTH_SHORT).show();
                    } else {
                        listView.stopLoadMore();
                        listView.getFooterView().hide();
                        listView.setPullLoadEnable(false);
                        Toast.makeText(GlobalContext.getInstance(), getString(R.string.older_message_empty), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case REFRESH_MYINFO:
                    UserBean newUserBean = (UserBean) msg.obj;
                    userBean = newUserBean;
                    AccountDBTask.asyncUpdateMyProfile(GlobalContext.getInstance().getAccountBean(), userBean);
                    AccountBean accountBean = GlobalContext.getInstance().getAccountBean();
                    accountBean.setInfo(userBean);
                    GlobalContext.getInstance().setAccountBean(accountBean);
                    buildContent();
                    refreshLayout.setRefreshing(false);
                    break;
                case NETWORK_ERROR:
                    listView.stopLoadMore();
                    refreshLayout.setRefreshing(false);
                    String errStr = (String) msg.obj;
                    Toast.makeText(GlobalContext.getInstance(), errStr, Toast.LENGTH_SHORT).show();
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
