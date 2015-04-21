package com.jasonchen.microlang.dao;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jasonchen.microlang.beans.CommentBean;
import com.jasonchen.microlang.debug.AppLogger;
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
public class DestroyCommentDao {
    private String access_token;
    private String cid;

    public DestroyCommentDao(String access_token, String cid) {
        this.access_token = access_token;
        this.cid = cid;
    }

    public boolean destroy() throws WeiboException {
        String url = URLHelper.COMMENT_DESTROY;
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", access_token);
        map.put("cid", cid);

        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Post, url, map);
        Gson gson = new Gson();

        try {
            CommentBean value = gson.fromJson(jsonData, CommentBean.class);
        } catch (JsonSyntaxException e) {
            AppLogger.e(e.getMessage());
            return false;
        }

        return true;
    }
}
