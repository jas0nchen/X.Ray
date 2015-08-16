package com.jasonchen.microlang.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.WriteWeiboActivity;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.swiperefresh.SwipeRefreshLayout;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.view.FloatingActionButton;

/**
 * jasonchen
 * 2015/04/16
 */
public class TimeLineBaseFragment extends AbstractAppFragment implements SwipeRefreshLayout.OnRefreshListener, LoadListView.IXListViewListener, View.OnClickListener {

    private View view;
    protected SwipeRefreshLayout refreshLayout;
    protected LoadListView listView;
    protected FloatingActionButton fab;

    public TimeLineBaseFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_timeline_base, container, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshLayout = ViewUtility.findViewById(view, R.id.refreshLayout);
        listView = ViewUtility.findViewById(view, R.id.listView);

        refreshLayout.setColorSchemeColors(getResources().getColor(SettingUtility.getThemeColor()));
        refreshLayout.setOnRefreshListener(this);
        listView.setXListViewListener(this);
        listView.setPullLoadEnable(true);

        fab = new FloatingActionButton.Builder(getActivity())
                .withGravity(Gravity.BOTTOM | Gravity.RIGHT)
                .withPaddings(16, 16, 16, 16)
                .withDrawable(getResources().getDrawable(R.drawable.ic_edit_white))
                .withButtonColor(SettingUtility.getIsNightTheme() ? getResources().getColor(R.color.listview_pic_background_dark) : getResources().getColor(SettingUtility.getThemeColor()))
                .withButtonSize(100)
                .create();
        fab.setOnClickListener(this);
        fab.showFloatingActionButton();
    }

    protected SwipeRefreshLayout getRefreshLayout(){
        return refreshLayout;
    }

    public LoadListView getListView(){
        return listView;
    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onLoadMore() {

    }

    @Override
    public void onClick(View view) {
        Intent intent = WriteWeiboActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean());
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
    }
}
