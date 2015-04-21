package com.jasonchen.microlang.gallery;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.debug.AppLogger;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.AnimationRect;
import com.jasonchen.microlang.utils.AnimationUtility;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.utils.file.FileManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * jasonchen
 * 2015/04/10
 */
@SuppressLint("NewApi")
public class GalleryAnimationActivity extends ActionBarActivity {

	private ArrayList<AnimationRect> rectList;
	private ArrayList<String> urls = new ArrayList<String>();
    private ArrayList<String> oriUrls = new ArrayList<String>();


	private Toolbar toolbar;
	private ViewPager pager;
    private FragmentPagerAdapter adapter;
	private View background;

    private PicSaveTask saveTask;
    private int initPosition;
	private ColorDrawable backgroundColor;

	public static Intent newIntent(MessageBean msg,
			ArrayList<AnimationRect> rectList, int initPosition) {
		Intent intent = new Intent(GlobalContext.getInstance(),
				GalleryAnimationActivity.class);
		intent.putExtra("msg", msg);
		intent.putExtra("rect", rectList);
		intent.putExtra("position", initPosition);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.galleryactivity_animation_layout);

		rectList = getIntent().getParcelableArrayListExtra("rect");
		MessageBean msg = getIntent().getParcelableExtra("msg");
		ArrayList<String> tmp = msg.getThumbnailPicUrls();
		for (int i = 0; i < tmp.size(); i++) {
            oriUrls.add(tmp.get(i));
			urls.add(tmp.get(i).replace("thumbnail", "large"));
		}

		boolean disableHardwareLayerType = false;

		for (String url : urls) {
			if (url.contains(".gif")) {
				disableHardwareLayerType = true;
				break;
			}
		}

		toolbar = ViewUtility.findViewById(this, R.id.toolbar);
		initPosition = getIntent().getIntExtra("position", 0);
        pager = (ViewPager) findViewById(R.id.pager);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbar.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_gradient));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		getSupportActionBar().setTitle("[1/" + urls.size() + "]");

        adapter = new ImagePagerAdapter(getSupportFragmentManager());
		pager.setAdapter(adapter);
		final boolean finalDisableHardwareLayerType = disableHardwareLayerType;
		pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				getSupportActionBar().setTitle("[" + String.valueOf(position + 1) + "/" + urls.size() + "]");
			}

			@Override
			public void onPageScrollStateChanged(int scrollState) {
				if (scrollState != ViewPager.SCROLL_STATE_IDLE
						&& finalDisableHardwareLayerType) {
					final int childCount = pager.getChildCount();
					for (int i = 0; i < childCount; i++) {
						View child = pager.getChildAt(i);
						if (child.getLayerType() != View.LAYER_TYPE_NONE) {
							child.setLayerType(View.LAYER_TYPE_NONE, null);
						}
					}
				}
			}
		});
		pager.setCurrentItem(getIntent().getIntExtra("position", 0));
		pager.setOffscreenPageLimit(1);
		pager.setPageTransformer(true, new ZoomOutPageTransformer());

		background = AnimationUtility.getAppContentView(this);

		if (savedInstanceState != null) {
			showBackgroundImmediately();
		}
	}

    private void saveBitmapToPictureDir(String filePath) {
        if (Utility.isTaskStopped(saveTask)) {
            saveTask = new PicSaveTask(GalleryAnimationActivity.this, filePath);
            saveTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

	private HashMap<Integer, ContainerFragment> fragmentMap = new HashMap<Integer, ContainerFragment>();

	private boolean alreadyAnimateIn = false;

	private class ImagePagerAdapter extends FragmentPagerAdapter {

		public ImagePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {

			ContainerFragment fragment = fragmentMap.get(position);
			if (fragment == null) {

				boolean animateIn = (initPosition == position)
						&& !alreadyAnimateIn;
				fragment = ContainerFragment.newInstance(oriUrls.get(position), urls.get(position),
                        rectList.get(position), animateIn,
                        initPosition == position);
				alreadyAnimateIn = true;
				fragmentMap.put(position, fragment);
			}

			return fragment;
		}

		// when activity is recycled, ViewPager will reuse fragment by theirs
		// name, so
		// getItem wont be called, but we need fragmentMap to animate close
		// operation
		@Override
		public void setPrimaryItem(ViewGroup container, int position,
				Object object) {
			super.setPrimaryItem(container, position, object);
			if (object instanceof Fragment) {
				fragmentMap.put(position, (ContainerFragment) object);
			}
		}

		@Override
		public int getCount() {
			return urls.size();
		}
	}

	public void showBackgroundImmediately() {
		if (background.getBackground() == null) {
			backgroundColor = new ColorDrawable(getResources().getColor(R.color.black));
			background.setBackground(backgroundColor);
		}
	}

	public ObjectAnimator showBackgroundAnimate() {
		backgroundColor = new ColorDrawable(getResources().getColor(R.color.black));
		background.setBackground(backgroundColor);
		ObjectAnimator bgAnim = ObjectAnimator.ofInt(backgroundColor, "alpha",
				0, 255);
		bgAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				background.setBackground(backgroundColor);
			}
		});
		return bgAnim;
	}

	@Override
	public void onBackPressed() {
		ContainerFragment fragment = fragmentMap.get(pager.getCurrentItem());
		if (fragment != null && fragment.canAnimateCloseActivity()) {
			backgroundColor = new ColorDrawable(getResources().getColor(R.color.black));
			ObjectAnimator bgAnim = ObjectAnimator.ofInt(backgroundColor,
					"alpha", 0);
			bgAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					background.setBackground(backgroundColor);
				}
			});
			bgAnim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					GalleryAnimationActivity.super.finish();
					overridePendingTransition(-1, -1);
				}
			});
			fragment.animationExit(bgAnim);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_activity_gallery, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final String url = urls.get(pager.getCurrentItem());
		final String path = FileManager.getFilePathFromUrl(url, FileLocationMethod.picture_large);
		int itemId = item.getItemId();
		if (itemId == R.id.save) {
			saveBitmapToPictureDir(path);
		} else if (itemId == R.id.share){
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("image/jpeg");
			if (!TextUtils.isEmpty(path)) {
				Uri uri = Uri.fromFile(new File(path));
				sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
				if (Utility.isIntentSafe(GalleryAnimationActivity.this,
						sharingIntent)) {
					startActivity(Intent.createChooser(sharingIntent,
							getString(R.string.share)));
				}
			}
		} else if (itemId == R.id.copy){
			ClipboardManager cm = (ClipboardManager) getSystemService(
					Context.CLIPBOARD_SERVICE);
			cm.setPrimaryClip(ClipData.newPlainText("sinaweibo", url));
			Toast.makeText(GalleryAnimationActivity.this,
					getString(R.string.copy_successfully),
					Toast.LENGTH_SHORT).show();
		}
		return false;
	}
}
