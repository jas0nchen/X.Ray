package com.jasonchen.microlang.workers;


import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.jasonchen.microlang.interfaces.IPictureWorker;

import java.lang.ref.WeakReference;

/**
 * jasonchen
 * 2015/04/10
 */
public class PictureBitmapDrawable extends ColorDrawable {
    private final WeakReference<IPictureWorker> bitmapDownloaderTaskReference;

    public PictureBitmapDrawable(IPictureWorker bitmapDownloaderTask) {
        super(Color.LTGRAY);
        bitmapDownloaderTaskReference =
                new WeakReference<IPictureWorker>(bitmapDownloaderTask);
    }

    public IPictureWorker getBitmapDownloaderTask() {
        return bitmapDownloaderTaskReference.get();
    }
}
