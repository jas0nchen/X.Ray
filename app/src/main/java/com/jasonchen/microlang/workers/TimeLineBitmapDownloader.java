package com.jasonchen.microlang.workers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.interfaces.IPictureWorker;
import com.jasonchen.microlang.interfaces.ISimRayDrawable;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.TaskCache;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.file.FileDownloaderHttpHelper;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.utils.file.FileManager;
import com.jasonchen.microlang.utils.image.ImageUtility;
import com.jasonchen.microlang.view.TimeLineImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 2015/04/10
 */
public class TimeLineBitmapDownloader {

	private int defaultPictureResId;

	private Handler handler;

	static volatile boolean pauseDownloadWork = false;
	static final Object pauseDownloadWorkLock = new Object();

	static volatile boolean pauseReadWork = false;
	static final Object pauseReadWorkLock = new Object();

	private static final Object lock = new Object();

	private static TimeLineBitmapDownloader instance;

	private TimeLineBitmapDownloader(Handler handler) {
		this.handler = handler;
		this.defaultPictureResId = R.drawable.timeline_image_loading;
	}

	public static TimeLineBitmapDownloader getInstance() {
		synchronized (lock) {
			if (instance == null) {
				instance = new TimeLineBitmapDownloader(new Handler(
						Looper.getMainLooper()));
			}
		}
		return instance;
	}

	public static void refreshThemePictureBackground() {
		synchronized (lock) {
			instance = new TimeLineBitmapDownloader(new Handler(
					Looper.getMainLooper()));
		}
	}

	/**
	 * Pause any ongoing background work. This can be used as a temporary
	 * measure to improve performance. For example background work could be
	 * paused when a ListView or GridView is being scrolled using a
	 * {@link android.widget.AbsListView.OnScrollListener} to keep scrolling
	 * smooth.
	 * <p/>
	 * If work is paused, be sure setPauseDownloadWork(false) is called again
	 * before your fragment or activity is destroyed (for example during
	 * {@link android.app.Activity#onPause()}), or there is a risk the
	 * background thread will never finish.
	 */
	public void setPauseDownloadWork(boolean pauseWork) {
		synchronized (pauseDownloadWorkLock) {
			TimeLineBitmapDownloader.pauseDownloadWork = pauseWork;
			if (!TimeLineBitmapDownloader.pauseDownloadWork) {
				pauseDownloadWorkLock.notifyAll();
			}
		}
	}

	public void setPauseReadWork(boolean pauseWork) {
		synchronized (pauseReadWorkLock) {
			TimeLineBitmapDownloader.pauseReadWork = pauseWork;
			if (!TimeLineBitmapDownloader.pauseReadWork) {
				pauseReadWorkLock.notifyAll();
			}
		}
	}

	protected Bitmap getBitmapFromMemCache(String key) {
		if (TextUtils.isEmpty(key)) {
			return null;
		} else {
			return GlobalContext.getInstance().getBitmapCache().get(key);
		}
	}

	public void downloadAvatar(ImageView view, UserBean user) {
		downloadAvatar(view, user, false);
	}

	public void downloadAvatar(ImageView view, UserBean user, Fragment fragment) {
		// boolean isFling = fragment.isListViewFling();
		downloadAvatar(view, user, false);
	}

	public void downloadAvatar(ImageView view, UserBean user, boolean isFling) {

		if (user == null) {
			view.setImageResource(defaultPictureResId);
			return;
		}

		String url;
		FileLocationMethod method;
		if (SettingUtility.getEnableBigAvatar()) {
			url = user.getAvatar_large();
			method = FileLocationMethod.avatar_large;
		} else {
			url = user.getProfile_image_url();
			method = FileLocationMethod.avatar_small;
		}
		displayImageView(view, url, method, isFling, false);
	}



