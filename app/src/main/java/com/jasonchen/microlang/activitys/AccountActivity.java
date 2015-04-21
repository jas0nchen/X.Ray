package com.jasonchen.microlang.activitys;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.database.AccountDBTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.view.FloatingActionButton;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * jasonchen
 * 2015/04/10
 */
public class AccountActivity extends ActionBarActivity implements
        LoaderManager.LoaderCallbacks<List<AccountBean>>, OnClickListener {

    private static final String ACTION_OPEN_FROM_APP_INNER = "com.jasonchen.microlang:accountactivity";
    private static final String ACTION_OPEN_FROM_APP_INNER_REFRESH_TOKEN = "com.jasonchen.microlang:accountactivity_refresh_token";
    private static final String REFRESH_ACTION_EXTRA = "refresh_account";

    private final int LOADER_ID = 0;
    private final int ADD_ACCOUNT_REQUEST_CODE = 0;

    private FloatingActionButton fab;
    private ListView mListView = null;
    private AccountAdapter listAdapter = null;
    private List<AccountBean> accountList = new ArrayList<AccountBean>();

    public static Intent newIntent() {
        Intent intent = new Intent(GlobalContext.getInstance(), AccountActivity.class);
        return intent;
    }

    public static Intent newIntent(AccountBean accountBean) {
        Intent intent = new Intent(GlobalContext.getInstance(), AccountActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        GlobalContext.getInstance().setCurrentRunningActivity(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            // Translucent status bar
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            RelativeLayout root = (RelativeLayout) findViewById(R.id.layout);
            root.setPadding(0, Utility.getStatusBarHeight(), 0, 0);
            View view = new View(this);
            LayoutParams lParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT, Utility.getStatusBarHeight());
            view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            view.setLayoutParams(lParams);
            ViewGroup viewGroup = (ViewGroup) getWindow().getDecorView();
            viewGroup.addView(view);
        }
        initView();
    }

    @SuppressLint("ResourceAsColor")
    private void initView() {
        // Initialize FAB
        fab = new FloatingActionButton.Builder(this)
                .withGravity(Gravity.BOTTOM | Gravity.RIGHT)
                .withPaddings(16, 16, 16, 16)
                .withDrawable(getResources().getDrawable(R.drawable.ic_plus))
                .withButtonColor(getResources().getColor(R.color.colorPrimary))
                .withButtonSize(100)
                .create();
        fab.setOnClickListener(this);
        fab.showFloatingActionButton();

        listAdapter = new AccountAdapter();
        mListView = (ListView) findViewById(R.id.account_list);
        mListView.setOnItemClickListener(new AccountListItemClickListener());
        mListView.setAdapter(listAdapter);
        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GlobalContext.getInstance().setCurrentRunningActivity(this);
    }

    @Override
    public void onClick(View view) {
        jumpToOAuthActivity();
    }

    private static class AccountDBLoader extends
            AsyncTaskLoader<List<AccountBean>> {

        public AccountDBLoader(Context context, Bundle args) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }

        public List<AccountBean> loadInBackground() {
            return AccountDBTask.getAccountList();
        }
    }

    @Override
    public Loader<List<AccountBean>> onCreateLoader(int id, Bundle args) {
        return new AccountDBLoader(AccountActivity.this, args);
    }

    @Override
    public void onLoadFinished(Loader<List<AccountBean>> loader,
                               List<AccountBean> data) {
        accountList = data;
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<AccountBean>> loader) {
        accountList = new ArrayList<AccountBean>();
        listAdapter.notifyDataSetChanged();
    }

    private class AccountListItemClickListener implements
            AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i,
                                long l) {
            if (!Utility.isTokenValid(accountList.get(i))) {
                jumpToOAuthActivity();
                return;
            } else if(!accountList.get(i).isBlack_magic() || !Utility.isHacyTokenValid(accountList.get(i))){
                jumToBlackActivity();
            } else {
                jumpToMainActivity(accountList.get(i));
            }

        }
    }

    private void jumToBlackActivity() {
        Intent intent = new Intent(AccountActivity.this, BlackMagicActivity.class);
        startActivityForResult(intent, ADD_ACCOUNT_REQUEST_CODE);
        overridePendingTransition(R.anim.push_left_in, R.anim.stay);
    }

    private class AccountAdapter extends BaseAdapter {

        public AccountAdapter() {

        }

        @Override
        public int getCount() {
            return accountList.size();
        }

        @Override
        public Object getItem(int i) {
            return accountList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return Long.valueOf(accountList.get(i).getUid());
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            final ViewHolder holder;
            if (view == null || view.getTag() == null) {
                LayoutInflater layoutInflater = getLayoutInflater();
                View mView = layoutInflater.inflate(
                        R.layout.activity_account_listview_item,
                        viewGroup, false);
                holder = new ViewHolder();
                holder.name = ViewUtility.findViewById(mView, R.id.username);
                holder.avatar = ViewUtility.findViewById(mView, R.id.avatar);
                holder.tokenInvalid = ViewUtility.findViewById(mView, R.id.token_expired);
                holder.more = ViewUtility.findViewById(mView, R.id.more);
                view = mView;
            } else {
                holder = (ViewHolder) view.getTag();
            }

            if (accountList.get(i).getInfo() != null) {
                holder.name.setText(accountList.get(i).getInfo()
                        .getScreen_name());
            } else {
                holder.name.setText(accountList.get(i).getUsernick());
            }

            if (!TextUtils.isEmpty(accountList.get(i).getAvatar_url())) {
                TimeLineBitmapDownloader.getInstance().downloadAvatar(holder.avatar,
                        accountList.get(i).getInfo(), false);
            }

            holder.tokenInvalid.setVisibility(!Utility.isTokenValid(accountList
                    .get(i)) ? View.VISIBLE : View.GONE);

            holder.more.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    final PopupMenu popupMenu = new PopupMenu(AccountActivity.this, holder.more);
                    popupMenu.inflate(R.menu.menu_account_activity_item);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            int id = menuItem.getItemId();
                            switch (id) {
                                case R.id.update:
                                    jumpToOAuthActivity();
                                    break;
                                case R.id.delete:
                                    remove(i);
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                }
            });

            return view;
        }
    }

    class ViewHolder {
        TextView name;
        ImageView avatar;
        TextView tokenInvalid;
        ImageView more;
    }

    public void jumpToMainActivity(AccountBean accountBean) {
        SettingUtility.setDefaultAccountId(accountBean.getUid());
		GlobalContext.getInstance().setAccountBean(accountBean);

		Intent intent = MainActivity.newIntent(accountBean);
		startActivity(intent);
        overridePendingTransition(R.anim.push_left_in, R.anim.stay);
		finish();
    }

    public void jumpToOAuthActivity() {
        Intent intent = new Intent(AccountActivity.this, OAuthActivity.class);
        startActivityForResult(intent, ADD_ACCOUNT_REQUEST_CODE);
        overridePendingTransition(R.anim.push_left_in, R.anim.stay);
    }

    // 接收从OauthActivity传回的数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        refresh();
    }

    private void refresh() {
        getLoaderManager().getLoader(LOADER_ID).forceLoad();
    }

    private void remove(int position) {
        Set<String> set = new HashSet<String>();
        long id = listAdapter.getItemId(position);
        set.add(String.valueOf(id));
        accountList = AccountDBTask.removeAndGetNewAccountList(set);
        listAdapter.notifyDataSetChanged();
    }

}
