package org.wordpress.passcodelock;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;

/**
 * Interface for AppLock implementations.
 * <p/>
 * There are situations where the AppLock should not be required within an app. Methods for
 * tracking
 * exempt {@link android.app.Activity}'s are provided and sub-class implementations are expected to
 * comply with requested exemptions.
 *
 * @see #isExemptActivity(String)
 * @see #setExemptActivities(String[])
 * @see #getExemptActivities()
 * <p/>
 * Applications can request a one-time delay in locking the app. This can be useful for activities
 * that launch external applications with the expectation that the user will return to the calling
 * application shortly.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public abstract class AbstractAppLock
    implements Application.ActivityLifecycleCallbacks {
    public static final String FINGERPRINT_VERIFICATION_BYPASS = "fingerprint-bypass__";
    public static final int EXTENDED_TIMEOUT_S = 60;

    private int mLockTimeout = EXTENDED_TIMEOUT_S;
    private String[] mExemptActivities;
    IUnlockLister iUnlockLister;

    public interface IUnlockLister {
        void onUnLock(boolean success, String password);
    }

    public void setiUnlockLister(IUnlockLister iUnlockLister) {
        this.iUnlockLister = iUnlockLister;
    }

    public IUnlockLister getiUnlockLister() {
        return iUnlockLister;
    }

    /**
     * 这个Activity是否不能被锁定
     *
     * @param name {@link Activity#getClass()}
     * @return true不需要锁定, false需要锁定
     */
    public boolean isExemptActivity(String name) {
        if (name == null) return false;
        for (String activityName : getExemptActivities()) {
            if (name.equals(activityName)) return true;
        }
        return false;
    }

    /**
     * 设置不需要锁定的Activities
     *
     * @param exemptActivities String[]
     */
    public void setExemptActivities(String[] exemptActivities) {
        mExemptActivities = exemptActivities;
    }

    /**
     * 获取不需要锁定的Activities
     *
     * @return String[]
     */
    public String[] getExemptActivities() {
        if (mExemptActivities == null) setExemptActivities(new String[0]);
        return mExemptActivities;
    }

    /**
     * 设置锁定超时
     *
     * @param timeout 单位s
     */
    public void setOneTimeTimeout(int timeout) {
        mLockTimeout = timeout;
    }

    /**
     * 获取锁定超时
     *
     * @return 单位s
     */
    public int getTimeout() {
        return mLockTimeout;
    }

    /**
     * 是否指纹锁
     *
     * @param password 密码
     * @return 是否指纹锁
     */
    protected boolean isFingerprintPassword(String password) {
        return FINGERPRINT_VERIFICATION_BYPASS.equals(password);
    }

    /**
     * 启用密码锁
     */
    public abstract void enable();

    /**
     * 禁用密码锁
     */
    public abstract void disable();

    /**
     * 设置下次启动是否需要密码锁
     *
     * @param lock {@link Boolean}
     */
    public abstract void forcePasswordLock(boolean lock);

    /**
     * 验证密码
     *
     * @param password 密码
     * @return 是否验证通过
     */
    public abstract boolean verifyPassword(String password);

    /**
     * 是否有密码锁
     *
     * @return 是否有密码锁
     */
    public abstract boolean isPasswordLocked();

    /**
     * 设置密码
     *
     * @param password 密码
     * @return 设置是否成功
     */
    public abstract boolean setPassword(String password);
}
