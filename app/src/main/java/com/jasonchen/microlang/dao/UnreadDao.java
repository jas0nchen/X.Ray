package com.jasonchen.microlang.dao;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jasonchen.microlang.beans.UnreadBean;
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
public class UnreadDao {

    protected String getUrl() {
        return URLHelper.UNREAD_COUNT;
    }

    private String getMsgListJson() throws WeiboException {
        String url = getUrl();

        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", GlobalContext.getInstance().getSpecialBlackToken());
        map.put("uid", uid);

        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Get, url, map);

        return jsonData;
    }

    public UnreadBean getCount() throws WeiboException {
        String json = getMsgListJson();
        Gson gson = new Gson();

        UnreadBean value = null;
        try {
            value = gson.fromJson(json, UnreadBean.class);
        } catch (JsonSyntaxException e) {

            AppLogger.e(e.getMessage());
            return null;
        }

        return value;
    }

    private String access_token;
    private String uid;

    public UnreadDao(String access_token, String uid) {
        this.access_token = access_token;
        this.uid = uid;
    }
}
