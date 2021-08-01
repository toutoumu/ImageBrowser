/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package freed.cam.apis.camera1.modules;

import android.media.MediaRecorder;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.Surface;

import com.troop.freedcam.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import freed.FreedApplication;
import freed.cam.apis.basecamera.Size;
import freed.cam.apis.basecamera.modules.ModuleAbstract;
import freed.cam.apis.basecamera.modules.ModuleHandlerAbstract.CaptureStates;
import freed.cam.apis.basecamera.record.VideoRecorder;
import freed.cam.apis.camera1.Camera1;
import freed.cam.apis.camera1.Camera1Utils;
import freed.cam.apis.camera1.CameraHolder;
import freed.cam.events.CameraStateEvents;
import freed.cam.ui.themesample.handler.UserMessageHandler;
import freed.file.holder.FileHolder;
import freed.settings.SettingKeys;
import freed.settings.SettingsManager;
import freed.utils.Log;
import freed.utils.PermissionManager;
import com.library.App;
import com.library.glide.ConcealUtil;

/**
 * Created by troop on 06.01.2016.
 */
public abstract class AbstractVideoModule extends ModuleAbstract<Camera1> implements MediaRecorder.OnInfoListener {
    VideoRecorder recorder;
    private String mediaSavePath;
    private final String TAG = AbstractVideoModule.class.getSimpleName();
    private ParcelFileDescriptor fileDescriptor;

    AbstractVideoModule(Camera1 cameraUiWrapper, Handler mBackgroundHandler, Handler mainHandler) {
        super(cameraUiWrapper, mBackgroundHandler, mainHandler);
        name = FreedApplication.getStringFromRessources(R.string.module_video);
    }

    @Override
    public void InitModule() {
        super.InitModule();
        changeCaptureState(CaptureStates.video_recording_stop);
        createPreview();
        initRecorder();
    }

    @Override
    public String ShortName() {
        return "Mov";
    }

    @Override
    public String LongName() {
        return "Movie";
    }

    //ModuleInterface START
    @Override
    public String ModuleName() {
        return name;
    }

    @Override
    public void DoWork() {
        startStopRecording();
    }

    private void startStopRecording() {
        if (!isWorking && !isLowStorage) {
            startRecording();
        } else if (isWorking) {
            stopRecording();
        }
        if (isLowStorage) {
            UserMessageHandler.sendMSG("Can't Record due to low storage space. Free some and try again.", false);
        }
    }

    @Override
    public boolean IsWorking() {
        return isWorking;
    }

    @Override
    public void IsLowStorage(Boolean x) {
        isLowStorage = x;
    }
    //ModuleInterface END

    private void createPreview() {
        Size sizefromCam = new Size("1920x1080");

        List<Size> sizes = new ArrayList<>();
        String[] stringsSizes = cameraUiWrapper.getParameterHandler().get(SettingKeys.PreviewSize).getStringValues();
        for (String s : stringsSizes) {
            sizes.add(new Size(s));
        }
        final Size size = Camera1Utils.getOptimalPreviewSize(sizes, sizefromCam.width, sizefromCam.height, false);

        if (size == null || cameraUiWrapper.getPreview().getSurfaceTexture() == null) { return; }
        cameraUiWrapper.getCameraHolder().StopPreview();
        cameraUiWrapper.getPreview().stop();

        cameraUiWrapper.getPreview().setSize(size.width, size.height);
        cameraUiWrapper.getPreview().setRotation(size.width, size.height, 0);
        if (cameraUiWrapper.getCameraHolder().canSetSurfaceDirect()) {
            cameraUiWrapper.getCameraHolder().setSurface((Surface) null);
            Surface surface = new Surface(cameraUiWrapper.getPreview().getSurfaceTexture());
            cameraUiWrapper.getCameraHolder().setSurface(surface);
        } else { cameraUiWrapper.getCameraHolder().setTextureView(cameraUiWrapper.getPreview().getSurfaceTexture()); }

        Log.d(TAG, "set size to " + size.width + "x" + size.height);
        cameraUiWrapper.getParameterHandler().get(SettingKeys.PreviewSize).SetValue(size.width + "x" + size.height, false);
        CameraStateEvents.fireCameraAspectRatioChangedEvent(size);
        cameraUiWrapper.getCameraHolder().StartPreview();
    }

