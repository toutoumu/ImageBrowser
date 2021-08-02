package com.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.cache.ExternalPreferredCacheDiskCacheFactory;
import com.kongzue.dialog.v3.TipDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import timber.log.Timber;

public class GlideCacheUtil {

    private static GlideCacheUtil instance;

    public static GlideCacheUtil getInstance() {
        if (instance == null) {
            instance = new GlideCacheUtil();
        }
        return instance;
    }

    /**
     * 清理图片磁盘缓存
     */
    public void clearImageDiskCache(final Context context) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.get(context).clearDiskCache();
                    }
                }).start();
            } else {
                Glide.get(context).clearDiskCache();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除图片内存缓存
     */
    public void clearImageMemoryCache(final Context context) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Glide.get(context).clearMemory();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除指定目录下的文件，这里用于缓存的删除
     *
     * @param filePath filePath
     * @param deleteThisPath deleteThisPath
     */
    private void deleteFolderFile(String filePath, boolean deleteThisPath) {
        if (!TextUtils.isEmpty(filePath)) {
            try {
                File file = new File(filePath);
                if (file.isDirectory()) {
                    File files[] = file.listFiles();
                    for (File file1 : files) {
                        deleteFolderFile(file1.getAbsolutePath(), true);
                    }
                }
                if (deleteThisPath) {
                    if (!file.isDirectory()) {
                        file.delete();
                        Timber.e("删除文件: %s", file.getName());
                    } else {
                        if (file.listFiles().length == 0) {
                            file.delete();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 清除图片所有缓存
     * 主要调用这个方法
     */
    @SuppressLint("CheckResult")
    public void clearImageAllCache(AppCompatActivity activity) {
        // clearImageDiskCache(activity);
        // clearImageMemoryCache(activity);
        Observable.just("")
            .doOnNext(picture -> {
                Glide.get(activity).clearDiskCache();
                String ImageExternalCatchDir =
                    activity.getExternalCacheDir() + ExternalPreferredCacheDiskCacheFactory.DEFAULT_DISK_CACHE_DIR;
                deleteFolderFile(ImageExternalCatchDir, true);
            })
            .subscribeOn(Schedulers.io())// 指定在这行代码之前的subscribe在io线程执行
            .observeOn(AndroidSchedulers.mainThread())//指定这行代码之后的subscribe在io线程执行
            .subscribe(picture -> {//图片处理完成后的回调
                Glide.get(activity).clearMemory();
                TipDialog.show(activity, "缓存清除成功！", TipDialog.TYPE.SUCCESS).setCancelable(true);
            }, throwable -> {
                Timber.e(throwable);
                TipDialog.show(activity, "缓存清除失败！", TipDialog.TYPE.ERROR);
            });
    }
}