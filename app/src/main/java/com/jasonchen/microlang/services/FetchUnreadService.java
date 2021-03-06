package com.jasonchen.microlang.services;


import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.CommentBean;
import com.jasonchen.microlang.beans.CommentListBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UnreadBean;
import com.jasonchen.microlang.dao.CommentToMeDao;
import com.jasonchen.microlang.dao.MentionsCommentTimeLineDao;
import com.jasonchen.microlang.dao.MentionsStatusTimeLineDao;
import com.jasonchen.microlang.dao.UnreadDao;
import com.jasonchen.microlang.database.AccountDBTask;
import com.jasonchen.microlang.database.CommentToMeTimeLineDBTask;
import com.jasonchen.microlang.database.MentionCommentsTimeLineDBTask;
import com.jasonchen.microlang.database.MentionWeiboTimeLineDBTask;
import com.jasonchen.microlang.database.NotificationDBTask;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.utils.AppNotificationCenter;
import com.jasonchen.microlang.utils.GlobalContext;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.jasonchen.microlang.exception.WeiboException;

/**
 * jasonchen
 * 2015/04/15
 */
public class FetchUnreadService extends IntentService {

    public static Intent newIntentFromAlarmManager() {
        Intent intent = new Intent(GlobalContext.getInstance(), FetchUnreadService.class);
        intent.setAction(ACTION_ALARM_MANAGER);
        return intent;
    }

    public static Intent newIntentFromOpenApp() {
        Intent intent = new Intent(GlobalContext.getInstance(), FetchUnreadService.class);
        intent.setAction(ACTION_OPEN_APP);
        return intent;
    }

    private static final String ACTION_ALARM_MANAGER = "com.jasonchen.microlang:alarmmanager";
    private static final String ACTION_OPEN_APP = "com.jasonchen.microlang:openapp";

    //close service between 1 clock and 8 clock
    private static final int NIGHT_START_TIME_HOUR = 1;
    private static final int NIGHT_END_TIME_HOUR = 7;

    public FetchUnreadService() {
        super("FetchUnreadService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (ACTION_ALARM_MANAGER.equals(action)) {
            AppLogger.i("FetchUnreadService is started by " + ACTION_ALARM_MANAGER);
            if (SettingUtility.disableFetchAtNight() && isNowNight()) {
                AppLogger.i("FetchUnreadService is disabled at night, so give up");
                return;
            }
        } else if (ACTION_OPEN_APP.equals(action)) {
            //empty
            AppLogger.i("FetchUnreadService is started by " + ACTION_OPEN_APP);
        } else {
            AppLogger.i("FetchUnreadService receive Intent whose Action is empty");
            //throw new IllegalArgumentException("Intent action is empty");
            return;
        }

        List<AccountBean> accountBeanList = AccountDBTask.getAccountList();
        if (accountBeanList.size() == 0) {
            return;
        }
        for (AccountBean account : accountBeanList) {
            try {
                AppLogger.i("FetchUnreadService start fetch " + account.getUsernick()
                        + "'s unread data");
                fetchMsg(account);
            } catch (WeiboException e) {
                e.printStackTrace();
            }
        }
        AppLogger.i("FetchUnreadService finished");
    }

