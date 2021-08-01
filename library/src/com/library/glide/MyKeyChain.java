package com.library.glide;

import android.content.Context;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.MacConfig;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.keychain.KeyChain;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import org.wordpress.passcodelock.R;
import timber.log.Timber;

/**
 * 加密秘钥
 */
public class MyKeyChain implements KeyChain {
    private final CryptoConfig mCryptoConfig = CryptoConfig.KEY_256;
    private final SecureRandom mSecureRandom;
    private final Context mContext;

    private byte[] mCipherKey;
    private boolean mSetCipherKey;
    private byte[] mMacKey;
    private boolean mSetMacKey;

    public MyKeyChain(Context context) {
        this.mContext = context.getApplicationContext();
        this.mSecureRandom = new SecureRandom();
    }

    @Override
    public byte[] getCipherKey() throws KeyChainException {
        if (!this.mSetCipherKey) {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.folder);
            try {
                this.mCipherKey = new byte[mCryptoConfig.keyLength];
                int read = inputStream.read(this.mCipherKey, 0, mCryptoConfig.keyLength);
                if (read == -1) {
                    throw new RuntimeException("文件太小了");
                }
                this.mSetCipherKey = true;
            } catch (IOException e) {
                Timber.e(e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
        }
        return this.mCipherKey;
    }

    @Override
    public byte[] getMacKey() throws KeyChainException {
        if (!this.mSetMacKey) {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.folder);
            try {
                this.mMacKey = new byte[MacConfig.DEFAULT.keyLength];
                int read = inputStream.read(this.mMacKey, 0, MacConfig.DEFAULT.keyLength);
                if (read == -1) {
                    throw new RuntimeException("文件太小了");
                }
                this.mSetMacKey = true;
            } catch (IOException e) {
                Timber.e(e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
        }
        return this.mMacKey;
    }

    @Override
    public byte[] getNewIV() throws KeyChainException {
        byte[] iv = new byte[this.mCryptoConfig.ivLength];
        this.mSecureRandom.nextBytes(iv);
        return iv;
    }

    @Override
    public void destroyKeys() {

    }
}
