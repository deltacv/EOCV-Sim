/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.pipeline;

import android.graphics.*;
import android.graphics.Rect;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

public class DefaultPipeline extends OpenCvPipeline {

    public int blur = 0;

    private Telemetry telemetry;

    private Paint boxPaint;
    private Paint textPaint;

    public DefaultPipeline(Telemetry telemetry) {
        this.telemetry = telemetry;

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.DEFAULT_ITALIC);
        textPaint.setTextSize(30);
        textPaint.setAntiAlias(true);

        boxPaint = new Paint();
        boxPaint.setColor(Color.BLACK);
        boxPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public Mat processFrame(Mat input) {

        double aspectRatio = (double) input.height() / (double) input.width();
        double aspectRatioPercentage = aspectRatio / (580.0 / 480.0);

        telemetry.addData("[>]", "Default pipeline selected.");
        telemetry.addData("[Aspect Ratio]", aspectRatio + " (" + String.format("%.2f", aspectRatioPercentage * 100) + "%)");
        telemetry.addData("[Blur]", blur + " (change this value in tuner menu)");
        telemetry.update();

        if (blur > 0 && blur % 2 == 1) {
            Imgproc.GaussianBlur(input, input, new Size(blur, blur), 0);
        } else if (blur > 0) {
            Imgproc.GaussianBlur(input, input, new Size(blur + 1, blur + 1), 0);
        }

        return input;
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        canvas.drawRect(new Rect(0, 0, 345, 45), boxPaint);
        canvas.drawText("Default pipeline selected", 5, 33, textPaint);
    }

}
