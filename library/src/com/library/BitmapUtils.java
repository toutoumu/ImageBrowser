package com.library;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.util.Size;
import androidx.exifinterface.media.ExifInterface;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE;
import static androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE;
import static androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION;

public class BitmapUtils {
    /**
     * 旋转图片，使图片保持正确的方向。
     *
     * @param bitmap 原始图片
     * @param degrees 原始图片的角度
     * @return Bitmap 旋转后的图片
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (degrees == 0 || null == bitmap) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return bmp;
    }

    public static int getRotation(String file) throws IOException {
        int rotation = getFileExifRotation(file);
        return getExifRotation(rotation);
    }

    /**
     * 获取图片宽度
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Point getImageSize(String fileName) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 最关键在此，把options.inJustDecodeBounds = true;
        // 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options); // 此时返回的bitmap为null
        //options.outHeight为原始图片的高
        Point point = new Point();
        point.set(options.outWidth, options.outHeight);

        ExifInterface exifInterface = new ExifInterface(fileName);
        int orientation = exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
        int rotation;
        switch (orientation) {
            case ORIENTATION_ROTATE_90:
            case ORIENTATION_TRANSPOSE:
                rotation = 90;
                break;
            case ORIENTATION_ROTATE_180:
            case ORIENTATION_FLIP_VERTICAL:
                rotation = 180;
                break;

            case ORIENTATION_ROTATE_270:
            case ORIENTATION_TRANSVERSE:
                rotation = 270;
                break;
            default:
                rotation = 0;
        }
        if (rotation == 90 || rotation == 270) {
            point.set(options.outHeight, options.outWidth);
        }
        return point;
    }

    public static Bitmap decodeContentStream(int width, int height, ContentResolver contentResolver, Uri data)
        throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream is = null;
        try {
            is = contentResolver.openInputStream(data);
            BitmapFactory.decodeStream(is, null, options);
        } finally {
            safeClose(is);
        }

        calculateInSampleSize(width, height, options);
        try {
            is = contentResolver.openInputStream(data);
            return BitmapFactory.decodeStream(is, null, options);
        } finally {
            safeClose(is);
        }
    }

    public static Bitmap decodeContentStream(int width, int height, String file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);

        calculateInSampleSize(width, height, options);

        return BitmapFactory.decodeFile(file, options);
    }

    /**
     * @param scale > 0 && < 1
     */
    public static Bitmap scale(Bitmap source, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale); //长和宽放大缩小的比例
        Bitmap result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        return result;
    }

    /**
     * 处理图片(保持原有比例)
     *
     * @param src 源文件
     * @param dest 目标文件
     * @param size 目标尺寸
     * @return
     */
    public static boolean compress(String src, File dest, Size size) {
        OutputStream fos = null;
        try {
            Bitmap srcBitmap = decodeContentStream(size.getWidth(), size.getHeight(), src);
            Bitmap resultBitmap =
                transformResult(srcBitmap, size.getWidth(), size.getHeight(), getFileExifRotation(src), true, false, true);
            if (srcBitmap != resultBitmap) {
                srcBitmap.recycle();
            }
            fos = new FileOutputStream(dest);
            return resultBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        } catch (Throwable e) {
            return false;
        } finally {
            safeClose(fos);
        }
    }

    /**
     * @param result
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     * @param exifOrientation
     * @param onlyScaleDown 原有图片小于这个值是否裁剪
     * @param centerCrop 从中间截取
     * @param centerInside 填充到中间
     * @return
     */
    private static Bitmap transformResult(Bitmap result,
                                          int targetWidth,
                                          int targetHeight,
                                          int exifOrientation,
                                          boolean onlyScaleDown,
                                          boolean centerCrop,
                                          boolean centerInside) {
        int inWidth = result.getWidth();
        int inHeight = result.getHeight();

        int drawX = 0;
        int drawY = 0;
        int drawWidth = inWidth;
        int drawHeight = inHeight;

        Matrix matrix = new Matrix();

        // EXIf interpretation should be done before cropping in case the dimensions need to
        // be recalculated
        if (exifOrientation != 0) {
            int exifRotation = getExifRotation(exifOrientation);
            int exifTranslation = getExifTranslation(exifOrientation);
            if (exifRotation != 0) {
                matrix.preRotate(exifRotation);
                if (exifRotation == 90 || exifRotation == 270) {
                    // Recalculate dimensions after exif rotation
                    int tmpHeight = targetHeight;
                    targetHeight = targetWidth;
                    targetWidth = tmpHeight;
                }
            }
            if (exifTranslation != 1) {
                matrix.postScale(exifTranslation, 1);
            }
        }

        if (centerCrop) {
            // Keep aspect ratio if one dimension is set to 0
            float widthRatio = targetWidth != 0 ? targetWidth / (float) inWidth : targetHeight / (float) inHeight;
            float heightRatio = targetHeight != 0 ? targetHeight / (float) inHeight : targetWidth / (float) inWidth;
            float scaleX, scaleY;
            if (widthRatio > heightRatio) {
                int newSize = (int) Math.ceil(inHeight * (heightRatio / widthRatio));
                drawY = (inHeight - newSize) / 2;
                drawHeight = newSize;
                scaleX = widthRatio;
                scaleY = targetHeight / (float) drawHeight;
            } else if (widthRatio < heightRatio) {
                int newSize = (int) Math.ceil(inWidth * (widthRatio / heightRatio));
                drawX = (inWidth - newSize) / 2;
                drawWidth = newSize;
                scaleX = targetWidth / (float) drawWidth;
                scaleY = heightRatio;
            } else {
                drawX = 0;
                drawWidth = inWidth;
                scaleX = scaleY = heightRatio;
            }
            if (shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                matrix.preScale(scaleX, scaleY);
            }
        } else if (centerInside) {
            // Keep aspect ratio if one dimension is set to 0
            float widthRatio = targetWidth != 0 ? targetWidth / (float) inWidth : targetHeight / (float) inHeight;
            float heightRatio = targetHeight != 0 ? targetHeight / (float) inHeight : targetWidth / (float) inWidth;
            float scale = widthRatio < heightRatio ? widthRatio : heightRatio;
            if (shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                matrix.preScale(scale, scale);
            }
        } else if ((targetWidth != 0 || targetHeight != 0) //
            && (targetWidth != inWidth || targetHeight != inHeight)) {
            // If an explicit target size has been specified and they do not match the results bounds,
            // pre-scale the existing matrix appropriately.
            // Keep aspect ratio if one dimension is set to 0.
            float sx = targetWidth != 0 ? targetWidth / (float) inWidth : targetHeight / (float) inHeight;
            float sy = targetHeight != 0 ? targetHeight / (float) inHeight : targetWidth / (float) inWidth;
            if (shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                matrix.preScale(sx, sy);
            }
        }

        return Bitmap.createBitmap(result, drawX, drawY, drawWidth, drawHeight, matrix, true);
    }

    private static void calculateInSampleSize(int reqWidth, int reqHeight, BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = (int) Math.floor((float) height / (float) reqHeight);
            final int widthRatio = (int) Math.floor((float) width / (float) reqWidth);
            sampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
    }

    /**
     * 是否需要Resize
     *
     * @param onlyScaleDown
     * @param inWidth
     * @param inHeight
     * @param targetWidth
     * @param targetHeight
     * @return
     */
    private static boolean shouldResize(boolean onlyScaleDown,
                                        int inWidth,
                                        int inHeight,
                                        int targetWidth,
                                        int targetHeight) {
        return !onlyScaleDown || inWidth > targetWidth || inHeight > targetHeight;
    }

    /**
     * 获取图片旋转方向
     *
     * @param orientation
     * @return
     */
    private static int getExifRotation(int orientation) {
        int rotation;
        switch (orientation) {
            case ORIENTATION_ROTATE_90:
            case ORIENTATION_TRANSPOSE:
                rotation = 90;
                break;
            case ORIENTATION_ROTATE_180:
            case ORIENTATION_FLIP_VERTICAL:
                rotation = 180;
                break;
            case ORIENTATION_ROTATE_270:
            case ORIENTATION_TRANSVERSE:
                rotation = 270;
                break;
            default:
                rotation = 0;
        }
        return rotation;
    }

    private static int getExifTranslation(int orientation) {
        int translation;
        switch (orientation) {
            case ORIENTATION_FLIP_HORIZONTAL:
            case ORIENTATION_FLIP_VERTICAL:
            case ORIENTATION_TRANSPOSE:
            case ORIENTATION_TRANSVERSE:
                translation = -1;
                break;
            default:
                translation = 1;
        }
        return translation;
    }

    /**
     * 获取文件的旋转方向的Exif
     *
     * @param src
     * @return
     * @throws IOException
     */
    private static int getFileExifRotation(String src) throws IOException {
        ExifInterface exifInterface = new ExifInterface(src);
        return exifInterface.getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL);
    }

    /** Closes the given stream inside a try/catch. Does nothing if stream is null. */
    private static void safeClose(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // Silent
            }
        }
    }

    /** Closes the given stream inside a try/catch. Does nothing if stream is null. */
    private static void safeClose(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                // Silent
            }
        }
    }
}
