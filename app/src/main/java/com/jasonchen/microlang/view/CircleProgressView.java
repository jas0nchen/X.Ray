package com.jasonchen.microlang.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * jasonchen
 * 2015/04/10
 */
public class CircleProgressView extends View {

    private Paint mPaint = new Paint();
    private Paint mPaintOuter = new Paint();

    private int progress = 0;
    private int max = 100;

    private ValueAnimator valueAnimator;

    private boolean isInitValue = true;

    public CircleProgressView(Context context) {
        this(context, null);
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint.setStrokeWidth(10);
        mPaint.setColor(Color.parseColor("#009688"));
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        //mPaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        mPaintOuter.setColor(Color.LTGRAY);
        mPaintOuter.setAntiAlias(true);
        mPaintOuter.setStyle(Paint.Style.STROKE);
        mPaintOuter.setStrokeWidth(10);
        int h = Math.min(width, height);
        canvas.drawCircle(width/2, height/2, (h - 20)/2, mPaintOuter);

        RectF oval2 = new RectF((width - h) / 2 + 10, (height - h) / 2 + 10, h + (width - h) / 2 -10,
                h + (height - h) / 2 - 10);

        if (getProgress() < 360) {
            mPaint.setColor(Color.parseColor("#009688"));
            mPaint.setStrokeWidth(10);
            canvas.drawArc(oval2, 270, getProgress(), false, mPaint);
            mPaint.setStrokeWidth(1);
            mPaint.setTextSize(25);
            canvas.drawText((getProgress()*100/360) + "%", width/2 - 20, height/2 + 10, mPaint);
        } else {
            mPaint.setColor(Color.LTGRAY);
            canvas.drawArc(oval2, 270, 360, false, mPaint);
        }
    }

    private int getProgress() {
        return 360 * progress / max;
    }

    public void setMax(int number) {
        this.max = number;
        invalidate();
    }

    public void setProgress(int progress) {
        if (progress == 0) {
            invalidate();
            return;
        }

        if (progress <= this.progress) {
            this.progress = progress;
            invalidate();
            return;
        }

        if (isInitValue) {
            isInitValue = false;
            this.progress = progress;
            invalidate();
            return;
        }

        int start = this.progress;

        if (valueAnimator != null && valueAnimator.isRunning()) {
            start = (Integer) valueAnimator.getAnimatedValue();
            valueAnimator.cancel();
        }

        valueAnimator = ValueAnimator.ofInt(start, progress);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @SuppressLint("NewApi")
			@Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                CircleProgressView.this.progress = value;
                postInvalidateOnAnimation();
            }
        });
        valueAnimator.start();
    }

    public void executeRunnableAfterAnimationFinish(final Runnable runnable) {
        if (valueAnimator != null && valueAnimator.isRunning()) {
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
