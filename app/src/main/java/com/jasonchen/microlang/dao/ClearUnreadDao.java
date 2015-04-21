package com.jasonchen.microlang.dao;

import com.jasonchen.microlang.beans.UnreadBean;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.http.HttpMethod;
import com.jasonchen.microlang.utils.http.HttpUtility;
import com.jasonchen.microlang.utils.http.URLHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.jasonchen.microlang.exception.WeiboException;

/**
 * jasonchen
 * 2015/04/10
 */
public class ClearUnreadDao {

    public static final String STATUS = "app_message";
    public static final String FOLLOWER = "follower";
    public static final String CMT = "cmt";
    public static final String DM = "dm";
    public static final String MENTION_STATUS = "mention_status";
    public static final String MENTION_CMT = "mention_cmt";

    protected String getUrl() {
        return URLHelper.UNREAD_CLEAR;
    }

    public boolean clearUnread() throws WeiboException {

        String url = getUrl();

        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", GlobalContext.getInstance().getSpecialBlackToken());
        map.put("type", type);

        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Get, url, map);

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            return jsonObject.optBoolean("result", false);
        } catch (JSONException e) {
            AppLogger.e(e.getMessage());
        }

        return false;
    }

    /**
     * first check server unread status,if unread count is the same,reset unread count
     */
    public boolean clearMentionStatusUnread(UnreadBean unreadBean, String accountId)
            throws WeiboException {
        int count = unreadBean.getMention_status();
        UnreadBean currentCount = new UnreadDao(access_token, accountId).getCount();
        if (currentCount == null) {
            return false;
        }
        //already reset or have new unread message
        if (count != currentCount.getMention_status()) {
            return false;
        }
        return new ClearUnreadDao(access_token, ClearUnreadDao.MENTION_STATUS).clearUnread();
    }

    public boolean clearMentionCommentUnread(UnreadBean unreadBean, String accountId)
            throws WeiboException {
        int count = unreadBean.getMention_cmt();
        UnreadBean currentCount = new UnreadDao(access_token, accountId).getCount();
        if (currentCount == null) {
            return false;
        }
        //already reset or have new unread message
        if (count != currentCount.getMention_cmt()) {
            return false;
        }
        return new ClearUnreadDao(access_token, ClearUnreadDao.MENTION_CMT).clearUnread();
    }

    public boolean clearCommentUnread(UnreadBean unreadBean, String accountId)
            throws WeiboException {
        int count = unreadBean.getCmt();
        UnreadBean currentCount = new UnreadDao(access_token, accountId).getCount();
        if (currentCount == null) {
            return false;
        }
        //already reset or have new unread message
        if (count != currentCount.getCmt()) {
            return false;
        }
        return new ClearUnreadDao(access_token, ClearUnreadDao.CMT).clearUnread();
    }

    private String access_token;
    private String type;

    public ClearUnreadDao(String access_token) {
        this.access_token = access_token;
    }

    public ClearUnreadDao(String access_token, String type) {
        this.access_token = access_token;
        this.type = type;
    }
}
