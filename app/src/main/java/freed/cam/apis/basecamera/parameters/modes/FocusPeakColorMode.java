package freed.cam.apis.basecamera.parameters.modes;

import com.troop.freedcam.R;

import org.greenrobot.eventbus.Subscribe;

import freed.FreedApplication;
import freed.cam.apis.basecamera.parameters.AbstractParameter;
import freed.cam.events.ValueChangedEvent;
import freed.cam.previewpostprocessing.Preview;
import freed.settings.SettingKeys;
import freed.settings.SettingsManager;
import freed.utils.Log;

/**
 * Created by KillerInk on 17.01.2018.
 */

public class FocusPeakColorMode extends AbstractParameter {

    private Preview focuspeakProcessor;

    public FocusPeakColorMode(Preview renderScriptManager, SettingKeys.Key settingMode) {
        super(settingMode);
        this.focuspeakProcessor = renderScriptManager;
        SetValue(GetStringValue(), false);
    }

    @Override
    public void SetValue(String valueToSet, boolean setToCamera) {
        if (focuspeakProcessor == null) { return; }
        try {
            SettingsManager.getGlobal(SettingKeys.FOCUSPEAK_COLOR).set(valueToSet);
            if (valueToSet.equals(FreedApplication.getStringFromRessources(R.string.fcolor_red))) {
                focuspeakProcessor.setRed(true);
                focuspeakProcessor.setGreen(false);
                focuspeakProcessor.setBlue(false);
            } else if (valueToSet.equals(FreedApplication.getStringFromRessources(R.string.fcolor_green))) {
                focuspeakProcessor.setRed(false);
                focuspeakProcessor.setGreen(true);
                focuspeakProcessor.setBlue(false);
            } else if (valueToSet.equals(FreedApplication.getStringFromRessources(R.string.fcolor_blue))) {
                focuspeakProcessor.setRed(false);
                focuspeakProcessor.setGreen(false);
                focuspeakProcessor.setBlue(true);
            } else if (valueToSet.equals(FreedApplication.getStringFromRessources(R.string.fcolor_white))) {
                focuspeakProcessor.setRed(true);
                focuspeakProcessor.setGreen(true);
                focuspeakProcessor.setBlue(true);
            } else if (valueToSet.equals(FreedApplication.getStringFromRessources(R.string.fcolor_yellow))) {
                focuspeakProcessor.setRed(true);
                focuspeakProcessor.setGreen(true);
                focuspeakProcessor.setBlue(false);
            } else if (valueToSet.equals(FreedApplication.getStringFromRessources(R.string.fcolor_magenta))) {
                focuspeakProcessor.setRed(true);
                focuspeakProcessor.setGreen(false);
                focuspeakProcessor.setBlue(true);
            } else if (valueToSet.equals(FreedApplication.getStringFromRessources(R.string.fcolor_cyan))) {
                focuspeakProcessor.setRed(false);
                focuspeakProcessor.setGreen(true);
                focuspeakProcessor.setBlue(true);
            }
        } catch (NullPointerException ex) {
            Log.WriteEx(ex);
        }
        fireStringValueChanged(valueToSet);
    }

    @Override
    public String[] getStringValues() {
        return SettingsManager.getGlobal(SettingKeys.FOCUSPEAK_COLOR).getValues();
    }

    @Override
    public String GetStringValue() {
        return SettingsManager.getGlobal(SettingKeys.FOCUSPEAK_COLOR).get();
    }

    @Subscribe
    public void onStringValueChanged(ValueChangedEvent<String> valueob) {
        if (valueob.key == SettingKeys.PREVIEW_POST_PROCESSING_MODE) {
            String value = valueob.newValue;
            if (value.equals(FreedApplication.getStringFromRessources(R.string.off_))) {
                setViewState(ViewState.Hidden);
            } else { setViewState(ViewState.Visible); }
        }
    }
}
