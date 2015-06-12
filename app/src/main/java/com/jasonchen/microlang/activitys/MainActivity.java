package com.jasonchen.microlang.activitys;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.CommentListBean;
import com.jasonchen.microlang.beans.GroupBean;
import com.jasonchen.microlang.beans.GroupListBean;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UnreadBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.FriendGroupDao;
import com.jasonchen.microlang.dao.GroupListDao;
import com.jasonchen.microlang.database.AccountDBTask;
import com.jasonchen.microlang.database.FriendsTimeLineDBTask;
import com.jasonchen.microlang.database.GroupDBTask;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.fragments.CommentFragment;
import com.jasonchen.microlang.fragments.FavFragment;
import com.jasonchen.microlang.fragments.MentionFragment;
import com.jasonchen.microlang.fragments.TimeLineBaseFragment;
import com.jasonchen.microlang.fragments.TimeLineFragment;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.BundleArgsConstants;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.MythouCrashHandler;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.view.AvatarBigImageView;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * jasonchen
 * 2015/04/10
 */
public class MainActivity extends ActionBarActivity implements ActionBar.OnNavigationListener, View.OnClickListener {

    private DrawerLayout mDrawer;
    private int mDrawerGravity;
    private ActionBarDrawerToggle mToggle;

    private AccountBean accountBean;
    private UserBean userBean;
    private String token;
    private GroupListBean groupListBean;

    // Drawer content
    private View mDrawerWrapper;
    private ScrollView mDrawerScroll;
    private TextView mName;
    private AvatarBigImageView mAvatar;
    private TextView mExchange;
    private Toolbar mToolbar;
    private List<AccountBean> accountBeanList;
    private LoadAccountTask loadAccountTask;
    private PopupMenu popupMenu;

    private int theme = 0;
    private boolean canExit = false;

    // Fragments
    private android.support.v4.app.Fragment currentFragemnt = null;

    public static Intent newIntent(AccountBean accountBean) {
        Intent intent = new Intent(GlobalContext.getInstance(), MainActivity.class);
        intent.putExtra("account", accountBean);
        return intent;
    }