	public void downContentPic(ImageView view, MessageBean msg,
			Fragment fragment) {
		String picUrl;

		// boolean isFling = fragment.isListViewFling();

		if (SettingUtility.getEnableBigPic()) {
			picUrl = msg.getOriginal_pic();
			displayImageView(view, picUrl, FileLocationMethod.picture_large,
					false, false);
		} else {
			picUrl = msg.getThumbnail_pic();
			displayImageView(view, picUrl,
					FileLocationMethod.picture_thumbnail, false, false);
		}
	}

	public void displayMultiPicture(ISimRayDrawable view, String picUrl,
			FileLocationMethod method, boolean isFling) {

		// boolean isFling = fragment.isListViewFling();

		display(view, picUrl, method, isFling, true);
	}

	public void displayMultiPicture(ISimRayDrawable view, String picUrl,
			FileLocationMethod method) {

		display(view, picUrl, method, false, true);
	}

	public void downContentPic(TimeLineImageView view, MessageBean msg,
			Boolean isFling) {
		String picUrl;

		// boolean isFling = fragment.isListViewFling();

		if (SettingUtility.getEnableBigPic()) {
			picUrl = msg.getOriginal_pic();
			displayContentPic(view, picUrl, FileLocationMethod.picture_large, isFling,
                    false);
		} else {
			picUrl = msg.getThumbnail_pic();
			displayContentPic(view, picUrl, FileLocationMethod.picture_thumbnail,
                    isFling, false);
		}
	}

	/**
	 * when user open weibo detail, the activity will setResult to previous
	 * Activity, timeline will refresh at the time user press back button to
	 * display the latest repost count and comment count. But sometimes, weibo
	 * detail's pictures are very large that bitmap memory cache has cleared
	 * those timeline bitmap to save memory, app have to read bitmap from sd
	 * card again, then app play annoying animation , this method will check
	 * whether we should read again or not.
	 */
	/*
     **/
	private boolean shouldReloadPicture(ImageView view, String urlKey) {
		if (urlKey.equals(view.getTag())
				&& view.getDrawable() != null
				&& view.getDrawable() instanceof BitmapDrawable
				&& ((BitmapDrawable) view.getDrawable() != null && ((BitmapDrawable) view
						.getDrawable()).getBitmap() != null)) {
			// AppLogger.d("shouldReloadPicture=false");
			return false;
		} else {
			view.setTag(null);
			// AppLogger.d("shouldReloadPicture=true");
			return true;
		}
	}

