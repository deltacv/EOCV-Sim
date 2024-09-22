/*
 * Copyright (c) 2024 FIRST
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.firstinspires.ftc.vision.opencv;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;

class PredominantColorProcessorImpl extends PredominantColorProcessor
{
    private Mat roiMat;
    private Mat roiMat_YCrCb;
    private int frameWidth;
    private int frameHeight;
    private Rect roi;
    private ImageRegion roiImg;
    private int roiNumPixels;
    private Mat roiFlattened;
    private byte[] roi_YCrCb_data;
    private float[] roiFlattened_data;

    private static int K = 5; // Get the top n color hues

    private final Paint boundingRectPaint;
    private final Paint boundingRectCrosshairPaint;

    private volatile Result result = new Result(null, 0);

    private final ArrayList<Swatch> swatches;

    PredominantColorProcessorImpl(ImageRegion roi, Swatch[] swatches)
    {
        this.roiImg = roi;

        boundingRectPaint = new Paint();
        boundingRectPaint.setAntiAlias(true);
        boundingRectPaint.setStrokeCap(Paint.Cap.ROUND);
        boundingRectPaint.setColor(Color.WHITE);
        boundingRectPaint.setStyle(Paint.Style.STROKE);

        boundingRectCrosshairPaint = new Paint();
        boundingRectCrosshairPaint.setAntiAlias(true);
        boundingRectCrosshairPaint.setStrokeCap(Paint.Cap.BUTT);
        boundingRectCrosshairPaint.setColor(Color.WHITE);

        this.swatches = new ArrayList<>(Arrays.asList(swatches));
    }

    @Override
    public void init(int width, int height, CameraCalibration calibration)
    {
        this.frameWidth = width;
        this.frameHeight = height;

        roi = roiImg.asOpenCvRect(width, height);
        this.roiNumPixels = roi.width * roi.height;
        this.roiFlattened = new Mat(roiNumPixels, 2, CvType.CV_32F);
        roiFlattened_data = new float[roiNumPixels*2];
        roi_YCrCb_data = new byte[roiNumPixels*3];
    }

    @Override
    public Object processFrame(Mat frame, long captureTimeNanos)
    {
        if (roiMat == null)
        {
            roiMat = frame.submat(roi);
            roiMat_YCrCb = roiMat.clone();
        }

        Imgproc.cvtColor(roiMat, roiMat_YCrCb, Imgproc.COLOR_RGB2YCrCb);

        int avgLuminance = (int) (Core.sumElems(roiMat_YCrCb).val[0] / roiNumPixels);

        // flatten data for K-means
        roiMat_YCrCb.get(0,0, roi_YCrCb_data);
        for (int i = 0; i < roiNumPixels; i++)
        {
            int cr = roi_YCrCb_data[i*3 + 1];
            int cb = roi_YCrCb_data[i*3 + 2];

            roiFlattened_data[i*2    ] = cr;
            roiFlattened_data[i*2 + 1] = cb;
        }
        roiFlattened.put(0,0, roiFlattened_data);

        // Perform K-Means clustering
        Mat labels = new Mat();
        Mat centers = new Mat(K, roiFlattened.cols(), roiFlattened.type());
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 2.0);

        Core.kmeans(roiFlattened, K, labels, criteria, 1, Core.KMEANS_PP_CENTERS, centers);

        int[] clusterCounts = new int[K];
        int maxCount = 0;
        int maxCountIndex = 0;

        int[] clusterIndicies = new int[roiNumPixels];
        labels.get(0,0, clusterIndicies);

        // Get the biggest count along the way
        for (int i = 0; i < roiNumPixels; i++)
        {
            int clusterIndex = clusterIndicies[i];
            int newCount = clusterCounts[clusterIndex]++;

            if (newCount > maxCount)
            {
                maxCount = newCount;
                maxCountIndex = clusterIndex;
            }
        }

        double Y  = avgLuminance; // Luminance
        double Cr = centers.get(maxCountIndex, 0)[0]; // Red-difference Chroma
        double Cb = centers.get(maxCountIndex, 1)[0]; // Blue-difference Chroma

        byte[] rgb = yCrCb2Rgb(new byte[] {(byte) Y, (byte) Cr, (byte) Cb});
        float[] hsv = new float[3];
        int color = Color.rgb(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);

        // Note this used 0-360, 0-1, 0-1
        Color.colorToHSV(color, hsv);

        float H = hsv[0];
        float S = hsv[1];
        float V = hsv[2];

        // Log.d("Best HSV", String.format("H:%3.0f, S:%4.2f, V:%4.2f", H, S, V));

        Swatch closestSwatch = null;

        // Check for Black or White before matching Hue.
        if ((S < 0.15 && V > 0.55) && swatches.contains(Swatch.WHITE))
        {
            closestSwatch = Swatch.WHITE;
        }
        else if ((V < 0.1) || (S < 0.2 || V < 0.2) && swatches.contains(Swatch.BLACK))
        {
            closestSwatch = Swatch.BLACK;
        }
        else
        {
            // now scan the colorHue table to find the table entry closest to the prime hue.
            // watch for hue wrap around at 360.
            int shortestHueDist = 360;

            for (Swatch swatch : swatches)
            {
                if (swatch.hue < 0)
                {
                    // Black or white
                    continue;
                }

                int hueError = Math.abs((int) H - swatch.hue);
                if (hueError > 180)
                {
                    // wrap it around
                    hueError = 360 - hueError;
                }
                if (hueError < shortestHueDist)
                {
                    shortestHueDist = hueError;
                    closestSwatch = swatch;
                }
            }
        }

        result = new Result(closestSwatch, color);

        return result;
    }


    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext)
    {
        android.graphics.Rect gfxRect = makeGraphicsRect(roi, scaleBmpPxToCanvasPx);

        boundingRectCrosshairPaint.setStrokeWidth(5 * scaleCanvasDensity);
        canvas.drawLine(gfxRect.centerX(), gfxRect.top, gfxRect.centerX(), gfxRect.bottom, boundingRectCrosshairPaint);
        canvas.drawLine(gfxRect.left, gfxRect.centerY(), gfxRect.right, gfxRect.centerY(), boundingRectCrosshairPaint);

        boundingRectPaint.setStrokeWidth(10 * scaleCanvasDensity);
        boundingRectPaint.setColor(((Result)userContext).rgb);
        canvas.drawRect(gfxRect, boundingRectPaint);

        canvas.drawPoint(gfxRect.left, gfxRect.top, boundingRectCrosshairPaint);
        canvas.drawPoint(gfxRect.left, gfxRect.bottom, boundingRectCrosshairPaint);
        canvas.drawPoint(gfxRect.right, gfxRect.top, boundingRectCrosshairPaint);
        canvas.drawPoint(gfxRect.right, gfxRect.bottom, boundingRectCrosshairPaint);
    }

    private android.graphics.Rect makeGraphicsRect(Rect rect, float scaleBmpPxToCanvasPx)
    {
        int left = Math.round(rect.x * scaleBmpPxToCanvasPx);
        int top = Math.round(rect.y * scaleBmpPxToCanvasPx);
        int right = left + Math.round(rect.width * scaleBmpPxToCanvasPx);
        int bottom = top + Math.round(rect.height * scaleBmpPxToCanvasPx);

        return new android.graphics.Rect(left, top, right, bottom);
    }

    @Override
    public Result getAnalysis()
    {
        return result;
    }

    byte[] yCrCb2Rgb(byte[] yCrCb)
    {
        Mat cvtColor = new Mat(1,1,CvType.CV_8UC3);
        cvtColor.put(0,0, yCrCb);
        Imgproc.cvtColor(cvtColor, cvtColor, Imgproc.COLOR_YCrCb2RGB);
        byte[] rgb = new byte[3];
        cvtColor.get(0,0, rgb);
        return rgb;
    }
}
