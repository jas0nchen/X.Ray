package com.jasonchen.microlang.dao;


import com.jasonchen.microlang.utils.http.URLHelper;

/**
 * jasonchen
 * 2015/05/28
 */
public class BilateralTimeLineDao extends MainFriendsTimeLineDao {

    public BilateralTimeLineDao(String access_token) {
        super(access_token, "20");
    }

    @Override
    protected String getUrl() {
        return URLHelper.BILATERAL_TIMELINE;
    }
}
