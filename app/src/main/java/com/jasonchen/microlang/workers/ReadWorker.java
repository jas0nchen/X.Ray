package com.jasonchen.microlang.workers;


import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.jasonchen.microlang.debug.DebugColor;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.TaskCache;
import com.jasonchen.microlang.utils.file.FileDownloaderHttpHelper;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.utils.file.FileManager;

import java.lang.ref.WeakReference;

/**
 * jasonchen
 * 2015/04/10
 */
public class ReadWorker extends AbstractWorker<String, Integer, Boolean> {

    private String data = "";
    private boolean isMultiPictures = false;

    private WeakReference<ImageView> viewWeakReference;
    private WeakReference<ProgressBar> pbWeakReference;

    private FileLocationMethod method;
    private com.jasonchen.microlang.interfaces.ISimRayDrawable ISimRayDrawable;

    public String getUrl() {
        return data;
    }

    public ReadWorker(ImageView view, String url, FileLocationMethod method,
            boolean isMultiPictures) {

        this.viewWeakReference = new WeakReference<ImageView>(view);
        this.data = url;
        this.method = method;
        this.isMultiPictures = isMultiPictures;
    }

    public ReadWorker(com.jasonchen.microlang.interfaces.ISimRayDrawable view, String url, FileLocationMethod method,
            boolean isMultiPictures) {

        this(view.getImageView(), url, method, false);
        this.ISimRayDrawable = view;
        this.pbWeakReference = new WeakReference<ProgressBar>(view.getProgressBar());
        view.setGifFlag(false);
        if (SettingUtility.getEnableBigPic()) {
            if (view.getProgressBar() != null) {
                view.getProgressBar().setVisibility(View.VISIBLE);
                view.getProgressBar().setProgress(0);
            }
        } else {
            if (view.getProgressBar() != null) {
                view.getProgressBar().setVisibility(View.INVISIBLE);
                view.getProgressBar().setProgress(0);
            }
        }
        this.isMultiPictures = isMultiPictures;
    }

    @Override
    protected Boolean doInBackground(String... url) {

        synchronized (TimeLineBitmapDownloader.pauseReadWorkLock) {
            while (TimeLineBitmapDownloader.pauseReadWork && !isCancelled()) {
                try {
                    TimeLineBitmapDownloader.pauseReadWorkLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (isCancelled()) {
            return null;
        }

        String path = FileManager.generateDownloadFileName(data);

        boolean result = TaskCache.waitForPictureDownload(data,
                (SettingUtility.getEnableBigPic() ? downloadListener : null), path, method);

        return result;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (TimeLineBitmapDownloader.pauseDownloadWork) {
            return;
        }

        ImageView imageView = viewWeakReference.get();
        if (!isMySelf(imageView)) {
            return;
        }

        if (pbWeakReference != null) {
            ProgressBar pb = pbWeakReference.get();
            if (pb != null) {
                Integer progress = values[0];
                Integer max = values[1];
                pb.setMax(max);
                pb.setProgress(progress);
            }
        }
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        ImageView imageView = viewWeakReference.get();
        if (!isMySelf(imageView)) {
            return;
        }
        if (pbWeakReference != null) {
            ProgressBar pb = pbWeakReference.get();
            if (pb != null) {
                pb.setVisibility(View.INVISIBLE);
            }
        }
        imageView.setImageDrawable(
                new ColorDrawable(DebugColor.DOWNLOAD_CANCEL));
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

            if (ISimRayDrawable != null) {
                newTask = new LocalWorker(ISimRayDrawable, getUrl(), method,
                        isMultiPictures);
                PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(newTask);
                ISimRayDrawable.setImageDrawable(downloadedDrawable);
            } else {
                newTask = new LocalWorker(imageView, getUrl(), method, isMultiPictures);
                PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(newTask);
                imageView.setImageDrawable(downloadedDrawable);
            }

            newTask.executeOnIO();
        } else {
            if (pbWeakReference != null) {
                ProgressBar pb = pbWeakReference.get();
                if (pb != null) {
                    pb.setVisibility(View.INVISIBLE);
                }
            }
            imageView.setImageDrawable(
                    new ColorDrawable(DebugColor.DOWNLOAD_FAILED));
        }
    }

    FileDownloaderHttpHelper.DownloadListener downloadListener
            = new FileDownloaderHttpHelper.DownloadListener() {
        @Override
        public void pushProgress(int progress, int max) {
            onProgressUpdate(progress, max);
        }

        @Override
        public void completed() {

        }

        @Override
        public void cancel() {

        }
    };
}
