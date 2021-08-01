package freed;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.library.App;
import com.library.glide.ConcealUtil;
import com.library.media.AlbumListActivity;
import com.library.media.BaseActivity;
import com.library.media.UiUtils;
import com.troop.freedcam.R;
import freed.cam.ActivityFreeDcamMain;
import java.util.List;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.passcodelock.SamplePreferenceActivity;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

/**
 * 指纹测试
 * 点击打开相册并选择相应的路径
 */
public class SampleActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks {
    private static final int READ_REQUEST_CODE = 42;
    private static final int RC_CAMERA_AND_LOCATION = 233;
    private static final int REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11 = 882; // aneroid 11 文件权限
    private static final int REQUEST_CODE_OPEN_ALBUM_LIST = 123;
    private static final int REQUEST_CODE_OPEN_SETTING = 124;

    private int count = 0;

    public boolean mNeedFinish = true; // 页面失去焦点(onPause)后是否需要结束掉该页面

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.sample_activity);
        super.onCreate(savedInstanceState);

        setTransparentForWindow();

        initToolbar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 管理全部文件的权限判断是否获取MANAGE_EXTERNAL_STORAGE权限：
            if (!Environment.isExternalStorageManager()) {
                mNeedFinish = false;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11);
            }
        }
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        UiUtils.setCenterTitle(this, toolbar, "选择操作");

        // 设置返回按钮
        Drawable stateButtonDrawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp).mutate();
        stateButtonDrawable.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
        toolbar.setNavigationIcon(stateButtonDrawable);

        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        count = 0;
        // 相机
        findViewById(R.id.camera).setOnClickListener(v -> {
            performFileSearch();
            count++;
            if (count < 3) { return; }
            count = 0;
            AppLockManager.getInstance().getAppLock().disable();
            startActivity(new Intent(SampleActivity.this, ActivityFreeDcamMain.class));
        });

        findViewById(R.id.empty).setOnClickListener(v -> count = 0);

        // 相册
        if (App.isLogin()) {
            findViewById(R.id.album).setOnClickListener(v -> {
                if (!App.isLogin()) {return;}
                performFileSearch();
                showPictureFragment();
            });
        }
        // 每次页面重新获取焦点,都应该标记为失去焦点(onPause)后应该结束掉该页面
        mNeedFinish = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 如果有所不关闭
        if (mNeedFinish && !AppLockManager.getInstance().getAppLock().isPasswordLocked()) {
            // 失去焦点结束掉,如果失去焦点的动作是打开新页面触发,则需要在此生命周期之前设置mNeedFinish = false
            setResult(Activity.RESULT_OK);//如果有前一个页面,通知前一个页面finish
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        AppLockManager.getInstance().setExtendedTimeout();
        AppLockManager.getInstance().getAppLock().forcePasswordLock(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample, menu);

        // 设置菜单按钮颜色
        MenuItem menuItem = menu.findItem(R.id.action_settings);
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

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.action_settings) {
            mNeedFinish = false;
            startActivityForResult(new Intent(SampleActivity.this, SamplePreferenceActivity.class),
                                   REQUEST_CODE_OPEN_SETTING);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /* *********************权限相关***************** */

    /**
     * 显示照片列表方法
     */
    @AfterPermissionGranted(RC_CAMERA_AND_LOCATION)
    void showPictureFragment() {
        count++;
        if (count < 3) { return; }
        count = 0;
        if (EasyPermissions.hasPermissions(this,
                                           Manifest.permission.READ_EXTERNAL_STORAGE,
                                           Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AppLockManager.getInstance().getAppLock().disable();
            mNeedFinish = false;
            startActivityForResult(new Intent(SampleActivity.this, AlbumListActivity.class), REQUEST_CODE_OPEN_ALBUM_LIST);
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this,
                                               "需要外部存储权限",
                                               RC_CAMERA_AND_LOCATION,
                                               Manifest.permission.READ_EXTERNAL_STORAGE,
                                               Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    /**
     * 选择可以被访问的sd卡目录,授权给该应用访问
     */
    public void performFileSearch() {
        // 只有启用SD卡才需要选择目录
        if (!ConcealUtil.NEED_SDCARD) return;
        SharedPreferences file = getSharedPreferences("file", 0);
        String url = file.getString("url", "");
        // 如果没有选择, 那么进行选择
        if (url.isEmpty()) {
            mNeedFinish = false;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if ((requestCode == REQUEST_CODE_OPEN_ALBUM_LIST || requestCode == REQUEST_CODE_OPEN_SETTING)
            && resultCode == RESULT_OK) {
            finish();
            return;
        }
        // aneroid 11 文件权限
        if (requestCode == REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11 && resultCode == Activity.RESULT_OK) {

            Toast.makeText(this, "已经授予权限", Toast.LENGTH_LONG).show();
            return;
        }
        // SD卡授权返回
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri rootUri;
            if (resultData != null) {
                rootUri = resultData.getData();
                //会得到这样的信息，应该是tree/后面的不同
                //content://com.android.externalstorage.documents/tree/0C3D-8650%3A
                //按照注释翻译过来是表明这个Uri需要进行持久化保存，后面参数的意义就是需要的权限，注释中倒是没有对其解释
                getContentResolver().takePersistableUriPermission(rootUri,
                                                                  Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                      | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                SharedPreferences file = getSharedPreferences("file", 0);
                SharedPreferences.Editor edit = file.edit();
                edit.putString("url", rootUri.toString());
                edit.apply();
            }
        }
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        new AppSettingsDialog.Builder(this)
            .setRationale("某些权限已被拒绝")
            .setTitle("缺少权限")
            .build()
            .show();
    }

    @Override
    public void onBackPressed() {
        // 回退到相机页面, 恢复默认的登录状态
        App.setLogin(true);
        finish();
    }
}
