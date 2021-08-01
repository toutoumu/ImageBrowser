package com.library.media;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.wordpress.passcodelock.R;
import timber.log.Timber;

public class UiUtils {

    /**
     * 获取 dimens.xml 文件定义的 dp 值对应的px值
     * <dimen name="toolbar_size">44dp</dimen>
     * 在三倍屏中返回 132
     *
     * @param context context
     * @param dimen R.dimen.toolbar_size 44dp
     * @return 三倍屏返回 132
     */
    public static int dimen2px(Context context, @DimenRes int dimen) {
        return context.getResources().getDimensionPixelSize(dimen);
    }

    public static int dp2px(Context context, final float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 获取 dimens.xml 文件定义的 dp 值
     * <dimen name="toolbar_size">44dp</dimen>
     * 输入 R.dimen.toolbar_size 返回44
     *
     * @param context context
     * @param id R.dimen.toolbar_size
     * @return
     */
    public static float dp(Context context, @DimenRes int id) {
        return px2dp(context.getResources().getDimensionPixelSize(id));
    }

    /**
     * EditText 后面带删除文本的按钮
     *
     * @param edit 文本框
     * @param del 删除按钮
     */
    public static void renderEditText(final EditText edit, final View del) {
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.setText("");
            }
        });
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                del.setVisibility(editable.length() > 0 ? View.VISIBLE : View.INVISIBLE);
            }
        });
        edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                del.setVisibility(hasFocus && edit.getText().length() > 0 ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    /**
     * 显示或隐藏密码
     *
     * @param mPassword
     * @param switchPassword
     */
    public static void showOrHide(EditText mPassword, ImageView switchPassword) {
        //记住光标开始的位置
        int pos = mPassword.getSelectionStart();
        if (mPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
            mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            // switchPassword.setImageResource(R.drawable.ic_password);
        } else {
            mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            // switchPassword.setImageResource(R.drawable.ic_password)
        }
        mPassword.setSelection(pos);
    }

    /**
     * 限制输入小数点后两位小数
     */
    public static void setDecimalDiaitsPoint(final EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(".")) {
                    if (s.length() - 1 - s.toString().indexOf(".") > 2) {
                        s = s.toString().subSequence(0, s.toString().indexOf(".") + 3);
                        editText.setText(s);
                        editText.setSelection(s.length());
                    }
                }
                if (s.toString().trim().substring(0).equals(".")) {
                    s = "0" + s;
                    editText.setText(s);
                    editText.setSelection(2);
                }

                if (s.toString().startsWith("0") && s.toString().trim().length() > 1) {
                    if (!s.toString().substring(1, 2).equals(".")) {
                        editText.setText(s.subSequence(0, 1));
                        editText.setSelection(1);
                        return;
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * 设置ToolBar 标题居中
     *
     * @param activity
     * @param toolbar
     * @param titleRes
     * @return
     */
    public static TextView setCenterTitle(Activity activity, Toolbar toolbar, int titleRes) {
        return setCenterTitle(activity, toolbar, activity.getString(titleRes));
    }

    /**
     * 设置ToolBar 标题居中
     *
     * @param activity
     * @param toolbar
     * @param title
     * @return
     */
    public static TextView setCenterTitle(Activity activity, Toolbar toolbar, String title) {
        TextView titleView = toolbar.findViewById(R.id.toolbar_center_title);
        if (titleView == null) {
            titleView = (TextView) activity.getLayoutInflater().inflate(R.layout.toolbar_center_title, toolbar, false);
            titleView.setId(R.id.toolbar_center_title);
            titleView.setSingleLine();
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            Toolbar.LayoutParams lp =
                new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.gravity = Gravity.CENTER;
            toolbar.addView(titleView, lp);
        }
        titleView.setText(title);
        return titleView;
    }

    /**
     * Toolbar设置右侧图标
     *
     * @param activity
     * @param toolbar
     * @param res
     * @return
     */
    public static ImageView setToolbarRightImageView(Activity activity, Toolbar toolbar, @DrawableRes int res) {
        ImageView rightView = toolbar.findViewById(R.id.toolbar_right_image);
        if (rightView == null) {
            rightView = (ImageView) activity.getLayoutInflater().inflate(R.layout.toolbar_right_image, toolbar, false);
            rightView.setId(R.id.toolbar_right_image);
            ((Toolbar.LayoutParams) rightView.getLayoutParams()).gravity = Gravity.CENTER | Gravity.END;
            toolbar.addView(rightView);
        }
        // 右上角图标
        rightView.setImageResource(res);
        return rightView;
    }

    /**
     * Toolbar设置左侧图标
     *
     * @param activity
     * @param toolbar
     * @param res
     * @return
     */
    public static ImageView setToolbarLeftImageView(Activity activity, Toolbar toolbar, @DrawableRes int res) {
        ImageView rightView = toolbar.findViewById(R.id.toolbar_left_image);
        if (rightView == null) {
            rightView = (ImageView) activity.getLayoutInflater().inflate(R.layout.toolbar_left_image, toolbar, false);
            rightView.setId(R.id.toolbar_left_image);
            ((Toolbar.LayoutParams) rightView.getLayoutParams()).gravity = Gravity.CENTER | Gravity.START;
            toolbar.addView(rightView);
        }
        // 右上角图标
        rightView.setImageResource(res);
        return rightView;
    }

    /**
     * Toolbar设置右侧文本
     *
     * @param activity
     * @param toolbar
     * @return
     */
    public static TextView setToolbarRightTextView(Activity activity, Toolbar toolbar, String text) {
        // 右上角图标
        TextView rightView = toolbar.findViewById(R.id.toolbar_right_text);
        if (rightView == null) {
            rightView = (TextView) activity.getLayoutInflater().inflate(R.layout.toolbar_right_text, toolbar, false);
            rightView.setId(R.id.toolbar_right_text);
            ((Toolbar.LayoutParams) rightView.getLayoutParams()).gravity = Gravity.CENTER | Gravity.END;
            toolbar.addView(rightView);
        }
        rightView.setText(text);
        return rightView;
    }

    /**
     * 根据属性ID获取对应的style文件的资源
     *
     * @param context {@link Context}
     * @param attr {@link AttrRes} 属性值eg:{@link R.attr#selectableItemBackground}
     * @return {@link AnyRes} 资源ID eg:{@link R.drawable#abc_list_focused_holo}
     */
    @AnyRes
    public static int getAttribute(@NonNull Context context, @AttrRes int attr) {
        final TypedValue mTypedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, mTypedValue, true);
        return mTypedValue.resourceId;
    }

    /**
     * 限制图片的最大宽高(保持原有比例)
     *
     * @param rW 实际宽度
     * @param rH 实际高度
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param imageView 需要设置宽度限制的图片
     * @return 返回实际设置的尺寸
     */
    public static Point setImageLayout(int rW, int rH, int maxWidth, int maxHeight, View imageView) {
        int videoWidth = rW;//图片缩放后的宽度
        int videoHeight = rH;//图片缩放后的高度
        if (videoWidth == 0) { videoWidth = maxWidth; }
        if (videoHeight == 0) { videoHeight = maxHeight; }
        if (rW > maxWidth || rH > maxHeight) {//只有宽高超过了才执行缩放
            double rR = (double) rW / (double) rH;//实际比例
            double radio = (double) maxWidth / (double) maxHeight;
            if (rR > radio) {//3:1 > 1:1 表示宽度超过限制,设置宽度为最大值,然后计算高度即可
                videoWidth = maxWidth;//设置宽度为最大宽度
                videoHeight = (int) (videoWidth / rR);//计算高度
            } else {//根据高度计算
                videoHeight = maxHeight;
                videoWidth = (int) (videoHeight * rR);
            }
        }
        if (videoWidth < 240) {
            videoWidth = 240;
        }
        if (videoHeight < 180) {
            videoHeight = 180;
        }
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        layoutParams.height = videoHeight;
        layoutParams.width = videoWidth;
        imageView.setLayoutParams(layoutParams);
        return new Point(videoWidth, videoHeight);
    }

    private static void processPrivateAPI(Window window, boolean lightStatusBar) {
        try {
            processFlyMe(window, lightStatusBar);
        } catch (Exception e) {
            try {
                processMIUI(window, lightStatusBar);
            } catch (Exception e2) {
                //
            }
        }
    }

    /**
     * 改变小米的状态栏字体颜色为黑色, 要求MIUI6以上
     * Tested on: MIUIV7 5.0 Redmi-Note3
     *
     * @param window {@link Window}
     * @param lightStatusBar
     * @throws Exception
     */
    private static void processMIUI(Window window, boolean lightStatusBar) throws Exception {
        Class<? extends Window> clazz = window.getClass();
        int darkModeFlag;
        Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
        Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
        darkModeFlag = field.getInt(layoutParams);
        Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
        extraFlagField.invoke(window, lightStatusBar ? darkModeFlag : 0, darkModeFlag);
    }

    /**
     * 改变魅族的状态栏字体为黑色，要求FlyMe4以上
     *
     * @param window
     * @param isLightStatusBar
     * @throws Exception
     */
    private static void processFlyMe(Window window, boolean isLightStatusBar) throws Exception {
        WindowManager.LayoutParams lp = window.getAttributes();
        Class<?> instance = Class.forName("android.view.WindowManager$LayoutParams");
        int value = instance.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON").getInt(lp);
        Field field = instance.getDeclaredField("meizuFlags");
        field.setAccessible(true);
        int origin = field.getInt(lp);
        if (isLightStatusBar) {
            field.set(lp, origin | value);
        } else {
            field.set(lp, (~value) & origin);
        }
    }

    /**
     * @param dialog
     * @param statusBar
     * @param isLight true: 白色背景, 深色文字
     */
    public static void requestStatusBarLightForDialog(Dialog dialog, View statusBar, boolean isLight) {
        Window window = dialog.getWindow();
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isLight) {
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            processPrivateAPI(window, isLight);
            if (isLight) {
                statusBar.setBackgroundColor(0xffffffff);
            } else {
                statusBar.setBackgroundColor(0xffcccccc);
            }
        } else {
            if (isLight) {
                statusBar.setBackgroundColor(0xffcccccc);
            } else {
                statusBar.setBackgroundColor(0xffffffff);
            }
        }
    }

    /**
     * Value of px to value of dp.
     *
     * @param pxValue The value of px.
     * @return value of dp
     */
    public static int px2dp(final float pxValue) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
