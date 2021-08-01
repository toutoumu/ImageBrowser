package freed.cam.ui;

import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

import freed.cam.apis.basecamera.CameraWrapperInterface;
import freed.cam.ui.themesample.cameraui.CameraUiFragment;
import freed.cam.ui.themesample.settings.SettingsMenuFragment;
import freed.file.holder.BaseHolder;
import freed.viewer.screenslide.modelview.ScreenSlideFragmentModelView;
import freed.viewer.screenslide.views.ScreenSlideFragment;
import com.library.App;

public class CameraUiSlidePagerAdapter extends FragmentStatePagerAdapter {
    private SettingsMenuFragment settingsMenuFragment = new SettingsMenuFragment();
    private ScreenSlideFragment screenSlideFragment = new ScreenSlideFragment();
    private CameraUiFragment cameraUiFragment = new CameraUiFragment();

    private CameraWrapperInterface cameraFragment;
    ScreenSlideFragment.ButtonClick click;

    public CameraUiSlidePagerAdapter(FragmentManager fm,
                                     ScreenSlideFragment.ButtonClick click,
                                     ScreenSlideFragmentModelView screenSlideFragmentModelView) {
        super(fm);
        this.click = click;
        screenSlideFragment.setScreenSlideFragmentModelView(screenSlideFragmentModelView);
    }

    public void setCameraFragment(CameraWrapperInterface cameraFragment) {
        this.cameraFragment = cameraFragment;
        settingsMenuFragment.setCameraToUi(cameraFragment);
        cameraUiFragment.setCameraToUi(cameraFragment);

        if (cameraFragment != null) {
            cameraFragment.getModuleHandler().ModuleHasChanged(cameraFragment.getModuleHandler().getCurrentModuleName());
        }
    }

    public void updateScreenSlideFile(List<BaseHolder> files) {
        //TODO
        //screenSlideFragment.NotifyDATAhasChanged(files);
    }

    @Override
    public Fragment getItem(int position) {
        // FIXME: 2018/3/23 登陆后只显示一个Fragment
        if (App.isLogin()) {
            if (cameraUiFragment == null) {
                cameraUiFragment = new CameraUiFragment();
            }
            if (cameraUiFragment != null) {
                cameraUiFragment.setCameraToUi(cameraFragment);
            }
            return cameraUiFragment;
        }
        if (position == 0) {
            if (settingsMenuFragment != null) {
                settingsMenuFragment.setCameraToUi(cameraFragment);
            }
            return settingsMenuFragment;
        } else if (position == 2) {
            if (screenSlideFragment != null) {

                screenSlideFragment.setOnBackClickListner(click);
            }
            return screenSlideFragment;
        } else {
            if (cameraUiFragment != null) { cameraUiFragment.setCameraToUi(cameraFragment); }
            return cameraUiFragment;
        }
    }

    @Override
    public int getCount() {
        // FIXME: 2018/3/23 登陆后只显示一个Fragment
        if (App.isLogin()) return 1;
        return 3;
    }

    @Nullable
    @Override
    public Parcelable saveState() {
        return null;
    }
}