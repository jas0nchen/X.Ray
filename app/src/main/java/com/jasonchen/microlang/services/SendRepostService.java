package com.jasonchen.microlang.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.WriteRepostActivity;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.RepostDraftBean;
import com.jasonchen.microlang.dao.RepostNewMsgDao;
import com.jasonchen.microlang.database.DraftDBManager;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.AppEventAction;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.NotificationUtility;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.Utility;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * jasonchen
 * 2015/04/10
 */
@SuppressLint("NewApi")
public class SendRepostService extends Service {

	private Map<WeiboSendTask, Boolean> tasksResult = new HashMap<WeiboSendTask, Boolean>();
	private Map<WeiboSendTask, Integer> tasksNotifications = new HashMap<WeiboSendTask, Integer>();

	private Handler handler = new Handler();
    private Vibrator vibrator;

	public static Intent newIntent(Context context, MessageBean msg, String contentString, boolean is_comment){
		Intent intent = new Intent(context,SendRepostService.class);
		intent.putExtra("oriMsg", msg);
		intent.putExtra("content", contentString);
		intent.putExtra("is_comment", is_comment);
		intent.putExtra("token", GlobalContext.getInstance().getSpecialToken());
		intent.putExtra("account", GlobalContext.getInstance().getAccountBean());
		return intent;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		int lastNotificationId = intent.getIntExtra("lastNotificationId", -1);
		if (lastNotificationId != -1) {
			NotificationUtility.cancel(lastNotificationId);
		}

		String token = intent.getStringExtra("token");
		AccountBean account = (AccountBean) intent
				.getParcelableExtra("account");
		String content = intent.getStringExtra("content");
		MessageBean oriMsg = (MessageBean) intent.getParcelableExtra("oriMsg");
		boolean is_comment = intent.getBooleanExtra("is_comment", false);

		RepostDraftBean repostDraftBean = (RepostDraftBean) intent
				.getParcelableExtra("draft");

		WeiboSendTask task = new WeiboSendTask(token, account, content, oriMsg,
				is_comment, repostDraftBean);
		task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);

		tasksResult.put(task, false);
		vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		return START_REDELIVER_INTENT;

	}

	private class WeiboSendTask extends MyAsyncTask<Void, Long, Void> {

		Notification notification;
		WeiboException e;

		String token;
		AccountBean account;
		String content;
		MessageBean oriMsg;
		boolean is_comment;
		RepostDraftBean repostDraftBean;

		public WeiboSendTask(String token, AccountBean account, String content,
				MessageBean oriMsg, boolean is_comment,
				RepostDraftBean repostDraftBean) {
			this.token = token;
			this.account = account;
			this.content = content;
			this.oriMsg = oriMsg;
			this.is_comment = is_comment;
			this.repostDraftBean = repostDraftBean;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Notification.Builder builder = new Notification.Builder(
					SendRepostService.this)
					.setTicker(getString(R.string.sending_repost))
					.setContentTitle(getString(R.string.sending_repost))
					.setContentText(content).setOnlyAlertOnce(true)
					.setOngoing(true).setSmallIcon(R.drawable.ic_status_sending);

			builder.setProgress(0, 100, true);

			int notificationId = new Random().nextInt(Integer.MAX_VALUE);

			notification = builder.getNotification();

			NotificationUtility.show(notification, notificationId);

			tasksNotifications.put(WeiboSendTask.this, notificationId);

		}

		private MessageBean sendText() throws WeiboException {
			RepostNewMsgDao dao = new RepostNewMsgDao(token, oriMsg.getId());
			if (is_comment) {
				dao.setIs_comment("1");
			}
			dao.setStatus(content);
			return dao.sendNewMsg();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				sendText();
			} catch (WeiboException e) {
				this.e = e;
				cancel(true);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			if (repostDraftBean != null)
				DraftDBManager.getInstance().remove(repostDraftBean.getId());
			showSuccessfulNotification(WeiboSendTask.this);

		}

		@Override
		protected void onCancelled(Void aVoid) {
			super.onCancelled(aVoid);
			showFailedNotification(WeiboSendTask.this);

		}

		private void showSuccessfulNotification(final WeiboSendTask task) {
			Notification.Builder builder = new Notification.Builder(
					SendRepostService.this)
					.setTicker(getString(R.string.send_successfully))
					.setContentTitle(getString(R.string.send_successfully))
					.setOnlyAlertOnce(true).setAutoCancel(true)
					.setSmallIcon(R.drawable.ic_status_send_success)
					.setOngoing(false);
			Notification notification = builder.getNotification();

			final int id = tasksNotifications.get(task);
			NotificationUtility.show(notification, id);

			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					NotificationUtility.cancel(id);
					stopServiceIfTasksAreEnd(task);
				}
			}, 3000);

            vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            long [] pattern = {100,400};
            if(SettingUtility.allowVibrate()) {
                vibrator.vibrate(pattern, -1);
            }

			LocalBroadcastManager
					.getInstance(SendRepostService.this)
					.sendBroadcast(
							new Intent(AppEventAction
									.buildSendRepostSuccessfullyAction(oriMsg)));

		}

		private void showFailedNotification(final WeiboSendTask task) {
			Notification.Builder builder = new Notification.Builder(
					SendRepostService.this)
					.setTicker(getString(R.string.send_failed))
					.setContentTitle(
							getString(R.string.send_faile_click_to_open))
					.setContentText(content).setOnlyAlertOnce(true)
					.setAutoCancel(true).setSmallIcon(R.drawable.ic_status_send_fail)
					.setOngoing(false);

			Intent notifyIntent = WriteRepostActivity.startBecauseSendFailed(
					SendRepostService.this, account, content, oriMsg,
					is_comment, repostDraftBean, e.getError());

			PendingIntent pendingIntent = PendingIntent.getActivity(
					SendRepostService.this, 0, notifyIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			builder.setContentIntent(pendingIntent);

			Notification notification;
			if (Utility.isJB()) {
				Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle(
						builder);
				bigTextStyle
						.setBigContentTitle(getString(R.string.send_faile_click_to_open));
				bigTextStyle.bigText(content);
				bigTextStyle.setSummaryText(account.getUsernick());
				builder.setStyle(bigTextStyle);

				Intent intent = new Intent(SendRepostService.this,
						SendRepostService.class);
				intent.putExtra("oriMsg", oriMsg);
				intent.putExtra("content", content);
				intent.putExtra("is_comment", is_comment);
				intent.putExtra("token", token);
				intent.putExtra("account", account);

				intent.putExtra("lastNotificationId",
						tasksNotifications.get(task));

				PendingIntent retrySendIntent = PendingIntent.getService(
						SendRepostService.this, 0, intent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				builder.addAction(R.drawable.ic_status_sending,
						getString(R.string.retry_send), retrySendIntent);
				notification = builder.build();

			} else {
				notification = builder.getNotification();
			}

			final int id = tasksNotifications.get(task);

			NotificationUtility.show(notification, id);

			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					stopServiceIfTasksAreEnd(task);
				}
			}, 3000);
		}

	}

	private void stopServiceIfTasksAreEnd(WeiboSendTask currentTask) {

		tasksResult.put(currentTask, true);

		boolean isAllTaskEnd = true;
		Set<WeiboSendTask> taskSet = tasksResult.keySet();
		for (WeiboSendTask task : taskSet) {
			if (!tasksResult.get(task)) {
				isAllTaskEnd = false;
				break;
			}
		}
		if (isAllTaskEnd) {
            vibrator.cancel();
			stopForeground(true);
			stopSelf();
		}
	}

}
