package com.jasonchen.microlang.preference;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Browser;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.SettingActivity;
import com.jasonchen.microlang.activitys.WriteWeiboActivity;
import com.jasonchen.microlang.debug.AppLogger;

/**
 * jasonchen
 * 2015/04/10
 */
public class AboutFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
        addPreferencesFromResource(R.xml.about_pref);

        findPreference(SettingActivity.OFFICIAL_WEIBO).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("com.jasonchen.mention://@撕嗒滴兔呆");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, getActivity().getPackageName());
                getActivity().startActivity(intent);
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                return true;
            }
        });
        findPreference(SettingActivity.THANKS).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("com.jasonchen.mention://@四次元App");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, getActivity().getPackageName());
                getActivity().startActivity(intent);
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                return true;
            }
        });
        findPreference(SettingActivity.SUGGEST)
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = WriteWeiboActivity.newIntent(getActivity(), buildContent());
                        startActivity(intent);
                        getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                        return true;
                    }
                });
        findPreference(SettingActivity.VERSION).setSummary(buildVersionInfo());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    private String buildVersionInfo() {
        String version = "";
        PackageManager packageManager = getActivity().getPackageManager();
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(getActivity().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            AppLogger.e(e.getMessage());
        }
        version = packInfo.versionName;

        if (!TextUtils.isEmpty(version)) {
            return version;
        } else {
            return "";
        }
    }

    private String buildContent() {
        String network = "";
        ConnectivityManager cm = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                network = "Wifi";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {

                int subType = networkInfo.getSubtype();

                if (subType == TelephonyManager.NETWORK_TYPE_GPRS) {
                    network = "GPRS";
                }
            }
        }
        return "#X.Ray反馈建议# 型号：" + android.os.Build.MANUFACTURER
                + " " + android.os.Build.MODEL + "，Android版本："
                + android.os.Build.VERSION.RELEASE + "，网络状态：" + network + "，应用版本："
                + buildVersionInfo() + " @撕嗒滴兔呆";
    }

}
