package com.jasonchen.microlang.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.interfaces.ISimRayDrawable;

/**
 * jasonchen
 * 2015/04/10
 */
public class TimeLineImageView extends FrameLayout implements ISimRayDrawable {

	private boolean showGif = false;
	private Paint paint = new Paint();
	private Bitmap gif;

	protected ImageView mImageView;
	private ProgressBar pb;
	private boolean parentPressState = true;

	public TimeLineImageView(Context context) {
		this(context, null);
	}

	public TimeLineImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimeLineImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initLayout(context);
	}

	protected void initLayout(Context context) {
		gif = BitmapFactory.decodeResource(getResources(),
				R.drawable.timeline_image_gif);
		LayoutInflater inflate = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflate.inflate(R.layout.timelineimageview_layout, this, true);
		mImageView = (ImageView) v.findViewById(R.id.imageview);
		mImageView.setImageDrawable(new ColorDrawable(getResources().getColor(
				R.color.gainsboro)));

		pb = (ProgressBar) v.findViewById(R.id.imageview_pb);
		this.setForeground(getResources().getDrawable(
				R.drawable.timelineimageview_cover));
		this.setAddStatesFromChildren(true);
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		if (showGif) {
			int bitmapHeight = gif.getHeight();
			int bitmapWidth = gif.getWidth();
			int x = getWidth() - bitmapWidth;
			int y = getHeight() - bitmapHeight;
			canvas.drawBitmap(gif, x, y, paint);
		}
	}

	public void setImageDrawable(Drawable drawable) {
		mImageView.setImageDrawable(drawable);
	}

	public void setImageBitmap(Bitmap bm) {
		mImageView.setImageBitmap(bm);
	}

	public ImageView getImageView() {
		return mImageView;
	}

	public void setProgress(int value, int max) {
		pb.setVisibility(View.VISIBLE);
		pb.setMax(max);
		pb.setProgress(value);
	}

	public ProgressBar getProgressBar() {
		return pb;
	}

	public void setGifFlag(boolean value) {
		if (showGif != value) {
			showGif = value;
			invalidate();
		}
	}

	@Override
	public void checkVerified(UserBean user) {

	}

	@Override
	public void setPressesStateVisibility(boolean value) {
		if (parentPressState == value) {
			return;
		}
		setForeground(value ? getResources().getDrawable(
				R.drawable.timelineimageview_cover) : null);
		parentPressState = value;
	}
}
