package org.wordpress.passcodelock;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import com.yalantis.ucrop.UCropActivity;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import static org.wordpress.passcodelock.AbstractPasscodeKeyboardActivity.PASSWORD_LENGTH;

public class DefaultAppLock extends AbstractAppLock {
    public static boolean isSupportedApi() {
        return true;
    }

    //页面失去焦点(onPause)后是否需要结束掉该页面
    private boolean mNeedFinish = true;

    private static final String CROP_CLASS_NAME = UCropActivity.class.getName();
    private static final String UNLOCK_CLASS_NAME = PasscodeUnlockActivity.class.getName();
    private static final String OLD_PASSWORD_SALT = "sadasauidhsuyeuihdahdiauhs";
    private static final String OLD_APP_LOCK_PASSWORD_PREF_KEY = "wp_app_lock_password_key";

    private final Application mCurrentApp;
    private final SharedPreferences mSharedPreferences;
    private Date mLostFocusDate;
    private boolean forcePasswordLock = false;

    public DefaultAppLock(Application app) {
        super();
        mCurrentApp = app;
        mSharedPreferences = mCurrentApp.getSharedPreferences("mysetting", Context.MODE_PRIVATE);
        // mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCurrentApp);
    }

    /**
     * 这个Activity是否不能被锁定
     *
     * @param activityName {@link Activity#getClass()}
     * @return true不需要锁定, false需要锁定
     * @discussion {@link PasscodeUnlockActivity} 不能豁免.
     */
    @Override
    public boolean isExemptActivity(String activityName) {
        return !UNLOCK_CLASS_NAME.equals(activityName) && super.isExemptActivity(activityName);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity.getClass().getName().equals(CROP_CLASS_NAME)) {
            if (mNeedFinish) {
                // 失去焦点结束掉,如果失去焦点的动作是打开新页面触发,则需要在此生命周期之前设置mNeedFinish = false
                activity.setResult(Activity.RESULT_OK);//如果有前一个页面,通知前一个页面finish
                activity.finish();
            }
            return;
        }
        if (!isExemptActivity(activity.getClass().getName())) mLostFocusDate = new Date();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity.getClass().getName().equals(CROP_CLASS_NAME)) {
            // 每次页面重新获取焦点,都应该标记为失去焦点(onPause)后应该结束掉该页面
            mNeedFinish = true;
            return;
        }
        if (!isExemptActivity(activity.getClass().getName()) && shouldShowUnlockScreen()) {
            Intent i = new Intent(activity.getApplicationContext(), PasscodeUnlockActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.getApplication().startActivity(i);
        }
    }

    @Override
    public void onActivityCreated(Activity arg0, Bundle arg1) {
    }

    @Override
    public void onActivityDestroyed(Activity arg0) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {
    }

    @Override
    public void onActivityStarted(Activity arg0) {
    }

    @Override
    public void onActivityStopped(Activity arg0) {
    }

    public void enable() {
        if (!isPasswordLocked()) return;
        if (isSupportedApi()) {
            mCurrentApp.unregisterActivityLifecycleCallbacks(this);
            mCurrentApp.registerActivityLifecycleCallbacks(this);
        }
    }

    public void disable() {
        if (isSupportedApi()) {
            mCurrentApp.unregisterActivityLifecycleCallbacks(this);
        }
    }

    public boolean isPasswordLocked() {
        return mSharedPreferences.contains(BuildConfig.PASSWORD_PREFERENCE_KEY)
            || mSharedPreferences.contains(OLD_APP_LOCK_PASSWORD_PREF_KEY);
    }

    public boolean setPassword(String password) {
        removePasswordFromPreferences();
        if (TextUtils.isEmpty(password)) {
            disable();
        } else {
            savePasswordToPreferences(password.hashCode());
            enable();
        }
        return true;
    }

    public void forcePasswordLock(boolean lock) {
        forcePasswordLock = lock;
    }

    public boolean verifyPassword(String password) {
        if (TextUtils.isEmpty(password)) return false;

        // successful fingerprint scan bypasses PIN security
        if (isFingerprintPassword(password)) {
            mLostFocusDate = new Date();
            return true;
        }

        String storedPassword = "";
        String securePassword = null;
        int updatedHash = -1;

        if (mSharedPreferences.contains(OLD_APP_LOCK_PASSWORD_PREF_KEY)) {
            // backwards compatibility
            storedPassword = getStoredLegacyPassword(OLD_APP_LOCK_PASSWORD_PREF_KEY);
            securePassword = legacyPasswordHash(password);
        } else if (mSharedPreferences.contains(BuildConfig.PASSWORD_PREFERENCE_KEY)) {
            if (shouldUpdatePassword()) {
                storedPassword = getStoredLegacyPassword(BuildConfig.PASSWORD_PREFERENCE_KEY);
                storedPassword = decryptPassword(storedPassword);
                storedPassword = stripSalt(storedPassword);
                securePassword = password;
                updatedHash = password.hashCode();
            } else {
                int storedHash = getStoredPassword();
                storedPassword = String.valueOf(storedHash);
                securePassword = String.valueOf(password.hashCode());
            }
        }

        if (!storedPassword.equalsIgnoreCase(securePassword)) return false;

        // password security updated, replace stored password with integer hash value
        if (updatedHash != -1) {
            removePasswordFromPreferences();
            savePasswordToPreferences(updatedHash);
        }
        mLostFocusDate = new Date();
        return true;
    }

    private String stripSalt(String saltedPassword) {
        if (TextUtils.isEmpty(saltedPassword) || saltedPassword.length() < PASSWORD_LENGTH) return "";
        int middle = saltedPassword.length() / 2;
        return saltedPassword.substring(middle - PASSWORD_LENGTH / 2, middle + PASSWORD_LENGTH / 2);
    }

    /** Show the unlock screen if there is a saved password and the timeout period has elapsed. */
    private boolean shouldShowUnlockScreen() {
        if (!isPasswordLocked()) return false;
        if (forcePasswordLock) {
            forcePasswordLock = false;
            //mLostFocusDate = null;
            return true;
        }
        if (mLostFocusDate == null) return true;

        int currentTimeOut = getTimeout();
        setOneTimeTimeout(EXTENDED_TIMEOUT_S);

        if (timeSinceLocked() < currentTimeOut) return false;
        mLostFocusDate = null;
        return true;
    }

    private int getStoredPassword() {
        return mSharedPreferences.getInt(BuildConfig.PASSWORD_PREFERENCE_KEY, -1);
    }

    private void savePasswordToPreferences(int password) {
        mSharedPreferences.edit().putInt(BuildConfig.PASSWORD_PREFERENCE_KEY, password).apply();
    }

    private void removePasswordFromPreferences() {
        mSharedPreferences.edit()
            .remove(OLD_APP_LOCK_PASSWORD_PREF_KEY)
            .remove(BuildConfig.PASSWORD_PREFERENCE_KEY)
            .apply();
    }

    private int timeSinceLocked() {
        return Math.abs((int) ((new Date().getTime() - mLostFocusDate.getTime()) / 1000));
    }

    //
    // Legacy methods for backwards compatibility of passwords stored using deprecated security
    //

    /** Update to hash-based security if password was stored using encryption-based security. */
    private boolean shouldUpdatePassword() {
        Object storedValue = mSharedPreferences.getAll().get(BuildConfig.PASSWORD_PREFERENCE_KEY);
        return storedValue != null && storedValue instanceof String;
    }

    private String getStoredLegacyPassword(String key) {
        return mSharedPreferences.getString(key, "");
    }

    private String legacyPasswordHash(String rawPassword) {
        return StringUtils.getMd5Hash(OLD_PASSWORD_SALT + rawPassword + OLD_PASSWORD_SALT);
    }

    private String decryptPassword(String encryptedPwd) {
        try {
            DESKeySpec keySpec = new DESKeySpec(BuildConfig.PASSWORD_ENC_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
            return new String(plainTextPwdBytes);
        } catch (Exception ignored) {
        }
        return encryptedPwd;
    }
}
