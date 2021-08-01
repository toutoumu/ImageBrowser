package com.library.glide;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Size;
import androidx.documentfile.provider.DocumentFile;
import androidx.heifwriter.HeifWriter;
import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.library.App;
import com.library.BitmapUtils;
import com.library.media.Picture;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import timber.log.Timber;

/**
 * https://blog.csdn.net/lengshang624/article/details/81510160
 */
@SuppressLint("ObsoleteSdkInt")
public class ConcealUtil {

    private static Crypto crypto = null;
    private static Entity mEntity = null;

    // 保存文件的扩展名
    public static final String STAFF = ".conceal";
    // 根目录名称
    private static final String ROOT_DIR_NAME = ".album";
    // 保存图片文件夹名称
    private static final String PICTURE_DIR_NAME = "图片";
    // 保存视频文件夹名称
    private static final String VIDEO_DIR_NAME = "视频";
    // 垃圾桶文件夹名称
    public static final String TRASH_DIR_NAME = "回收站";
    // 导出文件夹名称
    public static final String EXPORT_DIR_NAME = "导出文件";
    // 将要导入的文件夹名称
    public static final String IMPORT_DIR_NAME = "导入文件夹";

    // 内置存储卡,根路径
    public static File ROOT_DIR = new File(Environment.getExternalStorageDirectory(), ROOT_DIR_NAME);
    // 其他需要展示的文件夹
    public static File ROOT_DIR1 = new File(Environment.getExternalStorageDirectory(), ROOT_DIR_NAME);
    // 保存图片文件路径
    public static File PICTURE_DIR = new File(ROOT_DIR, PICTURE_DIR_NAME);
    // 保存视频文件路径
    public static File VIDEO_DIR = new File(ROOT_DIR, VIDEO_DIR_NAME);

    // 导出路径
    private static File EXPORT_DIR = null;
    // 导入路径
    public static File IMPORT_DIR = null;
    // 垃圾桶路径
    private static File TRASH_DIR = null;

    // 是否使用外置存储卡
    public static boolean NEED_SDCARD = false;
    // 外置存储卡目录
    public static File SDCARD_Root_DIR = null;

    /**
     * 初始化
     *
     * @param context context
     * @param pwd 密码
     */
    public static boolean init(Context context, String pwd) {
        initDirs(context.getApplicationContext());
        mEntity = Entity.create(pwd);
        crypto = AndroidConceal.get().createDefaultCrypto(new MyKeyChain(context.getApplicationContext()));

        if (!crypto.isAvailable()) {
            destroy();
            return false;
        }
        return true;
    }

