package com.jasonchen.microlang.dao;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.http.HttpMethod;
import com.jasonchen.microlang.utils.http.HttpUtility;
import com.jasonchen.microlang.utils.http.URLHelper;

import java.util.HashMap;
import java.util.Map;

import com.jasonchen.microlang.exception.WeiboException;

/**
 * jasonchen
 * 2015/04/10
 */
public class ShowUserDao {

    public UserBean getUserInfo() throws WeiboException {
        String url = (!TextUtils.isEmpty(domain) ? URLHelper.USER_DOMAIN_SHOW
                : URLHelper.USER_SHOW);
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", GlobalContext.getInstance().getSpecialBlackToken());
        map.put("uid", uid);
        map.put("screen_name", screen_name);
        map.put("domain", domain);

        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Get, url, map);

        Gson gson = new Gson();

        UserBean value = null;
        try {
            value = gson.fromJson(jsonData, UserBean.class);
        } catch (JsonSyntaxException e) {

            AppLogger.e(e.getMessage());
        }

        return value;
    }

    private String access_token;
    private String uid;
    private String screen_name;
    private String domain;

    public ShowUserDao(String access_token) {

        this.access_token = access_token;
    }

    public ShowUserDao setScreen_name(String screen_name) {
        this.screen_name = screen_name;
        return this;
    }

    public ShowUserDao setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
