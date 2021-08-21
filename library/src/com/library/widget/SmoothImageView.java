package com.library.widget;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/**
 * 平滑变化的显示图片的ImageView
 */
public class SmoothImageView extends AppCompatImageView {
    private static final int ANIMATION_DURATION = 300;// 动画时长

    // 状态
    public static final int STATE_NORMAL = 0; // 正常
    public static final int STATE_TRANSFORM_IN = 1; // 进入
    public static final int STATE_TRANSFORM_OUT = 2;// 退出
    public static final int STATE_TRANSFORM_MOVE = 3;// 拖动
    public static final int STATE_TRANSFORM_RESTORE = 4;// 恢复

    private int mState = STATE_NORMAL; // 当前状态
    private boolean mTransformStart = false; // 是否正在变幻

    // 缩略图控件的 frame 属性
    private int mOriginalWidth; // 缩略图View宽度
    private int mOriginalHeight; // 缩略图View高度
    private int mOriginalLocationX; // 缩略图偏移X
    private int mOriginalLocationY; // 缩略图偏移Y
    private int mBitmapWidth; // 图片的宽度(如果图片比例不是1:1那么与mOriginalWidth可能不一样)
    private int mBitmapHeight; // 图片的高度(如果图片比例不是1:1那么与mOriginalHeight可能不一样)

    private Paint mPaint; // 画笔
    private Paint mProgressPaint; // 画笔
    private Matrix mSmoothMatrix;
    private Transform mTransformData; // 变幻数据
    private TransformListener mTransformListener;
    private TransformListener mBeforeTransformListener;

    private int alpha = 0; // 背景透明度 0 透明 255 不透明

    private int duration = ANIMATION_DURATION;
    private IIndexChanged mIndexChanged;

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public SmoothImageView(Context context) {
        super(context);
        init();
    }

