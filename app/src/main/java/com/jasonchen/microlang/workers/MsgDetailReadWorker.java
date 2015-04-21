package com.jasonchen.microlang.workers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.gallery.GalleryAnimationActivity;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.AnimationRect;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.TaskCache;
import com.jasonchen.microlang.utils.file.FileDownloaderHttpHelper;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.utils.file.FileManager;
import com.jasonchen.microlang.utils.image.ImageUtility;
import com.jasonchen.microlang.view.WeiboDetailImageView;

import java.util.ArrayList;

/**
 * jasonchen
 * 2015/04/10
 */
public class MsgDetailReadWorker extends MyAsyncTask<Void, Integer, String> {

    private WeiboDetailImageView view;
    private ProgressBar pb;
    private Button retry;

    private MessageBean msg;

    public MsgDetailReadWorker(WeiboDetailImageView view, MessageBean msg) {
        this.view = view;
        this.pb = this.view.getProgressBar();
        this.msg = msg;
        this.retry = view.getRetryButton();
        retry.setVisibility(View.INVISIBLE);

        String oriPath = FileManager.getFilePathFromUrl(msg.getOriginal_pic(),
                FileLocationMethod.picture_large);

        if (ImageUtility.isThisBitmapCanRead(oriPath)
                && TaskCache.isThisUrlTaskFinished(msg.getOriginal_pic())) {

            onPostExecute(oriPath);
            cancel(true);
            return;
        }

        String picPath = FileManager.getFilePathFromUrl(msg.getBmiddle_pic(), FileLocationMethod.picture_bmiddle);

        if (ImageUtility.isThisBitmapCanRead(picPath)
                && TaskCache.isThisUrlTaskFinished(msg.getBmiddle_pic())) {
            onPostExecute(picPath);
            cancel(true);
            return;
        }

        pb.setVisibility(View.VISIBLE);
        pb.setIndeterminate(true);
    }

    public void setView(WeiboDetailImageView view) {
        this.view = view;
        this.pb = this.view.getProgressBar();
        this.retry = view.getRetryButton();
        retry.setVisibility(View.INVISIBLE);
    }

    @Override
    protected String doInBackground(Void... arg) {
        if (isCancelled()) {
            return null;
        }

        if (SettingUtility.getEnableBigPic()) {
            boolean result = TaskCache
                    .waitForPictureDownload(msg.getOriginal_pic(),
                            downloadListener, FileManager
                                    .generateDownloadFileName(msg
                                            .getOriginal_pic()),
                            FileLocationMethod.picture_large);

            return result ? FileManager.getFilePathFromUrl(
                    msg.getOriginal_pic(), FileLocationMethod.picture_large)
                    : null;
        } else {
            boolean result = TaskCache.waitForPictureDownload(
                    msg.getBmiddle_pic(), downloadListener,
                    FileManager.generateDownloadFileName(msg.getBmiddle_pic()),
                    FileLocationMethod.picture_bmiddle);

            return result ? FileManager.getFilePathFromUrl(
                    msg.getBmiddle_pic(), FileLocationMethod.picture_bmiddle)
                    : null;
        }
    }

    FileDownloaderHttpHelper.DownloadListener downloadListener = new FileDownloaderHttpHelper.DownloadListener() {

        @Override
        public void pushProgress(int progress, int max) {
            onProgressUpdate(progress, max);
        }
    };

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (this.getStatus() == Status.RUNNING) {
            pb.setVisibility(View.VISIBLE);
            pb.setIndeterminate(false);

            Integer progress = values[0];
            Integer max = values[1];

            pb.setMax(max);
            pb.setProgress(progress);
        }
    }

    @Override
    protected void onCancelled(String bitmap) {
        pb.setVisibility(View.INVISIBLE);
        super.onCancelled(bitmap);
    }

    @Override
    protected void onPostExecute(String path) {
        retry.setVisibility(View.INVISIBLE);
        pb.setIndeterminate(true);

        if (!TextUtils.isEmpty(path)) {

            if (!path.endsWith(".gif")) {
                readNormalPic(path);
            } else {
                view.setGif(path);
            }
            pb.setVisibility(View.INVISIBLE);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AnimationRect rect = AnimationRect.buildFromImageView(view
                            .getImageView());
                    ArrayList<AnimationRect> animationRectArrayList = new ArrayList<AnimationRect>();
                    animationRectArrayList.add(rect);
                    Intent intent = GalleryAnimationActivity.newIntent(msg, animationRectArrayList, 0);
                    GlobalContext.getInstance().getCurrentRunningActivity()
                            .startActivity(intent);
                }
            });
        } else {
            pb.setVisibility(View.INVISIBLE);
            view.setImageDrawable(new ColorDrawable(GlobalContext.getInstance()
                    .getResources().getColor(R.color.gainsboro)));
            retry.setVisibility(View.VISIBLE);
            retry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MsgDetailReadWorker picTask = new MsgDetailReadWorker(view,
                            msg);
                    picTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
        }
    }

    private void readNormalPic(String path) {

        Bitmap bitmap = ImageUtility.readNormalPic(path, 2000, 2000);

        view.setTag(true);
        view.getImageView().setTag(true);
        view.setVisibility(View.VISIBLE);
        view.setImageBitmap(bitmap);
        view.setAlpha(0.0f);
        view.animate().alpha(1.0f).setDuration(200);
    }
}