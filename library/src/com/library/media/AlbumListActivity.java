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
import com.kongzue.dialog.v3.BottomMenu;
import com.kongzue.dialog.v3.InputDialog;
import com.kongzue.dialog.v3.MessageDialog;
import com.kongzue.dialog.v3.TipDialog;
import com.kongzue.dialog.v3.WaitDialog;
import com.library.GlideCacheUtil;
import com.library.glide.ConcealUtil;
import com.library.glide.GlideApp;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.trello.rxlifecycle3.android.ActivityEvent;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.wordpress.passcodelock.R;
import timber.log.Timber;

import static com.library.glide.ConcealUtil.STAFF;
import static java.io.File.separator;

/**
 * 相册列表页面
 */
public class AlbumListActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener {
    private static final int REQUEST_CODE_SELECT_ALBUM = 34;
    private final int REQUEST_CODE_FINISH = 33;

    private GridView mGridView;
    private final List<Album> mAlbums = new ArrayList<>();
    private boolean mNeedFinish = true;//页面失去焦点(onPause)后是否需要结束掉该页面
    private TipDialog mProgressDialog;
    private SystemBarTintManager tintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        getSwipeBackLayout().setScrimColor(Color.TRANSPARENT);

        setTransparentForWindow();

        tintManager = new SystemBarTintManager(this);

        initToolbar();

        initGridView();

        loadData();
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Camera");
        toolbar.inflateMenu(R.menu.album);
        UiUtils.setCenterTitle(this, toolbar, "相册列表");
        // toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setOnMenuItemClickListener(this);
        // setSupportActionBar(toolbar);

