package com.jasonchen.microlang.utils;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.Display;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.GroupListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.database.AccountDBTask;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.smilepicker.SmileyMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * jasonchen
 * 2015/04/10
 */
public final class GlobalContext extends Application {

    //singleton
    private static GlobalContext globalContext = null;

    //image size
    private Activity activity = null;
    private Activity currentRunningActivity = null;
    
    //check update url
    private String checkUpdateUrl = null;

    private DisplayMetrics displayMetrics = null;
    private Handler handler = new Handler();

    //image memory cache
    private LruCache<String, Bitmap> appBitmapCache = null;

    //current account info
    private AccountBean accountBean = null;
    private GroupListBean group = null;
    
    //LinkedHashmap 的特点是put进去的对象位置未发生变化,而HashMap会发生变化
    private LinkedHashMap<Integer, LinkedHashMap<String, Bitmap>> emotionsPic
            = new LinkedHashMap<Integer, LinkedHashMap<String, Bitmap>>();

    public boolean tokenExpiredDialogIsShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        globalContext = this;
        buildCache();
        /*CrashManagerConstants.loadFromContext(this);
        CrashManager.registerHandler();
        if (Utility.isCertificateFingerprintCorrect(this)) {
           // Crashlytics.start(this);
        }*/
        //Application通过此接口提供了一套回调方法，用于让开发者对Activity的生命周期事件进行集中处理。
        registerActivityLifecycleCallbacks(new AppActivityLifecycleCallbacks());
    }

    public static GlobalContext getInstance() {
        return globalContext;
    }

    public Handler getUIHandler() {
        return handler;
    }
    
    /*public GroupListBean getGroup() {
        if (group == null) {
            group = GroupDBTask.get(GlobalContext.getInstance().getCurrentAccountId());
        }
        return group;
    }*/

    public void setGroup(GroupListBean group) {
        this.group = group;
    }

    public DisplayMetrics getDisplayMetrics() {
        if (displayMetrics != null) {
            return displayMetrics;
        } else {
            Activity a = getActivity();
            if (a != null) {
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);
                this.displayMetrics = metrics;
                return metrics;
            } else {
                //default screen is 800x480
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.widthPixels = 480;
                metrics.heightPixels = 800;
                return metrics;
            }
        }
    }

    public void setAccountBean(final AccountBean accountBean) {
        this.accountBean = accountBean;
    }

    public void updateUserInfo(final UserBean userBean) {
        this.accountBean.setInfo(userBean);
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (MyProfileInfoChangeListener listener : profileListenerSet) {
                    listener.onChange(userBean);
                }
            }
        });
    }

    public AccountBean getAccountBean() {
        if (accountBean == null) {
            String id = SettingUtility.getDefaultAccountId();
            if (!TextUtils.isEmpty(id)) {
                accountBean = AccountDBTask.getAccount(id);
            } else {
                List<AccountBean> accountList = AccountDBTask.getAccountList();
                if (accountList != null && accountList.size() > 0) {
                    accountBean = accountList.get(0);
                }
            }
        }

        return accountBean;
    }

    private Set<MyProfileInfoChangeListener> profileListenerSet
            = new HashSet<MyProfileInfoChangeListener>();

	//private MusicInfo musicInfo;

    public void registerForAccountChangeListener(MyProfileInfoChangeListener listener) {
        if (listener != null) {
            profileListenerSet.add(listener);
        }
    }

    public void unRegisterForAccountChangeListener(MyProfileInfoChangeListener listener) {
        profileListenerSet.remove(listener);
    }

    public static interface MyProfileInfoChangeListener {

        public void onChange(UserBean newUserBean);
    }

    public String getCurrentAccountId() {
        return getAccountBean().getUid();
    }

    public String getCurrentAccountName() {

        return getAccountBean().getUsernick();
    }

    public synchronized LruCache<String, Bitmap> getBitmapCache() {
        if (appBitmapCache == null) {
            buildCache();
        }
        return appBitmapCache;
    }

    public String getSpecialToken() {
        if (getAccountBean() != null) {
            return getAccountBean().getAccess_token();
        } else {
            return "";
        }
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Activity getCurrentRunningActivity() {
        return currentRunningActivity;
    }

    public void setCurrentRunningActivity(Activity currentRunningActivity) {
        this.currentRunningActivity = currentRunningActivity;
    }

    private void buildCache() {
    	// 获取单个进程可用内存的最大值 .   方式一：使用ActivityManager服务（计量单位为M）  
        int memClass = ((ActivityManager) getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();

        int cacheSize = Math.max(1024 * 1024 * 8, 1024 * 1024 * memClass / 5);

        appBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {

                return bitmap.getByteCount();
            }
        };
    }

    public synchronized Map<String, Bitmap> getEmotionsPics() {
        if (emotionsPic != null && emotionsPic.size() > 0) {
            return emotionsPic.get(SmileyMap.GENERAL_EMOTION_POSITION);
        } else {
            getEmotionsTask();
            return emotionsPic.get(SmileyMap.GENERAL_EMOTION_POSITION);
        }
    }

    public synchronized Map<String, Bitmap> getHuahuaPics() {
        if (emotionsPic != null && emotionsPic.size() > 0) {
            return emotionsPic.get(SmileyMap.HUAHUA_EMOTION_POSITION);
        } else {
            getEmotionsTask();
            return emotionsPic.get(SmileyMap.HUAHUA_EMOTION_POSITION);
        }
    }

    private void getEmotionsTask() {
        Map<String, String> general = SmileyMap.getInstance().getGeneral();
        emotionsPic.put(SmileyMap.GENERAL_EMOTION_POSITION, getEmotionsTask(general));
        Map<String, String> huahua = SmileyMap.getInstance().getHuahua();
        emotionsPic.put(SmileyMap.HUAHUA_EMOTION_POSITION, getEmotionsTask(huahua));
    }

    private LinkedHashMap<String, Bitmap> getEmotionsTask(Map<String, String> emotionMap) {
        List<String> index = new ArrayList<String>();
        index.addAll(emotionMap.keySet());
        LinkedHashMap<String, Bitmap> bitmapMap = new LinkedHashMap<String, Bitmap>();
        for (String str : index) {
            String name = emotionMap.get(str);
            AssetManager assetManager = GlobalContext.getInstance().getAssets();
            InputStream inputStream;
            try {
                inputStream = assetManager.open(name);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                            Utility.dip2px(getResources().getInteger(R.integer.emotion_size)),
                            Utility.dip2px(getResources().getInteger(R.integer.emotion_size)),
                            true);
                    if (bitmap != scaledBitmap) {
                        bitmap.recycle();
                        bitmap = scaledBitmap;
                    }
                    bitmapMap.put(str, bitmap);
                }
            } catch (IOException ignored) {

            }
        }

        return bitmapMap;
    }

    /*public void updateMusicInfo(MusicInfo musicInfo) {
        this.musicInfo = musicInfo;
    }

    public MusicInfo getMusicInfo() {
        return musicInfo;
    }*/

    public boolean checkUserIsLogin() {
        return getInstance().getAccountBean() != null;
    }
}

