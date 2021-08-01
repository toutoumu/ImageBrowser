package org.wordpress.passcodelock;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import com.readystatesoftware.systembartint.SystemBarTintManager;

public class SamplePreferenceActivity extends AppCompatActivity {
    private static final String KEY_PASSCODE_FRAGMENT = "passcode-fragment";

    private PasscodePreferenceFragment mPasscodePreferenceFragment;
    private SystemBarTintManager mTintManager;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences);

        //状态栏,导航栏颜色
        mTintManager = new SystemBarTintManager(this);
        mTintManager.setStatusBarTintEnabled(true);
        mTintManager.setNavigationBarTintEnabled(true);
        mTintManager.setTintColor(getResources().getColor(R.color.primary));

        mToolbar = findViewById(R.id.toolbar);
        // mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        setSupportActionBar(mToolbar);

        // 设置返回按钮
        Drawable stateButtonDrawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp).mutate();
        stateButtonDrawable.setColorFilter(getResources().getColor(R.color.title_text_color), PorterDuff.Mode.SRC_ATOP);
        mToolbar.setNavigationIcon(stateButtonDrawable);

        /*ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }*/

        FragmentManager fragmentManager = getSupportFragmentManager();
        mPasscodePreferenceFragment = (PasscodePreferenceFragment) fragmentManager.findFragmentByTag(KEY_PASSCODE_FRAGMENT);

        if (mPasscodePreferenceFragment == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, true);
            mPasscodePreferenceFragment = new PasscodePreferenceFragment();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            fragmentManager.beginTransaction()
                .replace(R.id.contener, mPasscodePreferenceFragment, KEY_PASSCODE_FRAGMENT)
                .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
