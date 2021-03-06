
package com.jasonchen.microlang.dao;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jasonchen.microlang.beans.UserListBean;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.http.HttpMethod;
import com.jasonchen.microlang.utils.http.HttpUtility;
import com.jasonchen.microlang.utils.http.URLHelper;

import java.util.HashMap;
import java.util.Map;

public class FriendListDao {

    private String access_token;
    private String uid;
    private String screen_name;
    private String count;
    private String cursor;
    private String trim_status;
    
    public FriendListDao(String token, String uid) {
        this.access_token = token;
        this.uid = uid;
        this.count = String.valueOf(20);
    }

    public void setScreen_name(String screen_name) {
        this.screen_name = screen_name;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public void setTrim_status(String trim_status) {
        this.trim_status = trim_status;
    }


    
    public UserListBean getGSONMsgList() throws WeiboException {

        String url = URLHelper.FRIENDS_LIST_BYID;

        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", GlobalContext.getInstance().getSpecialBlackToken());
        map.put("uid", uid);
        map.put("cursor", cursor);
        map.put("trim_status", trim_status);
        map.put("count", count);
        map.put("screen_name", screen_name);

        String jsonData = HttpUtility.getInstance().executeNormalTask(HttpMethod.Get, url, map);

        Gson gson = new Gson();

        UserListBean value = null;
        try {
            value = gson.fromJson(jsonData, UserListBean.class);
        } catch (JsonSyntaxException e) {
            AppLogger.e(e.getMessage());
        }

        return value;
    }


}
