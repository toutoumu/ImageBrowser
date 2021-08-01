package com.library.glide;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.wordpress.passcodelock.R;
import timber.log.Timber;

/**
 * 兼容SD卡读写的文件操作
 * 参考项目
 * https://github.com/jeisfeld/Augendiagnose
 */
@SuppressLint("ObsoleteSdkInt")
public class FileUtils {
    /**
     * The name of the primary volume (LOLLIPOP).
     */
    private static final String PRIMARY_VOLUME_NAME = "prdimary";

    public static boolean writeFile(Context context, @NonNull final InputStream inStream, @NonNull final File target) {
        OutputStream outStream = null;
        try {
            // First try the normal way
            if (isWritable(target)) {
                OutputStream outputStream = new FileOutputStream(target);
                int read;
                byte[] buffer = new byte[1024];
                while ((read = inStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    DocumentFile targetDocument = getDocumentFile(context, target, false, true, true);
                    if (targetDocument != null) {
                        outStream = context.getContentResolver().openOutputStream(targetDocument.getUri());
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    Uri uri = getUriFromFile(context, target.getAbsolutePath());
                    if (uri != null) {
                        outStream = context.getContentResolver().openOutputStream(uri);
                    }
                } else {
                    return false;
                }

                if (outStream != null) {
                    // Both for SAF and for Kitkat, write to output stream.
                    byte[] buffer = new byte[4096]; // MAGIC_NUMBER
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e);
            return false;
        } finally {
            try {
                inStream.close();
            } catch (Exception e) {
                // ignore exception
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (Exception e) {
                // ignore exception
            }
        }
        return true;
    }

    /**
     * Copy a file. The target file may even be on external SD card for Kitkat.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    @SuppressWarnings("null")
    public static boolean copyFile(Context context, @NonNull final File source, @NonNull final File target)
        throws IOException {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(source);

            // First try the normal way
            if (isWritable(target)) {
                // standard way
                outStream = new FileOutputStream(target);
                inChannel = inStream.getChannel();
                outChannel = ((FileOutputStream) outStream).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Storage Access Framework
                    DocumentFile targetDocument = getDocumentFile(context, target, false, true, true);
                    if (targetDocument != null) {
                        outStream = context.getContentResolver().openOutputStream(targetDocument.getUri());
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Workaround for Kitkat ext SD card
                    Uri uri = getUriFromFile(context, target.getAbsolutePath());
                    if (uri != null) {
                        outStream = context.getContentResolver().openOutputStream(uri);
                    }
                } else {
                    return false;
                }

                if (outStream != null) {
                    // Both for SAF and for Kitkat, write to output stream.
                    byte[] buffer = new byte[4096]; // MAGIC_NUMBER
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception e) {
                // ignore exception
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (Exception e) {
                // ignore exception
            }
            try {
                if (inChannel != null) {
                    inChannel.close();
                }
            } catch (Exception e) {
                // ignore exception
            }
            try {
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (Exception e) {
                // ignore exception
            }
        }
        return true;
    }

    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file the file to be deleted.
     * @return True if successfully deleted.
     */
    public static boolean deleteFile(Context context, @NonNull final File file) {
        // First try the normal deletion.
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(context, file, false, true, false);
            return document != null && document.delete();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();

            try {
                Uri uri = getUriFromFile(context, file.getAbsolutePath());
                if (uri != null) {
                    resolver.delete(uri, null, null);
                }
                return !file.exists();
            } catch (Exception e) {
                Timber.e(e);
                return false;
            }
        }

        return !file.exists();
    }

    /**
     * Move a file. The target file may even be on external SD card.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    public static boolean moveFile(Context context, @NonNull final File source, @NonNull final File target)
        throws IOException {
        // First try the normal rename.
        boolean success = source.renameTo(target);

        if (!success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && source.getParent()
            .equals(target.getParent())) {
            // Storage Access Framework
            DocumentFile sourceDocument = null;
            sourceDocument = getDocumentFile(context, source, false, true, false);
            success = sourceDocument.renameTo(target.getName());
        }

        if (!success) {
            success = copyFile(context, source, target);

            if (success && target.length() < source.length()) {
                Timber.w("Lengh reduced from "
                             + source.length()
                             + " to "
                             + target.length()
                             + " while copying file "
                             + source.getName()
                             + ". Trying once more.");
                success = copyFile(context, source, target);
            }
            if (success) {
                success = deleteFile(context, source);
            }
        }

        return success;
    }

    /**
     * Rename a folder. In case of extSdCard in Kitkat, the old folder stays in place, but files are moved.
     *
     * @param source The source folder.
     * @param target The target folder.
     * @return true if the renaming was successful.
     */
    public static boolean renameFolder(Context context, @NonNull final File source, @NonNull final File target)
        throws IOException {
        // First try the normal rename.
        if (source.renameTo(target)) {
            return true;
        }
        if (target.exists()) {
            return false;
        }

        // Try the Storage Access Framework if it is just a rename within the same parent folder.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && source.getParent().equals(target.getParent())) {
            DocumentFile document = getDocumentFile(context, source, true, true, true);
            if (document != null && document.renameTo(target.getName())) {
                return true;
            }
        }

        // Try the manual way, moving files individually.
        if (!mkdir(context, target)) {
            return false;
        }

        File[] sourceFiles = source.listFiles();

        if (sourceFiles == null) {
            return true;
        }

        for (File sourceFile : sourceFiles) {
            String fileName = sourceFile.getName();
            File targetFile = new File(target, fileName);
            if (!copyFile(context, sourceFile, targetFile)) {
                // stop on first error
                return false;
            }
        }
        // Only after successfully copying all files, delete files on source folder.
        for (File sourceFile : sourceFiles) {
            if (!deleteFile(context, sourceFile)) {
                // stop on first error
                return false;
            }
        }
        return true;
    }

    /**
     * Create a folder. The folder may even be on external SD card for Kitkat.
     *
     * @param file The folder to be created.
     * @return True if creation was successful.
     */
    public static boolean mkdir(Context context, @NonNull final File file) {
        if (file.exists()) {
            // nothing to create.
            return file.isDirectory();
        }

        // Try the normal way
        if (file.mkdir()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(context, file, true, true, true);
            // getDocumentFile implicitly creates the directory.
            return document != null && document.exists();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File tempFile = new File(file, "dummyImage.jpg");

            File dummySong = copyDummyFiles(context);
            if (dummySong == null) {
                return false;
            }
            int albumId = getAlbumIdFromAudioFile(context, dummySong);
            Uri albumArtUri = Uri.parse("content://media/external/audio/albumart/" + albumId);

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DATA, tempFile.getAbsolutePath());
            contentValues.put(MediaStore.Audio.AlbumColumns.ALBUM_ID, albumId);

            ContentResolver resolver = context.getContentResolver();
            if (resolver.update(albumArtUri, contentValues, null, null) == 0) {
                resolver.insert(Uri.parse("content://media/external/audio/albumart"), contentValues);
            }
            try {
                ParcelFileDescriptor fd = resolver.openFileDescriptor(albumArtUri, "r");
                if (fd != null) {
                    fd.close();
                }
            } catch (Exception e) {
                Timber.e(e);
                return false;
            } finally {
                deleteFile(context, tempFile);
            }

            return true;
        }

        return false;
    }

    /**
     * Delete a folder.
     *
     * @param file The folder name.
     * @return true if successful.
     */
    public static boolean rmdir(Context context, @NonNull final File file) {
        if (!file.exists()) {
            return true;
        }
        if (!file.isDirectory()) {
            return false;
        }
        String[] fileList = file.list();
        if (fileList != null && fileList.length > 0) {
            // Delete only empty folder.
            return false;
        }

        // Try the normal way
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile document = getDocumentFile(context, file, true, true, true);
            return document != null && document.delete();
        }

        // Try the Kitkat workaround.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Delete the created entry, such that content provider will delete the file.
            resolver.delete(MediaStore.Files.getContentUri("external"),
                            MediaStore.MediaColumns.DATA + "=?",
                            new String[] { file.getAbsolutePath() });
        }

