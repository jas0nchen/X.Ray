package com.jasonchen.microlang.preference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.SettingActivity;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.file.FileManager;

import java.io.File;

import me.drakeet.materialdialog.MaterialDialog;


/**
 * jasonchen
 * 2015/04/10
 */
public class SettingFragment extends PreferenceFragment {

    private BroadcastReceiver sdCardReceiver;
    private Preference cleanCachePre;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setting_activity_pref);

        buildCacheSummary();
        buildLogSummary();

        findPreference(SettingActivity.SAVED_PIC_PATH)
                .setSummary(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/x_ray");

        cleanCachePre = findPreference(SettingActivity.CLICK_TO_CLEAN_CACHE);

        if (FileManager.isExternalStorageMounted()) {
            new CalcCacheSize(cleanCachePre)
                    .executeOnExecutor(
                            MyAsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            cleanCachePre.setSummary(R.string.please_insert_sd_card);
        }
        cleanCachePre
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        final MaterialDialog deleteCacheDialog = new MaterialDialog(getActivity());
                        deleteCacheDialog.setTitle(getString(R.string.notice))
                                .setMessage(getString(R.string.clean_cache_will_be_done_sure)).setPositiveButton(getString(R.string.confirm), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        deleteCache();
                                        deleteCacheDialog.dismiss();
                                    }
                                }).setNegativeButton(getString(R.string.cancel), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        deleteCacheDialog.dismiss();
                                    }
                                });
                        deleteCacheDialog.show();
                        return true;
                    }
                });


    }

    private void deleteCache(){
        if (FileManager.isExternalStorageMounted()) {
            new CleanCacheTask(cleanCachePre)
                    .executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            Toast.makeText(getActivity(),
                    getString(R.string.please_insert_sd_card),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sdCardReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                buildCacheSummary();
                buildLogSummary();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");

        getActivity().registerReceiver(sdCardReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sdCardReceiver != null) {
            getActivity().unregisterReceiver(sdCardReceiver);
        }
    }

    private void buildCacheSummary() {
        File cachedDir = GlobalContext.getInstance().getExternalCacheDir();
        if (cachedDir != null) {
            findPreference(SettingActivity.CACHE_PATH).setSummary(cachedDir.getAbsolutePath());
        } else {
            findPreference(SettingActivity.CACHE_PATH)
                    .setSummary(getString(R.string.sd_card_in_not_mounted));
        }
    }

    private void buildLogSummary() {
        File cachedDir = GlobalContext.getInstance().getExternalCacheDir();
        if (cachedDir != null) {
            findPreference(SettingActivity.SAVED_LOG_PATH).setSummary(FileManager.getLogDir());
        } else {
            findPreference(SettingActivity.SAVED_LOG_PATH)
                    .setSummary(getString(R.string.sd_card_in_not_mounted));
        }
    }

    private class CalcCacheSize extends MyAsyncTask<Void, Void, String> {

        Preference preference;

        public CalcCacheSize(Preference preference) {
            this.preference = preference;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.preference.setSummary(R.string.getting_cache_size);
        }

        @Override
        protected String doInBackground(Void... params) {
            return FileManager.getPictureCacheSize();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (getActivity() == null) {
                return;
            }

            preference.setSummary(
                    getString(R.string.pref_max_file_cache_size_is_300mb_current_size_is, s));
        }
    }

    private class CleanCacheTask extends MyAsyncTask<Void, Void, Void> {

        Preference preference;

        public CleanCacheTask(Preference preference) {
            this.preference = preference;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getActivity(), getString(R.string.start_clean_cache),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            FileManager.deletePictureCache();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (getActivity() == null) {
                return;
            }

            Toast.makeText(getActivity(), getString(R.string.clean_cache_finish),
                    Toast.LENGTH_SHORT).show();

            new CalcCacheSize(preference)
                    .executeOnExecutor(
                            MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

}
