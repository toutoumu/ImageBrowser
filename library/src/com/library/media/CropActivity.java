package com.library.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.ColorInt;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import org.wordpress.passcodelock.R;
import timber.log.Timber;

/**
 * 裁剪页面
 */
public class CropActivity extends BaseActivityNoSwipeBack implements UCropFragmentCallback1 {
    private boolean mNeedFinish = true;//页面失去焦点(onPause)后是否需要结束掉该页面
    private UCropFragment1 fragment;
    private boolean mShowLoader = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        // getSwipeBackLayout().setScrimColor(Color.TRANSPARENT);
        // getSwipeBackLayout().setEnableGesture(false);
        setTransparentForWindow();

        setupAppBar();

        fragment = UCropFragment1.newInstance(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.contener, fragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次页面重新获取焦点,都应该标记为失去焦点(onPause)后应该结束掉该页面
        mNeedFinish = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNeedFinish) {
            // 失去焦点结束掉,如果失去焦点的动作是打开新页面触发,则需要在此生命周期之前设置mNeedFinish = false
            setResult(Activity.RESULT_OK);//如果有前一个页面,通知前一个页面finish
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void loadingProgress(boolean showLoader) {
        mShowLoader = showLoader;
        supportInvalidateOptionsMenu();
        // getSwipeBackLayout().setEnableGesture(!showLoader);
        // getSwipeBackLayout().setEnableGesture(false);
    }

    @Override
    public void onCropFinish(UCropFragment1.UCropResult result) {
        this.setResult(result.mResultCode, result.mResultData);//设置返回数据
        this.finish();//关闭Activity
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.ucrop_menu_activity, menu);

        // Change crop & loader menu icons color to match the rest of the UI colors
        MenuItem menuItemLoader = menu.findItem(R.id.menu_loader);
        Drawable menuItemLoaderIcon = menuItemLoader.getIcon();
        if (menuItemLoaderIcon != null) {
            try {
                menuItemLoaderIcon.mutate();
                menuItemLoaderIcon.setColorFilter(getResources().getColor(R.color.title_text_color),
                                                  PorterDuff.Mode.SRC_ATOP);
                menuItemLoader.setIcon(menuItemLoaderIcon);
            } catch (IllegalStateException e) {
                Timber.e(e);
            }
            ((Animatable) menuItemLoader.getIcon()).start();
        }

        MenuItem menuItemCrop = menu.findItem(R.id.menu_crop);
        Drawable menuItemCropIcon = ContextCompat.getDrawable(this, R.drawable.ic_save_white_24dp);
        if (menuItemCropIcon != null) {
            menuItemCropIcon.mutate();
            menuItemCropIcon.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
            menuItemCrop.setIcon(menuItemCropIcon);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_crop).setVisible(!mShowLoader);
        menu.findItem(R.id.menu_loader).setVisible(mShowLoader);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_crop) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VibrationEffect.createOneShot(80, 255));
            fragment.cropAndSaveImage();
        } else if (item.getItemId() == android.R.id.home && !mShowLoader) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Configures and styles both status bar and mToolbar.
     */
    private void setupAppBar() {
        setStatusBarColor(getResources().getColor(R.color.transparent));

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Cancel");
        UiUtils.setCenterTitle(this, toolbar, "裁剪");

        // Color buttons inside the Toolbar
        Drawable stateButtonDrawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp).mutate();
        stateButtonDrawable.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
        toolbar.setNavigationIcon(stateButtonDrawable);

        setSupportActionBar(toolbar);
        // final ActionBar actionBar = getSupportActionBar();
        // if (actionBar != null) {
        //     actionBar.setDisplayShowTitleEnabled(false);
        // }
    }

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(color);
            }
        }
    }
}
