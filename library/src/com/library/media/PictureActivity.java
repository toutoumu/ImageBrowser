package com.library.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.blankj.utilcode.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.kongzue.dialog.v3.BottomMenu;
import com.kongzue.dialog.v3.MessageDialog;
import com.kongzue.dialog.v3.TipDialog;
import com.kongzue.dialog.v3.WaitDialog;
import com.library.BitmapUtils;
import com.library.glide.ConcealUtil;
import com.library.glide.FileUtils;
import com.library.glide.GlideApp;
import com.library.glide.GlideRequests;
import com.library.widget.SmoothImageView;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.trello.rxlifecycle3.android.ActivityEvent;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;
import com.yalantis.ucrop.model.AspectRatio;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.wordpress.passcodelock.R;
import org.wordpress.passcodelock.databinding.ActivityPictureBinding;
import timber.log.Timber;

import static com.library.glide.ConcealUtil.STAFF;

/**
 * 图片列表,和图片展示页面的容器
 */
public class PictureActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener {
    /** 相册路径 /data/mAlbumName */
    public static final String ALBUM_PATH = "mPath";
    /** 相册名称 mAlbumName */
    public static final String ALBUM_NAME = "name";

    /** 单张图片移动选择相册 */
    private static final int REQUEST_CODE_SELECT_ALBUM = 34;
    /** 列表图片移动选择相册 */
    private static final int REQUEST_CODE_SELECT_ALBUM_LIST = 33;
    /** 动画时长 */
    private static final int ANIMATION_DURATION = 300;

    private String mAlbumPath; // 相册路径
    private String mAlbumName; // 相册名称
    private final List<Picture> mPictures = new ArrayList<>(); // 图片列表

    public boolean mNeedFinish = true; // 页面失去焦点(onPause)后是否需要结束掉该页面

    private boolean isSelectMode = false;// 列表是否为选择模式
    private int selectedCount = 0; // 列表选择图片数量

    private PictureAdapter2 mPictureAdapter;
    private PictureListAdapter mPictureListAdapter;

    private int viewType = 0; // 0: 网格 1: 图片浏览
    private int mPageIndex = 0; // 当前页索引

    private File mSourceFile;//需要剪切,处理的图片(已经解密)
    private File mDestinationFile;//剪切后的图片(uCrop裁剪后的文件)

    private TipDialog mProgressDialog;
    private SystemBarTintManager tintManager;
    public ActivityPictureBinding bind;

    RequestOptions mThumbnailRequestOptions;
    RequestManager mGlideRequests;

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {//返回按钮
            onBackPressed();
        } else if (itemId == R.id.action_all) {
            BottomMenu.show(
                this, new String[] { "全选", "全不选", "选择模式", "浏览模式", "解密选中项", "移动选中项到", "处理选择项", "删除选中项" }, (text, index) -> {
                    switch (text) {
                        case "全选": {
                            this.changeSelectMode(true, true);
                            break;
                        }
                        case "全不选": {
                            this.changeSelectMode(isSelectMode, false);
                            break;
                        }
                        case "浏览模式": {
                            this.changeSelectMode(false, false);
                            break;
                        }
                        case "选择模式": {
                            this.changeSelectMode(true, false);
                            break;
                        }
                        case "解密选中项": {
                            doListExport();
                            break;
                        }
                        case "移动选中项到": {
                            showListMove();
                            break;
                        }
                        case "处理选择项": {
                            handlerSelected();
                            break;
                        }
                        case "删除选中项": {
                            doListDelete();
                            break;
                        }
                    }
                }).setTitle("选择操作");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tintManager = new SystemBarTintManager(this);

        bind = ActivityPictureBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());

        setTransparentForWindow();

        // 必须在这里初始化
        ImmersionBar.with(this).reset()
            .fullScreen(false)
            .hideBar(BarHide.FLAG_SHOW_BAR)
            // .navigationBarEnable(false)
            .init();

        getSwipeBackLayout().setEnableGesture(true);
        getSwipeBackLayout().setScrimColor(Color.TRANSPARENT);

