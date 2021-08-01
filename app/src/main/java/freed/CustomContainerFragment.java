package freed;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;
import androidx.fragment.app.Fragment;
import com.kongzue.dialog.v3.TipDialog;
import com.library.App;
import com.library.glide.ConcealUtil;
import com.troop.freedcam.R;
import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.ui.themesample.cameraui.CameraUiFragment;
import java.util.List;
import org.wordpress.passcodelock.AppLockManager;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

/**
 * 这个....
 */
public class CustomContainerFragment extends Fragment implements EasyPermissions.PermissionCallbacks {
    private static final int READ_REQUEST_CODE = 42;
    private static final int RC_CAMERA_AND_LOCATION = 233;
    private static final int REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11 = 882; // aneroid 11 文件权限

    private View mask;
    //private int model;
    private int light;//屏幕亮度
    private int screenWidth;
    private int screenHeight;
    private Vibrator vibrator;
    private CameraUiFragment cameraUiFragment;
    private CameraWrapperInterface mInterface;

    private int clickCount = 0;
    private boolean pause = false;
    public static boolean needMoveToBack = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置旋转方向
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 管理全部文件的权限判断是否获取MANAGE_EXTERNAL_STORAGE权限：
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //model = getScreenMode();
        light = getScreenBrightness();
        //setScreenMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        setScreenBrightness(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        pause = false;
        needMoveToBack = true;
        clickCount = 0;
    }

    @Override
    public void onPause() {
        //setScreenMode(model);
        setScreenBrightness(light);
        pause = true;
        super.onPause();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.custom_fragment, container, false);
        this.mask = view.findViewById(R.id.mask);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getParentFragment() instanceof CameraUiFragment) {
            this.cameraUiFragment = ((CameraUiFragment) getParentFragment());
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            screenWidth = wm.getDefaultDisplay().getWidth();
            screenHeight = wm.getDefaultDisplay().getHeight();

            screenHeight = Math.max(screenWidth, screenHeight);
            screenWidth = Math.min(screenWidth, screenHeight);

            // 拍照
            mask.setOnTouchListener(new View.OnTouchListener() {
                int shotCount;
                long secClick;
                long firClick;
                boolean isWorking = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    clickCount = 0;
                    String moduleName = mInterface.getModuleHandler().getCurrentModuleName();
                    // 拍照
                    if (moduleName.equals(getResources().getString(R.string.module_picture))) {
                        if (mInterface == null || mInterface.getModuleHandler().getCurrentModule().IsWorking()) {
                            return true;
                        }
                        int action = MotionEventCompat.getActionMasked(event);
                        int index = event.getActionIndex();
                        switch (action) {
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_POINTER_UP: {
                                if (pause) {
                                    Timber.e("已经暂停了下面就不执行了吧");
                                    return true;
                                }
                                if (MotionEventCompat.getX(event, index) > 30
                                    && MotionEventCompat.getX(event, index) < screenWidth - 30
                                    && MotionEventCompat.getY(event, index) > 30
                                    && MotionEventCompat.getY(event, index) < screenHeight - 30) {
                                    cameraUiFragment.onClick((int) (mInterface.getPreviewWidth() * 0.725),
                                                             mInterface.getPreviewHeight() / 2);
                                }
                                break;
                            }
                        }
                        return true;
                    }
                    // 连续拍照
                    else if (moduleName.equals(getResources().getString(R.string.module_interval))) {
                        if (!isWorking) {
                            mInterface.getModuleHandler().startWork();
                            isWorking = true;
                        }
                        return true;
                    }
                    //录像
                    else if (moduleName.equals(getResources().getString(R.string.module_video))) {
                        if (MotionEvent.ACTION_DOWN == event.getAction()) {
                            shotCount++;
                            if (shotCount == 1) {
                                firClick = System.currentTimeMillis();
                                v.postDelayed(() -> {
                                    if (shotCount == 1) {
                                        shotCount = 0;
                                        firClick = 0;
                                        secClick = 0;
                                    }
                                }, 1000);
                            } else if (shotCount == 2) {
                                secClick = System.currentTimeMillis();
                                if (secClick - firClick < 1000) {
                                    //双击事件
                                    if (vibrator == null) {
                                        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
                                    }
                                    // 停止的时候两次
                                    if (mInterface.getModuleHandler().getCurrentModule().IsWorking()) {
                                        // long[] pattern = { 500, 30, 500, 30 };   // 停止 开启 停止 开启
                                        // vibrator.vibrate(pattern, -1);
                                        vibrator.vibrate(VibrationEffect.createOneShot(130, 255));
                                    } else {
                                        vibrator.vibrate(VibrationEffect.createOneShot(30, 255));
                                    }
                                    mInterface.getModuleHandler().startWork();
                                }
                                shotCount = 0;
                                firClick = 0;
                                secClick = 0;
                            }
                        }
                        return true;
                    }
                    return true;
                }
            });
        }

        view.findViewById(R.id.album).setOnClickListener(v -> {
            if (!App.isLogin()) {return;}
            performFileSearch();
            showPictureFragment();
        });
    }

    /**
     * 设置相机操作处理接口
     *
     * @param cameraUiWrapper {@link CameraWrapperInterface}
     */
    public void setWrap(CameraWrapperInterface cameraUiWrapper) {
        this.mInterface = cameraUiWrapper;
    }

    /**
     * 获得当前屏幕亮度值 0--255
     */
    private int getScreenBrightness() {
        int screenBrightness = 255;
        try {
            screenBrightness = Settings.System.getInt(getActivity().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception ignored) {

        }
        return screenBrightness;
    }

    /**
     * 保存当前的屏幕亮度值，并使之生效
     */
    private void setScreenBrightness(int paramInt) {
        Window localWindow = getActivity().getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        localLayoutParams.screenBrightness = paramInt / 255.0F;
        localWindow.setAttributes(localLayoutParams);
    }

    public void performFileSearch() {
        // 只有启用SD卡才需要选择目录
        if (!ConcealUtil.NEED_SDCARD) {
            return;
        }
        SharedPreferences file = requireContext().getSharedPreferences("file", 0);
        String url = file.getString("url", "");
        if (url.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
    }

    /**
     * 显示照片列表方法
     */
    @AfterPermissionGranted(RC_CAMERA_AND_LOCATION)
    void showPictureFragment() {
        clickCount++;
        if (clickCount < 3) { return; }
        clickCount = 0;
        if (EasyPermissions.hasPermissions(requireContext(),
                                           Manifest.permission.READ_EXTERNAL_STORAGE,
                                           Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Timber.e("进入SampleActivity");
            needMoveToBack = false;
            // 进入 SampleActivity 页面强制要求,输入密码
            AppLockManager.getInstance().getAppLock().enable();
            AppLockManager.getInstance().getAppLock().forcePasswordLock(true);
            startActivity(new Intent(requireContext(), SampleActivity.class));
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this,
                                               "需要外部存储权限",
                                               RC_CAMERA_AND_LOCATION,
                                               Manifest.permission.READ_EXTERNAL_STORAGE,
                                               Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == REQUEST_CODE_FILE_MANAGE_SELECT_PHOTO_ANDROID11 && resultCode == Activity.RESULT_OK) {
            TipDialog.show((AppCompatActivity) requireActivity(), "已经授予权限！", TipDialog.TYPE.WARNING);
            return;
        }

        // 选择SD卡路径授权返回
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri rootUri;
            if (resultData != null) {
                rootUri = resultData.getData();
                //会得到这样的信息，应该是tree/后面的不同
                //content://com.android.externalstorage.documents/tree/0C3D-8650%3A
                //按照注释翻译过来是表明这个Uri需要进行持久化保存，后面参数的意义就是需要的权限，注释中倒是没有对其解释
                requireContext()
                    .getContentResolver()
                    .takePersistableUriPermission(rootUri,
                                                  Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                      | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                SharedPreferences file = requireContext().getSharedPreferences("file", 0);
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
}
