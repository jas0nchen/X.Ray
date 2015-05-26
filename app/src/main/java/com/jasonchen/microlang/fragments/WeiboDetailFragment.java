package com.jasonchen.microlang.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.UserActivity;
import com.jasonchen.microlang.activitys.WeiboDetailActivity;
import com.jasonchen.microlang.adapter.WeiboDetailAdapter;
import com.jasonchen.microlang.beans.CommentListBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.MessageReCmtCountBean;
import com.jasonchen.microlang.beans.RepostListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.CommentsTimeLineByIdDao;
import com.jasonchen.microlang.dao.DestroyCommentDao;
import com.jasonchen.microlang.dao.RepostsTimeLineByIdDao;
import com.jasonchen.microlang.dao.TimeLineReCmtCountDao;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.gallery.GalleryAnimationActivity;
import com.jasonchen.microlang.interfaces.ISimRayDrawable;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.AnimationRect;
import com.jasonchen.microlang.utils.AppEventAction;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.HackyMovementMethod;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.view.HackyTextView;
import com.jasonchen.microlang.view.TimeLineRoundAvatarImageView;
import com.jasonchen.microlang.view.TimeTextView;
import com.jasonchen.microlang.view.WeiboDetailImageView;
import com.jasonchen.microlang.workers.MsgDetailReadWorker;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 2015/04/16
 */
public class WeiboDetailFragment extends AbstractAppFragment implements LoadListView.IXListViewListener {

    private static final int COMMENT_COMPLETE = 0;
    private static final int REPOST_COMPLETE = 1;
    private static final int NETWORK_ERROR = 2;
    private static final int REFRESH_COMMENT_REPOST_COUNT = 3;
    private static final int LOAD_COMMENT_COMPLETE = 4;
    private static final int LOAD_REPOST_COMPLETE = 5;

    private View view;
    private LinearLayout commentAndRepostLayout;
    private LoadListView listView;
    private WeiboDetailAdapter adapter;
    private LayoutInflater mInflater;
    private TextView repostLayout;
    private TextView commentLayout;

    private MessageBean messageBean;
    private UserBean userBean;
    private MessageReCmtCountBean msgReCmtCountBean;
    private CommentListBean commentList;
    private RepostListBean repostList;
    private boolean isCommentList;
    private boolean isRepostList;
    private boolean canLoadCommentData = true;
    private boolean canLoadRepostData = true;

    private MyHandler handler;
    private BroadcastReceiver sendCommentCompletedReceiver;
    private BroadcastReceiver sendRepostCompletedReceiver;
    private MsgDetailReadWorker picTask;

    public WeiboDetailFragment() {

    }

    public static WeiboDetailFragment newInstance(MessageBean messageBean) {
        WeiboDetailFragment fragment = new WeiboDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("message", messageBean);
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
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_weibodetail, container, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            messageBean = getArguments().getParcelable("message");
            userBean = messageBean.getUser();
        }
        commentAndRepostLayout = ViewUtility.findViewById(view, R.id.comment_repost_layout);
        listView = ViewUtility.findViewById(view, R.id.listView);
        repostLayout = ViewUtility.findViewById(view, R.id.weibodetail_repost_count);
        commentLayout = ViewUtility.findViewById(view, R.id.weibodetail_comment_count);
        mInflater = LayoutInflater.from(getActivity());
        msgReCmtCountBean = new MessageReCmtCountBean();
        handler = new MyHandler();

        listView.setXListViewListener(this);
        listView.setPullLoadEnable(true);
        listView.setDivider(null);

        initAdapter();
        initHeaderView();
        initCommentRepostLayout();