	private void displayImageView(final ImageView view, final String urlKey,
			final FileLocationMethod method, boolean isFling,
			boolean isMultiPictures) {
		view.clearAnimation();

		if (!shouldReloadPicture(view, urlKey)) {
			return;
		}

		final Bitmap bitmap = getBitmapFromMemCache(urlKey);
		if (bitmap != null) {
			view.setImageBitmap(bitmap);
			view.setTag(urlKey);
			if (view.getAlpha() != 1.0f) {
				view.setAlpha(1.0f);
			}
			cancelPotentialDownload(urlKey, view);
		} else {

			if (isFling) {
				view.setImageResource(defaultPictureResId);
				return;
			}

			if (!cancelPotentialDownload(urlKey, view)) {
				return;
			}

			final LocalOrNetworkChooseWorker newTask = new LocalOrNetworkChooseWorker(
					view, urlKey, method, isMultiPictures);
			PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(
					newTask);
			view.setImageDrawable(downloadedDrawable);

			// listview fast scroll performance
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {

					if (getBitmapDownloaderTask(view) == newTask) {
						newTask.executeOnNormal();
					}
					return;
				}
			}, 400);
		}
	}

    private void displayContentPic(final TimeLineImageView view, final String urlKey,
                         final FileLocationMethod method, boolean isFling,
                         boolean isMultiPictures) {
        view.getImageView().clearAnimation();

        if (!shouldReloadPicture(view.getImageView(), urlKey)) {

            return;
        }

        final Bitmap bitmap = getBitmapFromMemCache(urlKey);
        if (bitmap != null) {
            System.out.println("从缓存读取图片中...");
            float realHeight = bitmap.getHeight();
            float realWidth = bitmap.getWidth();
            List<Float> result = Utility.calculateWidthAndHeight(realWidth, realHeight);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();

            params.height = result.get(1).intValue();
            params.width = result.get(0).intValue();
            view.setLayoutParams(params);
            view.requestLayout();
            System.out.println("view params height:" + params.height + "width:" + params.width);
            view.getImageView().setScaleType(ImageView.ScaleType.FIT_XY);
            view.setImageBitmap(bitmap);
            view.getImageView().setTag(urlKey);
            if (view.getProgressBar() != null) {
                view.getProgressBar().setVisibility(View.INVISIBLE);
            }
            if (view.getImageView().getAlpha() != 1.0f) {
                view.getImageView().setAlpha(1.0f);
            }
            view.setGifFlag(ImageUtility.isThisPictureGif(urlKey));
            cancelPotentialDownload(urlKey, view.getImageView());
        } else {

            if (isFling) {
                view.getImageView().setImageResource(defaultPictureResId);
                if (view.getProgressBar() != null) {
                    view.getProgressBar().setVisibility(View.INVISIBLE);
                }
                view.setGifFlag(ImageUtility.isThisPictureGif(urlKey));
                // return;
            }

            if (!cancelPotentialDownload(urlKey, view.getImageView())) {
                return;
            }

            final SyncSizeLocalOrNetworkChooseWorker newTask = new SyncSizeLocalOrNetworkChooseWorker(
                    view, urlKey, method, isMultiPictures);
            PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(
                    newTask);
            view.setImageDrawable(downloadedDrawable);

            // listview fast scroll performance
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (getBitmapDownloaderTask(view.getImageView()) == newTask) {
                        newTask.executeOnNormal();
                    }
                    return;
                }
            }, 400);
        }
    }

	private void display(final ISimRayDrawable view, final String urlKey,
			final FileLocationMethod method, boolean isFling,
			boolean isMultiPictures) {
		view.getImageView().clearAnimation();

		if (!shouldReloadPicture(view.getImageView(), urlKey)) {
			return;
		}

		final Bitmap bitmap = getBitmapFromMemCache(urlKey);
		if (bitmap != null) {
			view.setImageBitmap(bitmap);
			view.getImageView().setTag(urlKey);
			if (view.getProgressBar() != null) {
				view.getProgressBar().setVisibility(View.INVISIBLE);
			}
			if (view.getImageView().getAlpha() != 1.0f) {
				view.getImageView().setAlpha(1.0f);
			}
			view.setGifFlag(ImageUtility.isThisPictureGif(urlKey));
			cancelPotentialDownload(urlKey, view.getImageView());
		} else {

			if (isFling) {
				view.getImageView().setImageResource(defaultPictureResId);
				if (view.getProgressBar() != null) {
					view.getProgressBar().setVisibility(View.INVISIBLE);
				}
				view.setGifFlag(ImageUtility.isThisPictureGif(urlKey));
				// return;
			}

			if (!cancelPotentialDownload(urlKey, view.getImageView())) {
				return;
			}

			final LocalOrNetworkChooseWorker newTask = new LocalOrNetworkChooseWorker(
					view, urlKey, method, isMultiPictures);
			PictureBitmapDrawable downloadedDrawable = new PictureBitmapDrawable(
					newTask);
			view.setImageDrawable(downloadedDrawable);

			// listview fast scroll performance
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {

					if (getBitmapDownloaderTask(view.getImageView()) == newTask) {
						newTask.executeOnNormal();
					}
					return;
				}
			}, 400);
		}
	}

	public void totalStopLoadPicture() {

	}

	private static boolean cancelPotentialDownload(String url,
			ImageView imageView) {
		IPictureWorker bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

		if (bitmapDownloaderTask != null) {
			String bitmapUrl = bitmapDownloaderTask.getUrl();
			if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
				if (bitmapDownloaderTask instanceof MyAsyncTask) {
					((MyAsyncTask) bitmapDownloaderTask).cancel(true);
				}
			} else {
				return false;
			}
		}
		return true;
	}

	private static IPictureWorker getBitmapDownloaderTask(ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof PictureBitmapDrawable) {
				PictureBitmapDrawable downloadedDrawable = (PictureBitmapDrawable) drawable;
				return downloadedDrawable.getBitmapDownloaderTask();
			}
		}
		return null;
	}

	public void display(final ImageView imageView, final int width,
			final int height, final String url,
			final FileLocationMethod method, boolean isFling) {
		ArrayList<ImageView> imageViewArrayList = new ArrayList<ImageView>();
		imageViewArrayList.add(imageView);
		display(imageViewArrayList, width, height, url, method, null, isFling);
	}

	public void displayRoundAvatar(final ImageView imageView, final int width,
			final int height, final String url,
			final FileLocationMethod method, boolean isFling) {

		//displayImageView(imageView, url, method, isFling, false);
		displayReoundAvatar(imageView, width, height, url, method, null,
				isFling);
	}

	public void displayReoundAvatar(final ImageView view, final int width,
			final int height, final String url,
			final FileLocationMethod method,
			final ArrayList<Animation> animations, boolean isFling) {
		if (TextUtils.isEmpty(url)) {
			return;
		}
		if (!shouldReloadPicture(view, url)) {
			return;
		}

		final Bitmap bitmap = getBitmapFromMemCache(url);
		if (bitmap != null) {
			view.setImageBitmap(bitmap);
			view.setTag(url);

			if (view.getAlpha() != 1.0f) {
				view.setAlpha(1.0f);
			}
			cancelPotentialDownload(url, view);
			return;
		}

		if (isFling) {
			view.setImageResource(defaultPictureResId);
		}

		new MyAsyncTask<Void, Bitmap, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... params) {
				Bitmap bitmap = null;

				String path = FileManager.getFilePathFromUrl(url, method);

				if (!(ImageUtility.isThisBitmapCanRead(path) && TaskCache
						.isThisUrlTaskFinished(url))) {

					boolean downloaded = TaskCache.waitForPictureDownload(url,
                            null, FileManager.generateDownloadFileName(url),
                            method);
					if (downloaded) {
						path = FileManager.getFilePathFromUrl(url, method);
					}
				}

				if (!TextUtils.isEmpty(path)) {
					bitmap = ImageUtility.readNormalPic(path, width, height);
				}
				return bitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				super.onPostExecute(bitmap);
				if (bitmap != null) {
					GlobalContext.getInstance().getBitmapCache()
							.put(url, bitmap);
					view.setImageDrawable(new BitmapDrawable(GlobalContext
							.getInstance().getResources(), bitmap));
					view.setTag(url);
					// Animation animation = animations.get(0);
					// view.startAnimation(animation);
				}
			}
		}.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void display(final ArrayList<ImageView> imageView, final int width,
			final int height, final String url,
			final FileLocationMethod method,
			final ArrayList<Animation> animations, boolean isFling) {
		if (TextUtils.isEmpty(url)) {
			return;
		}

		final Bitmap bitmap = getBitmapFromMemCache(url);
		if (bitmap != null && bitmap.getHeight() == height
				&& bitmap.getWidth() == width) {
			for (int i = 0; i < imageView.size(); i++) {
				ImageView imageView1 = imageView.get(i);
				imageView1.setImageDrawable(new BitmapDrawable(GlobalContext
						.getInstance().getResources(), bitmap));
				if (animations != null && animations.size() > i) {
					Animation animation = animations.get(i);
					imageView1.startAnimation(animation);
				}
			}
			return;
		}

		new MyAsyncTask<Void, Bitmap, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... params) {
				Bitmap bitmap = null;

				String path = FileManager.getFilePathFromUrl(url, method);

				if (!(ImageUtility.isThisBitmapCanRead(path) && TaskCache
						.isThisUrlTaskFinished(url))) {

					boolean downloaded = TaskCache.waitForPictureDownload(url,
                            null, FileManager.generateDownloadFileName(url),
                            method);
					if (downloaded) {
						path = FileManager.getFilePathFromUrl(url, method);
					}
				}

				if (!TextUtils.isEmpty(path)) {
					bitmap = ImageUtility.readNormalPic(path, width, height);
				}
				return bitmap;
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				super.onPostExecute(bitmap);
				if (bitmap != null) {
					GlobalContext.getInstance().getBitmapCache()
							.put(url, bitmap);
					for (int i = 0; i < imageView.size(); i++) {
						ImageView imageView1 = imageView.get(i);
						imageView1.setImageDrawable(new BitmapDrawable(
								GlobalContext.getInstance().getResources(),
								bitmap));

						if (animations != null && animations.size() > i) {
							Animation animation = animations.get(i);
							imageView1.startAnimation(animation);
						}
					}
				}
			}
		}.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static class DownloadCallback {

		public void onSubmitJobButNotBegin() {

		}

		public void onSubmitJobButAlreadyBegin() {

		}

		public void onBegin() {

		}

		public void onUpdate(int value, int max) {

		}

		public void onComplete(String localPath) {

		}
	}

	public void download(final Activity activity, final String url,
			final FileLocationMethod method, final DownloadCallback callback) {
		downloadInner(activity, url, method, callback);
	}

	public void download(final Fragment fragment, final String url,
			final FileLocationMethod method, final DownloadCallback callback) {
		downloadInner(fragment, url, method, callback);
	}

	@SuppressLint("NewApi")
	private void downloadInner(final Object object, final String url,
			final FileLocationMethod method, final DownloadCallback callback) {

		if (TextUtils.isEmpty(url)) {
			return;
		}

		if (TaskCache.isThisUrlTaskFinished(url)) {
			callback.onSubmitJobButNotBegin();
		} else {
			callback.onSubmitJobButAlreadyBegin();
		}

		new MyAsyncTask<Void, Integer, String>() {

			WeakReference<Object> activityRef;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				activityRef = new WeakReference<Object>(object);
				callback.onBegin();
			}

			@Override
			protected String doInBackground(Void... params) {

				String path = FileManager.getFilePathFromUrl(url, method);

				if (!(ImageUtility.isThisBitmapCanRead(path) && TaskCache
						.isThisUrlTaskFinished(url))) {
					boolean downloaded = TaskCache.waitForPictureDownload(url,
                            new FileDownloaderHttpHelper.DownloadListener() {
                                @Override
                                public void pushProgress(final int progress,
                                                         final int max) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            onProgressUpdate(progress, max);
                                        }
                                    });
                                }

                                @Override
                                public void completed() {

                                }

                                @Override
                                public void cancel() {

                                }
                            }, FileManager.getFilePathFromUrl(url, method),
                            method);
					if (downloaded) {
						path = FileManager.getFilePathFromUrl(url, method);
					}
				}

				return path;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);
				if (!isComponentLifeCycleFinished()) {
					int progress = values[0];
					int max = values[1];
					callback.onUpdate(progress, max);
				}
			}

			@Override
			protected void onPostExecute(String value) {
				super.onPostExecute(value);
				if (!isComponentLifeCycleFinished()) {
					callback.onComplete(value);
				}
			}

			boolean isComponentLifeCycleFinished() {
				Object object = activityRef.get();
				if (object == null) {
					return true;
				}

				if (object instanceof Fragment) {
					Fragment fragment = (Fragment) object;
					if (fragment.getActivity() == null) {
						return true;
					}
				} else if (object instanceof Activity) {
					Activity activity = (Activity) object;
					if (activity.isDestroyed()) {
						return true;
					}
				}

				return false;
			}
		}.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
	}
}