package com.jasonchen.microlang.workers;


import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;

import com.jasonchen.microlang.debug.DebugColor;
import com.jasonchen.microlang.interfaces.ISimRayDrawable;
import com.jasonchen.microlang.utils.TaskCache;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.utils.file.FileManager;
import com.jasonchen.microlang.utils.image.ImageUtility;

import java.lang.ref.WeakReference;

/**
 * jasonchen
 * 2015/04/10
 */
public class LocalOrNetworkChooseWorker extends AbstractWorker<String, Integer, Boolean> {

    private String data = "";
    private boolean isMultiPictures = false;

    private WeakReference<ImageView> viewWeakReference;

    private FileLocationMethod method;

    private ISimRayDrawable IWeiciyuanDrawable;

    public String getUrl() {
        return data;
    }

    public LocalOrNetworkChooseWorker(ImageView view, String url, FileLocationMethod method,
                                      boolean isMultiPictures) {

        this.viewWeakReference = new WeakReference<ImageView>(view);
        this.data = url;
        this.method = method;
        this.isMultiPictures = isMultiPictures;
    }

    public LocalOrNetworkChooseWorker(ISimRayDrawable view, String url,
                                      FileLocationMethod method,
                                      boolean isMultiPictures) {

        this(view.getImageView(), url, method, false);
        this.IWeiciyuanDrawable = view;
        this.isMultiPictures = isMultiPictures;
    }

    @Override
    protected Boolean doInBackground(String... url) {
        String path = FileManager.getFilePathFromUrl(data, method);
        return ImageUtility.isThisBitmapCanRead(path) && TaskCache.isThisUrlTaskFinished(data);
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        ImageView imageView = viewWeakReference.get();
        if (!isMySelf(imageView)) {
            return;
        }

        imageView.setImageDrawable(
                new ColorDrawable(DebugColor.CHOOSE_CANCEL));
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        ImageView imageView = viewWeakReference.get();
        if (!isMySelf(imageView)) {
            return;
        }


        if (result) {
            LocalWorker newTask = null;

            if (IWeiciyuanDrawable != null) {
                newTask = new LocalWorker(IWeiciyuanDrawable, getUrl(), method,
                        isMultiPictures);
                PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(newTask);
                IWeiciyuanDrawable.setImageDrawable(downloadedDrawable);
            } else {
                newTask = new LocalWorker(imageView, getUrl(), method, isMultiPictures);
                PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(newTask);
                imageView.setImageDrawable(downloadedDrawable);
            }

            newTask.executeOnIO();
        } else {

            ReadWorker newTask = null;

            if (IWeiciyuanDrawable != null) {
                newTask = new ReadWorker(IWeiciyuanDrawable, getUrl(), method,
                        isMultiPictures);
                PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(newTask);
                IWeiciyuanDrawable.setImageDrawable(downloadedDrawable);
            } else {
                newTask = new ReadWorker(imageView, getUrl(), method, isMultiPictures);
                PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(newTask);
                imageView.setImageDrawable(downloadedDrawable);
            }

            newTask.executeOnWaitNetwork();
        }

    }
}

