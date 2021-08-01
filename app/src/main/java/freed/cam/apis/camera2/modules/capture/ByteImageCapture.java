package freed.cam.apis.camera2.modules.capture;

import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.HardwareBuffer;
import android.media.Image;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.nio.ByteBuffer;

import freed.ActivityInterface;
import freed.cam.apis.basecamera.modules.ModuleInterface;
import freed.image.ImageSaveTask;
import freed.image.ImageTask;
import freed.utils.Log;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ByteImageCapture extends StillImageCapture {

    private final String TAG = ByteImageCapture.class.getSimpleName();

    public ByteImageCapture(Size size,
                            int format,
                            boolean setToPreview,
                            ActivityInterface activityInterface,
                            ModuleInterface moduleInterface,
                            String file_ending,
                            int max_images) {
        super(size, format, setToPreview, activityInterface, moduleInterface, file_ending, max_images);
    }

    @Override
    public ImageTask getSaveTask() {
        return super.getSaveTask();
    }

    @Override
    protected void createTask() {
        if (result == null || image == null) { return; }
        File file = new File(getFilepath() + file_ending);
        task = process_jpeg(image, file);
        image.close();
        image = null;
    }

    private ImageTask process_jpeg(Image image, File file) {

        if (image.getFormat() == ImageFormat.YUV_420_888) {
            Log.d(TAG, "Create JPEG");
            int width = image.getWidth(), height = image.getHeight();
            // size是宽乘高的1.5倍 可以通过ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)得到
            int i420Size = width * height * 3 / 2;

            Image.Plane[] planes = image.getPlanes();
            //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176 Y分量byte数组的size
            int remaining0 = planes[0].getBuffer().remaining();
            int remaining1 = planes[1].getBuffer().remaining();
            //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1 V分量byte数组的size
            int remaining2 = planes[2].getBuffer().remaining();
            //获取pixelStride，可能跟width相等，可能不相等
            int pixelStride = planes[2].getPixelStride();
            int rowOffest = planes[2].getRowStride();
            byte[] nv21 = new byte[i420Size];
            //分别准备三个数组接收YUV分量。
            byte[] yRawSrcBytes = new byte[remaining0];
            byte[] uRawSrcBytes = new byte[remaining1];
            byte[] vRawSrcBytes = new byte[remaining2];
            planes[0].getBuffer().get(yRawSrcBytes);
            planes[1].getBuffer().get(uRawSrcBytes);
            planes[2].getBuffer().get(vRawSrcBytes);
            if (pixelStride == width) {
                //两者相等，说明每个YUV块紧密相连，可以直接拷贝
                System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * height);
                System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * height, rowOffest * height / 2 - 1);
            } else {
                //根据每个分量的size先生成byte数组
                byte[] ySrcBytes = new byte[width * height];
                byte[] uSrcBytes = new byte[width * height / 2 - 1];
                byte[] vSrcBytes = new byte[width * height / 2 - 1];
                for (int row = 0; row < height; row++) {
                    //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                    System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, width * row, width);
                    //y执行两次，uv执行一次
                    if (row % 2 == 0) {
                        //最后一行需要减一
                        if (row == height - 2) {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, width * row / 2, width - 1);
                        } else {
                            System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, width * row / 2, width);
                        }
                    }
                }
                //yuv拷贝到一个数组里面
                System.arraycopy(ySrcBytes, 0, nv21, 0, width * height);
                System.arraycopy(vSrcBytes, 0, nv21, width * height, width * height / 2 - 1);
            }

            ImageSaveTask task = new ImageSaveTask(activityInterface, moduleInterface);

            // FIXME: 2021/6/28 增加尺寸设置用于保存 CameraApi2 JPEG 图片为 HEIF 格式
            task.setSize(new Size(image.getWidth(), image.getHeight()));
            task.setFormat(image.getFormat());
            task.setOrientation(orientation);

            task.setBytesTosave(nv21, ImageSaveTask.JPEG);
            task.setFilePath(file, externalSD);
            image.close();
            image = null;
            return task;
        } else {
            Log.d(TAG, "Create JPEG");
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            ImageSaveTask task = new ImageSaveTask(activityInterface, moduleInterface);

            // FIXME: 2021/6/28 增加尺寸设置用于保存 CameraApi2 JPEG 图片为 HEIF 格式
            task.setSize(new Size(image.getWidth(), image.getHeight()));
            task.setFormat(image.getFormat());
            task.setOrientation(orientation);

            task.setBytesTosave(bytes, ImageSaveTask.JPEG);
            task.setFilePath(file, externalSD);
            buffer.clear();
            image.close();
            buffer = null;
            image = null;
            return task;
        }
    }

    /**
     * 将两个byte数组合并为一个
     *
     * @param data1 要合并的数组1
     * @param data2 要合并的数组2
     * @return 合并后的新数组
     */
    public static byte[] mergeBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }
}
