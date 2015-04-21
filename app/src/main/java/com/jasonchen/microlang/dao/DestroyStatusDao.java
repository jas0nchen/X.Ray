package com.jasonchen.microlang.dao;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jasonchen.microlang.beans.MessageBean;
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
public class DestroyStatusDao {

    private String access_token;
    private String id;

    public DestroyStatusDao(String access_token, String id) {
        this.access_token = access_token;
        this.id = id;
    }

    public boolean destroy() throws WeiboException {
        String url = URLHelper.STATUSES_DESTROY;
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", access_token);
        map.put("id", id);

        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Post, url, map);
        Gson gson = new Gson();

        try {
            MessageBean value = gson.fromJson(jsonData, MessageBean.class);
        } catch (JsonSyntaxException e) {
            AppLogger.e(e.getMessage());
            return false;
        }

        return true;
    }
}
