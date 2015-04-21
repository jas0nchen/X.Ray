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
public class ReplyToCommentMsgDao {

    public CommentBean reply() throws WeiboException {
        String url = URLHelper.COMMENT_REPLY;
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", access_token);
        map.put("id", id);
        map.put("cid", cid);
        map.put("comment", comment);
        map.put("comment_ori", comment_ori);
        map.put("without_mention", without_mention);

        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Post, url, map);

        Gson gson = new Gson();

        CommentBean value = null;
        try {
            value = gson.fromJson(jsonData, CommentBean.class);
        } catch (JsonSyntaxException e) {

            AppLogger.e(e.getMessage());
        }

        return value;
    }

    public ReplyToCommentMsgDao(String token, CommentBean bean, String replyContent) {
        this.access_token = token;
        this.cid = bean.getId();
        this.id = bean.getStatus().getId();
        this.comment = replyContent;
    }

    private String access_token;
    private String cid;
    private String id;
    private String comment;
    private String without_mention;
    private String comment_ori;
}