        // 获取参数加载数据
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAlbumPath = extras.getString(ALBUM_PATH, ConcealUtil.PICTURE_DIR.getAbsolutePath());
            mAlbumName = extras.getString(ALBUM_NAME, ConcealUtil.PICTURE_DIR.getName());
        }

        mGlideRequests = Glide.with(this);
        mThumbnailRequestOptions = RequestOptions
            .fitCenterTransform()
            .override(500, 900)
            .error(R.drawable.ic_default_image_list)
            .placeholder(R.drawable.transparent_drawable)
            .dontAnimate()
            .dontTransform()
            .encodeQuality(80);

        // 加载文件
        loadData(mAlbumPath);

        // 初始化标题栏
        initToolbar();

        // 初始化图片网格
        initGridView();

        // 初始化图片浏览
        initViewPage();

        // 初始化点击事件
        initListeners();

        renderView();
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
    public void onBackPressed() {
        if (mPictures.size() == 0) { // 无图片直接退出
            super.onBackPressed();
            return;
        }
        if (isSelectMode) {// 切换到浏览模式
            changeSelectMode(false, false);
            return;
        }
        if (viewType == 1) {// 切换到网格视图
            bind.smoothImageView.transformOut();
            return;
        }

        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SELECT_ALBUM_LIST) {//选择相册回调
            if (data != null && data.getExtras() != null) {//正常选择相册返回是有数据的因此不需要结束掉页面
                String dir = data.getExtras().getString(AlbumSelectActivity.ALBUM_NAME);//相册名称
                String path = data.getExtras().getString(AlbumSelectActivity.ALBUM_PATH);//相册路径
                doListMove(path, dir);
            } else {// 选择相册页面退到后台后也会出发回调,但是回调是没有数据的
                setResult(Activity.RESULT_OK);
                finish();
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SELECT_ALBUM) {//选择相册回调
            if (data != null && data.getExtras() != null) {//正常选择相册返回是有数据的因此不需要结束掉页面
                String dir = data.getExtras().getString(AlbumSelectActivity.ALBUM_NAME);//相册名称
                String path = data.getExtras().getString(AlbumSelectActivity.ALBUM_PATH);//相册路径
                doMove(path, dir);
            } else {// 选择相册页面退到后台后也会出发回调,但是回调是没有数据的
                this.setResult(Activity.RESULT_OK);
                this.finish();
            }
        } else if (resultCode == Activity.RESULT_OK && data == null) {
            this.setResult(Activity.RESULT_OK);
            this.finish();
        } else if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            //final Uri resultUri = UCrop.getOutput(data);
            // 裁剪成功后的回调方法,删除临时文件,保存剪切后的文件
            MessageDialog.show(this, "提示", "是否保存图片?", "确定", "取消")
                .setCancelable(false)
                .setOnOkButtonClickListener((baseDialog, v) -> {
                    saveCropResult();
                    return false;
                })
                .setOnCancelButtonClickListener((baseDialog, v) -> {
                    //如果不保存删除剪切后的图片
                    mSourceFile.delete();
                    mDestinationFile.delete();
                    return false;
                });
        } else if (resultCode == UCrop.RESULT_ERROR) {//剪切失败
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Timber.e(cropError);
                mSourceFile.delete();
                mDestinationFile.delete();
                TipDialog.show(this, "裁剪文件失败!", TipDialog.TYPE.ERROR);
            }
        }
    }

    /**
     * 初始化点击事件
     */
    private void initListeners() {
        // 动画开始之前的回调
        bind.smoothImageView.setOnBeforeTransformListener(mode -> {
            gridClickable = false;
            bind.pager.setVisibility(View.INVISIBLE);
            bind.smoothImageView.setVisibility(View.VISIBLE);
            switch (mode) {
                case SmoothImageView.STATE_TRANSFORM_IN: { // 显示图片浏览器
                    Timber.e("------------显示图片浏览器之前------------");
                    viewType = 1; // 0: 网格 1: 图片浏览
                    toolbarInAnimation();
                    footerInAnimation();
                    getSwipeBackLayout().setEnableGesture(false);
                    renderView();
                    break;
                }
                case SmoothImageView.STATE_TRANSFORM_OUT: { // 显示网格列表
                    Timber.e("------------显示网格列表之前------------");
                    viewType = 0; // 0: 网格 1: 图片浏览
                    toolbarInAnimation();
                    footerOutAnimation();
                    getSwipeBackLayout().setEnableGesture(true);
                    renderView();
                    break;
                }
                case SmoothImageView.STATE_TRANSFORM_RESTORE: { // 恢复到图片浏览器
                    Timber.e("------------恢复到图片浏览器之前------------");
                    viewType = 1; // 0: 网格 1: 图片浏览
                    toolbarInAnimation();
                    footerInAnimation();
                    getSwipeBackLayout().setEnableGesture(false);
                    renderView();
                    break;
                }
                case SmoothImageView.STATE_TRANSFORM_MOVE: {
                    Timber.e("------------开始拖动之前------------");
                    toolBarOutAnimation();
                    footerOutAnimation();
                    break;
                }
            }
        });

        // 动画结束之后的回调
        bind.smoothImageView.setOnTransformListener(mode -> {
            gridClickable = true;
            bind.smoothImageView.setVisibility(View.INVISIBLE);
            switch (mode) {
                case SmoothImageView.STATE_TRANSFORM_IN: { // 显示图片浏览器
                    Timber.e("------------显示图片浏览器------------");
                    bind.pager.setVisibility(View.VISIBLE);
                    break;
                }
                case SmoothImageView.STATE_TRANSFORM_OUT: { // 显示网格列表
                    Timber.e("------------显示网格列表------------");
                    bind.pager.setVisibility(View.INVISIBLE);
                    break;
                }
                case SmoothImageView.STATE_TRANSFORM_RESTORE: { // 恢复到原来昨天
                    Timber.e("------------恢复到图片浏览器------------");
                    bind.pager.setVisibility(View.VISIBLE);
                }
            }
        });

        // 左下角返回相册
        bind.showAlbum.setOnClickListener(view12 -> {
            if (viewType == 1) {
                this.onBackPressed();
            }
        });

        // 下一张
        bind.next.setOnClickListener(v -> {
            if (mPageIndex < mPictures.size() - 1) {//如果不是最后一项
                mPageIndex = mPageIndex + 1;
                bind.pager.setCurrentItem(mPageIndex);
                renderView();
            } else {
                TipDialog.show(this, "已经是最后一张了!", TipDialog.TYPE.WARNING).setCancelable(true);
            }
        });

        // 上一张
        bind.preview.setOnClickListener(v -> {
            if (mPageIndex > 0) {//如果不是第一项
                mPageIndex = mPageIndex - 1;
                bind.pager.setCurrentItem(mPageIndex);
                renderView();
            } else {
                TipDialog.show(this, "已经是第一张了!", TipDialog.TYPE.WARNING).setCancelable(true);
            }
        });

        // 右下角操作
        bind.showAction.setOnClickListener(view1 -> showAction());
    }

    /**
     * 初始化图片浏览
     */
    private void initViewPage() {
        mPictureAdapter = new PictureAdapter2(this, mPictures);
        bind.pager.setAdapter(mPictureAdapter);
        bind.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Timber.e("onPageSelected position: %s", position);
                if (position != 0 && position > mPictures.size() - 1) {
                    Timber.e("position 超出了 mPictrues.size");
                    return;
                }
                mPageIndex = position;
                bind.gridView.smoothScrollToPosition(mPageIndex);
                renderView();
                updateTransferInfo();
            }
        });
        bind.pager.setCurrentItem(mPageIndex, false);
    }

    /**
     * 更新拖拽图片的动画参数,并加载对应位置的图片
     */
    private void updateTransferInfo() {
        Rect rect = computeBounds(bind.gridView, mPageIndex, R.id.image, 0);
        bind.smoothImageView.setOriginalInfo(rect.width(), rect.height(), rect.left, rect.top, 0, 0);

        if (mPictures.size() > 0) {
            Picture picture = mPictures.get(mPageIndex);
            if (picture != null) {
                // 加载数据
                bind.smoothImageView.setLoading(true);
                mGlideRequests.load(picture.getFilePath())
                    .apply(mThumbnailRequestOptions)
                    .error(R.drawable.ic_default_image_list)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e,
                                                    Object model,
                                                    Target<Drawable> target,
                                                    boolean isFirstResource) {
                            bind.smoothImageView.setLoading(false);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource,
                                                       Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            bind.smoothImageView.setLoading(false);
                            return false;
                        }
                    })
                    // .transition(DrawableTransitionOptions.withCrossFade())
                    .into(bind.smoothImageView);
            }
        }
    }

    private boolean gridClickable = true;

    /**
     * 初始化图片网格
     */
    private void initGridView() {
        mPictureListAdapter = new PictureListAdapter(this, mPictures, isSelectMode);
        bind.gridView.setAdapter(mPictureListAdapter);
        bind.gridView.setOnItemClickListener((parent, view, position, id) -> {
            // 选择模式
            if (isSelectMode) {
                Picture picture = mPictures.get(position);
                if (picture != null) {
                    picture.setSelected(!picture.isSelected());
                    View mask = view.findViewById(R.id.mask);
                    ImageView indicator = view.findViewById(R.id.checkmark);

                    if (picture.isSelected()) {//选中
                        selectedCount++;
                        mask.setVisibility(View.VISIBLE);
                        indicator.setVisibility(View.VISIBLE);
                        indicator.setImageResource(R.drawable.ic_checkbox_red_checked);
                    } else {//未选中
                        selectedCount--;
                        mask.setVisibility(View.GONE);
                        indicator.setVisibility(View.VISIBLE);
                        indicator.setImageResource(R.drawable.ic_checkbox_white_unchecked);
                    }
                    renderView();
                }
                return;
            }

            // 如果GridView不可点击
            if (!gridClickable) {
                return;
            }

            bind.pager.setCurrentItem(position, false);

            // 打开图片浏览
            int firstVisiblePosition = bind.gridView.getFirstVisiblePosition();
            View itemView = bind.gridView.getChildAt(position - firstVisiblePosition);
            ImageView img = itemView.findViewById(R.id.image);

            Rect rect = computeBounds(bind.gridView, position, R.id.image, 0);
            bind.smoothImageView.setImageDrawable(img.getDrawable()); // 必须在 setOriginalInfo 之前设置图片
            bind.smoothImageView.setOriginalInfo(rect.width(), rect.height(), rect.left, rect.top, 0, 0);
            bind.smoothImageView.transformIn();
        });

        bind.gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            changeSelectMode(!isSelectMode, false);
            return true;
        });

        bind.gridView.setPadding(0,
                                 tintManager.getConfig().getStatusBarHeight() + tintManager.getConfig().getActionBarHeight(),
                                 0,
                                 tintManager.getConfig().getNavigationBarHeight());
    }

    /**
     * 初始化标题栏
     */
    private void initToolbar() {
        // 初始化标题栏
        bind.toolbar.setTitle(mAlbumName);
        // bind.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        bind.toolbar.inflateMenu(R.menu.options);
        bind.toolbar.setOnMenuItemClickListener(this);
        bind.toolbar.setNavigationOnClickListener(v -> this.onBackPressed());

        // 设置返回按钮
        Drawable stateButtonDrawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp).mutate();
        stateButtonDrawable.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
        bind.toolbar.setNavigationIcon(stateButtonDrawable);

        // 设置菜单按钮颜色
        MenuItem menuItem = bind.toolbar.getMenu().findItem(R.id.action_all);
        Drawable menuIcon = menuItem.getIcon();
        if (menuIcon != null) {
            try {
                menuIcon.mutate();
                menuIcon.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
                menuItem.setIcon(menuIcon);
            } catch (IllegalStateException e) {
                Timber.e(e);
            }
        }

        // 设置标题栏的Margin属性,给状态栏留出空间
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bind.toolbar.getLayoutParams();
        params.setMargins(params.leftMargin,
                          tintManager.getConfig().getStatusBarHeight(),
                          params.rightMargin,
                          params.bottomMargin);
    }

    /**
     * 显示列表操作
     */
    private void showAction() {
        BottomMenu.show(this, new String[] { "编辑", "复制", "解密", "移动", "删除" }, (text, index) -> {
            switch (text) {
                case "复制": {
                    doCopy();
                    break;
                }
                case "编辑": {
                    // crop();
                    cropHeif();
                    break;
                }
                case "解密": {
                    export();
                    break;
                }
                case "移动": {
                    move();
                    break;
                }
                case "删除": {
                    delete();
                    break;
                }
            }
        }).setTitle("选择操作");
    }

    /**
     * 设置是否为选择模式
     *
     * @param selectMode bool 是否选择模式
     * @param selectAll bool 是否全选,或者全不选 ture:全选 false:全不选
     */
    public void changeSelectMode(boolean selectMode, boolean selectAll) {
        this.isSelectMode = selectMode;

        if (selectMode) { // 当前为选择模式, 那么是否全选根据 selectAll 设置
            selectedCount = selectAll ? mPictures.size() : 0;
            for (Picture mPicture : mPictures) {
                mPicture.setSelected(selectAll);
            }
        } else { // 当前为非选择模式, 那么设置为全不选
            selectedCount = 0;
            for (Picture mPicture : mPictures) {
                mPicture.setSelected(false);
            }
        }

        if (mPictureListAdapter != null) {
            mPictureListAdapter.setSelect(selectMode);
            mPictureListAdapter.notifyDataSetChanged();
        }
        renderView();
    }

    @SuppressLint("SetTextI18n")
    private void renderView() {
        if (viewType == 0) {// 网格
            if (isSelectMode) { // 网格才有选择模式
                bind.sum.setText("已选" + selectedCount + "张");
            } else {
                bind.sum.setText("共" + mPictures.size() + "张");
            }
            bind.toolbar.getMenu().findItem(R.id.action_all).setVisible(true);
        } else { // 图片浏览模式
            bind.toolbar.getMenu().findItem(R.id.action_all).setVisible(false);
            bind.sum.setText((mPageIndex + 1) + "/" + mPictures.size());
        }

        // 如果不是最后一项
        // bind.next.setEnabled(mPageIndex < mPictures.size() - 1);

        // 如果不是第一项
        // bind.preview.setEnabled(mPageIndex > 0);
    }

    /**
     * 切换底部,顶部操作按钮是否显示
     */
    public void toggleUI() {
        if (bind.footer.getVisibility() != View.VISIBLE) {//显示
            toolbarInAnimation();
            footerInAnimation();
        } else {//隐藏
            toolBarOutAnimation();
            footerOutAnimation();
        }
    }

    /**
     * 更新页面数据
     */
    private void updateGridAndViewPager() {
        mPictureListAdapter.notifyDataSetChanged();
        mPictureAdapter.notifyDataSetChanged();

        if (mPageIndex >= mPictures.size()) {
            mPageIndex = mPictures.size() - 1;
            if (mPageIndex < 0) { mPageIndex = 0; }
        }

        Timber.e("setCurrentItem %s", mPageIndex);
        bind.pager.setCurrentItem(mPageIndex, false);
        bind.gridView.smoothScrollToPosition(mPageIndex);

        this.renderView();
        this.updateTransferInfo();
    }

    @SuppressLint("CheckResult")
    private void export() {
        if (mPictures.size() == 0) {
            TipDialog.show(this, "已经没有数据了!", TipDialog.TYPE.WARNING);
            return;
        }
        MessageDialog.show(this, "解密文件", "确定要解密?", "确定", "取消")
            .setCancelable(false)
            .setOnOkButtonClickListener((baseDialog, v) -> {
                Observable.just(mPictures.get(bind.pager.getCurrentItem()))
                    .doOnNext(picture -> ConcealUtil.exPore(picture.getFilePath()))
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    //开始执行之前的准备工作
                    .doOnSubscribe(disposable -> WaitDialog.show(this, "正在解密..."))
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
                    .subscribe(picture -> {
                        TipDialog.show(this, "解密成功!", TipDialog.TYPE.SUCCESS);
                    }, throwable -> {
                        Timber.e(throwable);
                        TipDialog.show(this, "解密失败!", TipDialog.TYPE.ERROR);
                    });
                return false;
            });
    }

    @SuppressLint("CheckResult")
    private void doCopy() {
        if (mPictures.size() == 0) {
            TipDialog.show(this, "已经没有数据了!", TipDialog.TYPE.WARNING);
            return;
        }
        MessageDialog.show(this, "复制文件", "确定要复制文件?", "确定", "取消")
            .setCancelable(false)
            .setOnOkButtonClickListener((baseDialog, v) -> {
                Observable.just(mPictures.get(bind.pager.getCurrentItem()))
                    .doOnNext(picture -> {

                        // 如果文件名与一般的文件名长度不一致,那么先修改为一致的
                        String filePath = picture.getFilePath();
                        String oldFileName = picture.getFileName();
                        if (oldFileName.length() != 21) {
                            String newFileName = oldFileName.replace(ConcealUtil.STAFF, "").substring(0, 13) + STAFF;
                            filePath = filePath.replace(oldFileName, newFileName);
                        }

                        // 检查文件是否存在,如果存在那么循环加
                        int count = 0;
                        File toFile = new File(filePath.replace(STAFF, count + STAFF));
                        while (toFile.exists()) {
                            count++;
                            toFile = new File(filePath.replace(STAFF, count + STAFF));
                        }
                        boolean success = FileUtils.copyFile(this, new File(picture.getFilePath()), toFile);
                        if (success) {
                            Picture copy = new Picture();
                            copy.setFileName(toFile.getName());
                            copy.setFilePath(toFile.getAbsolutePath());
                            copy.setAlbumName(picture.getAlbumName());
                            copy.setSelected(false);
                            mPictures.add(bind.pager.getCurrentItem(), copy);
                        }
                    })
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    //开始执行之前的准备工作
                    .doOnSubscribe(disposable -> WaitDialog.show(this, "正在复制..."))
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
                    .subscribe(picture -> {
                        updateGridAndViewPager();
                        TipDialog.show(this, "复制成功!", TipDialog.TYPE.SUCCESS);
                    }, throwable -> {
                        Timber.e(throwable);
                        TipDialog.show(this, "复制失败!", TipDialog.TYPE.ERROR);
                    });
                return false;
            });
    }

    /**
     * 处理选中的文件
     */
    @SuppressLint("CheckResult")
    private void handlerSelected() {
        if (nothingPictureSelected("处理选择项")) {
            return;
        }

        MessageDialog.show((AppCompatActivity) this,
                           "提示",
                           "处理这 [ " + selectedCount + " ] 个文件?",
                           "确定",
                           "取消")
            .setOnOkButtonClickListener((baseDialog, v) -> {
                wakeLock();
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    int i = 0;
                    for (Picture picture : mPictures) {
                        if (picture.isSelected()) {
                            try {
                                // 1. 解密文件
                                mSourceFile = new File(this.getCacheDir(), System.currentTimeMillis() + ".src");
                                ConcealUtil.exPore(picture.getFilePath(), mSourceFile, true);

                                // 2. 移动符合条件的文件, 检查文件大小,未裁剪的不处理
                                /*Point imageSize = BitmapUtils.getImageSize(mSourceFile.getAbsolutePath());
                                if ((imageSize.x == 4512 && imageSize.y == 6016) ||
                                    (imageSize.x == 6016 && imageSize.y == 4512)) {
                                } else {
                                    ConcealUtil.moveFile(picture, ConcealUtil.ROOT_DIR + "/裁剪");
                                }
                                mSourceFile.delete();*/

                                // 2. 处理为heif图片
                                String toFile = picture.getFilePath().replace(mAlbumName, "HEIF");
                                ConcealUtil.encryptedToHEIF(mSourceFile.getAbsolutePath(), toFile);
                                // 3. 删除缓存文件
                                mSourceFile.delete();
                            } catch (KeyChainException | CryptoInitializationException | IOException | RuntimeException exception) {
                                e.onError(exception);
                            }
                            e.onNext(++i);
                        }
                    }
                    e.onComplete();
                })
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    .doOnSubscribe(disposable -> {
                        mProgressDialog = WaitDialog.show((AppCompatActivity) this, "正在处理...");
                    })//开始执行之前的准备工作
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
                    .subscribe(progress -> mProgressDialog.setMessage(progress + "/" + selectedCount),
                               throwable -> {
                                   unWakeLock();
                                   TipDialog.dismiss();
                                   MessageDialog.show((AppCompatActivity) this, "提示", "处理失败", "确定")
                                       .setCancelable(false)
                                       .setOnOkButtonClickListener((dialog, view) -> false);
                                   updateGridAndViewPager();
                                   Timber.e(throwable);
                               },
                               () -> {
                                   unWakeLock();
                                   TipDialog.dismiss();
                                   updateGridAndViewPager();
                                   MessageDialog.show((AppCompatActivity) this, "提示", "处理成功", "确定")
                                       .setCancelable(false)
                                       .setOnOkButtonClickListener((dailog, view) -> false);
                               });
                return false;
            });
    }

    @SuppressLint("CheckResult")
    private void cropHeif() {
        if (mPictures.size() == 0) {
            TipDialog.show(this, "已经没有数据了!", TipDialog.TYPE.WARNING).setCancelable(true);
            return;
        }
        WaitDialog.show(this, "准备裁剪...");

        // 使用glide处理图片, 这样图片图片旋转方向就没问题了
        // todo 如果有更好的方式处理旋转问题就好了
        mGlideRequests.asBitmap()
            .load(mPictures.get(bind.pager.getCurrentItem()))
            .apply(RequestOptions.overrideOf(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
            .into(new CustomTarget<Bitmap>() {

                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    saveAndCrop(resource);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) { }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    TipDialog.show(PictureActivity.this, "裁剪失败!", TipDialog.TYPE.WARNING).setCancelable(true);
                }
            });
    }

    /**
     * 原始图片处理成bitmap之后保存到文件, 并调用图片裁剪功能
     *
     * @param bitmap 处理后的bitmap
     */
    @SuppressLint("CheckResult")
    private void saveAndCrop(Bitmap bitmap) {
        Observable.just(bitmap)
            .doOnNext(picture -> {// 准备剪切的图片
                mSourceFile = new File(this.getCacheDir(), System.currentTimeMillis() + ".src");
                mDestinationFile = new File(this.getCacheDir(), System.currentTimeMillis() + ".dest");
                // 转换为jpeg
                ImageUtils.save(bitmap, mSourceFile, Bitmap.CompressFormat.JPEG, true);

                long orgSize = new File(mPictures.get(bind.pager.getCurrentItem()).getFilePath()).length();
                Timber.e("Glide处理前的大小:%s ,处理后的大小: %s", orgSize, mSourceFile.length());
            })
            .compose(bindUntilEvent(ActivityEvent.DESTROY))
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            //开始执行之前的准备工作
            .doOnSubscribe(disposable -> {})
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的 doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(picture -> {// 图片准备好之后,开始剪切操作
                TipDialog.dismiss();

                // 打开了相册所以页面被覆盖不能结束该页面,应该在关闭打开页面后觉得是否结束该页面
                // (打开页面退到后台会给出一个空数据回调,判断数据为空时候结束该页面)
                ((PictureActivity) this).mNeedFinish = false;
                Uri sourceUri = Uri.fromFile(mSourceFile);
                Uri destinationUri = Uri.fromFile(mDestinationFile);
                UCrop.Options options = new UCrop.Options();
                Display display = ((WindowManager) (this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
                Point point = new Point();
                display.getRealSize(point);

                options.setAspectRatioOptions(2,
                                              new AspectRatio("16:9", 9, 16),
                                              new AspectRatio("3:2", 2, 3),
                                              new AspectRatio("屏幕比例", point.x, point.y),
                                              new AspectRatio("4:3", 3, 4),
                                              new AspectRatio("1:1", 1, 1)
                );
                options.setCropGridColumnCount(9);
                options.setCropGridRowCount(6);
                options.setCompressionQuality(95);
                options.setToolbarCancelDrawable(R.drawable.ic_arrow_back_white_24dp);
                options.setToolbarCropDrawable(R.drawable.ic_save_white_24dp);
                // tabScale, tabRotate, tabAspectRatio
                options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.SCALE, UCropActivity.SCALE);
                Intent intent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .withMaxResultSize(40000, 40000)
                    .useSourceImageAspectRatio()
                    .getIntent(this);
                intent.setClass(this, CropActivity.class);
                startActivityForResult(intent, UCrop.REQUEST_CROP);
                //.start(this, PictureFragment.this);
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(PictureActivity.this, "裁剪失败!", TipDialog.TYPE.WARNING).setCancelable(true);
            });
    }

    @SuppressLint("CheckResult")
    private void crop() {
        if (mPictures.size() == 0) {
            TipDialog.show(this, "已经没有数据了!", TipDialog.TYPE.WARNING);
            return;
        }

        Observable.just(mPictures.get(bind.pager.getCurrentItem()))
            .doOnNext(picture -> {// 准备剪切的图片
                mSourceFile = new File(this.getCacheDir(), System.currentTimeMillis() + ".src");
                mDestinationFile = new File(this.getCacheDir(), System.currentTimeMillis() + ".dest");
                ConcealUtil.exPore(picture.getFilePath(), mSourceFile, true);
            })
            .compose(bindUntilEvent(ActivityEvent.DESTROY))
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            //开始执行之前的准备工作
            .doOnSubscribe(disposable -> WaitDialog.show(this, "准备裁剪..."))
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的 doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(picture -> {// 图片准备好之后,开始剪切操作
                TipDialog.dismiss();

                // 打开了相册所以页面被覆盖不能结束该页面,应该在关闭打开页面后觉得是否结束该页面
                // (打开页面退到后台会给出一个空数据回调,判断数据为空时候结束该页面)
                ((PictureActivity) this).mNeedFinish = false;
                Uri sourceUri = Uri.fromFile(mSourceFile);
                Uri destinationUri = Uri.fromFile(mDestinationFile);
                UCrop.Options options = new UCrop.Options();
                Display display = ((WindowManager) (this.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
                Point point = new Point();
                display.getRealSize(point);

                options.setAspectRatioOptions(2,
                                              new AspectRatio("16:9", 9, 16),
                                              new AspectRatio("3:2", 2, 3),
                                              new AspectRatio("屏幕比例", point.x, point.y),
                                              new AspectRatio("4:3", 3, 4),
                                              new AspectRatio("1:1", 1, 1)
                );
                options.setCropGridColumnCount(9);
                options.setCropGridRowCount(6);
                options.setCompressionQuality(95);
                options.setToolbarCancelDrawable(R.drawable.ic_arrow_back_white_24dp);
                options.setToolbarCropDrawable(R.drawable.ic_save_white_24dp);
                // tabScale, tabRotate, tabAspectRatio
                options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.SCALE, UCropActivity.SCALE);
                Intent intent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .withMaxResultSize(40000, 40000)
                    .useSourceImageAspectRatio()
                    .getIntent(this);
                intent.setClass(this, CropActivity.class);
                startActivityForResult(intent, UCrop.REQUEST_CROP);
                //.start(this, PictureFragment.this);
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(this, "解密失败!", TipDialog.TYPE.ERROR);
            });
    }

    /**
     * 显示移动,回调方法里面移动选择项
     */
    private void showListMove() {
        if (nothingPictureSelected("移动")) {
            return;
        }

        // 打开了相册所以页面被覆盖不能结束该页面,应该在关闭打开页面后觉得是否结束该页面
        // (打开页面退到后台会给出一个空数据回调,判断数据为空时候结束该页面)
        ((PictureActivity) this).mNeedFinish = false;
        Intent intent = new Intent(this, AlbumSelectActivity.class);
        intent.putExtra(AlbumSelectActivity.ALBUM_NAME, mAlbumName);//需要排除这个相册
        startActivityForResult(intent, REQUEST_CODE_SELECT_ALBUM_LIST);
    }

    /**
     * 删除选择项
     */
    @SuppressLint("CheckResult")
    private void doListDelete() {
        if (nothingPictureSelected("删除")) {
            return;
        }

        MessageDialog.show((AppCompatActivity) this, "提示", "删除 [ " + selectedCount + " ] 个文件?", "确定", "取消")
            .setOnOkButtonClickListener((baseDialog, v) -> {
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    try {
                        int i = 0;
                        Iterator<Picture> iterator = mPictures.iterator();
                        while (iterator.hasNext()) {
                            Picture picture = iterator.next();
                            if (picture.isSelected()) {
                                ConcealUtil.delete(picture);
                                iterator.remove();
                                e.onNext(++i);
                            }
                        }
                        e.onComplete();
                    } catch (Exception exception) {
                        e.onError(exception);
                    }
                })
                    .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    //开始执行之前的准备工作
                    .doOnSubscribe(disposable -> {
                        mProgressDialog = WaitDialog.show((AppCompatActivity) this, "正在删除...");
                    })
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
                    .subscribe(progress -> mProgressDialog.setMessage("" + progress + "/" + selectedCount),
                               throwable -> {
                                   TipDialog.show(this, "删除失败！", TipDialog.TYPE.ERROR);
                                   Timber.e(throwable);
                                   updateGridAndViewPager();
                               },
                               () -> {
                                   TipDialog.show(this, "删除成功！", TipDialog.TYPE.SUCCESS).setCancelable(true);
                                   selectedCount = 0;
                                   updateGridAndViewPager();
                               });
                return false;
            });
    }

    /**
     * 移动选择项到指定目录
     *
     * @param path 移动路径
     */
    @SuppressLint("CheckResult")
    private void doListMove(final String path, final String dir) {
        // 如果选择的是当前相册,跳过
        if (TextUtils.isEmpty(path) || mAlbumPath.equals(path)) {
            TipDialog.show(this, "不能移动到当前文件夹！", TipDialog.TYPE.WARNING);
            return;
        }
        if (nothingPictureSelected("移动")) {
            return;
        }
        MessageDialog.show(this,
                           "提示",
                           "移动 [ " + selectedCount + " ] 个文件到相册 [ " + dir + " ] ?",
                           "确定",
                           "取消")
            .setOnOkButtonClickListener((baseDialog, v) -> {
                wakeLock();
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    try {
                        int i = 0;
                        Iterator<Picture> iterator = mPictures.iterator();
                        while (iterator.hasNext()) {
                            Picture picture = iterator.next();
                            if (picture.isSelected()) {
                                ConcealUtil.moveFile(picture, path);
                                iterator.remove();
                                e.onNext(++i);
                            }
                        }
                        e.onComplete();
                    } catch (Exception e1) {
                        e.onError(e1);
                    }
                })
                    .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    .doOnSubscribe(disposable -> {
                        mProgressDialog = WaitDialog.show((AppCompatActivity) this, "正在移动...");
                    })//开始执行之前的准备工作
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
                    .subscribe(progress -> mProgressDialog.setMessage("" + progress + "/" + selectedCount),
                               throwable -> {
                                   unWakeLock();
                                   TipDialog.show(this, "移动失败！", TipDialog.TYPE.ERROR);
                                   Timber.e(throwable);
                                   updateGridAndViewPager();
                               },
                               () -> {
                                   unWakeLock();
                                   TipDialog.show(this, "移动成功！", TipDialog.TYPE.ERROR);
                                   selectedCount = 0;
                                   updateGridAndViewPager();
                               });
                return false;
            });
    }

    /**
     * 解密
     */
    @SuppressLint("CheckResult")
    private void doListExport() {
        if (nothingPictureSelected("解密")) {
            return;
        }

        MessageDialog.show((AppCompatActivity) this,
                           "提示",
                           "解密这 [ " + selectedCount + " ] 个文件?",
                           "确定",
                           "取消")
            .setOnOkButtonClickListener((baseDialog, v) -> {
                wakeLock();
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    int i = 0;
                    for (Picture picture : mPictures) {
                        if (picture.isSelected()) {
                            try {
                                // 拦截运行时异常,无论解密是否成功,都认为解密成功
                                ConcealUtil.exPore(picture.getFilePath());
                            } catch (KeyChainException | CryptoInitializationException | IOException | RuntimeException exception) {
                                e.onError(exception);
                            }
                            e.onNext(++i);
                        }
                    }
                    e.onComplete();
                })
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    .doOnSubscribe(disposable -> {
                        mProgressDialog = WaitDialog.show((AppCompatActivity) this, "正在解密...");
                    })//开始执行之前的准备工作
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
                    .subscribe(progress -> mProgressDialog.setMessage(progress + "/" + selectedCount),
                               throwable -> {
                                   unWakeLock();
                                   TipDialog.dismiss();
                                   MessageDialog.show((AppCompatActivity) this, "提示", "解密失败", "确定")
                                       .setCancelable(false)
                                       .setOnOkButtonClickListener((dialog, view) -> false);
                                   Timber.e(throwable);
                               },
                               () -> {
                                   unWakeLock();
                                   TipDialog.dismiss();
                                   MessageDialog.show((AppCompatActivity) this, "提示", "解密成功", "确定")
                                       .setCancelable(false)
                                       .setOnOkButtonClickListener((dailog, view) -> false);
                               });
                return false;
            });
    }

    @SuppressLint("CheckResult")
    private void delete() {
        if (mPictures.size() == 0) {
            TipDialog.show(this, "已经没有数据了!", TipDialog.TYPE.WARNING);
            return;
        }

        if (mAlbumName.equals(ConcealUtil.TRASH_DIR_NAME)) {
            MessageDialog.show(this, "警告", "确定要删除,删除后文件将不可恢复?", "删除", "取消")
                .setCancelable(false)
                .setOnOkButtonClickListener((baseDialog, v) -> {
                    doDelete();
                    return false;
                });
        } else {
            doDelete();
        }
    }

    @SuppressLint("CheckResult")
    private void doDelete() {
        Observable.just(mPictures.get(bind.pager.getCurrentItem()))
            .map(ConcealUtil::delete)
            .compose(bindUntilEvent(ActivityEvent.DESTROY))
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            //开始执行之前的准备工作
            .doOnSubscribe(disposable -> WaitDialog.show(this, "正在删除"))
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(success -> {
                if (success) {
                    WaitDialog.dismiss();
                    Toast.makeText(this, "删除成功", Toast.LENGTH_LONG).show();
                    mPictures.remove(mPictures.get(bind.pager.getCurrentItem()));
                    updateGridAndViewPager();
                } else {
                    TipDialog.show(this, "删除失败!", TipDialog.TYPE.SUCCESS);
                }
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(this, "删除失败!", TipDialog.TYPE.ERROR);
            });
    }

    /**
     * 选择相册用于移动相片
     */
    private void move() {
        // 打开了相册所以页面被覆盖不能结束该页面,应该在关闭打开页面后觉得是否结束该页面
        // (打开页面退到后台会给出一个空数据回调,判断数据为空时候结束该页面)
        mNeedFinish = false;
        Intent intent = new Intent(this, AlbumSelectActivity.class);
        intent.putExtra(AlbumSelectActivity.ALBUM_NAME, mAlbumName);//需要排除这个相册
        startActivityForResult(intent, REQUEST_CODE_SELECT_ALBUM);
    }

    /**
     * 移动选择项到指定目录
     *
     * @param path 移动路径
     * @param dir
     */
    @SuppressLint("CheckResult")
    private void doMove(final String path, String dir) {
        // 如果选择的是当前相册,跳过
        if (TextUtils.isEmpty(path) || mAlbumPath.equals(path)) {
            TipDialog.show(this, "不能移动到当前文件夹！", TipDialog.TYPE.WARNING);
            return;
        }
        Observable.just(mPictures.get(bind.pager.getCurrentItem()))
            .map(picture -> ConcealUtil.moveFile(picture, path))
            .compose(bindUntilEvent(ActivityEvent.DESTROY))
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            //开始执行之前的准备工作
            .doOnSubscribe(disposable -> WaitDialog.show(this, "正在删除"))
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(success -> {
                if (success) {
                    TipDialog.show(this, "移动成功!", TipDialog.TYPE.SUCCESS)
                        .setCancelable(true);
                    // 数据变更第一步就是通知数据改变
                    mPictures.remove(mPictures.get(bind.pager.getCurrentItem()));
                    updateGridAndViewPager();
                } else {
                    TipDialog.show(this, "移动失败!", TipDialog.TYPE.SUCCESS)
                        .setCancelable(true);
                }
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(this, "移动失败!", TipDialog.TYPE.ERROR).setCancelable(true);
            });
    }

    /**
     * 保存(加密)剪切后的图片
     * 剪切前将需要剪切的图片解密保存在cache目录
     * 剪切后的图片不是加密的保存在cache目录
     * 保存(加密)图片后需要删除cache目录文件
     */
    @SuppressLint("CheckResult")
    private void saveCropResult() {
        Observable.just(mPictures.get(bind.pager.getCurrentItem()))
            .doOnNext(picture -> {//处理图片
                File toFile = new File(picture.getFilePath());
                // ConcealUtil.encryptedFile(new FileInputStream(mDestinationFile), toFile, true);
                ConcealUtil.encryptedToHEIF(mDestinationFile.getAbsolutePath(), toFile.getAbsolutePath());
                mSourceFile.delete();
                mDestinationFile.delete();
                GlideApp.get(this).clearDiskCache();
            })
            .compose(bindUntilEvent(ActivityEvent.DESTROY))
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            //开始执行之前的准备工作
            .doOnSubscribe(disposable -> WaitDialog.show(this, "保存裁剪..."))
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(picture -> {//图片处理完成后的回调
                TipDialog.show(this, "裁剪成功！", TipDialog.TYPE.SUCCESS).setCancelable(true);
                GlideApp.get(this).clearMemory();
                updateGridAndViewPager();
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(this, "保存剪裁失败！", TipDialog.TYPE.ERROR);
            });
    }

    /**
     * 列表 删除,导出,移动等操作时的选择数量检测
     *
     * @return 选择项数量
     */
    private boolean nothingPictureSelected(String message) {
        int count = 0;
        for (Picture mPicture : mPictures) {
            if (mPicture.isSelected()) {
                count++;
            }
        }
        // 与全局保存的选择数量对比
        if (count != selectedCount) {
            TipDialog.show(this, "选择数量计算不正确,请检查代码！", TipDialog.TYPE.WARNING);
            return true;
        }
        if (count == 0) {
            TipDialog.show(this, "请选择需要" + message + "的文件！", TipDialog.TYPE.WARNING);
            return true;
        }
        return false;
    }

    /**
     * 加载指定文件夹下的图片文件
     *
     * @param path 相册目录 /Android/mAlbumName
     */
    @SuppressLint("CheckResult")
    private void loadData(final String path) {
        Observable.just(path)
            .compose(PictureActivity.this.bindUntilEvent(ActivityEvent.DESTROY))
            .map(param -> {
                if (!TextUtils.isEmpty(param)) {
                    // 加载所有图片
                    File file = new File(param);
                    String albumName = file.getName();
                    List<Picture> pictures = new ArrayList<>();
                    if (file.exists() && file.isDirectory()) {
                        String[] fileNames = file.list((dir, filename) -> {
                            return filename.endsWith(STAFF);//只加载图片文件
                        });
                        Arrays.sort(fileNames, (o1, o2) -> o2.compareTo(o1));
                        for (String fileName : fileNames) {
                            Picture picture = new Picture();
                            picture.setSelected(false);
                            picture.setAlbumName(albumName);
                            picture.setFileName(fileName);
                            // picture.setFilePath(path + File.separator + fileName);
                            picture.setFilePath(getRandomImage(path + File.separator + fileName));
                            pictures.add(picture);
                        }
                    }
                    return pictures;
                }
                return null;
            })
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            .doOnSubscribe(disposable -> mPictures.clear())//开始执行之前的准备工作
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(pictures -> {
                if (pictures != null) {
                    mPictures.addAll(pictures);
                    mPictureListAdapter.notifyDataSetChanged();
                    mPictureAdapter.notifyDataSetChanged();
                    renderView();
                }
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(PictureActivity.this, "加载失败！", TipDialog.TYPE.WARNING);
            });
    }

    private String getRandomImage(String content) {
        //红色
        String red;
        //绿色
        String green;
        //蓝色
        String blue;
        //生成随机对象
        Random random = new Random();
        //生成红色颜色代码
        red = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成绿色颜色代码
        green = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成蓝色颜色代码
        blue = Integer.toHexString(random.nextInt(256)).toUpperCase();

        //判断红色代码的位数
        red = red.length() == 1 ? "0" + red : red;
        //判断绿色代码的位数
        green = green.length() == 1 ? "0" + green : green;
        //判断蓝色代码的位数
        blue = blue.length() == 1 ? "0" + blue : blue;
        //生成十六进制颜色值
        String bgColor = "" + red + green + blue;
        String textColor = "" + green + blue + red;

        // https://fakeimg.pl/625x375/F44336/FFF/?font=noto&text=卢先生
        return "https://fakeimg.pl/625x375/"
            + bgColor + "/"
            + textColor + "/"
            + "?font=noto&text="
            + content;

        // https://placeholder.pics/svg/80x80/FF2030/FFF/分享
        /*return "https://placeholder.pics/svg/625x375/"
            + bgColor + "/"
            + textColor + "/"
            + content;*/
    }

    /**
     * 阻止休眠
     */
    private void wakeLock() {
        // 阻止休眠
        Window win = getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        params.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        win.setAttributes(params);
    }

    private void unWakeLock() {
        // 阻止休眠
        Window win = getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        params.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        params.flags &= ~WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        params.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        win.setAttributes(params);
    }

    private void toolbarInAnimation() {
        if (bind.toolbar.getVisibility() == View.VISIBLE) {
            return;
        }
        ImmersionBar.with(this).reset()
            .fullScreen(false)
            .hideBar(BarHide.FLAG_SHOW_BAR)
            // .navigationBarEnable(false)
            .init();

        TranslateAnimation translateAnimation =
            new TranslateAnimation(0, 0, -bind.toolbar.getHeight() + tintManager.getConfig().getStatusBarHeight(), 0);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new DecelerateInterpolator());
        animationSet.setDuration(ANIMATION_DURATION);
        animationSet.setAnimationListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animationSet);
                bind.toolbar.clearAnimation();
            }
        });
        bind.toolbar.clearAnimation();
        bind.toolbar.setVisibility(View.VISIBLE);
        bind.toolbar.startAnimation(animationSet);
    }

    private void toolBarOutAnimation() {
        if (bind.toolbar.getVisibility() != View.VISIBLE) {
            Timber.e("toolbar当前为不可见状态不执行动画");
            return;
        }
        ImmersionBar.with(this).reset()
            .fullScreen(true)
            .hideBar(BarHide.FLAG_HIDE_STATUS_BAR)
            // .navigationBarEnable(false)
            .init();

        TranslateAnimation translateAnimation =
            new TranslateAnimation(0, 0, 0, -bind.toolbar.getHeight() + tintManager.getConfig().getStatusBarHeight());
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new AccelerateInterpolator());
        animationSet.setDuration(ANIMATION_DURATION);
        animationSet.setAnimationListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animationSet);
                bind.toolbar.clearAnimation();
            }
        });
        bind.toolbar.clearAnimation();
        bind.toolbar.setVisibility(View.INVISIBLE);
        bind.toolbar.startAnimation(animationSet);
    }

    private void footerInAnimation() {
        if (bind.footer.getVisibility() == View.VISIBLE) {
            Timber.e("footer当前为可见状态不执行动画");
            return;
        }
        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, bind.footer.getHeight() / 3.0f, 0);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new AccelerateInterpolator()); // 加速
        // animationSet.setInterpolator(new DecelerateInterpolator()); // 减速
        animationSet.setDuration(ANIMATION_DURATION);
        animationSet.setAnimationListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animationSet);
                bind.footer.clearAnimation();
            }
        });
        bind.footer.clearAnimation();
        bind.footer.setVisibility(View.VISIBLE);
        bind.footer.startAnimation(animationSet);
    }

    private void footerOutAnimation() {
        if (bind.footer.getVisibility() != View.VISIBLE) {
            return;
        }
        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, bind.footer.getHeight() / 3.0f);
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setInterpolator(new AccelerateInterpolator()); // 加速
        // animationSet.setInterpolator(new DecelerateInterpolator()); // 减速
        animationSet.setDuration(ANIMATION_DURATION);
        animationSet.setAnimationListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animationSet);
                bind.footer.clearAnimation();
            }
        });
        bind.footer.clearAnimation();
        bind.footer.setVisibility(View.INVISIBLE);
        bind.footer.startAnimation(animationSet);
    }

    /**
     * 计算图片边界
     *
     * @param gridView
     * @param position
     * @param imageViewId
     * @param offset
     * @return
     */
    public static Rect computeBounds(GridView gridView, int position, @IdRes int imageViewId, int offset) {
        int firstVisiblePosition = gridView.getFirstVisiblePosition();
        View itemView = gridView.getChildAt(position - firstVisiblePosition);
        Rect bounds = new Rect();
        if (itemView != null) {
            View thumbView = itemView.findViewById(imageViewId);
            thumbView.getGlobalVisibleRect(bounds);
            if (offset != 0) {
                bounds.set(bounds.left, bounds.top + offset, bounds.right, bounds.bottom + offset);
            }
        }
        return bounds;
    }
}
