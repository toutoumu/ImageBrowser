package com.library.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.GridView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.kongzue.dialog.v3.TipDialog;
import com.library.glide.ConcealUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.trello.rxlifecycle3.android.ActivityEvent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.wordpress.passcodelock.R;
import timber.log.Timber;

import static com.library.glide.ConcealUtil.STAFF;
import static java.io.File.separator;

public class AlbumSelectActivity extends BaseActivity {
    /** 相册名称 AlbumName */
    public static final String ALBUM_NAME = "name";
    /** 相册路径 /data/AlbumName */
    public static final String ALBUM_PATH = "path";

    private GridView mGridView;
    private final List<Album> mAlbums = new ArrayList<>();
    private String mAlbumName;//需要排除的相册
    private boolean mNeedFinish = true;//页面失去焦点(onPause)后是否需要结束掉该页面
    private SystemBarTintManager tintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        getSwipeBackLayout().setScrimColor(Color.TRANSPARENT);

        setTransparentForWindow();

        // 设置列表的Margin属性,给状态栏和导航栏留出空间
        tintManager = new SystemBarTintManager(this);

        if (getIntent().getExtras() != null) {
            mAlbumName = getIntent().getExtras().getString(ALBUM_NAME, ConcealUtil.PICTURE_DIR.getName());
        }

        initToolbar();

        initGridView();

        loadData();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Cancel");
        UiUtils.setCenterTitle(this, toolbar, "选择相册");
        // toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(toolbar);

        // 设置返回按钮
        Drawable stateButtonDrawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp).mutate();
        stateButtonDrawable.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
        toolbar.setNavigationIcon(stateButtonDrawable);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        params.setMargins(params.leftMargin,
                          tintManager.getConfig().getStatusBarHeight(),
                          params.rightMargin,
                          params.bottomMargin);
    }

    private void initGridView() {
        mGridView = findViewById(R.id.grid_view);
        mGridView.setAdapter(new AlbumSelectAdapter(this, mAlbums));
        mGridView.setOnItemClickListener((parent, view, position, id) -> {
            if (position <= mAlbums.size()) {
                Album album = mAlbums.get(position);
                Intent intent = new Intent();//数据是使用Intent返回
                intent.putExtra(ALBUM_NAME, album.getName());//目标相册名称 "mAlbumName"
                intent.putExtra(ALBUM_PATH, album.getPath());//目标相册的路径 "/data/mAlbumName"
                AlbumSelectActivity.this.setResult(RESULT_OK, intent);//设置返回数据
                AlbumSelectActivity.this.finish();//关闭Activity
            }
        });
        mGridView.setPadding(0,
                             tintManager.getConfig().getStatusBarHeight() + tintManager.getConfig().getActionBarHeight(),
                             0,
                             tintManager.getConfig().getNavigationBarHeight());
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {//返回按钮
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @SuppressLint("CheckResult")
    private void loadData() {
        Observable<File> observable;
        if (ConcealUtil.NEED_SDCARD) {
            observable = Observable.just(ConcealUtil.ROOT_DIR, ConcealUtil.ROOT_DIR1, ConcealUtil.SDCARD_Root_DIR);
        } else {
            observable = Observable.just(ConcealUtil.ROOT_DIR, ConcealUtil.ROOT_DIR1);
        }
        observable.compose(bindUntilEvent(ActivityEvent.DESTROY))
            .map(file -> {
                if (file.isDirectory()) {
                    if (!file.isDirectory() || !file.exists()) { return null; }
                    return getAlbum(file, mAlbumName);
                }
                return null;
            })
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            .doOnSubscribe(disposable -> mAlbums.clear())//开始执行之前的准备工作
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(
                albums -> {
                    if (albums != null) {
                        mAlbums.addAll(albums);
                        ((AlbumSelectAdapter) mGridView.getAdapter()).notifyDataSetChanged();
                    }
                },
                throwable -> {
                    Timber.e(throwable);
                    TipDialog.show(AlbumSelectActivity.this, "加载失败！", TipDialog.TYPE.ERROR);
                });
    }

    /**
     * 获取路径下目录
     *
     * @param albumGroup 相册所在文件夹
     * @param exclude 排除这个名称的目录
     * @return 相册所在文件夹下所有文件夹
     */
    @NonNull
    private static List<Album> getAlbum(File albumGroup, final String exclude) {
        List<Album> albumList = new ArrayList<>();
        if (albumGroup.exists() && albumGroup.isDirectory()) {
            // 获取文件夹下的所有一级子文件夹
            File[] albumFiles = albumGroup.listFiles(pathname -> {
                return pathname.isDirectory() && !pathname.getName().equals(exclude);// 加载目录类型
            });
            // 遍历每一个相册组织数据
            if (albumFiles != null) {
                for (File albumFile : albumFiles) {
                    if (albumFile.getName().contains(ConcealUtil.IMPORT_DIR_NAME) || albumFile.getName()
                        .contains(ConcealUtil.EXPORT_DIR_NAME)) {
                        continue;
                    }
                    Album album = new Album();
                    album.setCover(loadCover(albumFile));
                    album.setName(albumFile.getName());
                    album.setPath(albumFile.getAbsolutePath());
                    albumList.add(album);
                }
            }
        }
        albumList.sort((album, t1) -> album.getName().compareTo(t1.getName()));
        return albumList;
    }

    /**
     * 获取封面 (获取文件夹下第一个扩展名为STAFF常量结尾的文件)
     *
     * @param albumFile 相册
     * @return 封面 {@link Picture} 对象
     */
    @NonNull
    private static Picture loadCover(final File albumFile) {
        Picture picture = new Picture();
        picture.setFilePath("");
        if (albumFile.exists() && albumFile.isDirectory()) {
            String[] pictureNames = albumFile.list((dir, pictureName) -> pictureName.endsWith(STAFF));
            if (pictureNames != null && pictureNames.length > 0) {// 获取第一张图片
                // 按文件名从小到大排列,减少排序开销
                //Arrays.sort(pictureNames, String::compareTo);

                // 循环一次查找效率更高
                String fileName = pictureNames[0];
                for (String pictureName : pictureNames) {
                    if (pictureName.compareTo(fileName) > 0) {
                        fileName = pictureName;
                    }
                }

                picture.setAlbumName(albumFile.getName());
                picture.setFileName(fileName);
                picture.setFilePath(albumFile.getAbsolutePath() + separator + fileName);
                //picture.setLength(new File(albumFile.getPath()).length());
            }
        }
        return picture;
    }
}
