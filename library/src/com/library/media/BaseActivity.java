package com.library.media;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.trello.rxlifecycle3.components.support.RxAppCompatActivity;

public class BaseActivity extends RxAppCompatActivity {
    private SystemBarTintManager mTintManager;
    private View mParentView;

    /**
     * 这里不会隐藏底部导航栏
     *
     * 设置内容显示到状态栏下层,并使状态栏透明
     * {@link Build.VERSION_CODES#KITKAT}以上系统调用此方法,可以是状态栏透明,
     * 并使得Activity内容显示在状态栏下层,内容被状态栏覆盖
     */
    public void setTransparentForWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                                       | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住。
            // 如果需要隐藏底部导航栏加上这个 View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            getWindow().getDecorView()
                .setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility()
                                           | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                           | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN /*|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION*/);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            // getWindow().setNavigationBarColor(Color.TRANSPARENT);

            // todo 白色状态栏图标用这个 并在 setContentView 之前调用 setStatusBarTransparent 方法
            // UiUtils.requestStatusBarLight(this, true);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住。
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 清除内容显示到状态栏下层,清除使状态栏透明
     */
    public void clearTransparentForWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            // 如果需要隐藏底部导航栏加上这个 View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            getWindow().getDecorView()
                .setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility()
                                           & ~(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN/*|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION*/));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住。
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
}