        // 设置返回按钮
        Drawable stateButtonDrawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp).mutate();
        stateButtonDrawable.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
        toolbar.setNavigationIcon(stateButtonDrawable);

        // 设置菜单按钮颜色
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.action_import);
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

        // 设置列表的Margin属性,给状态栏和导航栏留出空间
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
        params.setMargins(params.leftMargin,
                          tintManager.getConfig().getStatusBarHeight(),
                          params.rightMargin,
                          params.bottomMargin);
    }

    private void initGridView() {
        mGridView = findViewById(R.id.grid_view);
        mGridView.setAdapter(new AlbumListAdapter(this, mAlbums));
        mGridView.setOnItemClickListener((parent, view, position, id) -> {
            if (position <= mAlbums.size()) {
                // 打开了相册所以页面被覆盖不能结束该页面,应该在关闭打开页面后觉得是否结束该页面
                // (打开页面退到后台会给出一个空数据回调,判断数据为空时候结束该页面)
                mNeedFinish = false;
                Album album = mAlbums.get(position);
                Intent intent = new Intent(AlbumListActivity.this, PictureActivity.class);
                intent.putExtra(PictureActivity.ALBUM_PATH, album.getPath());
                intent.putExtra(PictureActivity.ALBUM_NAME, album.getName());
                startActivityForResult(intent, REQUEST_CODE_FINISH);
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
                    return getAlbum(file);
                }
                return null;
            })
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            .doOnSubscribe(disposable -> mAlbums.clear())//开始执行之前的准备工作
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(albums -> {
                if (albums != null) {
                    mAlbums.addAll(albums);
                    ((AlbumListAdapter) mGridView.getAdapter()).notifyDataSetChanged();
                }
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(this, "加载失败！", TipDialog.TYPE.ERROR);
            });
    }

    /**
     * 获取路径下目录
     *
     * @param albumGroup 相册所在文件夹
     * @return 相册所在文件夹下所有文件夹
     */
    @NonNull
    private static List<Album> getAlbum(File albumGroup) {
        List<Album> albumList = new ArrayList<>();
        if (albumGroup.exists() && albumGroup.isDirectory()) {
            // 获取文件夹下的所有一级子文件夹
            // 加载目录类型
            File[] albumFiles = albumGroup.listFiles(File::isDirectory);

            // 遍历每一个相册组织数据
            if (albumFiles != null) {
                for (File albumFile : albumFiles) {
                    // 排除掉导入导出文件夹
                    if (albumFile.getName().contains(ConcealUtil.IMPORT_DIR_NAME) ||
                        albumFile.getName().contains(ConcealUtil.EXPORT_DIR_NAME)) {
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
     * @param albumFile 相册文件夹
     * @return 封面 {@link Picture} 对象
     */
    @NonNull
    private static Picture loadCover(final File albumFile) {
        Picture picture = new Picture();
        picture.setFilePath("");
        if (albumFile.exists() && albumFile.isDirectory()) {
            String[] pictureNames = albumFile.list((dir, pictureName) -> pictureName.endsWith(STAFF));
            if (pictureNames == null) { pictureNames = new String[0];}
            if (pictureNames.length > 0) {// 获取第一张图片
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {//返回按钮
            onBackPressed();
        } else if (itemId == R.id.action_import) {
            showAction();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAction() {
        BottomMenu.show(this, new String[] { "创建相册", "导入", "清除缓存" }, (text, index) -> {
            switch (text) {
                case "创建相册": {
                    InputDialog.show(this, "创建相册", "相册名称", "确定", "取消")
                        .setOnOkButtonClickListener((baseDialog, v, inputStr) -> {
                            if (inputStr == null || inputStr.trim().length() == 0) {
                                TipDialog.show(this, "输入相册名称！", TipDialog.TYPE.WARNING);
                                return false;
                            }
                            File file = new File(ConcealUtil.ROOT_DIR, inputStr);
                            if (file.exists()) {
                                TipDialog.show(this, "相册已经存在！", TipDialog.TYPE.WARNING);
                            }
                            if (file.mkdir()) {
                                TipDialog.show(this, "相册创建成功！", TipDialog.TYPE.WARNING);
                            }

                            return false;
                        });
                    break;
                }
                case "导入": {
                    // 打开了相册所以页面被覆盖不能结束该页面,应该在关闭打开页面后觉得是否结束该页面
                    // (打开页面退到后台会给出一个空数据回调,判断数据为空时候结束该页面)
                    mNeedFinish = false;
                    Intent intent = new Intent(this, AlbumSelectActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SELECT_ALBUM);
                    break;
                }
                case "清除缓存": {
                    GlideCacheUtil.getInstance().clearImageAllCache(this);
                    break;
                }
            }
        }).setTitle("选择操作");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FINISH && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK);
            finish();// 如果被打开的页面说要结束掉这个页面
            return;
        }
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SELECT_ALBUM) {//选择相册回调
            if (data != null && data.getExtras() != null) {// 正常回调是有数据的因此不需要结束页面
                String albumName = data.getExtras().getString(AlbumSelectActivity.ALBUM_NAME);//相册名称
                final String dir = data.getExtras().getString(AlbumSelectActivity.ALBUM_PATH);//相册路径

                if (ConcealUtil.IMPORT_DIR == null) {
                    TipDialog.show(this, "没有需要导入的文件！", TipDialog.TYPE.WARNING);
                    return;
                }
                File[] files = ConcealUtil.IMPORT_DIR.listFiles(file -> !file.isDirectory());
                if (files == null || files.length == 0) {
                    TipDialog.show(this, "没有需要导入的文件！", TipDialog.TYPE.WARNING);
                    return;
                }
                MessageDialog.show(this, "提示", "导入 [ " + files.length + " ] 个文件到相册: [ " + albumName + " ] ?", "确定", "取消")
                    .setCancelable(false)
                    .setOnOkButtonClickListener((baseDialog, v) -> {
                        doImport(files.length, files, dir);
                        return false;
                    });
            } else {// 如果打开选择相册后,应用退到后台,选择页面finish后会回调,但是不带数据,因此页面需要跟随结束
                setResult(Activity.RESULT_OK);
                finish();
            }
        }
    }

    /**
     * 执行导入操作
     *
     * @param count 导入文件数目
     * @param files 导入文件列表
     * @param dir 导入到哪个目录 eg:/data/android/ddd
     */
    @SuppressLint("CheckResult")
    private void doImport(final int count, File[] files, String dir) {
        Observable.create((ObservableOnSubscribe<Integer>) e -> {
            try {
                int i = 0;
                for (File file : files) {
                    ConcealUtil.importFile(file, dir);
                    e.onNext(++i);
                }
                e.onComplete();
            } catch (Exception e1) {
                e.onError(e1);
            }
        })
            .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            .doOnSubscribe(disposable -> {//开始执行之前的准备工作
                mProgressDialog = WaitDialog.show(this, "正在导入...");
            })
            .subscribeOn(AndroidSchedulers.mainThread())//指定 前面的doOnSubscribe 在主线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(progress -> mProgressDialog.setMessage(progress + "/" + count),
                       throwable -> {
                           TipDialog.dismiss();
                           Timber.e(throwable);
                           MessageDialog.show(this, "提示", "导入失败", "确定", "取消")
                               .setCancelable(false)
                               .setOnOkButtonClickListener((baseDialog, v) -> false);
                       },
                       () -> {
                           TipDialog.dismiss();
                           MessageDialog.show(this, "提示", "导入成功", "确定", "取消")
                               .setCancelable(false)
                               .setOnOkButtonClickListener((baseDialog, v) -> false);
                       });
    }
}
