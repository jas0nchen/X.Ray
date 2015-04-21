package com.jasonchen.microlang.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.interfaces.ISimRayDrawable;
import com.jasonchen.microlang.utils.Utility;

/**
 * jasonchen
 * 2015/04/10
 */
public class TimeLineRoundAvatarImageView extends AvaterBaseImageView implements ISimRayDrawable {

    private Paint paint = new Paint();

    private boolean showPressedState = true;
    private boolean pressed = false;

    private int vType = UserBean.V_TYPE_NONE;

    public TimeLineRoundAvatarImageView(Context context) {
        this(context, null);
    }

    public TimeLineRoundAvatarImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeLineRoundAvatarImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initLayout(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap bitmap;
        Options opts = new Options();
    	opts.inSampleSize = 2;
        switch (vType) {
            case UserBean.V_TYPE_PERSONAL:
            	
               // bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_vip);
            	bitmap =  BitmapFactory.decodeResource(getResources(), R.drawable.avatar_vip, opts);
                canvas.drawBitmap(bitmap, getWidth() - bitmap.getWidth(),
                        getHeight() - bitmap.getHeight(), paint);
                break;
            case UserBean.V_TYPE_ENTERPRISE:
                bitmap = BitmapFactory
                        .decodeResource(getResources(), R.drawable.avatar_enterprise_vip, opts);
                canvas.drawBitmap(bitmap, getWidth() - bitmap.getWidth(),
                        getHeight() - bitmap.getHeight(), paint);
                break;
            default:
                break;
        }

        /*if (pressed) {
            canvas.drawColor(getResources().getColor(R.color.transparent_cover));
        }*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!showPressedState || !isClickable() || !isLongClickable()) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }

    protected void initLayout(Context context) {
        setPadding(Utility.dip2px(5), Utility.dip2px(5), Utility.dip2px(5), Utility.dip2px(5));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
    }

    @Override
    public ImageView getImageView() {
        return this;
    }

    @Override
    public void setProgress(int value, int max) {

    }

    @Override
    public ProgressBar getProgressBar() {
        return null;
    }

    @Override
    public void setGifFlag(boolean value) {

    }

    public void checkVerified(UserBean user) {
        if (user != null && user.isVerified() && !TextUtils.isEmpty(user.getVerified_reason())) {
            if (user.isPersonalV()) {
                verifiedPersonal();
            } else {
                verifiedEnterprise();
            }
        } else {
            reset();
        }
    }

    private void verifiedPersonal() {
        if (vType != UserBean.V_TYPE_PERSONAL) {
            vType = UserBean.V_TYPE_PERSONAL;
            invalidate();
        }
    }

    private void verifiedEnterprise() {
        if (vType != UserBean.V_TYPE_ENTERPRISE) {
            vType = UserBean.V_TYPE_ENTERPRISE;
            invalidate();
        }
    }

    private void reset() {
        if (vType != UserBean.V_TYPE_NONE) {
            vType = UserBean.V_TYPE_NONE;
            invalidate();
        }
    }

    @Override
    public void setPressesStateVisibility(boolean value) {
        if (showPressedState == value) {
            return;
        }
        showPressedState = value;
        invalidate();
    }
}
