package com.library.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ProgressImageView extends androidx.appcompat.widget.AppCompatImageView {

    private Paint mProgressPaint;
    private int startAngle = 0;

    public ProgressImageView(@NonNull Context context) {
        super(context);
        this.init();
    }

    public ProgressImageView(@NonNull Context context,
                             @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public ProgressImageView(@NonNull Context context,
                             @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        mProgressPaint = new Paint();
        mProgressPaint.setColor(Color.DKGRAY);
        mProgressPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLoading(canvas);
    }

    public void setLoading(boolean isLoading) {
        this.isLoading = isLoading;
        invalidate();
    }

    private boolean isLoading = false;

    /**
     * 绘制加载进度条
     *
     * @param canvas
     */
    private void drawLoading(Canvas canvas) {
        if (!isLoading) {
            return;
        }

        float size = 100;// Math.min(rect.width, rect.height);

        float left = (getMeasuredWidth() - size) / 2.0f;
        float top = (getMeasuredHeight() - size) / 2.0f;
        float right = left + size;
        float bottom = top + size;

        RectF oval = new RectF(left, top, right, bottom);

        // 为了绘制出透明度不同的圆环分两部来绘制：

        this.mProgressPaint.setARGB(200, 127, 255, 212);
        float ringWidth = 50.0f;
        this.mProgressPaint.setStrokeWidth(ringWidth);
        // 绘制不透明部分
        canvas.drawArc(oval, startAngle + 180, 90, false, mProgressPaint);
        canvas.drawArc(oval, startAngle, 90, false, mProgressPaint);
        //绘制透明部分
        this.mProgressPaint.setARGB(30, 127, 255, 212);
        canvas.drawArc(oval, startAngle + 90, 90, false, mProgressPaint);
        canvas.drawArc(oval, startAngle + 270, 90, false, mProgressPaint);

        // 上面的代码当startAngle = 0 时，绘制的是一个静态的透明度交替的圆弧。接着要让它转起来。增加代码：
        startAngle += 10;
        if (startAngle == 180) { startAngle = 0; }
        // 动起来
        invalidate();
    }
}
