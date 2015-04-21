package com.jasonchen.microlang.activitys;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.view.Menu;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.fragments.SearchStatusFragment;
import com.jasonchen.microlang.fragments.SearchUserFragment;
import com.jasonchen.microlang.gallery.ViewPagerFixed;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.view.SlidingTabLayout;
import com.jasonchen.microlang.view.SlidingTabStrip;

/**
 * jasonchen
 * 2015/04/17
 */
public class SearchActivity extends SwipeBackActivity {

    private String token;

    private SlidingTabLayout tab;
    private ViewPagerFixed pager;

    private SearchStatusFragment statusFragment;
    private SearchUserFragment userFragment;

    public static Intent newIntent(Context context, String token) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra("token", token);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_search;
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        getSupportActionBar().setTitle("");
        if (getIntent() != null) {
            token = getIntent().getStringExtra("token");
        }

        if (Build.VERSION.SDK_INT >= 21) {
            mToolbar.setElevation(0f);
        }

        tab = ViewUtility.findViewById(this, R.id.tab);
        pager = ViewUtility.findViewById(this, R.id.pager);

        statusFragment = SearchStatusFragment.newInstance(token);
        userFragment = SearchUserFragment.newInstance(token);

        pager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public android.support.v4.app.Fragment getItem(int position) {
                switch (position) {
                    case 1:
                        return statusFragment;
                    case 0:
                        return userFragment;
                    default:
                        return null;
                }
            }

            @Override
            public String getPageTitle(int position) {
                switch (position) {
                    case 1:
                        return "微博";
                    case 0:
                        return "用户";
                    default:
                        return "";
                }
            }
        });

        tab.setDistributeEvenly(true);
        tab.setViewPager(pager);

        final int color = getResources().getColor(R.color.white);
        tab.setCustomTabColorizer(new SlidingTabStrip.SimpleTabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return color;
            }

            @Override
            public int getSelectedTitleColor(int position) {
                return color;
            }
        });

        tab.notifyIndicatorColorChanged();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_search, menu);
        initSearchView(menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void initSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                int item = pager.getCurrentItem();
                if(item == 0){
                    userFragment.search(query);
                }else{
                    statusFragment.search(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    public static interface Searcher {
        public void search(String q);
    }
}