        return !file.exists();
    }

    /**
     * Delete all files in a folder.
     *
     * @param folder the folder
     * @return true if successful.
     */
    public static boolean deleteFilesInFolder(Context context, @NonNull final File folder) {
        boolean totalSuccess = true;

        String[] children = folder.list();
        if (children != null) {
            for (String child : children) {
                File file = new File(folder, child);
                if (!file.isDirectory()) {
                    boolean success = deleteFile(context, file);
                    if (!success) {
                        Timber.w("Failed to delete file %s", child);
                        totalSuccess = false;
                    }
                }
            }
        }
        return totalSuccess;
    }

    /**
     * Get the Album Id from an Audio file.
     *
     * @param file The audio file.
     * @return The Album ID.
     */
    @SuppressWarnings("resource")
    public static int getAlbumIdFromAudioFile(Context context, @NonNull final File file) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                       new String[] { MediaStore.Audio.AlbumColumns.ALBUM_ID },
                                       MediaStore.MediaColumns.DATA + "=?",
                                       new String[] { file.getAbsolutePath() },
                                       null);
        if (cursor == null || !cursor.moveToFirst()) {
            // Entry not available - create entry.
            if (cursor != null) {
                cursor.close();
            }
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
            values.put(MediaStore.MediaColumns.TITLE, "{MediaWrite Workaround}");
            values.put(MediaStore.MediaColumns.SIZE, file.length());
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
            values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, true);
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        }
        cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                new String[] { MediaStore.Audio.AlbumColumns.ALBUM_ID },
                                MediaStore.MediaColumns.DATA + "=?",
                                new String[] { file.getAbsolutePath() },
                                null);
        if (cursor == null) {
            return 0;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return 0;
        }
        int albumId = cursor.getInt(0);
        cursor.close();
        return albumId;
    }

    /**
     * Get the full path of a document from its tree URI.
     *
     * @param treeUri The tree URI.
     * @param volumeBasePath the base path of the volume.
     * @return The path (without trailing file separator).
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private static String getFullPathFromTreeUri(@Nullable final Uri treeUri, final String volumeBasePath) {
        if (treeUri == null) {
            return null;
        }
        if (volumeBasePath == null) {
            return File.separator;
        }
        String volumePath = volumeBasePath;
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            } else {
                return volumePath + File.separator + documentPath;
            }
        } else {
            return volumePath;
        }
    }

    /**
     * Get an Uri from an file path.
     *
     * @param path The file path.
     * @return The Uri.
     */
    public static Uri getUriFromFile(Context context, final String path) {
        ContentResolver resolver = context.getContentResolver();

        Cursor filecursor = resolver.query(MediaStore.Files.getContentUri("external"),
                                           new String[] { BaseColumns._ID },
                                           MediaStore.MediaColumns.DATA + " = ?",
                                           new String[] { path },
                                           MediaStore.MediaColumns.DATE_ADDED + " desc");
        if (filecursor == null) {
            return null;
        }
        filecursor.moveToFirst();

        if (filecursor.isAfterLast()) {
            filecursor.close();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, path);
            return resolver.insert(MediaStore.Files.getContentUri("external"), values);
        } else {
            int imageId = filecursor.getInt(filecursor.getColumnIndex(BaseColumns._ID));
            Uri uri = MediaStore.Files.getContentUri("external").buildUpon().appendPath(Integer.toString(imageId)).build();
            filecursor.close();
            return uri;
        }
    }

    /**
     * Copy the dummy image and dummy mp3 into the private folder, if not yet there. Required for the Kitkat workaround.
     *
     * @return the dummy mp3.
     */
    private static File copyDummyFiles(Context context) {
        try {
            copyDummyFile(context, R.raw.folder, "mkdirFiles", "albumart.jpg");
            return copyDummyFile(context, R.raw.folder, "mkdirFiles", "silence.mp3");
        } catch (IOException e) {
            Timber.e("Could not copy dummy files.");
            Timber.e(e);
            return null;
        }
    }

    /**
     * Copy a resource file into a private target directory, if the target does not yet exist. Required for the Kitkat
     * workaround.
     *
     * @param resource The resource file.
     * @param folderName The folder below app folder where the file is copied to.
     * @param targetName The name of the target file.
     * @return the dummy file.
     * @throws IOException thrown if there are issues while copying.
     */
    private static File copyDummyFile(Context context,
                                      final int resource,
                                      final String folderName,
                                      @NonNull final String targetName) throws IOException {
        File externalFilesDir = context.getExternalFilesDir(folderName);
        if (externalFilesDir == null) {
            return null;
        }
        File targetFile = new File(externalFilesDir, targetName);

        if (!targetFile.exists()) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = context.getResources().openRawResource(resource);
                out = new FileOutputStream(targetFile);
                byte[] buffer = new byte[4096]; // MAGIC_NUMBER
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        // do nothing
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        // do nothing
                    }
                }
            }
        }
        return targetFile;
    }

    /**
     * Get a DocumentFile corresponding to the given file (for writing on ExtSdCard on Android 5). If the file is not
     * existing, it is created.
     *
     * @param file The file.
     * @param isDirectory flag indicating if the file should be a directory.
     * @param createDirectories flag indicating if intermediate path directories should be created if not existing.
     * @param isNew 是否为新建
     * @return The DocumentFile
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static DocumentFile getDocumentFile(Context context,
                                               @NonNull final File file,
                                               final boolean isDirectory,
                                               final boolean createDirectories,
                                               final boolean isNew) {

        SharedPreferences preferences = context.getSharedPreferences("file", 0);
        String url = preferences.getString("url", "");
        if (url.isEmpty()) {
            return null;
        }

        Uri treeUri = null;
        Uri[] treeUris = new Uri[] { Uri.parse(url) };
        String fullPath;
        String baseFolder = null;
        try {
            fullPath = file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }

        // First try to get the base folder via unofficial StorageVolume API from the URIs.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            StorageVolume volume = storageManager.getStorageVolume(file);
            String uuid = volume.getUuid();
            for (int i = 0; baseFolder == null && i < treeUris.length; i++) {
                String volumeId = getVolumeIdFromTreeUri(treeUris[i]);
                if (uuid.equals(volumeId)) {
                    treeUri = treeUris[i];
                    // Use parcel to get the hidden path field from StorageVolume
                    Parcel parcel = Parcel.obtain();
                    volume.writeToParcel(parcel, 0);
                    parcel.setDataPosition(0);
                    parcel.readString();
                    parcel.readInt();
                    String volumeBasePath = parcel.readString();
                    parcel.recycle();
                    baseFolder = getFullPathFromTreeUri(treeUris[i], volumeBasePath);
                }
            }
        } else {
            for (int i = 0; baseFolder == null && i < treeUris.length; i++) {
                // Use Java Reflection to access hidden methods from StorageVolume
                String treeBase =
                    getFullPathFromTreeUri(treeUris[i], getVolumePath(context, getVolumeIdFromTreeUri(treeUris[i])));
                if (treeBase != null && fullPath.startsWith(treeBase)) {
                    treeUri = treeUris[i];
                    baseFolder = treeBase;
                }
            }
        }

        if (baseFolder == null) {
            // Alternatively, take root folder from device and assume that base URI works.
            treeUri = treeUris[0];
            baseFolder = getExtSdCardFolder(context, file);
        }

        if (baseFolder == null) {
            return null;
        }

        String relativePath = fullPath.substring(baseFolder.length() + 1);

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);

        // todo 优化此处的性能
        String[] parts = relativePath.split("\\/");

        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument;
            if (i < parts.length - 1) { // 如果不是最后一节(文件名,或目录名称)
                nextDocument = document.findFile(parts[i]);
            } else {
                if (!isNew) {// 如果不是新的文件
                    nextDocument = document.findFile(parts[i]);
                } else { // 新的文件
                    if (isDirectory) {
                        nextDocument = document.createDirectory(parts[i]);
                    } else {
                        nextDocument = document.createFile("image", parts[i]);
                    }
                }
            }

            if (nextDocument == null) {
                if (i < parts.length - 1) {
                    if (createDirectories) {
                        nextDocument = document.createDirectory(parts[i]);
                    } else {
                        return null;
                    }
                } else if (isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                } else {
                    nextDocument = document.createFile("image", parts[i]);
                }
            }
            document = nextDocument;
        }

        return document;
    }

    /**
     * Get the volume ID from the tree URI.
     *
     * @param treeUri The tree URI.
     * @return The volume ID.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        } else {
            return null;
        }
    }

    /**
     * Get the document path (relative to volume name) for a tree URI (LOLLIPOP).
     *
     * @param treeUri The tree URI.
     * @return the document path.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        } else {
            return File.separator;
        }
    }

    /**
     * Get the path of a certain volume.
     *
     * @param volumeId The volume id.
     * @return The path.
     */
    private static String getVolumePath(Context context, final String volumeId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }

        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(storageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 检查文件是否可写。检测外部 SD 卡上的写入问题。
     *
     * Check is a file is writable. Detects write issues on external SD card.
     *
     * @param file The file
     * @return true if the file is writable.
     */
    public static boolean isWritable(@NonNull final File file) {
        boolean isExisting = file.exists();

        try {
            FileOutputStream output = new FileOutputStream(file, true);
            try {
                output.close();
            } catch (IOException e) {
                // do nothing.
            }
        } catch (FileNotFoundException e) {
            return false;
        }
        boolean result = file.canWrite();

        // Ensure that file is not created during this process.
        if (!isExisting) {
            // noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        return result;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private static String[] getExtSdCardPaths(Context context) {
        List<String> paths = new ArrayList<>();
        for (File file : context.getExternalFilesDirs("external")) {
            if (file != null && !file.equals(context.getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Timber.w("Unexpected external file dir: %s", file.getAbsolutePath());
                } else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    } catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        return paths.toArray(new String[paths.size()]);
    }

    /**
     * Determine the main folder of the external SD card containing the given file.
     *
     * @param file the file.
     * @return The main folder of the external SD card containing this file, if the file is on an SD card. Otherwise,
     * null is returned.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    public static String getExtSdCardFolder(Context context, @NonNull final File file) {
        String[] extSdPaths = getExtSdCardPaths(context);
        try {
            for (String extSdPath : extSdPaths) {
                if (file.getCanonicalPath().startsWith(extSdPath)) {
                    return extSdPath;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * 获取外置存储卡
     *
     * @param context {@link Context}
     * @param is_removable true 外部存储
     * @return 外置存储卡路径 null 表示外置存储卡不存在
     */
    public static String getStoragePath(Context context, boolean is_removable) {
        StorageManager storageManager =
            (StorageManager) context.getApplicationContext().getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            if (storageManager == null) { return null; }
            Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(storageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removable == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Timber.e(e);
        }
        return null;
    }
}