        asyncRefreshCommentAndRepostCount();
    }

    private void initCommentRepostLayout() {
        repostLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToRepost();
            }

        });
        commentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToComment();
            }
        });
    }

    private void initAdapter() {
        commentList = new CommentListBean();
        repostList = new RepostListBean();
        canLoadCommentData = true;
        canLoadRepostData = true;
        adapter = new WeiboDetailAdapter(this, getActivity(), messageBean, null, null, listView);
        listView.setAdapter(adapter);
        asyncDownloadComment();
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
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem < 1) {
                    commentAndRepostLayout.setVisibility(View.GONE);
                    if (Build.VERSION.SDK_INT >= 21) {
                        ((WeiboDetailActivity) getActivity()).getToolbar().setElevation(getToolbarElevation());
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= 21) {
                        ((WeiboDetailActivity) getActivity()).getToolbar().setElevation(0f);
                    }
                    isCommentList = adapter.getIsCommentList();
                    if (isCommentList) {
                        commentLayout.setTextSize(18f);
                        repostLayout.setTextSize(14f);
                    } else {
                        commentLayout.setTextSize(14f);
                        repostLayout.setTextSize(18f);
                    }
                    commentAndRepostLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        commentLayout.setText("评论 " + messageBean.getCommentscountString());
        repostLayout.setText("转发 " + messageBean.getRepostscountString());
    }

    private void initHeaderView() {
        LinearLayout headerLayout = (LinearLayout) mInflater.inflate(R.layout.weibodetail_header_layout, listView, false);
        TextView username = ViewUtility.findViewById(headerLayout, R.id.username);
        TimeTextView time = ViewUtility.findViewById(headerLayout, R.id.time);
        TextView source = ViewUtility.findViewById(headerLayout, R.id.source);
        TimeLineRoundAvatarImageView avatar = ViewUtility.findViewById(headerLayout, R.id.avatar);
        HackyTextView content = ViewUtility.findViewById(headerLayout, R.id.content);
        WeiboDetailImageView detail_image = ViewUtility.findViewById(headerLayout, R.id.content_pic);
        GridLayout detail_multi_image = ViewUtility.findViewById(headerLayout, R.id.content_pic_multi);
        if (!TextUtils.isEmpty(userBean.getRemark())) {
            username.setText(new StringBuilder(userBean.getScreen_name())
                    .append("(").append(userBean.getRemark()).append(")")
                    .toString());
        } else {
            username.setText(userBean.getScreen_name());
        }
        avatar.checkVerified(userBean);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserActivity.newIntent(getActivity(), userBean);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
            }
        });
        TimeLineBitmapDownloader.getInstance().displayRoundAvatar(
                avatar.getImageView(), avatar.getImageView().getWidth(),
                avatar.getImageView().getHeight(),
                userBean.getProfile_image_url(),
                FileLocationMethod.avatar_small, false);
        int prefFontSizeSp = SettingUtility.getFontSize();
        float currentWidgetTextSizePx;
        currentWidgetTextSizePx = content.getTextSize();
        if (Utility.sp2px(prefFontSizeSp) != currentWidgetTextSizePx) {
            content.setTextSize(prefFontSizeSp);
        }
        content.setText(messageBean.getWeiboDetailSpannableString());
        content.setMovementMethod(HackyMovementMethod.getInstance());
        time.setTime(messageBean.getMills());
        if (source != null) {
            source.setText(messageBean.getSourceString());
        }

        if (SettingUtility.getIntelligencePic()) {
            if (Utility.isWifi(getActivity())) {
                if (messageBean.havePicture() && messageBean.getRetweeted_status() == null) {
                    displayPictures(messageBean, detail_multi_image, detail_image, true);
                }
            }
        } else {
            if (messageBean.havePicture() && messageBean.getRetweeted_status() == null) {
                displayPictures(messageBean, detail_multi_image, detail_image, true);
            }
        }

        LinearLayout repost_layout = ViewUtility.findViewById(headerLayout, R.id.repost_layout);
        HackyTextView repost_content = ViewUtility.findViewById(headerLayout, R.id.repost_content);
        WeiboDetailImageView repost_image = ViewUtility.findViewById(headerLayout, R.id.repost_content_pic);
        GridLayout repost_image_multi = ViewUtility.findViewById(headerLayout, R.id.repost_content_pic_multi);
        TextView repost_repost_count = ViewUtility.findViewById(headerLayout, R.id.repost_msg_repost_count);
        TextView repost_comment_count = ViewUtility.findViewById(headerLayout, R.id.repost_msg_comment_count);
        final MessageBean repostMsg = messageBean.getRetweeted_status();
        repost_layout.setVisibility(repostMsg != null ? View.VISIBLE
                : View.GONE);
        if (repostMsg != null) {
            repost_content.setVisibility(View.VISIBLE);
            repost_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (repostMsg.getUser() != null) {
                        Intent intent = WeiboDetailActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), repostMsg,
                                GlobalContext.getInstance().getSpecialToken());
                        startActivity(intent);
                        getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                    }
                }
            });
            currentWidgetTextSizePx = repost_content.getTextSize();

            if (Utility.sp2px(prefFontSizeSp - 2) != currentWidgetTextSizePx) {
                repost_content.setTextSize(prefFontSizeSp - 2);
            }
            repost_content.setText(repostMsg.getWeiboDetailSpannableString());
            repost_content.setMovementMethod(HackyMovementMethod.getInstance());
            repost_comment_count.setText(String.valueOf(repostMsg.getCommentscountString()));
            repost_repost_count.setText(String.valueOf(repostMsg.getRepostscountString()));
            repost_image.setVisibility(View.GONE);
            repost_image_multi.setVisibility(View.GONE);

            if (SettingUtility.getIntelligencePic()) {
                if (Utility.isWifi(getActivity())) {
                    if (repostMsg.havePicture()) {
                        displayPictures(repostMsg, repost_image_multi, repost_image, true);
                    }
                }
            } else {
                if (repostMsg.havePicture()) {
                    displayPictures(repostMsg, repost_image_multi, repost_image, true);
                }
            }
        }
        listView.addHeaderView(headerLayout);
    }

    private void switchToRepost() {
        enableLoad();
        isCommentList = adapter.getIsCommentList();
        if (isCommentList) {
            repostLayout.setTextSize(18f);
            changeToRepostList();
            boolean firstLoadRepost = adapter.getIsFirstLoadRepost();
            if (firstLoadRepost) {
                adapter.notifyDataSetChanged();
                asyncDownloadRepost();
            } else {
                if (adapter.getRepostListBean() != null
                        && adapter.getRepostListBean().getItemList().size() > 0) {
                    adapter.notifyDataSetChanged();
                } else {
                    asyncDownloadRepost();
                }
            }
        } else {
            asyncDownloadRepost();
        }
    }

    @SuppressLint("ResourceAsColor")
    private void switchToComment() {
        isCommentList = adapter.getIsCommentList();
        enableLoad();
        if (!isCommentList) {
            commentLayout.setTextSize(18f);
            changeToCommentList();
            if (adapter.getCommentListBean() != null
                    && adapter.getCommentListBean().getItemList().size() > 0) {
                adapter.notifyDataSetChanged();
            } else {
                asyncDownloadComment();
            }
        } else {
            asyncDownloadComment();
        }
    }


    private void changeToRepostList() {
        adapter.setIsRepostList(true);
        adapter.setIsCommentList(false);
    }

    private void changeToCommentList() {
        adapter.setIsRepostList(false);
        adapter.setIsCommentList(true);
    }

    public MessageBean getMessageBean(){
        return messageBean;
    }

    private void asyncDownloadRepost() {
        new Thread() {
            public void run() {
                RepostsTimeLineByIdDao dao = new RepostsTimeLineByIdDao(
                        GlobalContext.getInstance().getSpecialToken(),
                        messageBean.getId());
                dao.setCount(String.valueOf(20));
                RepostListBean result = null;
                try {
                    result = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = REPOST_COMPLETE;
                    msg.obj = result;
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

    private void asyncDownloadComment() {

        new Thread() {
            public void run() {
                CommentsTimeLineByIdDao dao = new CommentsTimeLineByIdDao(
                        GlobalContext.getInstance().getSpecialToken(),
                        messageBean.getId());
                dao.setCount(String.valueOf(20));
                CommentListBean result = null;
                try {
                    result = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = COMMENT_COMPLETE;
                    msg.obj = result;
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

    private void asyncRefreshCommentAndRepostCount() {

        new Thread() {
            public void run() {
                List<String> msgIds = new ArrayList<String>();
                msgIds.add(messageBean.getId());
                TimeLineReCmtCountDao dao = new TimeLineReCmtCountDao(
                        GlobalContext.getInstance().getSpecialToken(), msgIds);
                try {
                    List<MessageReCmtCountBean> list = new ArrayList<MessageReCmtCountBean>();
                    list = dao.get();
                    Message msg = Message.obtain();
                    msg.what = REFRESH_COMMENT_REPOST_COUNT;
                    msg.obj = list;
                    handler.sendMessage(msg);
                } catch (WeiboException e) {
                    e.printStackTrace();
                }
            }

            ;
        }.start();
    }

    private void displayPictures(final MessageBean msg,
                                 final GridLayout layout, WeiboDetailImageView view,
                                 boolean refreshPic) {
        if (!msg.isMultiPics()) {
            view.setVisibility(View.VISIBLE);
            if (Utility.isTaskStopped(picTask) && refreshPic) {
                picTask = new MsgDetailReadWorker(view, msg);
                picTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                picTask.setView(view);
            }
        } else {
            layout.setVisibility(View.VISIBLE);

            final int count = msg.getPicCount();
            for (int i = 0; i < count; i++) {
                final ISimRayDrawable pic = (ISimRayDrawable) layout
                        .getChildAt(i);
                pic.setVisibility(View.VISIBLE);

                if (SettingUtility.getEnableBigPic()) {
                    TimeLineBitmapDownloader.getInstance().displayMultiPicture(
                            pic, msg.getHighPicUrls().get(i),
                            FileLocationMethod.picture_large);
                } else {
                    TimeLineBitmapDownloader.getInstance().displayMultiPicture(
                            pic, msg.getMiddlePicUrls().get(i),
                            FileLocationMethod.picture_bmiddle);
                }

                final int finalI = i;
                pic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        ArrayList<AnimationRect> animationRectArrayList = new ArrayList<AnimationRect>();
                        for (int i = 0; i < count; i++) {
                            final ISimRayDrawable pic = (ISimRayDrawable) layout
                                    .getChildAt(i);
                            ImageView imageView = (ImageView) pic;
                            if (imageView.getVisibility() == View.VISIBLE) {
                                AnimationRect rect = AnimationRect
                                        .buildFromImageView(imageView);
                                animationRectArrayList.add(rect);
                            }
                        }
                        Intent intent = GalleryAnimationActivity.newIntent(msg,
                                animationRectArrayList, finalI);
                        startActivity(intent);
                    }
                });
            }

            if (count < 9) {
                for (int i = count; i < 9; i++) {
                    ImageView pic = (ImageView) layout.getChildAt(i);
                    pic.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onLoadMore() {

        if (adapter.getIsCommentList()
                && adapter.getCommentListBean() != null && adapter.getCommentListBean().getSize() > 0 && canLoadCommentData) {
            enableLoad();
            asyncLoadComment();
        } else if ((!adapter.getIsCommentList()) && adapter.getRepostListBean() != null) {
            if (adapter.getRepostListBean().getSize() > 0 && canLoadRepostData) {
                enableLoad();
                asyncLoadRepost();
            }
        } else {
            disableLoad();
        }

    }

    private void disableLoad() {
        listView.getFooterView().hide();
    }

    private void enableLoad() {
        listView.getFooterView().show();
    }

    private void asyncLoadRepost() {
        enableLoad();
        new Thread() {
            public void run() {
                RepostListBean bean = adapter.getRepostListBean();
                long maxId = bean.getItemList().get(bean.getSize() - 1)
                        .getIdLong();
                String max_id = String.valueOf(maxId - 1);
                RepostsTimeLineByIdDao dao = new RepostsTimeLineByIdDao(
                        GlobalContext.getInstance().getSpecialToken(),
                        messageBean.getId());
                dao.setCount(String.valueOf(20));
                dao.setMax_id(max_id);
                RepostListBean result = null;
                try {
                    result = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = LOAD_REPOST_COMPLETE;
                    msg.obj = result;
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

    private void asyncLoadComment() {
        enableLoad();
        new Thread() {
            public void run() {
                CommentListBean bean = adapter.getCommentListBean();
                String maxId = bean.getItemList().get(bean.getSize() - 1)
                        .getId();
                long max_id = Long.valueOf(maxId) - 1;
                CommentsTimeLineByIdDao dao = new CommentsTimeLineByIdDao(
                        GlobalContext.getInstance().getSpecialToken(),
                        messageBean.getId());
                dao.setCount(String.valueOf(20));
                dao.setMax_id(String.valueOf(max_id));
                CommentListBean result = null;
                try {
                    result = dao.getGSONMsgList();
                    Message msg = Message.obtain();
                    msg.what = LOAD_COMMENT_COMPLETE;
                    msg.obj = result;
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

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case COMMENT_COMPLETE:
                    CommentListBean list = (CommentListBean) msg.obj;
                    commentList.replaceAll(list);
                    adapter.setCommentList(commentList);
                    adapter.notifyDataSetChanged();
                    if (list.getSize() > 0) {
                        listView.getFooterView().show();
                    }
                    break;
                case REPOST_COMPLETE:
                    RepostListBean newRepostList = (RepostListBean) msg.obj;
                    repostList.replaceAll((RepostListBean) msg.obj);
                    adapter.setRepostList((RepostListBean) msg.obj);
                    adapter.notifyDataSetChanged();
                    if (newRepostList.getSize() > 0) {
                        listView.getFooterView().show();
                    }
                    break;
                case NETWORK_ERROR:
                    String errorStr = (String) msg.obj;
                    Toast.makeText(GlobalContext.getInstance(), errorStr,
                            Toast.LENGTH_SHORT).show();
                    break;
                case REFRESH_COMMENT_REPOST_COUNT:
                    List<MessageReCmtCountBean> repCmtList = new ArrayList<MessageReCmtCountBean>();
                    repCmtList = (List<MessageReCmtCountBean>) msg.obj;
                    if (repCmtList.size() == 1) {
                        MessageReCmtCountBean bean = repCmtList.get(0);
                        msgReCmtCountBean = bean;
                        int repostCount = bean.getReposts();
                        int commentCount = bean.getComments();
                        adapter.setComRepCount(repostCount, commentCount);
                        messageBean.setReposts_count(repostCount);
                        messageBean.setComments_count(commentCount);
                        setResultData(messageBean);
                        adapter.notifyDataSetChanged();
                        commentLayout.setText(String.valueOf("评论 " + messageBean.getCommentscountString()));
                        repostLayout.setText(String.valueOf("转发 " + messageBean.getRepostscountString()));
                    }
                    break;
                case LOAD_COMMENT_COMPLETE:
                    CommentListBean oldCommentList = (CommentListBean) msg.obj;
                    if (oldCommentList != null && oldCommentList.getSize() > 0) {
                        commentList.addOldData(oldCommentList);
                        CommentListBean newCommentListBean = adapter
                                .getCommentListBean();
                        newCommentListBean.addOldData(oldCommentList);
                        adapter.setCommentList(newCommentListBean);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(GlobalContext.getInstance(), R.string.no_more_comment, Toast.LENGTH_SHORT)
                                .show();
                        disableLoad();
                        canLoadCommentData = false;
                    }

                    listView.stopLoadMore();
                    break;
                case LOAD_REPOST_COMPLETE:
                    RepostListBean oldRepostList = (RepostListBean) msg.obj;
                    if (oldRepostList != null && oldRepostList.getSize() > 0) {
                        repostList.addOldData((RepostListBean) msg.obj);
                        RepostListBean newRepostListBean = adapter
                                .getRepostListBean();
                        newRepostListBean.addOldData(oldRepostList);
                        adapter.setRepostList(newRepostListBean);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(GlobalContext.getInstance(), R.string.no_more_repost, Toast.LENGTH_SHORT)
                                .show();
                        disableLoad();
                        canLoadRepostData = false;
                    }
                    listView.stopLoadMore();
                    break;
            }
        }
    }

    public void setResultData(MessageBean bean) {
        if (getActivity() != null) {
            ((WeiboDetailActivity) getActivity()).getSwipeBackLayout().setResultData(bean);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        sendCommentCompletedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                asyncRefreshCommentAndRepostCount();
                asyncDownloadComment();
                asyncDownloadRepost();
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                sendCommentCompletedReceiver,
                new IntentFilter(AppEventAction
                        .buildSendCommentOrReplySuccessfullyAction(messageBean)));

        sendRepostCompletedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                asyncRefreshCommentAndRepostCount();
                asyncDownloadComment();
                asyncDownloadRepost();
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                sendRepostCompletedReceiver,
                new IntentFilter(AppEventAction
                        .buildSendRepostSuccessfullyAction(messageBean)));
    }

    public float getToolbarElevation() {
        if (Build.VERSION.SDK_INT >= 21) {
            return 12.8f;
        } else {
            return -1;
        }
    }
}
