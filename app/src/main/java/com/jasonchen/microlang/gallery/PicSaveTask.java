package com.jasonchen.microlang.gallery;


import android.app.Activity;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.file.FileManager;

/**
 * jasonchen
 * 2015/04/10
 */
public class PicSaveTask extends MyAsyncTask<Void, Boolean, Boolean> {

    private String path;
    private Activity activity;

    public PicSaveTask(Activity activity, String path) {
        this.path = path;
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return FileManager.saveToPicDir(path);
    }

    @Override
    protected void onPostExecute(Boolean value) {
        super.onPostExecute(value);
        if (value) {
            Toast.makeText(activity, activity.getString(R.string.save_to_album_successfully),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, activity.getString(R.string.cant_save_pic),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
