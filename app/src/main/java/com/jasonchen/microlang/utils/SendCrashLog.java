package com.jasonchen.microlang.utils;

import android.widget.Toast;

import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.file.FileManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 15/06/03
 */

public class SendCrashLog extends MyAsyncTask<String, String, Boolean> {
    public SendCrashLog() {
    }

    @Override
    protected Boolean doInBackground(String... params) {
        ArrayList<String> list = getFileNameList(new File(FileManager.getLogDir()));
        AppLogger.e(list.toString());
        String uploadUrl = "http://xraybug.sinaapp.com/savelog.php";
        //这里把相关的异常信息转为http post请求的数据参数
        if(list !=null && list.size() > 0) {
            uploadFile(uploadUrl, list.get(0));
        }
        AppLogger.d("Device model sent.");
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {

    }

    private ArrayList<String> getFileNameList(File path) {
        ArrayList<String> items = null;
        try{
            items = new ArrayList<String>();
            File[] files = path.listFiles();// 列出所有文件
            // 将所有文件存入list中
            if(files != null){
                int count = files.length;// 文件个数
                for (int i = 0; i < count; i++) {
                    File file = files[i];
                    items.add(file.getName());
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }

        return items;
    }


    /* 上传文件至Server，uploadUrl：接收文件的处理页面 */
    private void uploadFile(String uploadUrl, String srcPath) {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "******";
        AppLogger.e(srcPath);
        try {
            URL url = new URL(uploadUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url
                    .openConnection();
            // 设置每次传输的流大小，可以有效防止手机因为内存不足崩溃
            // 此方法用于在预先不知道内容长度时启用没有进行内部缓冲的 HTTP 请求正文的流。
            httpURLConnection.setChunkedStreamingMode(128 * 1024);// 128K
            // 允许输入输出流
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            // 使用POST方法
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Charset", "UTF-8");
            httpURLConnection.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(
                    httpURLConnection.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + end);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\"; filename=\""
                    + srcPath.substring(srcPath.lastIndexOf("/") + 1)
                    + "\""
                    + end);
            dos.writeBytes(end);

            FileInputStream fis = new FileInputStream(srcPath);
            byte[] buffer = new byte[8192]; // 8k
            int count = 0;
            // 读取文件
            while ((count = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, count);
            }
            fis.close();

            dos.writeBytes(end);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
            dos.flush();

            InputStream is = httpURLConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String result = br.readLine();

            Toast.makeText(GlobalContext.getInstance(), result, Toast.LENGTH_SHORT).show();
            dos.close();
            is.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

