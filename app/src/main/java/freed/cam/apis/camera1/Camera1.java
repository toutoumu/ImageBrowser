package freed.cam.apis.camera1;

import freed.cam.apis.basecamera.AbstractCamera;
import freed.cam.apis.camera1.cameraholder.CameraHolderLG;
import freed.cam.apis.camera1.cameraholder.CameraHolderLegacy;
import freed.cam.apis.camera1.cameraholder.CameraHolderMTK;
import freed.cam.apis.camera1.cameraholder.CameraHolderMotoX;
import freed.cam.apis.camera1.cameraholder.CameraHolderSony;
import freed.cam.apis.camera1.parameters.ParametersHandler;
import freed.cam.events.CameraStateEvents;
import freed.cam.events.EventBusHelper;
import freed.settings.Frameworks;
import freed.settings.SettingKeys;
import freed.settings.SettingsManager;
import freed.utils.Log;

public class Camera1 extends AbstractCamera<ParametersHandler, CameraHolder, ModuleHandler> {
    private static final String TAG = Camera1.class.getSimpleName();

    private boolean cameraIsOpen = false;

    @Override
    public void createCamera() {
        Log.d(TAG,
              "FrameWork:"
                  + SettingsManager.getInstance().getFrameWork()
                  + " openlegacy:"
                  + SettingsManager.get(SettingKeys.openCamera1Legacy).get());

        if (SettingsManager.getInstance().getFrameWork() == Frameworks.LG) {
            cameraHolder = new CameraHolderLG(this, Frameworks.LG);
            Log.d(TAG, "create LG camera");
        } else if (SettingsManager.getInstance().getFrameWork() == Frameworks.Moto_Ext) {
            cameraHolder = new CameraHolderMotoX(this, Frameworks.Moto_Ext);
            Log.d(TAG, "create MotoExt camera");
        } else if (SettingsManager.getInstance().getFrameWork() == Frameworks.MTK) {
            cameraHolder = new CameraHolderMTK(this, Frameworks.MTK);
            Log.d(TAG, "create Mtk camera");
        } else if (SettingsManager.getInstance().getFrameWork() == Frameworks.SonyCameraExtension) {
            cameraHolder = new CameraHolderSony(this, Frameworks.SonyCameraExtension);
        } else if (SettingsManager.get(SettingKeys.openCamera1Legacy).get()) {
            cameraHolder = new CameraHolderLegacy(this, Frameworks.Default);
            Log.d(TAG, "create Legacy camera");
        } else {
            cameraHolder = new CameraHolder(this, Frameworks.Default);
            Log.d(TAG, "create Normal camera");
        }
        moduleHandler = new ModuleHandler(this);

        parametersHandler = new ParametersHandler(this);

        //moduleHandler.addListner(Camera1Fragment.this);
        focusHandler = new FocusHandler(this);

        Log.d(TAG, "initModules");
        moduleHandler.initModules();
        Log.d(TAG, "Check Focuspeak");
    }

    @Override
    public void initCamera() {
        ((FocusHandler) focusHandler).startListning();
        parametersHandler.LoadParametersFromCamera();
        CameraStateEvents.fireCameraOpenFinishEvent(this);
    }

    @Override
    public void startCamera() {
        EventBusHelper.register(this);
        if (!cameraIsOpen) {
            cameraIsOpen = cameraHolder.OpenCamera(SettingsManager.getInstance().getCameraIds()[SettingsManager.getInstance()
                .GetCurrentCamera()]);
        }
        Log.d(TAG, "startCamera");
    }

    @Override
    public void stopCamera() {
        EventBusHelper.unregister(this);
        Log.d(TAG, "Stop Camera");
        getPreview().close();
        if (cameraHolder != null) { cameraHolder.CloseCamera(); }
        cameraIsOpen = false;
    }

    @Override
    public void restartCamera() {
        Log.d(TAG, "Stop Camera");
        getPreview().close();
        cameraHolder.CloseCamera();
        cameraIsOpen = false;
        if (!cameraIsOpen) {
            cameraIsOpen = cameraHolder.OpenCamera(SettingsManager.getInstance().getCameraIds()[SettingsManager.getInstance()
                .GetCurrentCamera()]);
        }
        Log.d(TAG, "startCamera");
    }

    @Override
    public void startPreview() {
        Log.d(TAG, "Start Preview");
        cameraHolder.StartPreview();
    }

    @Override
    public void stopPreview() {
        try {
            Log.d(TAG, "Stop Preview");
            if (cameraHolder != null) { cameraHolder.StopPreview(); }
        } catch (NullPointerException ex) {
            Log.WriteEx(ex);
        }
    }
}