    private void startRecording() {
        if (cameraUiWrapper.getActivityInterface()
            .getPermissionManager()
            .isPermissionGranted(PermissionManager.Permissions.RecordAudio)) {
            if (SettingsManager.getGlobal(SettingKeys.LOCATION_MODE)
                .get()
                .equals(FreedApplication.getStringFromRessources(R.string.on_))) {
                cameraUiWrapper.getCameraHolder()
                    .SetLocation(cameraUiWrapper.getActivityInterface().getLocationManager().getCurrentLocation());
            }
            prepareRecorder();
        } else {
            cameraUiWrapper.getActivityInterface()
                .getPermissionManager()
                .requestPermission(PermissionManager.Permissions.RecordAudio);
        }
    }

    private void prepareRecorder() {
        try {
            Log.d(TAG, "InitMediaRecorder");
            isWorking = true;
            ((CameraHolder) cameraUiWrapper.getCameraHolder()).GetCamera().unlock();
            mediaSavePath = cameraUiWrapper.getActivityInterface()
                .getFileListController()
                .getNewFilePath(SettingsManager.getInstance().GetWriteExternal(), ".mp4");

            // FIXME: 2018/3/23 如果已经登录那么视频保存到指定目录
            if (App.isLogin()) {
                mediaSavePath = ConcealUtil.VIDEO_DIR.getPath() + File.separator + System.currentTimeMillis() + ".mp8";
            }

            File tosave = new File(mediaSavePath);
            recorder.setRecordingFile(tosave);
            recorder.setErrorListener((mr, what, extra) -> {
                Log.e("MediaRecorder", "ErrorCode: " + what + " Extra: " + extra);
                changeCaptureState(CaptureStates.video_recording_stop);
                ((CameraHolder) cameraUiWrapper.getCameraHolder()).GetCamera().lock();
            });

            if (!tosave.getParentFile().exists()) { tosave.getParentFile().mkdirs(); }

            recorder.setInfoListener(this);

            if (!SettingsManager.get(SettingKeys.orientationHack).get().equals("0")) {
                recorder.setOrientation(Integer.parseInt(SettingsManager.get(SettingKeys.orientationHack).get()));
            } else { recorder.setOrientation(0); }

            recorder.setPreviewSurface(((CameraHolder) cameraUiWrapper.getCameraHolder()).getSurfaceHolder());

            Log.d(TAG, "Preparing Recorder");
            if (recorder.prepare()) {
                Log.d(TAG, "Recorder Prepared, Starting Recording");
                recorder.start();

                //fix exposer flick after video recording on first set parameters to camera.
                // first call will under expose then second call will fix exposure.
                cameraUiWrapper.getParameterHandler().SetParameters();
                cameraUiWrapper.getParameterHandler().SetParameters();

                Log.d(TAG, "Recording started");
                sendStartToUi();
            } else {
                Log.e(TAG, "Recording failed");
                UserMessageHandler.sendMSG("Start Recording failed ", false);
                isWorking = false;
                ((CameraHolder) cameraUiWrapper.getCameraHolder()).GetCamera().lock();
                recorder.reset();
                isWorking = false;
                sendStopToUi();
            }
        } catch (NullPointerException ex) {
            Log.WriteEx(ex);
            UserMessageHandler.sendMSG("Start Recording failed", false);
            isWorking = false;
            ((CameraHolder) cameraUiWrapper.getCameraHolder()).GetCamera().lock();
            recorder.reset();
            isWorking = false;
            sendStopToUi();
        }
    }

    private void sendStopToUi() {
        changeCaptureState(CaptureStates.video_recording_stop);
    }

    private void sendStartToUi() {
        changeCaptureState(CaptureStates.video_recording_start);
    }

    protected abstract void initRecorder();

    void stopRecording() {
        try {
            recorder.stop();
            Log.e(TAG, "Stop Recording");
        } catch (Exception ex) {
            Log.e(TAG, "Stop Recording failed, was called bevor start");
            UserMessageHandler.sendMSG("Stop Recording failed, was called bevor start", false);
            Log.e(TAG, ex.getMessage());
            isWorking = false;
        } finally {
            ((CameraHolder) cameraUiWrapper.getCameraHolder()).GetCamera().lock();
            recorder.reset();
            isWorking = false;
            try {
                if (VERSION.SDK_INT > VERSION_CODES.KITKAT && fileDescriptor != null) {
                    fileDescriptor.close();
                    fileDescriptor = null;
                }
            } catch (IOException e1) {
                Log.WriteEx(e1);
            }
            // FIXME: 2018/3/23 如果已经登录那么不通知文件改变
            if (!App.isLogin()) {
                File file = new File(mediaSavePath);
                fireOnWorkFinish(new FileHolder(file, SettingsManager.getInstance().GetWriteExternal()));
            }
            sendStopToUi();
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            recordnextFile(mr);
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            recordnextFile(mr);
        }
    }

    private void recordnextFile(MediaRecorder mr) {
        stopRecording();
        startRecording();
    }
}
