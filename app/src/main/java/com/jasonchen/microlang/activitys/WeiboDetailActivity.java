package com.jasonchen.microlang.activitys;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.FavBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.dao.DestroyStatusDao;
import com.jasonchen.microlang.dao.FavDao;
import com.jasonchen.microlang.database.FriendsTimeLineDBTask;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.fragments.WeiboDetailFragment;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.Utility;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/10
 */
public class WeiboDetailActivity extends SwipeBackActivity{

    private static final String ACTION_WITH_ID = "action_with_id";
    private static final String ACTION_WITH_DETAIL = "action_with_detail";

    private AccountBean accountBean;
    private MessageBean messageBean;
    private String messageId;
    private String token;

    private FavTask favTask;
    private UnFavTask unFavTask;
    private DestroyStatusTask deleteTask;

    public static Intent newIntent(Context context, AccountBean accountBean, String id, String token) {
        Intent intent = new Intent(context, WeiboDetailActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("account", accountBean);
        intent.putExtra("token", token);
        intent.setAction(ACTION_WITH_ID);
        return intent;
    }

    public static Intent newIntent(Context context, AccountBean accountBean, MessageBean messageBean, String token){
        Intent intent = new Intent(context, WeiboDetailActivity.class);
        intent.putExtra("token", token);
        intent.putExtra("account", accountBean);
        intent.putExtra("message", messageBean);
        intent.setAction(ACTION_WITH_DETAIL);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_weibodetail;
        super.onCreate(savedInstanceState);
        initView(savedInstanceState);
    }

    private void initView(Bundle savedInstanceState) {
        getSupportActionBar().setTitle(getString(R.string.weibo_detail));
        if (savedInstanceState != null) {
            accountBean = savedInstanceState.getParcelable("account");
            messageBean = savedInstanceState.getParcelable("message");
            token = savedInstanceState.getString("token");
            if (messageBean != null) {
                buildContent();
            } else {
                messageId = getIntent().getStringExtra("id");
                fetchMessage();
            }

        } else {
            String action = getIntent().getAction();
            if (ACTION_WITH_ID.equalsIgnoreCase(action)) {
                token = getIntent().getStringExtra("token");
                messageId = getIntent().getStringExtra("id");
                accountBean = getIntent().getParcelableExtra("account");
            } else if (ACTION_WITH_DETAIL.equalsIgnoreCase(action)) {
                Intent intent = getIntent();
                token = intent.getStringExtra("token");
                messageBean = intent.getParcelableExtra("message");
                messageId = messageBean.getId();
                accountBean = getIntent().getParcelableExtra("account");
            } else {
                throw new IllegalArgumentException("activity intent action must be " + ACTION_WITH_DETAIL + " or "
                        + ACTION_WITH_ID);
            }
            buildContent();
        }
    }

    private void buildContent() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getSupportFragmentManager().findFragmentByTag(WeiboDetailFragment.class.getName()) == null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, WeiboDetailFragment.newInstance(messageBean),
                                    WeiboDetailFragment.class.getName())
                            .commitAllowingStateLoss();
                    getSupportFragmentManager().executePendingTransactions();
                    findViewById(R.id.container).setBackgroundDrawable(null);
                }
            }
        });
    }

    public Toolbar getToolbar(){
        return mToolbar;
    }

    private void fetchMessage() {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("message", messageBean);
        outState.putString("token", token);
        outState.putParcelable("account", accountBean);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (messageBean == null) {
            return super.onCreateOptionsMenu(menu);
        }
        getMenuInflater().inflate(R.menu.menu_activity_weibodetail, menu);
        if (messageBean.getUser() != null && messageBean.getUser().getId().equals(GlobalContext.getInstance().getCurrentAccountId())) {
            menu.findItem(R.id.menu_delete).setVisible(true);
        }
        if(messageBean.isFavorited()){
            menu.findItem(R.id.menu_unfav).setVisible(true);
        }else{
            menu.findItem(R.id.menu_fav).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (messageBean == null) {
            return super.onPrepareOptionsMenu(menu);
        }

        if(messageBean.isFavorited()){
            menu.findItem(R.id.menu_unfav).setVisible(true);
            menu.findItem(R.id.menu_fav).setVisible(false);
        }else{
            menu.findItem(R.id.menu_unfav).setVisible(false);
            menu.findItem(R.id.menu_fav).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int itemId = item.getItemId();
        if (itemId == R.id.menu_repost) {
            intent = WriteRepostActivity.newIntent(WeiboDetailActivity.this, accountBean, messageBean, token);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_comment) {
            intent = WriteCommentActivity.newIntent(WeiboDetailActivity.this, accountBean, messageBean, token);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menu_copy) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("sinaweibo", messageBean.getText()));
            Toast.makeText(this, getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_fav) {
            if (Utility.isTaskStopped(favTask) && Utility.isTaskStopped(unFavTask)) {
                favTask = new FavTask(WeiboDetailActivity.this);
                favTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
            return true;
        } else if (itemId == R.id.menu_unfav) {
            if (Utility.isTaskStopped(favTask) && Utility.isTaskStopped(unFavTask)) {
                unFavTask = new UnFavTask(WeiboDetailActivity.this);
                unFavTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
            }
            return true;
        } else if (itemId == R.id.menu_delete) {
            final MaterialDialog deleteDialog = new MaterialDialog(WeiboDetailActivity.this);
            deleteDialog.setTitle(getString(R.string.notice))
                    .setMessage(getString(R.string.delete_status))
                    .setPositiveButton(getString(R.string.confirm), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            deleteDialog.dismiss();
                            if(Utility.isTaskStopped(deleteTask)){
                                deleteTask = new DestroyStatusTask(WeiboDetailActivity.this);
                                deleteTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            deleteDialog.dismiss();
                        }
                    }).show();
            return true;
        }
        return false;
    }

    private class FavTask extends MyAsyncTask<Void, Void, FavBean> {

        FavBean favBean = null;
        Context context;
        WeiboException e;

        private FavTask(Context context) {
            this.context = context;
        }

        @Override
        protected FavBean doInBackground(Void... params) {
            FavDao dao = new FavDao(GlobalContext.getInstance().getSpecialToken(), messageBean.getId());
            try {
                favBean = dao.favIt();
            } catch (WeiboException e) {
                e.printStackTrace();
                this.e = e;
                cancel(true);
            }
            return favBean;
        }

        @Override
        protected void onCancelled(FavBean favBean) {
            super.onCancelled(favBean);
            if (this.e != null) {
                Toast.makeText(GlobalContext.getInstance(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(FavBean favBean) {
            super.onPostExecute(favBean);
            if (favBean != null) {
                Toast.makeText(context, getResources().getString(R.string.favorite_success), Toast.LENGTH_SHORT).show();
                messageBean.setFavorited(true);
                FriendsTimeLineDBTask.asyncUpdateMsg(messageBean.getId(), true);
            }
        }
    }

    private class UnFavTask extends MyAsyncTask<Void, Void, FavBean> {

        FavBean favBean = null;
        Context context;
        WeiboException e;

        private UnFavTask(Context context) {
            this.context = context;
        }

        @Override
        protected FavBean doInBackground(Void... params) {
            FavDao dao = new FavDao(GlobalContext.getInstance().getSpecialToken(), messageBean.getId());
            try {
                favBean = dao.unFavIt();
            } catch (WeiboException e) {
                e.printStackTrace();
                this.e = e;
                cancel(true);
                return null;
            }
            return favBean;
        }

        @Override
        protected void onCancelled(FavBean favBean) {
            super.onCancelled(favBean);
            if (this.e != null) {
                Toast.makeText(GlobalContext.getInstance(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(FavBean favBean) {
            super.onPostExecute(favBean);
            if (favBean != null) {
                Toast.makeText(context, getResources().getString(R.string.unfavorite_success), Toast.LENGTH_SHORT).show();
                messageBean.setFavorited(false);
                FriendsTimeLineDBTask.asyncUpdateMsg(messageBean.getId(), false);
            }
        }
    }

    private class DestroyStatusTask extends
            MyAsyncTask<Void, Void, DestroyResult> {

        boolean result;
        Context context;

        public DestroyStatusTask(Context context) {
            super();
            this.context = context;
        }

        @Override
        protected DestroyResult doInBackground(Void... params) {
            DestroyStatusDao dao = new DestroyStatusDao(GlobalContext.getInstance().getSpecialToken(), messageId);
            try {
                result = dao.destroy();
            } catch (WeiboException e) {
                e.printStackTrace();
            }
            if (result) {
                return DestroyResult.success;
            } else {
                return DestroyResult.failed;
            }
        }

        @Override
        protected void onPostExecute(DestroyResult result) {
            super.onPostExecute(result);
            switch (result) {
                case success:
                    Toast.makeText(context, getResources().getString(R.string.remove_successfully), Toast.LENGTH_SHORT).show();
                    FriendsTimeLineDBTask.deleteMsg(GlobalContext.getInstance()
                            .getAccountBean().getUid(), messageId);
                    break;

                case failed:
                    Toast.makeText(context, getResources().getString(R.string.remove_fail), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    public enum DestroyResult {
        success, failed
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if (getSupportFragmentManager().findFragmentByTag(WeiboDetailFragment.class.getName()) != null) {
                messageBean = ((WeiboDetailFragment) getSupportFragmentManager().findFragmentByTag(WeiboDetailFragment.class.getName())).getMessageBean();
                Intent data = new Intent();
                data.putExtra("msg", messageBean);
                AppLogger.d("评论数 " + messageBean.getCommentscountString());
                setResult(1, data);
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
