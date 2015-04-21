package com.jasonchen.microlang.dao;

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
public class Mid2IdDao {

    private String token;
    private String mid;

    public Mid2IdDao(String token, String mid) {
        this.token = GlobalContext.getInstance().getSpecialBlackToken();
        this.mid = mid;
    }

    public String getId() throws WeiboException {
        String url = URLHelper.MID_TO_ID;
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", token);
        map.put("source", URLHelper.HACK_APP_KEY);
        map.put("mid", mid);
        map.put("type", "1");
        map.put("isBase62", "1");

        String json = HttpUtility.getInstance().executeNormalTask(HttpMethod.Get, url, map);
        try {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.optString("id", "0");
        } catch (JSONException e) {
            AppLogger.e(e.getMessage());
        }
        return "0";
    }
}
