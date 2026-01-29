package org.opencv.android;

import android.graphics.Bitmap;
import org.jetbrains.skia.ColorType;
import org.jetbrains.skia.impl.BufferUtil;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.WeakHashMap;

public class Utils {

    /**
     * Converts Android Bitmap to OpenCV Mat.
     * <p>
     * This function converts an Android Bitmap image to the OpenCV Mat.
     * <br>'ARGB_8888' and 'RGB_565' input Bitmap formats are supported.
     * <br>The output Mat is always created of the same size as the input Bitmap and of the 'CV_8UC4' type,
     * it keeps the image in RGBA format.
     * <br>This function throws an exception if the conversion fails.
     * @param bmp is a valid input Bitmap object of the type 'ARGB_8888' or 'RGB_565'.
     * @param mat is a valid output Mat object, it will be reallocated if needed, so it may be empty.
     * @param unPremultiplyAlpha is a flag, that determines, whether the bitmap needs to be converted from alpha premultiplied format (like Android keeps 'ARGB_8888' ones) to regular one; this flag is ignored for 'RGB_565' bitmaps.
     */
    public static void bitmapToMat(Bitmap bmp, Mat mat, boolean unPremultiplyAlpha) {
        if (bmp == null)
            throw new IllegalArgumentException("bmp == null");
        if (mat == null)
            throw new IllegalArgumentException("mat == null");
        nBitmapToMat2(bmp, mat, unPremultiplyAlpha);
    }

    /**
     * Short form of the bitmapToMat(bmp, mat, unPremultiplyAlpha=false).
     * @param bmp is a valid input Bitmap object of the type 'ARGB_8888' or 'RGB_565'.
     * @param mat is a valid output Mat object, it will be reallocated if needed, so Mat may be empty.
     */
    public static void bitmapToMat(Bitmap bmp, Mat mat) {
        bitmapToMat(bmp, mat, false);
    }

    /**
     * Converts OpenCV Mat to Android Bitmap.
     * <p>
     * <br>This function converts an image in the OpenCV Mat representation to the Android Bitmap.
     * <br>The input Mat object has to be of the types 'CV_8UC1' (gray-scale), 'CV_8UC3' (RGB) or 'CV_8UC4' (RGBA).
     * <br>The output Bitmap object has to be of the same size as the input Mat and of the types 'ARGB_8888' or 'RGB_565'.
     * <br>This function throws an exception if the conversion fails.
     *
     * @param mat is a valid input Mat object of types 'CV_8UC1', 'CV_8UC3' or 'CV_8UC4'.
     * @param bmp is a valid Bitmap object of the same size as the Mat and of type 'ARGB_8888' or 'RGB_565'.
     * @param premultiplyAlpha is a flag, that determines, whether the Mat needs to be converted to alpha premultiplied format (like Android keeps 'ARGB_8888' bitmaps); the flag is ignored for 'RGB_565' bitmaps.
     */
    public static void matToBitmap(Mat mat, Bitmap bmp, boolean premultiplyAlpha) {
        if (mat == null)
            throw new IllegalArgumentException("mat == null");
        if (bmp == null)
            throw new IllegalArgumentException("bmp == null");
        nMatToBitmap2(mat, bmp, premultiplyAlpha);
    }

    /**
     * Short form of the <b>matToBitmap(mat, bmp, premultiplyAlpha=false)</b>
     * @param mat is a valid input Mat object of the types 'CV_8UC1', 'CV_8UC3' or 'CV_8UC4'.
     * @param bmp is a valid Bitmap object of the same size as the Mat and of type 'ARGB_8888' or 'RGB_565'.
     */
    public static void matToBitmap(Mat mat, Bitmap bmp) {
        matToBitmap(mat, bmp, false);
    }

    private static void nBitmapToMat2(Bitmap b, Mat mat, boolean unPremultiplyAlpha) {
        mat.create(new Size(b.getWidth(), b.getHeight()), CvType.CV_8UC4);

        int size = b.getWidth() * b.getHeight() * 3;

        long addr = b.theBitmap.peekPixels().getAddr();
        ByteBuffer buffer = BufferUtil.INSTANCE.getByteBufferFromPointer(addr, size);

        if( b.theBitmap.getImageInfo().getColorType() == ColorType.RGBA_8888 )
        {
            Mat tmp = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC4, buffer);
            if(unPremultiplyAlpha) Imgproc.cvtColor(tmp, mat, Imgproc.COLOR_mRGBA2RGBA);
            else tmp.copyTo(mat);

            tmp.release();
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            Mat tmp = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC2, buffer);
            Imgproc.cvtColor(tmp, mat, Imgproc.COLOR_BGR5652RGBA);

            tmp.release();
        }
    }

    private static ThreadLocal<WeakHashMap<Integer, byte[]>> threadLocalm2bReusableBuffers = ThreadLocal.withInitial(WeakHashMap::new);

    private static void nMatToBitmap2(Mat src, Bitmap b, boolean premultiplyAlpha) {
        Mat tmp;

        if(b.getConfig() == Bitmap.Config.ARGB_8888) {
            tmp = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC4);

            if(src.type() == CvType.CV_8UC1)
            {
                Imgproc.cvtColor(src, tmp, Imgproc.COLOR_GRAY2BGRA);
            } else if(src.type() == CvType.CV_8UC3){
                Imgproc.cvtColor(src, tmp, Imgproc.COLOR_RGB2BGRA);
            } else if(src.type() == CvType.CV_8UC4){
                if(premultiplyAlpha) Imgproc.cvtColor(src, tmp, Imgproc.COLOR_RGBA2mRGBA);
                else Imgproc.cvtColor(src, tmp, Imgproc.COLOR_RGBA2BGRA);
            }
        } else {
            tmp = new Mat(b.getWidth(), b.getHeight(), CvType.CV_8UC2);

            if(src.type() == CvType.CV_8UC1)
            {
                Imgproc.cvtColor(src, tmp, Imgproc.COLOR_GRAY2BGR565);
            } else if(src.type() == CvType.CV_8UC3){
                Imgproc.cvtColor(src, tmp, Imgproc.COLOR_RGB2BGR565);
            } else if(src.type() == CvType.CV_8UC4){
                Imgproc.cvtColor(src, tmp, Imgproc.COLOR_RGBA2BGR565);
            }
        }

        int size = tmp.rows() * tmp.cols() * tmp.channels();

        WeakHashMap<Integer, byte[]> m2bArrays = threadLocalm2bReusableBuffers.get();

        byte[] m2bData = m2bArrays.get(size);

        if(m2bData == null || m2bData.length != size) {
            m2bData = new byte[size];
            m2bArrays.put(size, m2bData);
        }

        tmp.get(0, 0, m2bData);

        long addr = b.theBitmap.peekPixels().getAddr();
        ByteBuffer buffer = BufferUtil.INSTANCE.getByteBufferFromPointer(addr, size);

        buffer.put(m2bData);

        tmp.release();
    }
}
