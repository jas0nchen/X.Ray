package com.jasonchen.microlang.activitys;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.preference.SettingFragment;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.utils.GlobalContext;

/**
 * jasonchen
 * 2015/04/15
 */
public class SettingActivity extends SwipeBackActivity {
	public static final String SOUND = "sound";

    public static final String AUTO_REFRESH = "auto_refresh";
    public static final String SHOW_REPOST_CONTENT = "show_repost_content";
    public static final String NIGHT_THEME = "night_theme";

    //appearance
    public static final String THEME = "theme";
    public static final String LIST_AVATAR_MODE = "list_avatar_mode";
    public static final String LIST_PIC_MODE = "list_pic_mode";
    public static final String LIST_HIGH_PIC_MODE = "list_high_pic_mode";
    public static final String LIST_FAST_SCROLL = "list_fast_scroll";
    public static final String FONT_SIZE = "font_size";
    public static final String SHOW_BIG_PIC = "show_big_pic";
    public static final String SHOW_BIG_AVATAR = "show_big_avatar";
    public static final String MSG_DETAIL_SHOW_BIG_PIC = "msg_detail_show_big_pic";
    public static final String INVERT_READ = "invert_read";
    public static final String RIGHT_HAND = "right_hand";


    //read
    public static final String READ_STYLE = "read_style";

    //notification
    public static final String FREQUENCY = "frequency";
    public static final String ENABLE_FETCH_MSG = "enable_fetch_msg";
    public static final String DISABLE_FETCH_AT_NIGHT = "disable_fetch_at_night";
    public static final String ENABLE_VIBRATE = "vibrate";
    public static final String ENABLE_LED = "led";
    public static final String ENABLE_RINGTONE = "ringtone";
    public static final String JBNOTIFICATION_STYLE = "jbnotification";
    public static final String ENABLE_MENTION_TO_ME = "mention_to_me";
    public static final String ENABLE_COMMENT_TO_ME = "comment_to_me";
    public static final String ENABLE_MENTION_COMMENT_TO_ME = "mention_comment_to_me";

    //filter
    public static final String FILTER = "filter";

    //traffic control
    public static final String UPLOAD_PIC_QUALITY = "upload_pic_quality";
    public static final String COMMENT_REPOST_AVATAR = "comment_repost_list_avatar";
    public static final String SHOW_COMMENT_REPOST_AVATAR = "show_comment_repost_list_avatar";
    public static final String DISABLE_DOWNLOAD_AVATAR_PIC = "disable_download";
    public static final String MSG_COUNT = "msg_count";
    public static final String WIFI_UNLIMITED_MSG_COUNT = "enable_wifi_unlimited_msg_count";
    public static final String WIFI_AUTO_DOWNLOAD_PIC = "enable_wifi_auto_download_pic";
    public static final String INTELLIGENCE_PIC = "intelligence_pic";
    public static final String SHOW_PIC_WHEN_INTELLIGENT = "show_pic_when_intelligent";

    //performance
    public static final String DISABLE_HARDWARE_ACCELERATED
            = "pref_disable_hardware_accelerated_key";

    //other
    public static final String ENABLE_INTERNAL_WEB_BROWSER = "enable_internal_web_browser";
    public static final String ENABLE_CLICK_TO_CLOSE_GALLERY = "enable_click_to_close_gallery";
    public static final String CLICK_TO_CLEAN_CACHE = "click_to_clean_cache";
    public static final String FILTER_SINA_AD = "filter_sina_ad";

    //about
    public static final String OFFICIAL_WEIBO = "pref_official_weibo_key";
    public static final String THANKS = "pref_thanks";
    public static final String SUGGEST = "pref_suggest_key";
    public static final String VERSION = "pref_version_key";
    public static final String RECOMMEND = "pref_recommend_key";
    public static final String DONATE = "pref_donate_key";
    public static final String CACHE_PATH = "pref_cache_path_key";
    public static final String SAVED_PIC_PATH = "pref_saved_pic_path_key";
    public static final String SAVED_LOG_PATH = "pref_saved_log_path_key";
    public static final String AUTHOR = "pref_author_key";
    public static final String DEBUG_MEM_INFO = "pref_mem_key";
    public static final String CRASH = "pref_crash_key";

    public static Intent newIntent(Context context){
        Intent intent = new Intent(context, SettingActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_setting;
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.container, new SettingFragment()).commit();
        }
        getSupportActionBar().setTitle(getString(R.string.setting));
    }
}
