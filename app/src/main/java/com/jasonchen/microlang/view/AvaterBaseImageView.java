package com.jasonchen.microlang.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

/**
 * jasonchen
 * 2015/04/10
 */
@Deprecated
public class AvaterBaseImageView extends RoundImageView {

    private boolean mMeasuredExactly = false;

    private boolean mBlockMeasurement = false;

    public AvaterBaseImageView(Context context) {
        super(context);
    }

    public AvaterBaseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AvaterBaseImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        mBlockMeasurement = true;
        super.setImageDrawable(drawable);
        mBlockMeasurement = false;
    }

    @Override
    public void requestLayout() {
        if (mBlockMeasurement && mMeasuredExactly) {
            // Ignore request

        } else {
            super.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMeasuredExactly = isMeasuredExactly(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private boolean isMeasuredExactly(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMeasureSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMeasureSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        return widthMeasureSpecMode == MeasureSpec.EXACTLY
                && heightMeasureSpecMode == MeasureSpec.EXACTLY;
    }
}