package com.image.browser;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.image.browser.databinding.ActivityPictureBinding;
import com.kongzue.dialog.v3.BottomMenu;
import com.kongzue.dialog.v3.MessageDialog;
import com.kongzue.dialog.v3.TipDialog;
import com.kongzue.dialog.v3.WaitDialog;
import com.library.widget.SmoothImageView;
import com.trello.rxlifecycle3.android.ActivityEvent;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import timber.log.Timber;

/**
 * 图片列表,和图片展示页面的容器
 */
public class PictureActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener {
    /** 动画时长 */
    private static final int ANIMATION_DURATION = 300;

    private final List<Picture> mPictures = new ArrayList<>(); // 图片列表

    private boolean isSelectMode = false;// 列表是否为选择模式
    private int selectedCount = 0; // 列表选择图片数量

    private PictureAdapter2 mPictureAdapter;
    private PictureListAdapter mPictureListAdapter;

    private int mPageIndex = 0; // 当前页索引

    private TipDialog mProgressDialog;
    public ActivityPictureBinding bind;

    // 注意这里的缩略图, 图片列表, 和SmoothImageView都是使用这个图片,也就是缩略图,和过渡动画使用同一个大小的图片,以保证可以在缩略图加载的同时也加载过渡图片
    RequestOptions mThumbnailRequestOptions;
    RequestManager mGlideRequests;

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {//返回按钮
            onBackPressed();
        } else if (itemId == R.id.action_all) {
            BottomMenu.show(
                this, new String[] { "全选", "全不选", "选择模式", "浏览模式", "删除选择项" }, (text, index) -> {
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
                        case "删除选择项": {
                            handlerSelected();
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

        bind = ActivityPictureBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());

        setTransparentForWindow();

        // 必须在这里初始化
        ImmersionBar.with(this).reset()
            .fullScreen(false)
            .hideBar(BarHide.FLAG_SHOW_BAR)
            .init();

        mGlideRequests = Glide.with(this);
        mThumbnailRequestOptions = RequestOptions
            .fitCenterTransform()
            .override(500, 900)
            .error(R.drawable.ic_default_image_list)
            .placeholder(R.drawable.transparent_drawable)
            .dontAnimate()
            .dontTransform()
            .encodeQuality(80);

        bind.dragLayout.setListView(bind.gridView);
        bind.dragLayout.setViewPager(bind.pager);
        bind.dragLayout.setImageView(bind.smoothImageView);

        // 加载文件
        loadData();

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
    public void onBackPressed() {
        if (mPictures.size() == 0) { // 无图片直接退出
            super.onBackPressed();
            return;
        }
        if (isSelectMode) {// 切换到浏览模式
            changeSelectMode(false, false);
            return;
        }
        if (bind.dragLayout.getViewType() == 1) {// 切换到网格视图
            bind.smoothImageView.transformOut();
            return;
        }

        super.onBackPressed();
    }

    /**
     * 初始化点击事件
     */
    private void initListeners() {
        // 动画开始之前的回调
        bind.dragLayout.setBeforeTransformListener(mode -> {
            switch (mode) {
                case SmoothImageView.STATE_TRANSFORM_IN: // 恢复到图片浏览器
                case SmoothImageView.STATE_TRANSFORM_RESTORE: { // 显示图片浏览器
                    toolbarInAnimation();
                    footerInAnimation();
                    renderView();
                    break;
                }
                case SmoothImageView.STATE_TRANSFORM_OUT: { // 显示网格列表
                    toolbarInAnimation();
                    footerOutAnimation();
                    renderView();
                    break;
                }
                case SmoothImageView.STATE_TRANSFORM_MOVE: {
                    toolBarOutAnimation();
                    footerOutAnimation();
                    break;
                }
            }
        });

        // 动画结束之后的回调
        /*
        bind.dragLayout.setTransformListener(mode -> {
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
        */

        // 左下角返回相册
        bind.showAlbum.setOnClickListener(view12 -> {
            if (bind.dragLayout.getViewType() == 1) {
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
                if (position > mPictures.size() - 1) {
                    Timber.e("position 超出了 mPictrues.size");
                    return;
                }
                mPageIndex = position;
                bind.gridView.smoothScrollToPosition(mPageIndex);
                renderView();

                // 更新当前图片缩略图的位置信息
                Rect bounds = SmoothImageView.getBounds(bind.gridView, position, R.id.image);
                bind.smoothImageView.setOriginalInfo(bounds, 0, 0);

                // 更新 smoothImageView 显示的图片
                bind.smoothImageView.setLoading(true);
                mGlideRequests.load(mPictures.get(position).getFilePath())
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
                    .into(bind.smoothImageView);
            }
        });
        bind.pager.setCurrentItem(mPageIndex, false);
    }

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

            bind.pager.setCurrentItem(position, false);

            // 打开图片浏览
            ImageView imageView = view.findViewById(R.id.image);
            Rect bounds = SmoothImageView.getBounds(view);
            bind.smoothImageView.setImageDrawable(imageView.getDrawable());
            bind.smoothImageView.setOriginalInfo(bounds, 0, 0);
            bind.smoothImageView.transformIn();

            // 打开图片浏览
            // bind.smoothImageView.transformIn(view, R.id.image, 0, 0);

            // 打开图片浏览
            // bind.smoothImageView.transformIn(bind.gridView, position, R.id.image, 0, 0);
        });

        bind.gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            changeSelectMode(!isSelectMode, false);
            return true;
        });

        bind.gridView.setPadding(0,
                                 ImmersionBar.getStatusBarHeight(this) + ImmersionBar.getActionBarHeight(this),
                                 0,
                                 ImmersionBar.getNavigationBarHeight(this));
    }

    /**
     * 初始化标题栏
     */
    private void initToolbar() {
        // 初始化标题栏
        bind.toolbar.setTitle("图片浏览");
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
                          ImmersionBar.getStatusBarHeight(this),
                          params.rightMargin,
                          params.bottomMargin);
    }

    /**
     * 显示列表操作
     */
    private void showAction() {
        BottomMenu.show(this, new String[] { "删除", "保存到相册" }, (text, index) -> {
            switch (text) {
                case "删除": {
                    handlerCurrent();
                    break;
                }
                case "保存到相册": {
                    TipDialog.show(this, "保存到相册!", TipDialog.TYPE.SUCCESS);
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
    private void changeSelectMode(boolean selectMode, boolean selectAll) {
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
        if (bind.dragLayout.getViewType() == 0) {// 网格
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
     * 如果列表有变更(删除,新增,修改等等),那么需要调用这个方法,更新页面数据
     */
    private void updateGridAndViewPager() {
        mPictureListAdapter.notifyDataSetChanged();
        mPictureAdapter.notifyDataSetChanged();

        if (mPageIndex > mPictures.size() - 1) {
            mPageIndex = mPictures.size() - 1;
            if (mPageIndex < 0) { mPageIndex = 0; }
        }

        Timber.e("setCurrentItem %s", mPageIndex);
        bind.pager.setCurrentItem(mPageIndex, false);
        bind.gridView.smoothScrollToPosition(mPageIndex);

        this.renderView();

        // 由于列表数据变了, 所以需要重新初始化
        if (mPictures.size() != 0) {
            // 更新当前图片缩略图的位置信息
            Rect bounds = SmoothImageView.getBounds(bind.gridView, mPageIndex, R.id.image);
            bind.smoothImageView.setOriginalInfo(bounds, 0, 0);

            // 更新 smoothImageView 显示的图片
            bind.smoothImageView.setLoading(true);
            mGlideRequests.load(mPictures.get(mPageIndex).getFilePath())
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
                .into(bind.smoothImageView);
        }
    }

    /**
     * 删除选中的文件
     */
    @SuppressLint("CheckResult")
    private void handlerSelected() {
        if (nothingPictureSelected("删除")) {
            return;
        }

        MessageDialog.show(this,
                           "提示",
                           "删除这 [ " + selectedCount + " ] 个文件?",
                           "确定",
                           "取消")
            .setOnOkButtonClickListener((baseDialog, v) -> {
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    try {
                        int i = 0;
                        Iterator<Picture> iterator = mPictures.iterator();
                        while (iterator.hasNext()) {
                            Picture next = iterator.next();
                            if (next.isSelected()) {
                                iterator.remove();
                                e.onNext(++i);
                            }
                        }
                    } catch (Exception exception) {
                        e.onError(exception);
                    }
                    e.onComplete();
                })
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    .doOnSubscribe(disposable -> {
                        mProgressDialog = WaitDialog.show(this, "正在删除...");
                    })//开始执行之前的准备工作
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在主线程执行
                    .subscribe(progress -> mProgressDialog.setMessage(progress + "/" + selectedCount),
                               throwable -> {
                                   TipDialog.dismiss();
                                   MessageDialog.show(this, "提示", "删除失败", "确定")
                                       .setCancelable(false)
                                       .setOnOkButtonClickListener((dialog, view) -> false);
                                   updateGridAndViewPager();
                                   Timber.e(throwable);
                               },
                               () -> {
                                   TipDialog.dismiss();
                                   updateGridAndViewPager();
                                   MessageDialog.show(this, "提示", "删除成功", "确定")
                                       .setCancelable(false)
                                       .setOnOkButtonClickListener((dailog, view) -> false);
                               });
                return false;
            });
    }

    @SuppressLint("CheckResult")
    private void handlerCurrent() {
        if (mPictures.size() == 0) {
            TipDialog.show(this, "没得删了!", TipDialog.TYPE.SUCCESS);
            return;
        }
        MessageDialog.show(this, "提示", "删除这个文件?", "确定", "取消")
            .setOnOkButtonClickListener((baseDialog, v) -> {
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    try {
                        mPictures.remove(bind.pager.getCurrentItem());
                    } catch (Exception exception) {
                        e.onError(exception);
                    }
                    e.onNext(0);
                    e.onComplete();
                })
                    .compose(bindUntilEvent(ActivityEvent.DESTROY))
                    .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
                    .doOnSubscribe(disposable -> {
                        mProgressDialog = WaitDialog.show(this, "正在删除...");
                    })//开始执行之前的准备工作
                    .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
                    .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在主线程执行
                    .subscribe(progress -> {
                                   // TipDialog.dismiss();
                                   TipDialog.show(this, "删除成功!", TipDialog.TYPE.SUCCESS);
                                   updateGridAndViewPager();
                               },
                               throwable -> {
                                   TipDialog.dismiss();
                                   MessageDialog.show(this, "提示", "删除失败", "确定")
                                       .setCancelable(false)
                                       .setOnOkButtonClickListener((dialog, view) -> false);
                                   updateGridAndViewPager();
                                   Timber.e(throwable);
                               });
                return false;
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
     */
    @SuppressLint("CheckResult")
    private void loadData() {
        Observable.just("")
            .compose(PictureActivity.this.bindUntilEvent(ActivityEvent.DESTROY))
            .map(param -> {
                List<Picture> pictures = new ArrayList<>();
                for (int i = 0; i < 10000; i++) {
                    Picture picture = new Picture();
                    picture.setSelected(false);
                    picture.setFilePath(getRandomImage("图片" + i));
                    pictures.add(picture);
                }
                return pictures;
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

    /**
     * 构造图片地址
     *
     * @param content .
     * @return .
     */
    private String getRandomImage(String content) {
        //生成随机对象
        Random random = new Random();
        //生成红色颜色代码
        String red = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成绿色颜色代码
        String green = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成蓝色颜色代码
        String blue = Integer.toHexString(random.nextInt(256)).toUpperCase();

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
            new TranslateAnimation(0, 0, -bind.toolbar.getHeight() + ImmersionBar.getStatusBarHeight(this), 0);
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
            new TranslateAnimation(0, 0, 0, -bind.toolbar.getHeight() + ImmersionBar.getStatusBarHeight(this));
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
}
