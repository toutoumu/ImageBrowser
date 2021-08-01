package freed.image;

import android.content.Context;
import android.graphics.ImageFormat;
import android.location.Location;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Size;
import com.library.App;
import com.library.glide.ConcealUtil;
import freed.ActivityInterface;
import freed.cam.apis.basecamera.modules.ModuleInterface;
import freed.dng.DngProfile;
import freed.file.holder.BaseHolder;
import freed.file.holder.FileHolder;
import freed.file.holder.UriHolder;
import freed.jni.ExifInfo;
import freed.jni.GpsInfo;
import freed.jni.OpCode;
import freed.jni.RawToDng;
import freed.settings.SettingsManager;
import freed.utils.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import timber.log.Timber;

/**
 * Created by KillerInk on 13.11.2017.
 */

public class ImageSaveTask extends ImageTask {
    private final String TAG = ImageSaveTask.class.getSimpleName();

    public final static int JPEG = 0;
    public final static int RAW10 = 1;
    public final static int RAW12 = 2;
    public final static int RAW_SENSOR = 3;
    public final static int DUMP_RAWDATA = 4;

    // FIXME: 2021/6/28 增加尺寸设置用于保存 CameraApi2 JPEG 图片为 HEIF 格式
    private Size size;
    private int format;

    private byte[] bytesTosave;
    private int imageFormat = JPEG;
    private DngProfile profile;
    private File filename;
    private boolean externalSD;
    private int orientation = 0;
    private Location location;
    private boolean forceRawToDng = false;

    private float fnum, focal = 0;
    private int mISO;
    private float exposureTime;
    private int flash = 0;
    private float expoindex;
    private String whitebalance;
    private ActivityInterface activityInterface;
    private ModuleInterface moduleInterface;

    private Thread currentThread;
    private OpCode opcode;
    private float baselineExposure = 0;
    private int greensplit = 0;

    public ImageSaveTask(ActivityInterface activityInterface, ModuleInterface moduleInterface) {
        this.activityInterface = activityInterface;
        this.moduleInterface = moduleInterface;
    }

    private void clear() {
        this.activityInterface = null;
        this.whitebalance = null;
        this.location = null;
        this.filename = null;
        this.profile = null;
        this.bytesTosave = null;
    }

    // FIXME: 2021/6/28 增加尺寸设置用于保存 CameraApi2 JPEG 图片为 HEIF 格式
    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public int getFormat() {
        return format;
    }

    public void setBytesTosave(byte[] bytes, int imageFormat) {
        this.bytesTosave = bytes;
        this.imageFormat = imageFormat;
    }

    public void setDngProfile(DngProfile profile) {
        this.profile = profile;
    }

    public void setFilePath(File file, boolean externalSD) {
        this.filename = file;
        this.externalSD = externalSD;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setForceRawToDng(boolean forceRawToDng) {
        this.forceRawToDng = forceRawToDng;
    }

    public void setFnum(float fnum) {
        this.fnum = fnum;
    }

    public void setFocal(float focal) {
        this.focal = focal;
    }

    public void setIso(int iso) {
        this.mISO = iso;
    }

    public void setExposureTime(float exposureTime) {
        this.exposureTime = exposureTime;
    }

    public void setFlash(int flash) {
        this.flash = flash;
    }

    public void setExposureIndex(float expoindex) {
        this.expoindex = expoindex;
    }

    public void setWhiteBalance(String wb) {
        this.whitebalance = wb;
    }

    public void setOpCode(OpCode opCode) {
        this.opcode = opCode;
    }

    public void setBaselineExposure(float baselineExposure) {
        this.baselineExposure = baselineExposure;
    }

    public void setBayerGreenSplit(int greenSplit) {
        this.greensplit = greenSplit;
    }

    @Override
    public boolean process() {
        if (imageFormat == RAW10 || (imageFormat == RAW_SENSOR && forceRawToDng)) {
            Log.d(TAG, "saveRawToDng");
            saveRawToDng();
            clear();
            return true;
        } else if (imageFormat == JPEG) {
            Log.d(TAG, "saveJpeg");
            saveJpeg();
            clear();
            return true;
        } else if (imageFormat == DUMP_RAWDATA) {
            saveJpeg();
            clear();
        }
        Log.d(TAG, "Save done");
        return false;
    }

    @Override
    public Thread getThread() {
        return currentThread;
    }

    private void saveRawToDng() {
        RawToDng rawToDng = RawToDng.GetInstance();
        ParcelFileDescriptor pfd = null;
        if (location != null) {
            GpsInfo gpsInfo = new GpsInfo(location);
            rawToDng.SetGpsData(gpsInfo.getByteBuffer());
        }
        ExifInfo info = new ExifInfo(mISO, flash, exposureTime, focal, fnum, expoindex, "", orientation + "");
        rawToDng.setExifData(info);
        //        if (whitebalance != null)
        //            rawToDng.SetWBCT(whitebalance);
        if (SettingsManager.getInstance().getOpCode() != null) {
            rawToDng.setOpCode(SettingsManager.getInstance().getOpCode());
        } else if (opcode != null) { rawToDng.setOpCode(opcode); }

        rawToDng.setBaselineExposure(baselineExposure);
        rawToDng.setBayerGreenSplit(greensplit);
        BaseHolder fileholder = activityInterface.getFileListController().getNewImgFileHolder(filename);
        if (fileholder instanceof FileHolder) {
            rawToDng.setBayerData(bytesTosave, filename.getAbsolutePath());
        } else if (fileholder instanceof UriHolder) {
            try {
                pfd = ((UriHolder) fileholder).getParcelFileDescriptor();
                rawToDng.SetBayerDataFD(bytesTosave, pfd, filename.getName());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        rawToDng.WriteDngWithProfile(profile);
        /*if (pfd != null)
            try {
                pfd.close();
            } catch (IOException e) {
                Log.WriteEx(e);
            }*/
        moduleInterface.internalFireOnWorkDone(fileholder);
    }

    Vibrator vibrator;

    private void saveJpeg() {
        if (App.isLogin()) { // FIXME: 2018/3/23 如果已经登录,震动一下
            Timber.e("震动一下");
            if (vibrator == null) {
                vibrator = (Vibrator) App.getAppContext().getSystemService(Context.VIBRATOR_SERVICE);
            }
            vibrator.vibrate(VibrationEffect.createOneShot(30, 255));
            //如果已经登录文件保存到指定目录

            try {
                if (format == ImageFormat.YUV_420_888) {
                    ConcealUtil.encryptedYUVToHEIF(bytesTosave, size, orientation);
                } else if (format == ImageFormat.JPEG) {
                    // ConcealUtil.encryptedFile(bytesTosave);
                    // FIXME: 2021/6/28 增加尺寸设置用于保存 CameraApi2 JPEG 图片为 HEIF 格式
                    ConcealUtil.encryptedFileToHEIF(bytesTosave, size, orientation);
                } else {
                    ConcealUtil.encryptedFile(bytesTosave);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
            return;
        }

        Log.d(TAG, "Start Saving Bytes");
        BaseHolder fileholder = activityInterface.getFileListController().getNewImgFileHolder(filename);
        try {
            BufferedOutputStream outStream = new BufferedOutputStream(fileholder.getOutputStream());
            outStream.write(bytesTosave);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            Log.WriteEx(e);
        }
        if (fileholder != null) { moduleInterface.internalFireOnWorkDone(fileholder); }
        Log.d(TAG, "End Saving Bytes");
    }
}
