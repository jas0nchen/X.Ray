package com.jasonchen.microlang.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.MainActivity;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.utils.BundleArgsConstants;
import com.jasonchen.microlang.utils.UnreadTabIndex;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.view.LinearViewPagerIndicator;
import com.jasonchen.microlang.view.SlidingTabLayout;
import com.jasonchen.microlang.view.SlidingTabStrip;

/**
 * jasonchen
 * 2015/04/10
 */
public class MentionFragment extends AbstractAppFragment {

    private View view;
    private static final String ARGUMENTS_ACCOUNT_EXTRA = MentionFragment.class
            .getName() + ":account_extra";
    private static final String ARGUMENTS_USER_EXTRA = MentionFragment.class
            .getName() + ":userBean_extra";
    private static final String ARGUMENTS_TOKEN_EXTRA = MentionFragment.class
            .getName() + ":token_extra";

    private AccountBean accountBean;
    private UserBean userBean;
    private String token;
    private MentionTimeLineFragment statusFragment;
    private MentionCommentFragment commentFragment;

    private SlidingTabLayout tab;
    private ViewPager mPager;

    public static MentionFragment newInstance(AccountBean accountBean,
                                              UserBean userBean, String token) {
        MentionFragment fragment = new MentionFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGUMENTS_ACCOUNT_EXTRA, accountBean);
        bundle.putParcelable(ARGUMENTS_USER_EXTRA, userBean);
        bundle.putString(ARGUMENTS_TOKEN_EXTRA, token);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (view == null) {
            view = inflater.inflate(R.layout.fragment_mention, container, false);
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        userBean = getArguments().getParcelable(ARGUMENTS_USER_EXTRA);
        accountBean = getArguments().getParcelable(ARGUMENTS_ACCOUNT_EXTRA);
        token = getArguments().getString(ARGUMENTS_TOKEN_EXTRA);
        buildInterface();
    }

    private void buildInterface() {
        // Initialize views
        tab = ViewUtility.findViewById(view, R.id.tab);
        mPager = (ViewPager) view.findViewById(R.id.mentions_pager);
        if (Build.VERSION.SDK_INT >= 21) {
            ((MainActivity) getActivity()).getSupportActionBar().setElevation(0.0f);
        }

        // View Pager
        statusFragment = (MentionTimeLineFragment) getStatusFragment();
        commentFragment = (MentionCommentFragment) getCommentFragment();

        mPager.setAdapter(new FragmentStatePagerAdapter(getFragmentManager()) {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public android.support.v4.app.Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return statusFragment;
                    case 1:
                        return commentFragment;
                    default:
                        return null;
                }
            }

            @Override
            public String getPageTitle(int position) {
                switch (position) {
                    case 1:
                        return "评论";
                    case 0:
                        return "微博";
                    default:
                        return "";
                }
            }
        });

        // Initialize indicator
        tab.setDistributeEvenly(true);
        tab.setViewPager(mPager);
        final int color = SettingUtility.getIsNightTheme() ? getResources().getColor(R.color.light_gray) : getResources().getColor(R.color.white);
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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((MainActivity) getActivity()).getIntent().getSerializableExtra(BundleArgsConstants.OPEN_NAVIGATION_INDEX_EXTRA) != null) {
            UnreadTabIndex unreadTabIndex = (UnreadTabIndex) ((MainActivity) getActivity()).getIntent().getSerializableExtra(BundleArgsConstants.OPEN_NAVIGATION_INDEX_EXTRA);
            switch (unreadTabIndex) {
                case MENTION_WEIBO:
                    mPager.setCurrentItem(0);
//                    Fragment statusfragment = getChildFragmentManager().findFragmentByTag(MentionTimeLineFragment.class.getName());
//                    //((MentionTimeLineFragment) ((FragmentStatePagerAdapter) mPager.getAdapter()).getItem(0)).refreshUnread();
//                    if (statusfragment != null) {
//                        AppLogger.e("提及微博！！！！！");
//                        ((MentionTimeLineFragment) statusfragment).refreshUnread();
//                    }
                    ((MainActivity) getActivity()).getIntent().putExtra(BundleArgsConstants.OPEN_NAVIGATION_INDEX_EXTRA,
                            UnreadTabIndex.NONE);
                    break;
                case MENTION_COMMENT:
                    mPager.setCurrentItem(1);
//                    Fragment commentfragment = getChildFragmentManager().findFragmentByTag(MentionCommentFragment.class.getName());
//                    //((MentionTimeLineFragment) ((FragmentStatePagerAdapter) mPager.getAdapter()).getItem(0)).refreshUnread();
//                    if (commentfragment != null) {
//
//                        ((MentionCommentFragment) commentfragment).refreshUnread();
//                    }
                    //((MentionCommentFragment) ((FragmentStatePagerAdapter) mPager.getAdapter()).getItem(1)).refreshUnread();
                    ((MainActivity) getActivity()).getIntent().putExtra(BundleArgsConstants.OPEN_NAVIGATION_INDEX_EXTRA,
                            UnreadTabIndex.NONE);
                    break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    public interface refreshUnread {
        public void refreshUnread();
    }

    private Fragment getStatusFragment(){
        Fragment fragment = getChildFragmentManager().findFragmentByTag(MentionTimeLineFragment.class.getName());
        if(fragment == null){
            fragment = MentionTimeLineFragment.newInstance(accountBean, userBean, token);
            getChildFragmentManager().beginTransaction().add(fragment, MentionTimeLineFragment.class.getName());
        }
        return fragment;
    }

    private Fragment getCommentFragment(){
        Fragment fragment = getChildFragmentManager().findFragmentByTag(MentionCommentFragment.class.getName());
        if(fragment == null){
            fragment = MentionCommentFragment.newInstance(accountBean, userBean, token);
            getChildFragmentManager().beginTransaction().add(fragment, MentionCommentFragment.class.getName());
        }
        return fragment;
    }
}
