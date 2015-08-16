package com.jasonchen.microlang.workers;


import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.interfaces.IPictureWorker;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.utils.GlobalContext;

import java.lang.ref.WeakReference;

/**
 * jasonchen
 * 2015/04/10
 */
public class PictureBitmapDrawable extends ColorDrawable {
    private final WeakReference<IPictureWorker> bitmapDownloaderTaskReference;

    public PictureBitmapDrawable(IPictureWorker bitmapDownloaderTask) {
        super(Color.LTGRAY);
        if(SettingUtility.getIsNightTheme()) {
            setColor(GlobalContext.getInstance().getResources().getColor(R.color.listview_pic_background_dark));
        }
        bitmapDownloaderTaskReference =
                new WeakReference<IPictureWorker>(bitmapDownloaderTask);
    }

    public IPictureWorker getBitmapDownloaderTask() {
        return bitmapDownloaderTaskReference.get();
    }
}