    /**
     * 初始化各种目录
     *
     * @param context {@link Context}
     */
    private static void initDirs(Context context) {
        ROOT_DIR1 = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), ROOT_DIR_NAME);
        PICTURE_DIR = new File(ROOT_DIR, PICTURE_DIR_NAME);
        VIDEO_DIR = new File(ROOT_DIR, VIDEO_DIR_NAME);

        boolean b = false;
        if (!ROOT_DIR1.exists()) {
            b = ROOT_DIR1.mkdirs();
        }
        // 内置存储卡
        // 内置存储卡---相册根目录
        if (!ROOT_DIR.exists()) {
            b = ROOT_DIR.mkdirs();
        }
        // 内置存储卡---图片目录
        if (!PICTURE_DIR.exists()) {
            b = PICTURE_DIR.mkdirs();
        }
        // 内置存储卡---视频目录
        /*if (!VIDEO_DIR.exists()) {
            b = VIDEO_DIR.mkdirs();
        }*/

        // 外置存储卡
        if (NEED_SDCARD) {
            String storagePath = FileUtils.getStoragePath(context.getApplicationContext(), true);
            if (storagePath != null) {// 如果外置存储卡存在
                // 外置存储卡---相册根目录
                SDCARD_Root_DIR = new File(storagePath, ROOT_DIR_NAME);
                if (!SDCARD_Root_DIR.exists()) {
                    b = SDCARD_Root_DIR.mkdirs();
                }

                // 外置存储卡---导出目录
                EXPORT_DIR = new File(SDCARD_Root_DIR, EXPORT_DIR_NAME);
                if (!EXPORT_DIR.exists()) {
                    b = EXPORT_DIR.mkdirs();
                }

                // 外置存储卡---导入目录
                IMPORT_DIR = new File(SDCARD_Root_DIR, IMPORT_DIR_NAME);
                if (!IMPORT_DIR.exists()) {
                    b = IMPORT_DIR.mkdirs();
                }

                //外置存储卡---垃圾桶
                TRASH_DIR = new File(SDCARD_Root_DIR, TRASH_DIR_NAME);
                if (!TRASH_DIR.exists()) {
                    b = TRASH_DIR.mkdirs();
                }
            }
        }

        // 内置存储卡---文件导出目录,如果当前没有在SD卡创建该目录,那么在内置SD卡创建
        if (EXPORT_DIR == null || !EXPORT_DIR.exists()) {
            EXPORT_DIR = new File(ROOT_DIR, EXPORT_DIR_NAME);
            b = EXPORT_DIR.mkdirs();
        }

        // 内置存储卡---导入目录
        IMPORT_DIR = new File(ROOT_DIR, IMPORT_DIR_NAME);
        if (!IMPORT_DIR.exists()) {
            b = IMPORT_DIR.mkdirs();
        }

        // 内置存储卡---垃圾桶,如果当前没有在SD卡创建该目录,那么在内置SD卡创建
        if (TRASH_DIR == null || !TRASH_DIR.exists()) {
            TRASH_DIR = new File(ROOT_DIR, TRASH_DIR_NAME);
            b = TRASH_DIR.mkdirs();
        }

        Timber.e("去掉警告%s", b);
    }

    /**
     * 注销
     */
    public static void destroy() {
        mEntity = null;
        crypto = null;
    }

    private static OutputStream getCipherOutputStream(File file)
        throws IOException, KeyChainException, CryptoInitializationException {
        OutputStream fileStream = new FileOutputStream(file);
        return getCipherOutputStream(fileStream);
    }

    private static OutputStream getCipherOutputStream(OutputStream fileStream)
        throws KeyChainException, CryptoInitializationException, IOException {
        check();
        return crypto.getCipherOutputStream(fileStream, mEntity);
    }

    public static InputStream getCipherInputStream(String file)
        throws KeyChainException, CryptoInitializationException, IOException {
        return getCipherInputStream(new File(file));
    }

    public static InputStream getCipherInputStream(File file)
        throws IOException, KeyChainException, CryptoInitializationException {
        check();
        InputStream inputStream = new FileInputStream(file);
        return crypto.getCipherInputStream(inputStream, mEntity);
    }

    /**
     * 将字节数组加密保存到{@link ConcealUtil#PICTURE_DIR}
     *
     * @param data byte[]字节数据
     */
    public static void encryptedFile(byte[] data) throws KeyChainException, CryptoInitializationException, IOException {
        long start = System.currentTimeMillis();
        String fileName = System.currentTimeMillis() + STAFF;
        File toFile = new File(PICTURE_DIR, fileName);
        Timber.e("图片将要保存的路径 :%s", toFile.getAbsolutePath());
        encryptedFile(data, toFile, true);
        start = System.currentTimeMillis() - start;
        Timber.e("jpeg编码用时: %s, 文件大小: %s", start, toFile.length());
    }

    /**
     * 将字节数组加密保存到{@link ConcealUtil#PICTURE_DIR}
     *
     * @param data byte[]字节数据
     * @param orientation 旋转角度
     */
    public static void encryptedFileToHEIF(byte[] data, Size size, int orientation) throws Exception {

        long start = System.currentTimeMillis();

        // 准备bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, new BitmapFactory.Options());
        Timber.e("heif宽度: %s ,高度: %s ,旋转角度:%s,", bitmap.getWidth(), bitmap.getHeight(), orientation);

        // 生成 HIEC 图片
        File tempFile = new File(PICTURE_DIR, start + STAFF + ".temp");
        HeifWriter writer = new HeifWriter
            .Builder(tempFile.getAbsolutePath(), size.getWidth(), size.getHeight(), HeifWriter.INPUT_MODE_BITMAP)
            .setGridEnabled(true)
            .setMaxImages(1)
            .setRotation(orientation)
            .setQuality(95)
            .build();
        writer.start();
        writer.addBitmap(bitmap);
        // writer.addExifData(0, array, 0, array.length);
        // writer.addYuvBuffer(ImageFormat.YUV_420_888, data);
        writer.stop(0);
        writer.close();

        // 加密文件,并删除临时文件
        File toFile = new File(PICTURE_DIR, start + STAFF);
        encryptedFile(new FileInputStream(tempFile), toFile, false);
        boolean delete = tempFile.delete();
        if (delete) {
            Timber.e("临时文件删除成功");
        }

        start = System.currentTimeMillis() - start;
        Timber.e("heif编码用时: %s, 文件大小: %s", start, toFile.length());
    }

    /**
     * 保存bitmap形式的图片
     *
     * @param jpegFile jpeg图片地址
     * @param toPath 保存到哪里
     * @throws Exception .
     */
    public static void encryptedToHEIF(String jpegFile, String toPath) throws Exception {

        long start = System.currentTimeMillis();

        // 准备bitmap
        int orientation = BitmapUtils.getRotation(jpegFile);
        Bitmap bitmap = BitmapFactory.decodeFile(jpegFile, new BitmapFactory.Options());
        Timber.e("旋转角度:%s, 宽度: %s ,高度: %s", orientation, bitmap.getWidth(), bitmap.getHeight());

        // 生成 HIEC 图片
        File tempFile = new File(PICTURE_DIR, start + STAFF + ".temp");
        HeifWriter writer = new HeifWriter
            .Builder(tempFile.getAbsolutePath(), bitmap.getWidth(), bitmap.getHeight(), HeifWriter.INPUT_MODE_BITMAP)
            .setGridEnabled(true)
            .setMaxImages(1)
            .setRotation(orientation)
            .setQuality(95)
            .build();
        writer.start();
        writer.addBitmap(bitmap);
        // writer.addExifData(0, array, 0, array.length);
        // writer.addYuvBuffer(ImageFormat.YUV_420_888, data);
        writer.stop(0);
        writer.close();

        // 加密文件,并删除临时文件
        File toFile = new File(toPath);
        encryptedFile(new FileInputStream(tempFile), toFile, true);
        boolean delete = tempFile.delete();
        if (delete) {
            Timber.e("HEIF临时文件删除成功");
        }

        start = System.currentTimeMillis() - start;
        long orgSize = new File(jpegFile).length();
        long currentSize = toFile.length();
        float radio = (float) orgSize / (float) currentSize;
        Timber.e("编码用时1: %s ,原始文件大小%s ,文件大小: %s ,压缩比例: %s", start, orgSize, currentSize, radio);
    }

    /**
     * yuv 数据存储
     *
     * @param data
     * @param size
     * @param orientation
     * @throws Exception
     */
    public static void encryptedYUVToHEIF(byte[] data, Size size, int orientation)
        throws Exception {

        long start = System.currentTimeMillis();

        String fileName = System.currentTimeMillis() + STAFF;

        // 临时文件
        File tempFile = new File(PICTURE_DIR, fileName + ".temp");

        // 生成 HIEC 图片
        HeifWriter writer = new HeifWriter
            .Builder(tempFile.getAbsolutePath(), size.getWidth(), size.getHeight(), HeifWriter.INPUT_MODE_BUFFER)
            .setGridEnabled(true)
            .setMaxImages(1)
            .setRotation(orientation)
            .setQuality(95)
            .build();
        writer.start();
        // writer.addBitmap(bitmap);
        // writer.addExifData(0, array, 0, array.length);
        writer.addYuvBuffer(ImageFormat.YUV_420_888, data);
        writer.stop(0);
        writer.close();

        // 加密文件,并删除临时文件
        File toFile = new File(PICTURE_DIR, fileName);
        encryptedFile(new FileInputStream(tempFile), toFile, false);
        boolean delete = tempFile.delete();
        if (delete) {
            Timber.e("临时文件删除成功");
        }

        start = System.currentTimeMillis() - start;
        Timber.e("heif编码用时: %s, 文件大小: %s", start, toFile.length());
    }

    /**
     * 加密保存到文件
     *
     * @param data 数据
     * @param toFile 保存后的文件
     * @param cover 是否覆盖
     */
    private static void encryptedFile(byte[] data, File toFile, boolean cover)
        throws KeyChainException, CryptoInitializationException, IOException {
        if (!cover && (toFile.exists() || toFile.isDirectory())) {
            throw new RuntimeException("文件已经存在");
        }
        // 如果可以直接读写
        if (FileUtils.isWritable(toFile)) {
            OutputStream outStream = getCipherOutputStream(toFile);
            if (outStream != null) {
                outStream.write(data);
                outStream.flush();
                outStream.close();
            }
        } else {
            OutputStream outStream = null;
            Context context = App.getAppContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Storage Access Framework
                DocumentFile targetDocument = FileUtils.getDocumentFile(context, toFile, false, true, true);
                if (targetDocument != null) {
                    outStream = context.getContentResolver().openOutputStream(targetDocument.getUri());
                }
            } else {
                // Workaround for Kitkat ext SD card
                Uri uri = FileUtils.getUriFromFile(context, toFile.getAbsolutePath());
                if (uri != null) {
                    outStream = context.getContentResolver().openOutputStream(uri);
                }
            }

            if (outStream != null) {
                outStream = getCipherOutputStream(outStream);
                outStream.write(data);
                outStream.flush();
                outStream.close();
            }
        }
    }

    /**
     * 加密保存输入流
     *
     * @param inputStream {@link InputStream}
     * @param toFile 保存后的文件
     * @param cover 是否覆盖
     */
    public static void encryptedFile(InputStream inputStream, File toFile, boolean cover)
        throws IOException, KeyChainException, CryptoInitializationException {
        if (!cover && (toFile.exists() || toFile.isDirectory())) {
            throw new RuntimeException("文件已经存在");
        }
        int read;
        byte[] buffer = new byte[1024];

        // 如果可以直接读写
        if (FileUtils.isWritable(toFile)) {
            OutputStream outputStream = getCipherOutputStream(toFile);
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        } else {
            OutputStream outStream = null;
            Context context = App.getAppContext();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Storage Access Framework
                DocumentFile targetDocument = FileUtils.getDocumentFile(context, toFile, false, true, true);
                if (targetDocument != null) {
                    outStream = context.getContentResolver().openOutputStream(targetDocument.getUri());
                }
            } else {
                // Workaround for Kitkat ext SD card
                Uri uri = FileUtils.getUriFromFile(context, toFile.getAbsolutePath());
                if (uri != null) {
                    outStream = context.getContentResolver().openOutputStream(uri);
                }
            }

            if (outStream != null) {
                outStream = getCipherOutputStream(outStream);
                // Both for SAF and for Kitkat, write to output stream.
                while ((read = inputStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, read);
                }

                inputStream.close();
                outStream.flush();
                outStream.close();
            }
        }
    }

    /**
     * (解密)导出指定文件
     *
     * @param path 需要导出的文件
     */
    public static void exPore(String path) throws KeyChainException, CryptoInitializationException, IOException {
        String toFileName = path.substring(path.lastIndexOf("/") + 1).replace(STAFF, ".jpg");
        File toFile = new File(EXPORT_DIR, toFileName);
        exPore(path, toFile, false);
    }

    /**
     * (解密)导出指定文件
     *
     * @param path 需要导出的加密文件
     * @param toFile 解密后的文件
     * @param cover 是否覆盖
     */
    public static void exPore(String path, File toFile, boolean cover)
        throws KeyChainException, CryptoInitializationException, IOException {
        if (!cover && (toFile.exists() || toFile.isDirectory())) {
            throw new RuntimeException("文件已经存在");
        }
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            throw new RuntimeException("需要导出的文件不存在");
        }
        InputStream inputStream = getCipherInputStream(path);
        FileUtils.writeFile(App.getAppContext(), inputStream, toFile);
    }

    /**
     * 导入未加密文件
     *
     * @param file 需要导入的文件
     * @param dir 导入到哪个目录 /data/dd
     */
    public static void importFile(File file, String dir)
        throws IOException, KeyChainException, CryptoInitializationException {
        if (file == null || !file.exists() || file.isDirectory()) {
            throw new RuntimeException("需要导入的文件不存在");
        }
        String staff = file.getName().substring(file.getName().lastIndexOf("."));
        encryptedFile(new FileInputStream(file), new File(dir, file.getName().replace(staff, STAFF)), false);
    }

    /**
     * <pre>删除文件
     * 1. 如果是垃圾桶文件那么直接删除
     * 2. 如果不是垃圾桶文件,移动到垃圾桶
     * </pre>
     *
     * @param mPicture 文件路径
     */
    public static boolean delete(Picture mPicture) throws IOException {
        File oldFile = new File(mPicture.getFilePath());
        // 如果文件不存在,或者是文件夹
        if (!oldFile.exists() || oldFile.isDirectory()) { return false; }

        // 如果是垃圾桶文件,直接删除
        if (mPicture.getAlbumName().equals(TRASH_DIR_NAME)) {
            return FileUtils.deleteFile(App.getAppContext(), oldFile);
        }

        // 如果不是垃圾桶文件,移动到垃圾桶
        // File newFile = new File(mPicture.getFilePath().replace(mPicture.getAlbumName(), TRASH_DIR_NAME));
        File newFile = new File(TRASH_DIR, mPicture.getFileName());
        if (!newFile.exists()) {
            return FileUtils.moveFile(App.getAppContext(), oldFile, newFile);
        }
        return false;
    }

    /**
     * 移动到指定目录,首先尝试renameFile不成功再moveFile
     *
     * @param picture {@link Picture} 需要移动的文件
     * @param toPath 目标目录绝对路径 /data/data/xxx
     * @return .
     */
    public static boolean moveFile(Picture picture, String toPath) throws IOException {
        File oldFile = new File(picture.getFilePath());
        if (oldFile.exists() && oldFile.isFile()) {
            File newFile = new File(toPath, picture.getFileName());
            if (!newFile.exists()) {
                return FileUtils.moveFile(App.getAppContext(), oldFile, newFile);
            }
        }
        return false;
    }

    private static void check() {
        if (crypto == null || mEntity == null) {
            throw new RuntimeException("请初始化.....");
        }
    }
}


