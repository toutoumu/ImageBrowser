package org.wordpress.passcodelock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import androidx.annotation.Nullable;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;
import android.view.View;

/**
 * 解锁页面
 */
public class PasscodeUnlockActivity extends AbstractPasscodeKeyboardActivity {

    @Override
    public void onResume() {
        super.onResume();

        if (isFingerprintSupported()) {
            mCancel = new CancellationSignal();
            mFingerprintManager.authenticate(null, 0, mCancel, getFingerprintCallback(), null);
            View view = findViewById(R.id.image_fingerprint);
            view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        getAppLock().forcePasswordLock(true);
        if (getAppLock().getiUnlockLister() != null) {
            getAppLock().getiUnlockLister().onUnLock(false, null);
        }
        Intent i = new Intent();
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
        finish();
    }

    @Override
    protected void onPinLockInserted() {
        String passLock = mPinCodeField.getText().toString();
        if (getAppLock().verifyPassword(passLock)) {
            if (getAppLock().getiUnlockLister() != null) {
                getAppLock().getiUnlockLister().onUnLock(true, passLock);
            }
            authenticationSucceeded();
        } else {
            if (getAppLock().getiUnlockLister() != null) {
                getAppLock().getiUnlockLister().onUnLock(false, null);
            }
            authenticationFailed();
        }
    }

    @Override
    protected FingerprintManagerCompat.AuthenticationCallback getFingerprintCallback() {
        return new FingerprintManagerCompat.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                // without the call to verifyPassword the unlock screen will show multiple times
                getAppLock().verifyPassword(AbstractAppLock.FINGERPRINT_VERIFICATION_BYPASS);
                if (getAppLock().getiUnlockLister() != null) {
                    getAppLock().getiUnlockLister().onUnLock(true, null);
                }
                authenticationSucceeded();
            }

            @Override
            public void onAuthenticationFailed() {
                if (getAppLock().getiUnlockLister() != null) {
                    getAppLock().getiUnlockLister().onUnLock(false, null);
                }
                authenticationFailed();
            }

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            }
        };
    }

    private boolean isFingerprintSupported() {
        return mFingerprintManager.isHardwareDetected()
            && mFingerprintManager.hasEnrolledFingerprints();
    }
}
