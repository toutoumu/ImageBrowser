package com.library;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import com.blankj.utilcode.util.FileUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import timber.log.Timber;

/**
 * https://blog.csdn.net/hongye_main/article/details/100521226 Android Q沙盒机制 使用探究
 * 系统文件操作
 * android:requestLegacyExternalStorage="true"
 * Environment.isExternalStorageLegacy() 来判断是传统模式还是 默认是false，也就是Filtered View
 */
public class MediaStoreUtils {
    /**
     * 在Android10中, 系统不允许普通App请求android.permission.READ_PHONE_STATE权限, 故新版App需要取消该动态权限的申请。
     * 当前获取设备唯一ID的方式为使用SSAID, 若获取为空的话则使用UUID.randomUUID().toString()获得一个随机ID并存储起来, 该ID保证唯一, 但App卸载重装之后就会改变
     *
     * @param context
     * @return
     */
    public static String getAndroidId(Context context) {
        @SuppressLint("HardwareIds")
        String id = android.provider.Settings.Secure.getString(context.getContentResolver(),
                                                               android.provider.Settings.Secure.ANDROID_ID);
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    /**
     * 复制文件到沙盒中, 所有版本通用
     *
     * @param context .
     * @param source {@link Uri} 源文件
     * @param toFile {@link String} 目标位置 沙盒文件地址 context.getFilesDir() context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
     * @return 是否成功
     */
    public static boolean copyToPackage(Context context, Uri source, File toFile) {
        try {
            boolean b;
            if (toFile.exists()) {
                b = toFile.delete();
                if (!b) {
                    Timber.e("删除文件失败: %s", toFile.getAbsolutePath());
                    return false;
                }
            }
            if (!toFile.getParentFile().exists()) {
                b = toFile.getParentFile().mkdirs();
                if (!b) {
                    Timber.e("创建文件失败: %s", toFile.getAbsolutePath());
                    return false;
                }
            }
            if (!toFile.exists()) {
                b = toFile.createNewFile();
                if (!b) {
                    Timber.e("创建文件失败: %s", toFile.getAbsolutePath());
                    return false;
                }
            }
            ContentResolver resolver = context.getContentResolver();
            ParcelFileDescriptor fileDescriptor = resolver.openFileDescriptor(source, "r");
            if (fileDescriptor == null) {
                Timber.e("ContentResolver打开文件失败");
                return false;
            }
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileOutputStream outputStream = new FileOutputStream(toFile);
            // 复制
            copy(inputStream, outputStream);
            return true;
        } catch (IOException e) {
            Timber.e(e);
        }
        return false;
    }

    /**
     * 将文件复制到指定的uri
     *
     * @param context .
     * @param source 源文件
     * @param toUri 目标文件
     * @return 成功 | 失败
     */
    public static boolean copyToUri(Context context, File source, Uri toUri) {
        try {
            if (source == null || !source.exists() || source.isDirectory()) {return false;}
            if (toUri == null) {return false;}

            ContentResolver resolver = context.getContentResolver();
            OutputStream outputStream = resolver.openOutputStream(toUri, "w");
            if (outputStream == null) {
                Timber.e("ContentResolver打开输出流失败!");
                return false;
            }
            FileInputStream inputStream = new FileInputStream(source);
            // 复制
            copy(inputStream, outputStream);
            return true;
        } catch (IOException e) {
            Timber.e(e);
        }
        return false;
    }

    /**
     * 通过MediaStore保存，兼容AndroidQ，保存成功自动添加到相册数据库，无需再发送广播告诉系统插入相册
     *
     * @param context context
     * @param sourceFile 源文件
     * @param saveDirName picture子目录
     * @return 成功或者失败
     */
    public static Uri saveImageToPublic(Context context, File sourceFile, String saveDirName) {
        long taken = sourceFile.lastModified() == 0L ? System.currentTimeMillis() : sourceFile.lastModified() / 1000;
        String relativePath = Environment.DIRECTORY_PICTURES;

        if (saveDirName != null && !saveDirName.trim().isEmpty()) {
            relativePath = Environment.DIRECTORY_PICTURES + "/" + saveDirName;
        }

        // 创建一行数据, 填充下面这些列
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.getName());// 存在重名的会自动重命名
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.TITLE, "Image.jpeg");
        values.put(MediaStore.Images.Media.DATE_MODIFIED, taken);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // values.put(MediaStore.Images.Media.DATE_TAKEN, taken);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
        } else {
            // 如果设置了子目录
            if (saveDirName != null && !saveDirName.trim().isEmpty()) {
                File file = new File(getPublicDirAndroid9(context, Environment.DIRECTORY_PICTURES), saveDirName);
                if (!file.exists()) {
                    boolean b = file.mkdirs();
                    if (!b) {
                        return null;
                    }
                }
                String path = file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg";
                values.put(MediaStore.Images.Media.DATA, path);
            }
        }

        // 插入到哪张表
        Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        // 使用下面这个会,java.lang.UnsupportedOperationException: Writing to internal storage is not supported.
        // Uri external = MediaStore.Images.Media.INTERNAL_CONTENT_URI;
        ContentResolver resolver = context.getContentResolver();

        Uri insertUri = resolver.insert(external, values);
        if (insertUri == null) {
            Timber.e("ContentResolver插入数据失败!");
            return null;
        }
        try {
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            OutputStream outputStream = resolver.openOutputStream(insertUri);
            copy(inputStream, outputStream);
            return insertUri;
        } catch (IOException e) {
            Timber.e(e);
        }
        return null;
    }

    /**
     * 通过MediaStore保存，兼容AndroidQ，保存成功自动添加到相册数据库，无需再发送广播告诉系统插入相册
     *
     * @param context context
     * @param sourceFile 源文件
     * @param saveDirName picture子目录
     * @return 成功或者失败
     */
    public static Uri saveVideoToPublic(Context context, File sourceFile, String saveDirName) {
        long taken = sourceFile.lastModified() == 0L ? System.currentTimeMillis() : sourceFile.lastModified() / 1000;
        String relativePath = Environment.DIRECTORY_MOVIES;

        if (saveDirName != null && !saveDirName.trim().isEmpty()) {
            relativePath = Environment.DIRECTORY_MOVIES + "/" + saveDirName;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DESCRIPTION, "This is an video");
        values.put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.getName());// 存在重名的会自动重命名
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.TITLE, "Video.mp4");
        values.put(MediaStore.Video.Media.DATE_MODIFIED, taken);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // values.put(MediaStore.Video.Media.DATE_TAKEN, taken);
            values.put(MediaStore.Video.Media.RELATIVE_PATH, relativePath);
        } else {
            // 如果设置了子目录
            if (saveDirName != null && !saveDirName.trim().isEmpty()) {
                File file = new File(getPublicDirAndroid9(context, Environment.DIRECTORY_MOVIES), saveDirName);
                if (!file.exists()) {
                    boolean b = file.mkdirs();
                    if (!b) {
                        return null;
                    }
                }
                String path = file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp4";
                values.put(MediaStore.Video.Media.DATA, path);
            }
        }

        Uri external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = context.getContentResolver();

        Uri insertUri = resolver.insert(external, values);
        if (insertUri == null) {
            Timber.e("ContentResolver插入数据失败!");
            return null;
        }
        try {
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            OutputStream outputStream = resolver.openOutputStream(insertUri);
            copy(inputStream, outputStream);
            return insertUri;
        } catch (IOException e) {
            Timber.e(e);
        }
        return null;
    }

    /**
     * 通过MediaStore保存，兼容AndroidQ，保存成功自动添加到相册数据库，无需再发送广播告诉系统插入相册
     *
     * @param context context
     * @param sourceFile 源文件
     * @param saveDirName picture子目录
     * @return 成功或者失败
     */
    public static Uri saveAudioToPublic(Context context, File sourceFile, String saveDirName) {
        long taken = sourceFile.lastModified() == 0L ? System.currentTimeMillis() : sourceFile.lastModified() / 1000;
        String relativePath = Environment.DIRECTORY_MUSIC;

        if (saveDirName != null && !saveDirName.trim().isEmpty()) {
            relativePath = Environment.DIRECTORY_MUSIC + "/" + saveDirName;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, sourceFile.getName());// 存在重名的会自动重命名
        values.put(MediaStore.Audio.Media.MIME_TYPE, getMIMEType(sourceFile.getName()));
        values.put(MediaStore.Audio.Media.TITLE, "Audio.mp3");
        values.put(MediaStore.Audio.Media.DATE_MODIFIED, taken);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // values.put(MediaStore.Video.Media.DATE_TAKEN, taken);
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath);
        } else {
            // 如果设置了子目录
            if (saveDirName != null && !saveDirName.trim().isEmpty()) {
                File file = new File(getPublicDirAndroid9(context, Environment.DIRECTORY_MUSIC), saveDirName);
                if (!file.exists()) {
                    boolean b = file.mkdirs();
                    if (!b) {
                        return null;
                    }
                }
                String path = file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".mp3";
                values.put(MediaStore.Video.Media.DATA, path);
            }
        }

        Uri external = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = context.getContentResolver();

        Uri insertUri = resolver.insert(external, values);
        if (insertUri == null) {
            Timber.e("ContentResolver插入数据失败!");
            return null;
        }
        try {
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
            OutputStream outputStream = resolver.openOutputStream(insertUri);
            copy(inputStream, outputStream);
            return insertUri;
        } catch (IOException e) {
            Timber.e(e);
        }
        return null;
    }

    /**
     * 保存到download目录
     *
     * @param context 上下文
     * @param sourceFile 源文件地址
     * @param saveDirName 子目录名称
     */
    public static Uri saveFileToDownload(Context context, File sourceFile, String saveDirName) {
        // android 10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            long taken = sourceFile.lastModified() == 0L ? System.currentTimeMillis() : sourceFile.lastModified() / 1000;
            String relativePath = Environment.DIRECTORY_DOWNLOADS;
            if (saveDirName != null && !saveDirName.trim().isEmpty()) {
                relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + saveDirName;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.getName());// 存在重名的会自动重命名
            values.put(MediaStore.Downloads.MIME_TYPE, getMIMEType(sourceFile.getName()));
            values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);
            values.put(MediaStore.Downloads.DATE_MODIFIED, taken);
            // values.put(MediaStore.Downloads.DATE_TAKEN, taken);

            ContentResolver resolver = context.getContentResolver();
            Uri insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (insertUri == null) {
                return null;
            }

            try {
                BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
                OutputStream outputStream = resolver.openOutputStream(insertUri);
                copy(inputStream, outputStream);
                return insertUri;
            } catch (IOException e) {
                Timber.e(e);
            }
            return null;
        }

        String download = getPublicDirAndroid9(context, Environment.DIRECTORY_DOWNLOADS);
        File toFileDir = new File(download, saveDirName);
        if (!toFileDir.exists()) {
            boolean b = toFileDir.mkdirs();
            if (!b) {
                return null;
            }
        }
        File toFile = new File(toFileDir, System.currentTimeMillis() + FileUtils.getFileExtension(sourceFile));
        Uri toUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0及以上
            String authority = context.getPackageName() + ".provider";
            toUri = FileProvider.getUriForFile(context, authority, toFile);
        } else {
            toUri = Uri.fromFile(toFile);
        }

        boolean b = copyToUri(context, sourceFile, toUri);
        if (b) {
            return toUri;
        }
        return null;
    }

    /**
     * 保存安装包到公共存储
     *
     * @param context 上下文
     * @param sourceFile 源文件地址
     * @param saveDirName 子目录名称
     * todo 测试Android 10 是否可以从沙箱文件安装
     */
    public static Uri saveApkToPublic(Context context, File sourceFile, String saveDirName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            long taken = sourceFile.lastModified() == 0L ? System.currentTimeMillis() : sourceFile.lastModified() / 1000;
            String relativePath = Environment.DIRECTORY_DOWNLOADS;
            if (saveDirName != null && !saveDirName.trim().isEmpty()) {
                relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + saveDirName;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, System.currentTimeMillis() + ".apk");// 存在重名的会自动重命名
            values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
            values.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);
            values.put(MediaStore.Downloads.DATE_MODIFIED, taken);
            // values.put(MediaStore.Downloads.DATE_TAKEN, taken);

            Uri external = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = context.getContentResolver();
            Uri insertUri = resolver.insert(external, values);
            if (insertUri == null) {
                Timber.e("ContentResolver插入数据失败!");
                return null;
            }

            try {
                BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
                OutputStream outputStream = resolver.openOutputStream(insertUri);
                copy(inputStream, outputStream);
                return insertUri;
            } catch (IOException e) {
                Timber.e(e);
            }
            return null;
        }

        String download = getPublicDirAndroid9(context, Environment.DIRECTORY_DOWNLOADS);
        File toFileDir = new File(download, saveDirName);
        if (!toFileDir.exists()) {
            boolean b = toFileDir.mkdirs();
            if (!b) {
                return null;
            }
        }
        File toFile = new File(toFileDir, System.currentTimeMillis() + ".apk");
        Uri toUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0及以上
            String authority = context.getPackageName() + ".provider";
            toUri = FileProvider.getUriForFile(context, authority, toFile);
        } else {
            toUri = Uri.fromFile(toFile);
        }

        boolean b = copyToUri(context, sourceFile, toUri);
        if (b) {
            return toUri;
        }
        return null;
    }

    /**
     * 创建图片地址uri,用于保存拍照后的照片 Android 10以后使用这种方法
     */
    public static Uri createImageUri(Context context) {
        String status = Environment.getExternalStorageState();
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()) {
            return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return context.getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    /**
     * 创建图片URI用于写入文件
     *
     * @param context ..
     * @param toFile 需要写入的文件
     * @return ..
     */
    public static Uri createImageUri(Context context, File toFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {//10.0+
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DATA, toFile.getAbsolutePath());
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, toFile.getName());
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            String status = Environment.getExternalStorageState();
            // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
            if (status.equals(Environment.MEDIA_MOUNTED) || !Environment.isExternalStorageRemovable()) {
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            } else {
                return context.getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, contentValues);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0及以上
            // 9.0 及以下会将文件写入到 toFile
            String authority = context.getPackageName() + ".provider";
            return FileProvider.getUriForFile(context, authority, toFile);
        } else {
            // 9.0 及以下会将文件写入到 toFile
            return Uri.fromFile(toFile);
        }
    }

    /**
     * 根据path获取文件Uri
     *
     * @param context 上下文
     * @param path 路径
     * @return 文件路径
     */
    public static Uri getImageContentUri(Context context, String path) {
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                           new String[] { MediaStore.Images.Media._ID },
                                                           MediaStore.Images.Media.DATA + "=? ",
                                                           new String[] { path },
                                                           null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            // 如果图片不在手机的共享图片数据库，就先把它插入。
            if (new File(path).exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, path);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * 获取沙箱路径
     * 适用于Android任何版本
     *
     * @param dir {@link Environment#DIRECTORY_PICTURES},{@link Environment#DIRECTORY_DOWNLOADS}
     */
    public static String getPackageFilesDir(Context context, String dir) {
        String cachePath = null;
        // 优先使用外部存储
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
            || !Environment.isExternalStorageRemovable()) {
            //此目录下的是外部存储下的私有的fileName目录
            //mnt/sdcard/Android/data/com.my.app/files/dir
            cachePath = context.getExternalFilesDir(dir).getAbsolutePath();
        } else {
            //data/data/com.my.app/files
            cachePath = context.getFilesDir().getAbsolutePath() + "/" + dir;
        }
        File file = new File(cachePath);
        if (!file.exists()) {
            boolean b = file.mkdirs();
            if (!b) {
                return "";
            }
        }
        //mnt/sdcard/Android/data/com.my.app/files/dir
        return file.getAbsolutePath();
    }

    /**
     * 获取沙箱路径
     * 适用于Android任何版本
     *
     * @param dir 子目录
     */
    public static String getPackageCacheDir(Context context, String dir) {
        String cachePath = null;
        if (dir == null) {
            dir = "";
        }
        // 优先使用外部存储
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
            || !Environment.isExternalStorageRemovable()) {
            //此目录下的是外部存储下的私有的fileName目录
            //mnt/sdcard/Android/data/com.my.app/files/dir
            cachePath = context.getExternalCacheDir().getAbsolutePath() + "/" + dir;
        } else {
            //data/data/com.my.app/files
            cachePath = context.getCacheDir().getAbsolutePath() + "/" + dir;
        }
        File file = new File(cachePath);
        if (!file.exists()) {
            boolean b = file.mkdirs();
            if (!b) {
                return "";
            }
        }
        //mnt/sdcard/Android/data/com.my.app/files/dir
        return file.getAbsolutePath();
    }

    /**
     * 获取公共目录，如果公共目录不存在则获取外部存储目录(也就是Android 10 所说的沙箱目录)
     * 注意，只适合android9.0以下的
     *
     * @param fileDir {@link Environment#DIRECTORY_PICTURES},{@link Environment#DIRECTORY_DOWNLOADS}
     */
    public static String getPublicDirAndroid9(final Context context, final String fileDir) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            throw new UnsupportedOperationException("Android 10 不支持获取公共存储目录.");
        }*/
        String filePath = null;
        // 优先使用外部存储
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
            || !Environment.isExternalStorageRemovable()) {
            filePath = Environment.getExternalStoragePublicDirectory(fileDir).getAbsolutePath();
        } else {
            filePath = context.getExternalFilesDir(fileDir).getAbsolutePath();
        }
        File file = new File(filePath);
        if (!file.exists()) {
            boolean b = file.mkdirs();
            if (b) {
                return "";
            }
        }
        return file.getAbsolutePath();
    }

    /**
     * Android 8.0 需要以下权限
     * <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     * 自Android N开始，是通过FileProvider共享相关文件，但是Android Q对公有目录 File API进行了限制
     * 从代码上看，又变得和以前低版本一样了，只是必须加上权限代码Intent.FLAG_GRANT_READ_URI_PERMISSION
     *
     * @param mFilePath 这个路径必须是公共存储路径
     */
    public static void installApk(Context context, String mFilePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //适配Android Q,注意mFilePath是通过ContentResolver得到的，上述有相关代码
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(mFilePath), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
            return;
        }

        File file = new File(mFilePath);
        if (!file.exists()) { return; }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName() + ".provider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(intent);
    }

    /**
     * 根据文件后缀名获得对应的MIME类型
     *
     * @param fileName 文件名，需要包含后缀.xml类似这样的
     */
    public static String getMIMEType(String fileName) {
        String type = "*/*";
        if (fileName == null || fileName.trim().isEmpty()) {
            return type;
        }
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /* 获取文件的后缀名*/
        String end = fileName.substring(dotIndex).toLowerCase();
        if (end.equals("")) return type;
        // 在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (String[] strings : MIME_MapTable) {
            if (end.equals(strings[0])) { type = strings[1]; }
        }
        return type;
    }

    /**
     * 获取文件的mimetype类型
     */
    private static final String[][] MIME_MapTable = {
        //{后缀名，MIME类型}
        { ".3gp", "video/3gpp" },
        { ".apk", "application/vnd.android.package-archive" },
        { ".asf", "video/x-ms-asf" },
        { ".avi", "video/x-msvideo" },
        { ".bin", "application/octet-stream" },
        { ".bmp", "image/bmp" },
        { ".c", "text/plain" },
        { ".class", "application/octet-stream" },
        { ".conf", "text/plain" },
        { ".cpp", "text/plain" },
        { ".doc", "application/msword" },
        { ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
        { ".xls", "application/vnd.ms-excel" },
        { ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
        { ".exe", "application/octet-stream" },
        { ".gif", "image/gif" },
        { ".gtar", "application/x-gtar" },
        { ".gz", "application/x-gzip" },
        { ".h", "text/plain" },
        { ".htm", "text/html" },
        { ".html", "text/html" },
        { ".jar", "application/java-archive" },
        { ".java", "text/plain" },
        { ".jpeg", "image/jpeg" },
        { ".jpg", "image/jpeg" },
        { ".js", "application/x-javascript" },
        { ".log", "text/plain" },
        { ".m3u", "audio/x-mpegurl" },
        { ".m4a", "audio/mp4a-latm" },
        { ".m4b", "audio/mp4a-latm" },
        { ".m4p", "audio/mp4a-latm" },
        { ".m4u", "video/vnd.mpegurl" },
        { ".m4v", "video/x-m4v" },
        { ".mov", "video/quicktime" },
        { ".mp2", "audio/x-mpeg" },
        { ".mp3", "audio/x-mpeg" },
        { ".mp4", "video/mp4" },
        { ".mpc", "application/vnd.mpohun.certificate" },
        { ".mpe", "video/mpeg" },
        { ".mpeg", "video/mpeg" },
        { ".mpg", "video/mpeg" },
        { ".mpg4", "video/mp4" },
        { ".mpga", "audio/mpeg" },
        { ".msg", "application/vnd.ms-outlook" },
        { ".ogg", "audio/ogg" },
        { ".pdf", "application/pdf" },
        { ".png", "image/png" },
        { ".pps", "application/vnd.ms-powerpoint" },
        { ".ppt", "application/vnd.ms-powerpoint" },
        { ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation" },
        { ".prop", "text/plain" },
        { ".rc", "text/plain" },
        { ".rmvb", "audio/x-pn-realaudio" },
        { ".rtf", "application/rtf" },
        { ".sh", "text/plain" },
        { ".tar", "application/x-tar" },
        { ".tgz", "application/x-compressed" },
        { ".txt", "text/plain" },
        { ".wav", "audio/x-wav" },
        { ".wma", "audio/x-ms-wma" },
        { ".wmv", "audio/x-ms-wmv" },
        { ".wps", "application/vnd.ms-works" },
        { ".xml", "text/plain" },
        { ".z", "application/x-compress" },
        { ".zip", "application/x-zip-compressed" },
        { "", "*/*" }
    };

    /**
     * 输入流复制到输出流
     *
     * @param inputStream 输入
     * @param outputStream 输出
     * @throws IOException 异常信息
     */
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[1024 * 4];
            int byteCount = 0;
            while ((byteCount = inputStream.read(buffer)) != -1) {  // 循环从输入流读取 buffer字节
                outputStream.write(buffer, 0, byteCount);        // 将读取的输入流写入到输出流
            }
            outputStream.flush();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
        }
    }
}
