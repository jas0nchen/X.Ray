package com.jasonchen.microlang.activitys;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.FriendshipsDao;
import com.jasonchen.microlang.dao.ShowUserDao;
import com.jasonchen.microlang.dao.StatusesTimeLineDao;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.fragments.UserFragment;
import com.jasonchen.microlang.fragments.WeiboDetailFragment;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/10
 */
public class UserActivity extends SwipeBackActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String ACTION_WITH_DETAIL = "action_with_detail";

    private UserBean userBean;
    private String id;

    private FechUserTask fechUserTask;
    private FollowTask followTask;
    private UnFollowTask unFollowTask;

    public static Intent newIntent(Context context, UserBean userBean) {
        Intent intent = new Intent(context, UserActivity.class);
        intent.putExtra("user", userBean);
        intent.setAction(ACTION_WITH_DETAIL);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_user;
        super.onCreate(savedInstanceState);

        initView();
    }

    private void initView() {
        getSupportActionBar().setTitle(getString(R.string.app_name));
        String action = getIntent().getAction();
        if (action.equals(ACTION_WITH_DETAIL)) {
            handleUserInfoWithDetail();
        } else {
            Uri data = getIntent().getData();
            if (data != null) {
                String name = data.toString();
                AppLogger.e(name);
                handleUserInfoWithId(name);
            }
        }
    }

    private void handleUserInfoWithId(String name) {
        userBean = new UserBean();
        fetchUserWithId(name);
    }

    private void fetchUserWithId(String name) {
        if (Utility.isTaskStopped(fechUserTask)) {
            fechUserTask = new FechUserTask(UserActivity.this, name);
            fechUserTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void handleUserInfoWithDetail() {
        userBean = getIntent().getParcelableExtra("user");
        getSupportActionBar().setTitle(userBean.getScreen_name());
        buildContent();

    }

    private void buildContent() {
        getSupportActionBar().setTitle(userBean.getScreen_name());
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getSupportFragmentManager().findFragmentByTag(UserFragment.class.getName()) == null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, UserFragment.newInstance(userBean),
                                    UserFragment.class.getName())
                            .commitAllowingStateLoss();
                    getSupportFragmentManager().executePendingTransactions();
                    findViewById(R.id.container).setBackgroundDrawable(null);
                }
            }
        });
    }

    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
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
    public void onRefresh() {

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_user, menu);
        if (userBean != null && !GlobalContext.getInstance().getAccountBean().getUid().equals(userBean.getId())) {
            if (userBean.isFollowing()) {
                menu.findItem(R.id.menu_unfollow).setVisible(true);
            } else {
                menu.findItem(R.id.menu_follow).setVisible(true);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (userBean != null && !GlobalContext.getInstance().getAccountBean().getUid().equals(userBean.getId())) {
            if (userBean.isFollowing()) {
                menu.findItem(R.id.menu_unfollow).setVisible(true);
                menu.findItem(R.id.menu_follow).setVisible(false);
            } else {
                menu.findItem(R.id.menu_follow).setVisible(true);
                menu.findItem(R.id.menu_unfollow).setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_at) {
            Intent intent = WriteWeiboActivity.newIntent(UserActivity.this, GlobalContext.getInstance().getAccountBean(), userBean.getScreen_name());
            startActivity(intent);
            openActivityWithAnimation();
        } else if (itemId == R.id.menu_follow) {
            if (Utility.isTaskStopped(followTask) && Utility.isTaskStopped(unFollowTask)) {
                followTask = new FollowTask(UserActivity.this, userBean, GlobalContext.getInstance().getSpecialToken());
                followTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else if (itemId == R.id.menu_unfollow) {
            if (Utility.isTaskStopped(followTask) && Utility.isTaskStopped(unFollowTask)) {
                unFollowTask = new UnFollowTask(UserActivity.this, userBean, GlobalContext.getInstance().getSpecialToken());
                unFollowTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
        return false;
    }

    private class FollowTask extends MyAsyncTask<Void, Void, UserBean> {

        UserActivity activity;
        UserBean user;
        String token;
        WeiboException e;

        private FollowTask(UserActivity activity, UserBean user, String token) {
            this.activity = activity;
            this.user = user;
            this.token = token;
        }

        @Override
        protected UserBean doInBackground(Void... params) {
            FriendshipsDao dao = new FriendshipsDao(token);
            dao.setUid(user.getId());
            UserBean followedUser = null;
            try {
                followedUser = dao.followIt();
            } catch (WeiboException e) {
                e.printStackTrace();
                this.e = e;
                cancel(true);
            }
            return followedUser;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Toast.makeText(UserActivity.this, e.getError(), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(UserBean userBean) {
            super.onPostExecute(userBean);
            if (userBean != null) {
                Toast.makeText(activity, activity.getString(R.string.follow_successfully), Toast.LENGTH_SHORT).show();
                UserBean oldUser = userBean;
                oldUser.setFollowing(true);
                activity.setUserBean(oldUser);
            } else {
                Toast.makeText(activity, activity.getString(R.string.follow_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UnFollowTask extends MyAsyncTask<Void, Void, UserBean> {

        UserActivity activity;
        UserBean user;
        String token;
        WeiboException e;

        private UnFollowTask(UserActivity activity, UserBean user, String token) {
            this.activity = activity;
            this.user = user;
            this.token = token;
        }

        @Override
        protected UserBean doInBackground(Void... params) {
            FriendshipsDao dao = new FriendshipsDao(token);
            dao.setUid(user.getId());
            UserBean unFollowedUser = null;
            try {
                unFollowedUser = dao.unFollowIt();
            } catch (WeiboException e) {
                e.printStackTrace();
                this.e = e;
                cancel(true);
            }
            return unFollowedUser;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Toast.makeText(UserActivity.this, e.getError(), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(UserBean userBean) {
            super.onPostExecute(userBean);
            if (userBean != null) {
                Toast.makeText(activity, activity.getString(R.string.unfollow_successfully), Toast.LENGTH_SHORT).show();
                UserBean oldUser = userBean;
                oldUser.setFollowing(false);
                activity.setUserBean(oldUser);
            } else {
                Toast.makeText(activity, activity.getString(R.string.unfollow_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class ProgressFragment extends DialogFragment {

        MyAsyncTask asyncTask = null;

        public static ProgressFragment newInstance() {
            ProgressFragment frag = ProgressFragment.newInstance();
            frag.setRetainInstance(true);
            Bundle args = new Bundle();
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getString(R.string.fech_user_info));
            dialog.setIndeterminate(false);
            dialog.setCancelable(true);
            return dialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (asyncTask != null) {
                asyncTask.cancel(true);
            }
            super.onCancel(dialog);
        }

        void setAsyncTask(MyAsyncTask task) {
            asyncTask = task;
        }
    }

    private class FechUserTask extends MyAsyncTask<Void, Void, UserBean> {

        Context context;
        String username;
        UserBean bean = null;
        WeiboException e;
        ProgressFragment progressFragment;

        private FechUserTask(Context context, String name) {
            this.context = context;
            this.username = name;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressFragment = new ProgressFragment();
            progressFragment.setAsyncTask(this);
            progressFragment.show(getSupportFragmentManager(), "");
        }

        @Override
        protected UserBean doInBackground(Void... params) {
            ShowUserDao dao = new ShowUserDao(GlobalContext.getInstance()
                    .getSpecialToken());
            int index = username.lastIndexOf("@");
            final String newValue = username.substring(index + 1);
            dao.setScreen_name(newValue);
            try {
                bean = dao.getUserInfo();
            } catch (WeiboException e) {
                e.printStackTrace();
                this.e = e;
                cancel(true);
                return null;
            }
            return bean;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressFragment.dismissAllowingStateLoss();
            Toast.makeText(GlobalContext.getInstance(), e.getError(), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(UserBean userBean) {
            super.onPostExecute(userBean);
            if (userBean != null) {
                setUserBean(userBean);
                buildContent();
            }
            progressFragment.dismissAllowingStateLoss();
        }
    }
}

