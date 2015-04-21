package com.jasonchen.microlang.utils.http;

import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.utils.file.FileDownloaderHttpHelper;
import com.jasonchen.microlang.utils.file.FileUploaderHttpHelper;

import java.util.Map;

/**
 * jasonchen
 * 2015/04/10
 */
public class HttpUtility {

    private static HttpUtility httpUtility = new HttpUtility();

    private HttpUtility() {
    }

    public static HttpUtility getInstance() {
        return httpUtility;
    }

    public String executeNormalTask(HttpMethod httpMethod, String url, Map<String, String> param)
            throws WeiboException {
        return new JavaHttpUtility().executeNormalTask(httpMethod, url, param);
    }

    public boolean executeDownloadTask(String url, String path,
            FileDownloaderHttpHelper.DownloadListener downloadListener) {
        return !Thread.currentThread().isInterrupted() && new JavaHttpUtility()
                .doGetSaveFile(url, path, downloadListener);
    }

    public boolean executeUploadTask(String url, Map<String, String> param, String path,
            String imageParamName, FileUploaderHttpHelper.ProgressListener listener)
            throws WeiboException {
        return !Thread.currentThread().isInterrupted() && new JavaHttpUtility()
                .doUploadFile(url, param, path, imageParamName, listener);
    }
}