    /*
     * notification bar
	 */
    public static Intent newIntent(AccountBean accountBean,
                                   MessageListBean mentionsWeiboData,
                                   CommentListBean mentionsCommentData,
                                   CommentListBean commentsToMeData, UnreadBean unreadBean) {
        Intent intent = new Intent();
        intent.putExtra(BundleArgsConstants.ACCOUNT_EXTRA, accountBean);
        intent.putExtra(BundleArgsConstants.MENTIONS_WEIBO_EXTRA,
                mentionsWeiboData);
        intent.putExtra(BundleArgsConstants.MENTIONS_COMMENT_EXTRA,
                mentionsCommentData);
        intent.putExtra(BundleArgsConstants.COMMENTS_TO_ME_EXTRA,
                commentsToMeData);
        intent.putExtra(BundleArgsConstants.UNREAD_EXTRA, unreadBean);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            theme = SettingUtility.getTheme();
        } else {
            theme = savedInstanceState.getInt("theme");
        }
        configTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GlobalContext.getInstance().setActivity(this);
        GlobalContext.getInstance().setCurrentRunningActivity(MainActivity.this);
        Thread.setDefaultUncaughtExceptionHandler(new MythouCrashHandler());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            LinearLayout contentLayout = (LinearLayout) findViewById(R.id.layout);
            View view = new View(this);
            LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Utility.getStatusBarHeight());
            int color = SettingUtility.getThemeColor();
            view.setBackgroundColor(getResources().getColor(color));
            view.setLayoutParams(lParams);
            contentLayout.addView(view, 0);
        }

        // Initialize views
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            mToolbar.setElevation(getToolbarElevation());
        }

        mDrawer = ViewUtility.findViewById(this, R.id.drawer);
        mDrawerWrapper = ViewUtility.findViewById(this, R.id.drawer_wrapper);
        mDrawerScroll = ViewUtility.findViewById(this, R.id.drawer_scroll);
        mName = ViewUtility.findViewById(this, R.id.my_name);
        mAvatar = ViewUtility.findViewById(this, R.id.my_avatar);
        mExchange = ViewUtility.findViewById(this, R.id.exchange);

        View me = ViewUtility.findViewById(this, R.id.my_account);
        View home = ViewUtility.findViewById(this, R.id.drawer_home);
        View at = ViewUtility.findViewById(this, R.id.drawer_at);
        View comment = ViewUtility.findViewById(this, R.id.drawer_comment);
        View fav = ViewUtility.findViewById(this, R.id.drawer_fav);
        View search = ViewUtility.findViewById(this, R.id.drawer_search);
        View set = ViewUtility.findViewById(this, R.id.drawer_set);
        if (Utility.isTaskStopped(loadAccountTask)) {
            loadAccountTask = new LoadAccountTask();
            loadAccountTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
        me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = UserActivity.newIntent(MainActivity.this, GlobalContext.getInstance().getAccountBean().getInfo());
                startActivity(intent);
                overridePendingTransition(R.anim.push_left_in, R.anim.stay);
            }
        });

        mAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserActivity.newIntent(MainActivity.this, GlobalContext.getInstance().getAccountBean().getInfo());
                startActivity(intent);
                overridePendingTransition(R.anim.push_left_in, R.anim.stay);
            }
        });
        mToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragemnt instanceof TimeLineBaseFragment) {
                    Utility.stopListViewScrollingAndScrollToTop(((TimeLineBaseFragment) currentFragemnt).getListView());
                }
            }
        });
        mExchange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu = new PopupMenu(MainActivity.this, mExchange);
                Menu menu = popupMenu.getMenu();
                int i = 0;
                final List<AccountBean> list = new ArrayList<AccountBean>();
                for (AccountBean bean : accountBeanList) {
                    if (!accountBean.getUid().equals(bean.getUid())) {
                        menu.add(Menu.NONE, Menu.FIRST + i, i, bean.getUsernick());
                        list.add(bean);
                        i++;
                    }
                }
                final int finalNumber = i;
                menu.add(Menu.NONE, Menu.FIRST + i, i, "添加新账号");
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == (Menu.FIRST + finalNumber)) {
                            Intent intent = AccountActivity.newIntent();
                            startActivity(intent);
                            finish();
                            overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                            return true;
                        } else {
                            for (int i = 0; i < finalNumber; i++) {
                                if (item.getItemId() == (Menu.FIRST + i)) {
                                    Intent intent = MainActivity.newIntent(list.get(i));
                                    SettingUtility.setDefaultAccountId(list.get(i).getUid());
                                    GlobalContext.getInstance().setAccountBean(list.get(i));
                                    finish();
                                    startActivity(intent);
                                    return true;
                                }
                            }
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        home.setOnClickListener(this);
        at.setOnClickListener(this);
        comment.setOnClickListener(this);
        fav.setOnClickListener(this);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = SearchActivity.newIntent(GlobalContext.getInstance(), GlobalContext.getInstance().getSpecialToken());
                startActivity(intent);
                overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                mDrawer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawer.closeDrawers();
                    }
                }, 300);
            }
        });
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.push_left_in, R.anim.stay);
            }
        });

        accountBean = getIntent().getParcelableExtra("account");
        userBean = accountBean.getInfo();
        token = accountBean.getAccess_token();

        TimeLineBitmapDownloader.getInstance().display(
                mAvatar.getImageView(),
                mAvatar.getImageView().getWidth(),
                mAvatar.getImageView().getHeight(),
                userBean.getAvatar_large(),
                FileLocationMethod.avatar_large, false);
        mName.setText(userBean.getScreen_name());

        // Detect if the user chose to use right-handed mode
        boolean rightHanded = SettingUtility.isRightHand();

        mDrawerGravity = rightHanded ? Gravity.RIGHT : Gravity.LEFT;

        // Set gravity
        View nav = findViewById(R.id.nav);
        DrawerLayout.LayoutParams p = (DrawerLayout.LayoutParams) nav.getLayoutParams();
        p.gravity = mDrawerGravity;
        nav.setLayoutParams(p);

        // Initialize naviagtion drawer
        mToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, 0, 0) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                hideFab();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                showFab();
            }
        };

        mToggle.setDrawerIndicatorEnabled(true);
        mDrawer.setDrawerListener(mToggle);

        // Use system shadow for Lollipop but fall back for pre-L
        if (Build.VERSION.SDK_INT >= 21) {
            nav.setElevation(10.0f);
        } else if (mDrawerGravity == Gravity.LEFT) {
            mDrawer.setDrawerShadow(R.drawable.main_drawer_shadow, Gravity.LEFT);
        }

        // Initialize ActionBar Style
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);

        initGroupList();

        // Fragments
        TimeLineFragment homeFragment = (TimeLineFragment) getTimeLineFragment();
        if (homeFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction().show(homeFragment).commit();
        } else {
            getSupportFragmentManager().beginTransaction().add(R.id.container, homeFragment).show(homeFragment).commit();
            getSupportFragmentManager().beginTransaction().add(homeFragment, TimeLineBaseFragment.class.getName());
        }
        getSupportActionBar().setTitle(accountBean.getUsernick());
        currentFragemnt = homeFragment;

        // Adjust drawer layout params
        mDrawerWrapper.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mDrawerScroll.getMeasuredHeight() > mDrawerWrapper.getMeasuredHeight()) {
                    // On poor screens, we add a scroll over the drawer content
                    ViewGroup.LayoutParams lp = mDrawerScroll.getLayoutParams();
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    mDrawerScroll.setLayoutParams(lp);
                }
            }
        });
    }

    private void showFab(){
        TimeLineFragment homeFragment = (TimeLineFragment) getTimeLineFragment();
        homeFragment.showFab();
    }

    private void hideFab(){
        TimeLineFragment homeFragment = (TimeLineFragment) getTimeLineFragment();
        homeFragment.hideFab();
    }

    private void configTheme() {
        if (theme == SettingUtility.getTheme()) {
            setTheme(theme);
        } else {
            reload();
            return;
        }
    }

    private void reload() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();

        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    private void initGroupList() {
        groupListBean = GroupDBTask.get(GlobalContext.getInstance().getCurrentAccountId());
    }

    private void asyncGetGroupInfo() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                FriendGroupDao dao = new FriendGroupDao(GlobalContext.getInstance().getSpecialToken());
                try {
                    GroupListBean list = dao.getGroup();
                    GroupDBTask.update(list, GlobalContext.getInstance().getCurrentAccountId());
                } catch (WeiboException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mToggle.syncState();
        // Override the click event of ActionBarDrawerToggle to avoid crash in right handed mode
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOrCloseDrawer();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Dirty fix strange focus
        findViewById(R.id.container).requestFocus();
        if (!Utility.isTokenValid(GlobalContext.getInstance().getAccountBean()) || !Utility.isHacyTokenValid(GlobalContext.getInstance().getAccountBean())) {
            Utility.showExpiredTokenDialogOrNotification();
        }
        configTheme();
    }

    private class LoadAccountTask extends MyAsyncTask<Void, Void, List<AccountBean>> {

        private LoadAccountTask() {
        }

        @Override
        protected List<AccountBean> doInBackground(Void... params) {
            List<AccountBean> list = null;
            list = AccountDBTask.getAccountList();
            return list;
        }

        @Override
        protected void onCancelled(List<AccountBean> list) {
            super.onCancelled(list);
        }

        @Override
        protected void onPostExecute(List<AccountBean> list) {
            super.onPostExecute(list);
            if (list != null) {
                accountBeanList = list;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        setIntent(i);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("theme", theme);
    }

    public void openOrCloseDrawer() {
        if (mDrawer.isDrawerOpen(mDrawerGravity)) {
            mDrawer.closeDrawer(mDrawerGravity);
        } else {
            mDrawer.openDrawer(mDrawerGravity);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onNavigationItemSelected(int id, long itemId) {
        return true;
    }

    private void setShowTitle(boolean show) {
        getSupportActionBar().setDisplayShowTitleEnabled(show);
    }

    @Override
    public void onClick(View v) {
        android.support.v4.app.Fragment newFragment = null;
        switch (v.getId()) {
            case R.id.drawer_home:
                newFragment = getTimeLineFragment();
                String currentGroupId = FriendsTimeLineDBTask.getRecentGroupId(GlobalContext.getInstance().getCurrentAccountId());
                if("0".equals(currentGroupId)) {
                    mToolbar.setTitle(accountBean.getUsernick());
                }else if("1".equals(currentGroupId)){
                    mToolbar.setTitle("好友圈");
                }else{
                    GroupListBean groupListBean = GroupDBTask.get(GlobalContext.getInstance().getCurrentAccountId());
                    for(GroupBean bean : groupListBean.getLists()){
                        if(currentGroupId.equals(bean.getIdstr())){
                            mToolbar.setTitle(bean.getName());
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    mToolbar.setElevation(getToolbarElevation());
                }
                break;
            case R.id.drawer_at:
                newFragment = getMentionFragment();
                mToolbar.setTitle(getString(R.string.mention));
                if (Build.VERSION.SDK_INT >= 21) {
                    mToolbar.setElevation(0f);
                }
                break;
            case R.id.drawer_comment:
                newFragment = getCommentFragment();
                mToolbar.setTitle(getString(R.string.comment_me));
                if (Build.VERSION.SDK_INT >= 21) {
                    mToolbar.setElevation(getToolbarElevation());
                }
                break;
            case R.id.drawer_fav:
                newFragment = getFavFragment();
                mToolbar.setTitle(getString(R.string.fav));
                if (Build.VERSION.SDK_INT >= 21) {
                    mToolbar.setElevation(getToolbarElevation());
                }
                break;
        }
        if (currentFragemnt != newFragment) {
            if (newFragment.isAdded()) {
                getSupportFragmentManager().beginTransaction().show(newFragment).hide(currentFragemnt).commit();
            } else {
                getSupportFragmentManager().beginTransaction().add(R.id.container, newFragment).show(newFragment).hide(currentFragemnt).commit();
            }

            currentFragemnt = newFragment;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawer.closeDrawers();
            }
        }, 250);
    }

    private android.support.v4.app.Fragment getTimeLineFragment() {
        TimeLineBaseFragment fragment = ((TimeLineBaseFragment) getSupportFragmentManager()
                .findFragmentByTag(TimeLineBaseFragment.class.getName()));
        if (fragment == null) {
            fragment = TimeLineFragment.newInstance(accountBean, userBean, token);
            getSupportFragmentManager().beginTransaction().add(fragment, TimeLineBaseFragment.class.getName());
        }
        return fragment;
    }

    private android.support.v4.app.Fragment getMentionFragment() {
        MentionFragment fragment = (MentionFragment) getSupportFragmentManager().findFragmentByTag(MentionFragment.class.getName());
        if (fragment == null) {
            fragment = MentionFragment.newInstance(accountBean, userBean, token);
            getSupportFragmentManager().beginTransaction().add(fragment, MentionFragment.class.getName());
        }
        return fragment;
    }

    private android.support.v4.app.Fragment getCommentFragment() {
        CommentFragment fragment = (CommentFragment) getSupportFragmentManager().findFragmentByTag(CommentFragment.class.getName());
        if (fragment == null) {
            fragment = CommentFragment.newInstance(accountBean, userBean, token);
            getSupportFragmentManager().beginTransaction().add(fragment, CommentFragment.class.getName());
        }
        return fragment;
    }

    private android.support.v4.app.Fragment getFavFragment() {
        FavFragment fragment = (FavFragment) getSupportFragmentManager().findFragmentByTag(FavFragment.class.getName());
        if (fragment == null) {
            fragment = FavFragment.newInstance(accountBean, userBean, token);
            getSupportFragmentManager().beginTransaction().add(fragment, FavFragment.class.getName());
        }
        return fragment;
    }

    /*private android.support.v4.app.Fragment getSearchFragment() {
        SearchFragment fragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(SearchFragment.class.getName());
        if (fragment == null) {
            fragment = SearchFragment.newInstance(accountBean, userBean, token);
            getSupportFragmentManager().beginTransaction().add(fragment, SearchFragment.class.getName());
        }
        return fragment;
    }*/

    public float getToolbarElevation() {
        if (Build.VERSION.SDK_INT >= 21) {
            return 12.8f;
        } else {
            return -1;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(mDrawer.isDrawerOpen(mDrawerGravity)){
                mDrawer.closeDrawers();
                return false;
            }else {
                if (!canExit) {
                    Toast.makeText(GlobalContext.getInstance(), "再按返回键退出", Toast.LENGTH_SHORT).show();
                    canExit = true;
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            canExit = false;
                        }
                    }, 3000);
                    return false;
                } else {
                    finish();
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