    private boolean isNowNight() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= NIGHT_START_TIME_HOUR && hour <= NIGHT_END_TIME_HOUR;
    }

    private void fetchMsg(AccountBean accountBean) throws WeiboException {
        CommentListBean commentResult = null;
        MessageListBean mentionStatusesResult = null;
        CommentListBean mentionCommentsResult = null;
        UnreadBean unreadBean = null;

        String token = accountBean.getAccess_token();

        UnreadDao unreadDao = new UnreadDao(token, accountBean.getUid());
        unreadBean = unreadDao.getCount();
        if (unreadBean == null) {
            return;
        }
        int unreadCommentCount = unreadBean.getCmt();
        int unreadMentionStatusCount = unreadBean.getMention_status();
        int unreadMentionCommentCount = unreadBean.getMention_cmt();

        AppLogger.e("评论：" + unreadCommentCount + ",提及微博:" + unreadMentionStatusCount + ",提及评论: "+ unreadMentionCommentCount);

        if (unreadCommentCount > 0 && SettingUtility.allowCommentToMe()) {
            CommentToMeDao dao = new CommentToMeDao(token);
            CommentListBean oldData = null;
            int total = CommentToMeTimeLineDBTask.getTotalNumber(accountBean.getUid());
            CommentListBean commentTimeLineData = CommentToMeTimeLineDBTask
                    .getCommentLineMsgList(accountBean.getUid(), total);
            if (commentTimeLineData != null) {
                oldData = commentTimeLineData;
            }
            if (oldData != null && oldData.getSize() > 0) {
                dao.setSince_id(oldData.getItem(0).getId());
            }
            NotificationDBTask.addUnreadFlag(GlobalContext.getInstance().getCurrentAccountId(), NotificationDBTask.UnreadDBType.commentsToMe);
            commentResult = dao.getGSONMsgListWithoutClearUnread();
        }

        if (unreadMentionStatusCount > 0 && SettingUtility.allowMentionToMe()) {
            MentionsStatusTimeLineDao dao = new MentionsStatusTimeLineDao(token);
            MessageListBean oldData = null;
            int total = MentionWeiboTimeLineDBTask.getTotalNumber(accountBean.getUid());
            MessageListBean mentionStatusTimeLineData = MentionWeiboTimeLineDBTask
                    .getRepostLineMsgList(accountBean.getUid(), total);
            if (mentionStatusTimeLineData != null) {
                oldData = mentionStatusTimeLineData;
            }
            if (oldData != null && oldData.getSize() > 0) {
                dao.setSince_id(oldData.getItem(0).getId());
            }
            NotificationDBTask.addUnreadFlag(GlobalContext.getInstance().getCurrentAccountId(), NotificationDBTask.UnreadDBType.mentionsWeibo);
            mentionStatusesResult = dao.getGSONMsgListWithoutClearUnread();
        }

        if (unreadMentionCommentCount > 0 && SettingUtility.allowMentionCommentToMe()) {
            MentionsCommentTimeLineDao dao = new MentionsCommentTimeLineDao(token);
            CommentListBean oldData = null;
            int total = MentionCommentsTimeLineDBTask.getTotalNumber(accountBean.getUid());
            CommentListBean commentTimeLineData = MentionCommentsTimeLineDBTask
                    .getCommentLineMsgList(accountBean.getUid(), total);
            if (commentTimeLineData != null) {
                oldData = commentTimeLineData;
            }
            if (oldData != null && oldData.getSize() > 0) {
                dao.setSince_id(oldData.getItem(0).getId());
            }
            AppLogger.e("ready to write unread mention comment flag!!!");
            NotificationDBTask.addUnreadFlag(GlobalContext.getInstance().getCurrentAccountId(), NotificationDBTask.UnreadDBType.mentionsComment);
            mentionCommentsResult = dao.getGSONMsgListWithoutClearUnread();
        }

        clearDatabaseUnreadInfo(accountBean.getUid(), unreadBean.getMention_status(),
                unreadBean.getMention_cmt(), unreadBean.getCmt());

        boolean mentionsWeibo = (mentionStatusesResult != null
                && mentionStatusesResult.getSize() > 0);
        boolean mentionsComment = (mentionCommentsResult != null
                && mentionCommentsResult.getSize() > 0);
        boolean commentsToMe = (commentResult != null && commentResult.getSize() > 0);

        if (mentionsWeibo || mentionsComment || commentsToMe) {
            sendTwoKindsOfBroadcast(accountBean, commentResult, mentionStatusesResult,
                    mentionCommentsResult, unreadBean);
            //Toast.makeText(GlobalContext.getInstance(), "有新消息！", Toast.LENGTH_SHORT).show();
        } else {
//            NotificationManager notificationManager = (NotificationManager) getApplicationContext()
//                    .getSystemService(NOTIFICATION_SERVICE);
//            notificationManager.cancel(
//                    NotificationServiceHelper.getMentionsWeiboNotificationId(accountBean));
        }
    }

    private void clearDatabaseUnreadInfo(String accountId, int mentionsWeibo, int mentionsComment,
                                         int cmt) {
        if (mentionsWeibo == 0) {
            NotificationDBTask
                    .asyncCleanUnread(accountId, NotificationDBTask.UnreadDBType.mentionsWeibo);
        }
        if (mentionsComment == 0) {
            NotificationDBTask
                    .asyncCleanUnread(accountId, NotificationDBTask.UnreadDBType.mentionsComment);
        }
        if (cmt == 0) {
            NotificationDBTask
                    .asyncCleanUnread(accountId, NotificationDBTask.UnreadDBType.commentsToMe);
        }
    }

    private void sendTwoKindsOfBroadcast(AccountBean accountBean,
                                         CommentListBean commentResult,
                                         MessageListBean mentionStatusesResult,
                                         CommentListBean mentionCommentsResult,
                                         UnreadBean unreadBean) {

        AppLogger.i("Send unread data to ");

        if (unreadBean != null) {
            AppNotificationCenter.getInstance().addUnreadBean(accountBean, unreadBean);
        }
        if (mentionStatusesResult != null) {
            AppNotificationCenter.getInstance()
                    .addUnreadMentions(accountBean, mentionStatusesResult);
        }
        if (mentionCommentsResult != null) {
            AppNotificationCenter.getInstance()
                    .addUnreadMentionsComment(accountBean, mentionCommentsResult);

        }
        if (commentResult != null) {
            AppNotificationCenter.getInstance().addUnreadComments(accountBean, commentResult);
        }
        AppNotificationCenter.getInstance().refreshToUI(accountBean);

        AppNotificationCenter.getInstance().showAndroidNotification(accountBean);
    }
}
