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
public class FriendshipsDao {

    public UserBean followIt() throws WeiboException {
        String url = URLHelper.FRIENDSHIPS_CREATE;
        return executeTask(url);
    }

    public UserBean unFollowIt() throws WeiboException {
        String url = URLHelper.FRIENDSHIPS_DESTROY;
        return executeTask(url);
    }

    private UserBean executeTask(String url) throws WeiboException {
        if (TextUtils.isEmpty(uid) && TextUtils.isEmpty(screen_name)) {
            AppLogger.e("uid or screen name can't be empty");
            return null;
        }

        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", GlobalContext.getInstance().getSpecialBlackToken());
        if (!TextUtils.isEmpty(uid)) {
            map.put("uid", uid);
        } else {
            map.put("screen_name", screen_name);
        }
        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Post, url, map);
        try {
            UserBean value = new Gson().fromJson(jsonData, UserBean.class);
            if (value != null) {
                return value;
            }
        } catch (JsonSyntaxException e) {

            AppLogger.e(e.getMessage());
        }
        return null;
    }

    private String access_token;
    private String uid;
    private String screen_name;

    public FriendshipsDao(String token) {
        this.access_token = token;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getScreen_name() {
        return screen_name;
    }

    public void setScreen_name(String screen_name) {
        this.screen_name = screen_name;
    }
}