    public SmoothImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmoothImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Style.FILL);
        mSmoothMatrix = new Matrix();

        mProgressPaint = new Paint();
        mProgressPaint.setColor(Color.DKGRAY);
        mProgressPaint.setStyle(Style.STROKE);
    }

    /**
     * int[] location = new int[2];
     * view.getLocationOnScreen(location);
     * imageView.setOriginTransform(view.getWidth(), view.getHeight(), location[0], location[1]);
     *
     * 注意: 必须在 setOriginalInfo 之前设置图片
     *
     * @param width 缩略图图片宽度
     * @param height 缩略图高度
     * @param locationX 屏幕上的位置x
     * @param locationY 屏幕上的位置y
     * @param offsetX x方向偏移量
     * @param offsetY y方向的偏移量(一般指的是状态栏+ 标题栏)
     */
    public void setOriginalInfo(int width, int height, int locationX, int locationY, int offsetX, int offsetY) {
        // 重置一些数据
        mBitmapWidth = 0;
        mBitmapHeight = 0;
        mTransformData = null;
        mSmoothMatrix = new Matrix();

        mOriginalWidth = width;
        mOriginalHeight = height;
        // 因为是屏幕坐标，所以要转换为该视图内的坐标，因为我所用的该视图是MATCH_PARENT，所以不用定位该视图的位置,如果不是的话，还需要定位视图的位置，然后计算mOriginalLocationX和mOriginalLocationY
        mOriginalLocationX = locationX - offsetX;
        mOriginalLocationY = locationY - offsetY;

        initTransform();
    }

    /**
     * 用于开始进入的方法。 调用此方前，需已经调用过setOriginalInfo
     *
     * @param gridView
     * @param position
     * @param imageId
     * @param offset
     */
    public void transformIn(GridView gridView, int position, @IdRes int imageId, int offset) {
        setupWithGridView(gridView, position, imageId, offset);
        mState = STATE_TRANSFORM_IN;
        if (mBeforeTransformListener != null) {
            mBeforeTransformListener.onTransformComplete(mState);
        }
        mTransformStart = true;
        invalidate();
    }

    /**
     * 用于开始退出的方法。 调用此方前，需已经调用过setOriginalInfo
     */
    public void transformOut() {
        if (mTransformData == null) {
            throw new NullPointerException("请先调用 setOriginalInfo 进行初始化");
        }
        mState = STATE_TRANSFORM_OUT;
        if (mBeforeTransformListener != null) {
            mBeforeTransformListener.onTransformComplete(mState);
        }
        mTransformStart = true;
        invalidate();
    }

    /**
     * 移动
     *
     * @param scale 缩放
     * @param alpha 透明度 0 透明 255 不透明
     * @param transformX x方向偏移
     * @param transformY y方向偏移
     * @param sx 手指点击位置在视图中的位置(中心点 0.5)
     * @param sy 手指点击位置在视图中的位置(中心点 0.5)
     */
    public void transformMove(float scale, int alpha, float transformX, float transformY, float sx, float sy) {
        if (mTransformData == null) {
            throw new NullPointerException("请先调用 setOriginalInfo 进行初始化");
        }
        mState = STATE_TRANSFORM_MOVE; // 更新状态
        mTransformStart = false; // 不执行动画
        this.alpha = alpha; // 更新透明度

        // 计算并缓存此次拖动的,缩放,偏移,尺寸等信息
        float currentScale = scale * mTransformData.endScale; // 图片的实际缩放值(小图会被拉伸到填充屏幕)
        float targetWidth = mBitmapWidth * currentScale;// 本次移动图片最终的宽度
        float targetHeight = mBitmapHeight * currentScale;// 本次移动图片最终的宽度

        mTransformData.scale = currentScale;
        mTransformData.rect.width = targetWidth;
        mTransformData.rect.height = targetHeight;
        mTransformData.rect.left =
            transformX + (getWidth() - targetWidth) / 2.0f + (mTransformData.endRect.width - targetWidth) * (sx - 0.5f);
        mTransformData.rect.top =
            transformY + (getHeight() - targetHeight) / 2.0f + (mTransformData.endRect.height - targetHeight) * (sy - 0.5f);

        invalidate();
    }

    /**
     * 移动后还原
     */
    public void transformRestore() {
        if (mTransformData == null) {
            throw new NullPointerException("请先调用 setOriginalInfo 进行初始化");
        }
        mState = STATE_TRANSFORM_RESTORE;
        if (mBeforeTransformListener != null) {
            mBeforeTransformListener.onTransformComplete(mState);
        }
        mTransformStart = true;
        startTransform(STATE_TRANSFORM_RESTORE);
    }

    /**
     * 初始化动画所需的信息,进入时的 位置尺寸, 放大后的尺寸
     */
    private void initTransform() {
        if (getWidth() == 0 || getHeight() == 0) { return; }

        if (mBitmapWidth == 0 || mBitmapHeight == 0) {
            Drawable drawable = getDrawable();
            if (drawable == null) {
                mBitmapWidth = 200;
                mBitmapHeight = 200;
            } else if (drawable instanceof TransitionDrawable) {// 貌似是Glide加载的图片就这样
                mBitmapWidth = ((TransitionDrawable) drawable).getCurrent().getIntrinsicWidth();
                mBitmapHeight = ((TransitionDrawable) drawable).getCurrent().getIntrinsicHeight();
            } else if (drawable instanceof BitmapDrawable) {
                Bitmap mBitmap = ((BitmapDrawable) drawable).getBitmap();
                mBitmapWidth = mBitmap.getWidth();
                mBitmapHeight = mBitmap.getHeight();
            } else if (drawable instanceof ColorDrawable) {
                ColorDrawable colorDrawable = (ColorDrawable) drawable;
                mBitmapWidth = colorDrawable.getIntrinsicWidth();
                mBitmapHeight = colorDrawable.getIntrinsicHeight();
            } /*else if (drawable instanceof GlidePlaceholderDrawable) { // placeholder 为自定义的
                mBitmapWidth = drawable.getMinimumWidth();
                mBitmapHeight = drawable.getMinimumHeight();
            }*/ else if (drawable instanceof NinePatchDrawable) {// placeholder 为 .9 图
                mBitmapWidth = drawable.getIntrinsicWidth();
                mBitmapHeight = drawable.getIntrinsicHeight();
            } else {
                mBitmapWidth = drawable.getIntrinsicWidth();
                mBitmapHeight = drawable.getIntrinsicHeight();
            }
            if (mBitmapWidth <= 0) {
                mBitmapWidth = 200;
            }
            if (mBitmapHeight <= 0) {
                mBitmapHeight = 200;
            }
        }

        // 防止mTransform重复的做同样的初始化
        if (mTransformData != null) { return; }
        mTransformData = new Transform();

        /* 下面为缩放的计算 */
        /* 计算初始的缩放值，初始值因为是 CENTER_CROP 效果，所以要保证图片的宽和高至少1个能匹配原始的宽和高，另1个大于 */
        float xSScale = mOriginalWidth / ((float) mBitmapWidth);
        float ySScale = mOriginalHeight / ((float) mBitmapHeight);
        mTransformData.startScale = Math.max(xSScale, ySScale);
        /* 计算结束时候的缩放值，结束值因为要达到 FIT_CENTER 效果，所以要保证图片的宽和高至少1个能匹配原始的宽和高，另1个小于 */
        float xEScale = getWidth() / ((float) mBitmapWidth);
        float yEScale = getHeight() / ((float) mBitmapHeight);
        mTransformData.endScale = Math.min(xEScale, yEScale);

        /*
         * 下面计算 Canvas Clip 的范围，也就是图片的显示的范围，因为图片是慢慢变大，并且是等比例的，所以这个效果还需要裁减图片显示的区域
         * ，而显示区域的变化范围是在原始 CENTER_CROP 效果的范围区域
         * ，到最终的 FIT_CENTER 的范围之间的，区域我用 LocationSizeF 更好计算
         * ，他就包括左上顶点坐标，和宽高，最后转为 Canvas 裁剪的 Rect.
         */
        /* 开始区域 */
        mTransformData.startRect = new LocationSizeF();
        mTransformData.startRect.left = mOriginalLocationX;
        mTransformData.startRect.top = mOriginalLocationY;
        mTransformData.startRect.width = mOriginalWidth;
        mTransformData.startRect.height = mOriginalHeight;
        /* 结束区域 */
        mTransformData.endRect = new LocationSizeF();
        float bitmapEndWidth = mBitmapWidth * mTransformData.endScale;// 图片最终的宽度
        float bitmapEndHeight = mBitmapHeight * mTransformData.endScale;// 图片最终的宽度
        mTransformData.endRect.left = (getWidth() - bitmapEndWidth) / 2;
        mTransformData.endRect.top = (getHeight() - bitmapEndHeight) / 2;
        mTransformData.endRect.width = bitmapEndWidth;
        mTransformData.endRect.height = bitmapEndHeight;

        mTransformData.rect = new LocationSizeF();
    }

    public void setState(int state) {
        mState = state;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        // todo 这里图片已经加载了,根据 Drawable 不同来处理页面的变幻数据
        // if (drawable instanceof BitmapDrawable) {
        mBitmapWidth = 0;
        mBitmapHeight = 0;
        mTransformData = null;
        mSmoothMatrix = new Matrix();
        this.initTransform();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) {
            drawLoading(canvas);
            return;
        } // couldn't resolve the URI
        if (mState == STATE_TRANSFORM_MOVE) { // 移动
            if (mTransformData == null) {
                super.onDraw(canvas);
                return;
            }

            mPaint.setAlpha(alpha);
            canvas.drawPaint(mPaint);

            int saveCount = canvas.getSaveCount();
            canvas.save();
            getBmpMatrix(); // 先得到图片在此刻的图像Matrix矩阵

            canvas.translate(mTransformData.rect.left, mTransformData.rect.top);
            canvas.clipRect(0, 0, mTransformData.rect.width, mTransformData.rect.height);
            canvas.concat(mSmoothMatrix);
            getDrawable().draw(canvas);
            canvas.restoreToCount(saveCount);
        } else if (mState == STATE_TRANSFORM_IN || mState == STATE_TRANSFORM_OUT || mState == STATE_TRANSFORM_RESTORE) {
            if (mTransformStart) {
                initTransform();
            }

            if (mTransformData == null) {
                super.onDraw(canvas);
                return;
            }

            if (mTransformStart) {
                if (mState == STATE_TRANSFORM_IN) {
                    Timber.d("STATE_TRANSFORM_IN");
                    mTransformData.initStartIn();
                } else if (mState == STATE_TRANSFORM_OUT) {
                    Timber.d("STATE_TRANSFORM_OUT");
                    // mTransform.initStartOut();
                }
            }

            mPaint.setAlpha(alpha);
            canvas.drawPaint(mPaint);

            int saveCount = canvas.getSaveCount();
            canvas.save();
            getBmpMatrix(); // 先得到图片在此刻的图像Matrix矩阵

            canvas.translate(mTransformData.rect.left, mTransformData.rect.top);
            canvas.clipRect(0, 0, mTransformData.rect.width, mTransformData.rect.height);
            canvas.concat(mSmoothMatrix);
            getDrawable().draw(canvas);
            canvas.restoreToCount(saveCount);

            if (mTransformStart) {
                mTransformStart = false;
                startTransform(mState);
            }
        } else {
            //当Transform In变化完成后，把背景改为黑色，使得Activity不透明
            mPaint.setAlpha(255);
            canvas.drawPaint(mPaint);
            super.onDraw(canvas);
        }
        drawLoading(canvas);
    }

    public void setLoading(boolean isLoading) {
        this.isLoading = isLoading;
        invalidate();
    }

    private boolean isLoading = false;
    int startAngle = 0;
    float ringWidth = 50.0f;

    private void drawLoading(Canvas canvas) {
        if (!isLoading) {
            return;
        }

        // 圆环尺寸
        float size = 100;// Math.min(rect.width, rect.height);

        RectF oval;
        if (mTransformData != null) {
            LocationSizeF rect = mTransformData.rect;
            float left = rect.left - (size - rect.width) / 2.0f;
            float top = rect.top - (size - rect.height) / 2.0f;
            float right = left + size;
            float bottom = top + size;
            oval = new RectF(left, top, right, bottom);
        } else {
            float left = (getMeasuredWidth() - size) / 2.0f;
            float top = (getMeasuredHeight() - size) / 2.0f;
            float right = left + size;
            float bottom = top + size;
            oval = new RectF(left, top, right, bottom);
        }

        // 为了绘制出透明度不同的圆环分两部来绘制：

        this.mProgressPaint.setARGB(200, 127, 255, 212);
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

    private void getBmpMatrix() {
        /* 下面实现了CENTER_CROP的功能 */
        mSmoothMatrix.setScale(mTransformData.scale, mTransformData.scale);
        mSmoothMatrix.postTranslate(-(mTransformData.scale * mBitmapWidth / 2 - mTransformData.rect.width / 2),
                                    -(mTransformData.scale * mBitmapHeight / 2 - mTransformData.rect.height / 2));
    }

    /**
     * 执行动画
     *
     * @param state .
     */
    private void startTransform(final int state) {
        if (mTransformData == null) { return; }
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setDuration(duration);
        // valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator()); // 加速减速
        // valueAnimator.setInterpolator(new DecelerateInterpolator()); // 减速
        if (state == STATE_TRANSFORM_IN) {// 显示
            PropertyValuesHolder scaleHolder =
                PropertyValuesHolder.ofFloat("scale", mTransformData.startScale, mTransformData.endScale);
            PropertyValuesHolder leftHolder =
                PropertyValuesHolder.ofFloat("left", mTransformData.startRect.left, mTransformData.endRect.left);
            PropertyValuesHolder topHolder =
                PropertyValuesHolder.ofFloat("top", mTransformData.startRect.top, mTransformData.endRect.top);
            PropertyValuesHolder widthHolder =
                PropertyValuesHolder.ofFloat("width", mTransformData.startRect.width, mTransformData.endRect.width);
            PropertyValuesHolder heightHolder =
                PropertyValuesHolder.ofFloat("height", mTransformData.startRect.height, mTransformData.endRect.height);
            PropertyValuesHolder alphaHolder = PropertyValuesHolder.ofInt("alpha", 0, 255);
            valueAnimator.setValues(scaleHolder, leftHolder, topHolder, widthHolder, heightHolder, alphaHolder);
        } else if (state == STATE_TRANSFORM_OUT) {// 退出
            if (mTransformData.scale == 0) {
                PropertyValuesHolder scaleHolder =
                    PropertyValuesHolder.ofFloat("scale", mTransformData.endScale, mTransformData.startScale);
                PropertyValuesHolder leftHolder =
                    PropertyValuesHolder.ofFloat("left", mTransformData.endRect.left, mTransformData.startRect.left);
                PropertyValuesHolder topHolder =
                    PropertyValuesHolder.ofFloat("top", mTransformData.endRect.top, mTransformData.startRect.top);
                PropertyValuesHolder widthHolder =
                    PropertyValuesHolder.ofFloat("width", mTransformData.endRect.width, mTransformData.startRect.width);
                PropertyValuesHolder heightHolder =
                    PropertyValuesHolder.ofFloat("height", mTransformData.endRect.height, mTransformData.startRect.height);
                PropertyValuesHolder alphaHolder = PropertyValuesHolder.ofInt("alpha", mPaint.getAlpha(), 0);
                valueAnimator.setValues(scaleHolder, leftHolder, topHolder, widthHolder, heightHolder, alphaHolder);
            } else {
                PropertyValuesHolder scaleHolder =
                    PropertyValuesHolder.ofFloat("scale", mTransformData.scale, mTransformData.startScale);
                PropertyValuesHolder leftHolder =
                    PropertyValuesHolder.ofFloat("left", mTransformData.rect.left, mTransformData.startRect.left);
                PropertyValuesHolder topHolder =
                    PropertyValuesHolder.ofFloat("top", mTransformData.rect.top, mTransformData.startRect.top);
                PropertyValuesHolder widthHolder =
                    PropertyValuesHolder.ofFloat("width", mTransformData.rect.width, mTransformData.startRect.width);
                PropertyValuesHolder heightHolder =
                    PropertyValuesHolder.ofFloat("height", mTransformData.rect.height, mTransformData.startRect.height);
                PropertyValuesHolder alphaHolder = PropertyValuesHolder.ofInt("alpha", mPaint.getAlpha(), 0);
                valueAnimator.setValues(scaleHolder, leftHolder, topHolder, widthHolder, heightHolder, alphaHolder);
            }
        } else if (state == STATE_TRANSFORM_RESTORE) {// 复位
            PropertyValuesHolder scaleHolder =
                PropertyValuesHolder.ofFloat("scale", mTransformData.scale, mTransformData.endScale);
            PropertyValuesHolder leftHolder =
                PropertyValuesHolder.ofFloat("left", mTransformData.rect.left, mTransformData.endRect.left);
            PropertyValuesHolder topHolder =
                PropertyValuesHolder.ofFloat("top", mTransformData.rect.top, mTransformData.endRect.top);
            PropertyValuesHolder widthHolder =
                PropertyValuesHolder.ofFloat("width", mTransformData.rect.width, mTransformData.endRect.width);
            PropertyValuesHolder heightHolder =
                PropertyValuesHolder.ofFloat("height", mTransformData.rect.height, mTransformData.endRect.height);
            PropertyValuesHolder alphaHolder = PropertyValuesHolder.ofInt("alpha", mPaint.getAlpha(), 255);
            valueAnimator.setValues(scaleHolder, leftHolder, topHolder, widthHolder, heightHolder, alphaHolder);
        } else {
            return;
        }

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public synchronized void onAnimationUpdate(ValueAnimator animation) {
                mTransformData.scale = (Float) animation.getAnimatedValue("scale");
                mTransformData.rect.left = (Float) animation.getAnimatedValue("left");
                mTransformData.rect.top = (Float) animation.getAnimatedValue("top");
                mTransformData.rect.width = (Float) animation.getAnimatedValue("width");
                mTransformData.rect.height = (Float) animation.getAnimatedValue("height");
                alpha = (Integer) animation.getAnimatedValue("alpha");
                invalidate(); // 重新绘制
            }
        });
        valueAnimator.addListener(new ValueAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mTransformListener != null) {
                    mTransformListener.onTransformComplete(state);
                }
                duration = ANIMATION_DURATION;
                /*
                 * 如果是进入的话，当然是希望最后停留在center_crop的区域。但是如果是out的话，就不应该是center_crop的位置了
                 * ， 而应该是最后变化的位置，因为当out的时候结束时，不恢复视图是Normal，要不然会有一个突然闪动回去的bug
                 */
                // TODO 这个可以根据实际需求来修改
                if (state == STATE_TRANSFORM_IN) {
                    mState = STATE_NORMAL;
                }
                if (state == STATE_TRANSFORM_MOVE) {
                    mState = STATE_NORMAL;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) { }
        });
        valueAnimator.start();
    }

    /**
     * 设置动画完成监听
     *
     * @param listener .
     */
    public void setOnTransformListener(TransformListener listener) {
        mTransformListener = listener;
    }

    /**
     * 设置动画完成监听
     *
     * @param listener .
     */
    public void setOnBeforeTransformListener(TransformListener listener) {
        mBeforeTransformListener = listener;
    }

    public TransformListener getBeforeTransformListener() {
        return mBeforeTransformListener;
    }

    public void setIndexChanged(IIndexChanged indexChanged) {
        mIndexChanged = indexChanged;
    }

    public void setCurrentIndex(GridView gridView, int pageIndex, int imageId, int offset) {
        setupWithGridView(gridView, pageIndex, imageId, 0);
        if (mIndexChanged != null) {
            mIndexChanged.onIndexChanged(this, pageIndex);
        }
    }

    /**
     * 变幻
     */
    public static class Transform {
        float startScale;// 图片开始的缩放值
        float endScale;// 图片结束的缩放值
        float scale;// 属性ValueAnimator计算出来的值 (当前值)

        LocationSizeF startRect;// 开始的区域
        LocationSizeF endRect;// 结束的区域
        LocationSizeF rect;// 属性ValueAnimator计算出来的值 (当前值)

        void initStartIn() {
            scale = startScale;
            try {
                rect = (LocationSizeF) startRect.clone();
            } catch (CloneNotSupportedException e) {
                Timber.e(e);
            }
        }

        void initStartOut() {
            scale = endScale;
            try {
                rect = (LocationSizeF) endRect.clone();
            } catch (CloneNotSupportedException e) {
                Timber.e(e);
            }
        }
    }

    /**
     * 位置
     */
    public static class LocationSizeF implements Cloneable {
        float left;
        float top;
        float width;
        float height;

        @Override
        public @NotNull String toString() {
            return "[left:" + left + " top:" + top + " width:" + width + " height:" + height + "]";
        }

        @Override
        public @NotNull Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * 动画完成监听接口
     */
    public interface TransformListener {
        /**
         * @param mode STATE_TRANSFORM_IN 1 ,STATE_TRANSFORM_OUT 2
         */
        void onTransformComplete(int mode);// mode 1
    }

    /**
     * 计算图片边界
     *
     * @param gridView {@link GridView}
     * @param position 当前显示的图片的索引
     * @param imageViewId 图片对应的id
     * @param offset Y方向偏移量
     */
    private void setupWithGridView(GridView gridView, int position, @IdRes int imageViewId, int offset) {
        int firstVisiblePosition = gridView.getFirstVisiblePosition();
        View itemView = gridView.getChildAt(position - firstVisiblePosition);
        Rect bounds = new Rect();
        if (itemView != null) {
            ImageView thumbView = itemView.findViewById(imageViewId);
            thumbView.getGlobalVisibleRect(bounds);
            if (offset != 0) {
                bounds.set(bounds.left, bounds.top + offset, bounds.right, bounds.bottom + offset);
            }
            setImageDrawable(thumbView.getDrawable()); // 必须在 setOriginalInfo 之前设置图片
            setOriginalInfo(bounds.width(), bounds.height(), bounds.left, bounds.top, 0, 0);
        }
    }
}
