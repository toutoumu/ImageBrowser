package com.library.media;

public interface UCropFragmentCallback1 {

    /**
     * Return loader status
     *
     * @param showLoader
     */
    void loadingProgress(boolean showLoader);

    /**
     * Return cropping result or error
     *
     * @param result
     */
    void onCropFinish(UCropFragment1.UCropResult result);
}
