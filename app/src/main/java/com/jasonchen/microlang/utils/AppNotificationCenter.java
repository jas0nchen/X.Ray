package com.jasonchen.microlang.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.MainActivity;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.CommentBean;
import com.jasonchen.microlang.beans.CommentListBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UnreadBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.database.CommentToMeTimeLineDBTask;
import com.jasonchen.microlang.database.MentionCommentsTimeLineDBTask;
import com.jasonchen.microlang.database.MentionWeiboTimeLineDBTask;
import com.jasonchen.microlang.database.NotificationDBTask;
import com.jasonchen.microlang.debug.AppLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AppNotificationCenter {

    public static class Callback {

        public void unreadMentionsChanged(AccountBean account, MessageListBean data) {

        }

        public void unreadMentionsCommentChanged(AccountBean account, CommentListBean data) {

        }

        public void unreadCommentsChanged(AccountBean account, CommentListBean data) {

        }
    }

    private static AppNotificationCenter instance = new AppNotificationCenter();

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private HashSet<Callback> callbackList = new HashSet<Callback>();

    private HashMap<AccountBean, MessageListBean> unreadMentions
            = new HashMap<AccountBean, MessageListBean>();
    private HashMap<AccountBean, CommentListBean> unreadMentionsComment
            = new HashMap<AccountBean, CommentListBean>();
    private HashMap<AccountBean, CommentListBean> unreadComments
            = new HashMap<AccountBean, CommentListBean>();
    private HashMap<AccountBean, UnreadBean> unreadBeans = new HashMap<AccountBean, UnreadBean>();

    public static synchronized AppNotificationCenter getInstance() {
        return instance;
    }

    public void addCallback(Callback callback) {
        this.callbackList.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.callbackList.remove(callback);
    }

    public UnreadBean getUnreadBean(AccountBean account) {
        UnreadBean data = this.unreadBeans.get(account);
        return data;
    }

    public void addUnreadBean(AccountBean account, UnreadBean data) {
        this.unreadBeans.put(account, data);
    }

    public void addUnreadMentions(final AccountBean account, final MessageListBean data) {
        this.unreadMentions.put(account, data);
    }

    public void addUnreadMentionsComment(final AccountBean account, final CommentListBean data) {
        this.unreadMentionsComment.put(account, data);
    }

    public void addUnreadComments(final AccountBean account, final CommentListBean data) {
        this.unreadComments.put(account, data);
    }

    public void refreshToUI(final AccountBean account) {

        final MessageListBean mentions = unreadMentions.get(account);
        final CommentListBean comments = unreadComments.get(account);
        final CommentListBean mentionsComment = unreadMentionsComment.get(account);

        this.uiHandler.post(new Runnable() {
            @Override
            public void run() {
                AppLogger.i("Post unread data to ui");

                for (Callback callback : callbackList) {
                    if (mentions != null) {
                        AppLogger.i("Post unread mention weibo data to ui: " + callback.getClass()
                                .getSimpleName());
                        callback.unreadMentionsChanged(account, mentions);
                    }
                    if (comments != null) {
                        AppLogger.i("Post unread comment to me data to ui: " + callback.getClass()
                                .getSimpleName());
                        callback.unreadCommentsChanged(account, comments);
                    }
                    if (mentionsComment != null) {
                        AppLogger
                                .i("Post unread mention comments data to ui: " + callback.getClass()
                                        .getSimpleName());
                        callback.unreadMentionsCommentChanged(account, mentionsComment);
                    }
                }
            }
        });
    }

    public void showAndroidNotification(AccountBean account) {

        UnreadBean unreadBean = unreadBeans.get(account);
        MessageListBean mentions = unreadMentions.get(account);
        CommentListBean comments = unreadComments.get(account);
        CommentListBean mentionsComment = unreadMentionsComment.get(account);

        String accountId = account.getUid();
        if (mentions == null) {
            mentions = new MessageListBean();
        }
        //addDatabaseUnreadMentionsWeibo(accountId, mentions);
        Utility.removeDuplicateAndSortStatus(mentions.getItemList());

        if (comments == null) {
            comments = new CommentListBean();
        }
        //addDatabaseUnreadCommentsToMe(accountId, comments);
        Utility.removeDuplicateAndSortComment(comments.getItemList());

        if (mentionsComment == null) {
            mentionsComment = new CommentListBean();
        }
        //addDatabaseUnreadMentionsComment(accountId, mentionsComment);
        Utility.removeDuplicateAndSortComment(mentionsComment.getItemList());

        showNotification(GlobalContext.getInstance(), account, mentions, comments,
                mentionsComment, unreadBean);
    }

    private void addDatabaseUnreadMentionsWeibo(String accountId, MessageListBean mentions) {
        MessageListBean dbData = MentionWeiboTimeLineDBTask
                .getRepostLineMsgList(accountId, 100);
        List<MessageBean> itemList = dbData.getItemList();

        HashMap<Long, MessageBean> map = new HashMap<>();

        for (MessageBean msg : itemList) {
            map.put(msg.getIdLong(), msg);
        }

        /*for (Object object : newMsgIds) {
            long id;
            if (object instanceof Double) {
                Double value = (Double) object;
                id = value.longValue();
            } else {
                id = (Long) object;
            }

            MessageBean msg = map.get(id);
            if (msg != null) {
                mentions.getItemList().add(msg);
            }
        }*/
    }

    private void addDatabaseUnreadMentionsComment(String accountId, CommentListBean mentions) {
        CommentListBean dbData = MentionCommentsTimeLineDBTask
                .getCommentLineMsgList(accountId, 100);
        List<CommentBean> itemList = dbData.getItemList();

        HashMap<Long, CommentBean> map = new HashMap<>();

        for (CommentBean msg : itemList) {
            map.put(msg.getIdLong(), msg);
        }

       /* for (Object object : newMsgIds) {
            long id;
            if (object instanceof Double) {
                Double value = (Double) object;
                id = value.longValue();
            } else {
                id = (Long) object;
            }

            CommentBean msg = map.get(id);
            if (msg != null) {
                mentions.getItemList().add(msg);
            }
        }*/
    }

    private void addDatabaseUnreadCommentsToMe(String accountId, CommentListBean mentions) {
        CommentListBean dbData = CommentToMeTimeLineDBTask
                .getCommentLineMsgList(accountId, 100);
        List<CommentBean> itemList = dbData.getItemList();

        HashMap<Long, CommentBean> map = new HashMap<>();

        for (CommentBean msg : itemList) {
            map.put(msg.getIdLong(), msg);
        }

        /*for (Object object : newMsgIds) {
            long id;
            if (object instanceof Double) {
                Double value = (Double) object;
                id = value.longValue();
            } else {
                id = (Long) object;
            }

            CommentBean msg = map.get(id);
            if (msg != null) {
                mentions.getItemList().add(msg);
            }
        }*/
    }

    private void showNotification(Context context, AccountBean accountBean,
            MessageListBean mentionsWeiboData, CommentListBean commentsToMeData
            , CommentListBean mentionsCommentData, UnreadBean unreadBean) {

        Intent clickNotificationToOpenAppPendingIntentInner = MainActivity.newIntent(accountBean, mentionsWeiboData, mentionsCommentData,
                commentsToMeData, unreadBean);
        clickNotificationToOpenAppPendingIntentInner
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        String accountId = accountBean.getUid();

        Set<String> dbUnreadMentionsWeibo = NotificationDBTask.getUnreadMsgIds(accountId,
                NotificationDBTask.UnreadDBType.mentionsWeibo);
        Set<String> dbUnreadMentionsComment = NotificationDBTask.getUnreadMsgIds(accountId,
                NotificationDBTask.UnreadDBType.mentionsComment);
        Set<String> dbUnreadCommentsToMe = NotificationDBTask.getUnreadMsgIds(accountId,
                NotificationDBTask.UnreadDBType.commentsToMe);

        if (mentionsWeiboData != null && mentionsWeiboData.getSize() > 0) {

            List<MessageBean> msgList = mentionsWeiboData.getItemList();
            Iterator<MessageBean> iterator = msgList.iterator();
            while (iterator.hasNext()) {
                MessageBean msg = iterator.next();
                if (dbUnreadMentionsWeibo.contains(msg.getId())) {
                    iterator.remove();
                }
            }
        }

        if (mentionsCommentData != null && mentionsCommentData.getSize() > 0) {
            List<CommentBean> msgList = mentionsCommentData.getItemList();
            Iterator<CommentBean> iterator = msgList.iterator();
            while (iterator.hasNext()) {
                CommentBean msg = iterator.next();
                if (dbUnreadMentionsComment.contains(msg.getId())) {
                    iterator.remove();
                }
            }
        }

        if (commentsToMeData != null && commentsToMeData.getSize() > 0) {
            List<CommentBean> msgList = commentsToMeData.getItemList();
            Iterator<CommentBean> iterator = msgList.iterator();
            while (iterator.hasNext()) {
                CommentBean msg = iterator.next();
                if (dbUnreadCommentsToMe.contains(msg.getId())) {
                    iterator.remove();
                }
            }
        }

        boolean mentionsWeibo = (mentionsWeiboData != null
                && mentionsWeiboData.getSize() > 0);
        boolean mentionsComment = (mentionsCommentData != null
                && mentionsCommentData.getSize() > 0);
        boolean commentsToMe = (commentsToMeData != null && commentsToMeData.getSize() > 0);

        if (!mentionsWeibo && !mentionsComment && !commentsToMe) {
            return;
        }

        String ticker = NotificationUtility
                .getTicker(unreadBean, mentionsWeiboData, mentionsCommentData,
                        commentsToMeData);
        Intent intent = BigTextNotificationService
                .newIntent(accountBean, mentionsWeiboData, commentsToMeData, mentionsCommentData,
                        unreadBean, clickNotificationToOpenAppPendingIntentInner, ticker, 0);
        context.startService(intent);
    }

}
