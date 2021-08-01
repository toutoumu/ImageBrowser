package freed.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.troop.freedcam.R;

import freed.settings.Frameworks;
import freed.settings.SettingKeys;
import freed.settings.SettingsManager;

public class PdafDetector extends BaseParameter1Detector {
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectPDAF(cameraCharacteristics);
    }

    private void detectPDAF(Camera.Parameters parameters) {
        if (SettingsManager.getInstance().getFrameWork() == Frameworks.MTK) {
            SettingsManager.get(SettingKeys.PDAF).setIsSupported(false);
            return;
        } else {
            detectMode(parameters, R.string.pdaf, R.string.pdaf_mode, SettingsManager.get(SettingKeys.PDAF));
        }
    }
}
