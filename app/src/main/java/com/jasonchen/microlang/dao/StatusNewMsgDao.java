package com.jasonchen.microlang.dao;

import android.text.TextUtils;

import com.jasonchen.microlang.beans.GeoBean;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.utils.file.FileUploaderHttpHelper;
import com.jasonchen.microlang.utils.http.HttpMethod;
import com.jasonchen.microlang.utils.http.HttpUtility;
import com.jasonchen.microlang.utils.http.URLHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * jasonchen
 * 2015/04/10
 */
public class StatusNewMsgDao {

    private String access_token;
    private String pic;
    private GeoBean geoBean;
    private int visibility;

    public StatusNewMsgDao setGeoBean(GeoBean geoBean) {
        this.geoBean = geoBean;
        return this;
    }

    public StatusNewMsgDao setVisibility(int visibility){
        this.visibility = visibility;
        return this;
    }

    public StatusNewMsgDao setPic(String pic) {
        this.pic = pic;
        return this;
    }

    public StatusNewMsgDao(String access_token) {

        this.access_token = access_token;
    }

    public boolean sendNewMsg(String str, FileUploaderHttpHelper.ProgressListener listener)
            throws WeiboException {

        if (!TextUtils.isEmpty(pic)) {
            return sendNewMsgWithPic(str, listener);
        }
        String url = URLHelper.STATUSES_UPDATE;
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", access_token);
        map.put("status", str);
        map.put("visible", String.valueOf(visibility));
        if (geoBean != null) {
            map.put("lat", String.valueOf(geoBean.getLat()));
            map.put("long", String.valueOf(geoBean.getLon()));
        }

        HttpUtility.getInstance().executeNormalTask(HttpMethod.Post, url, map);
        return true;
    }

    private boolean sendNewMsgWithPic(String str, FileUploaderHttpHelper.ProgressListener listener)
            throws WeiboException {
        String url = URLHelper.STATUSES_UPLOAD;
        Map<String, String> map = new HashMap<String, String>();
        map.put("access_token", access_token);
        map.put("status", str);
        map.put("visible", String.valueOf(visibility));
        if (geoBean != null) {
            map.put("lat", String.valueOf(geoBean.getLat()));
            map.put("long", String.valueOf(geoBean.getLon()));
        }

        return HttpUtility.getInstance().executeUploadTask(url, map, pic, "pic", listener);
    }
}
