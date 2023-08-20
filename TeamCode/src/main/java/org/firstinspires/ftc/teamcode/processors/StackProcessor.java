package org.firstinspires.ftc.teamcode.processors;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class StackProcessor implements VisionProcessor {
    public enum RingNumber {
        ZERO,
        ONE,
        FOUR;

        public double avgHueValue;
    }

    private final static int TEST_RECT_X = 240;
    private final static int TEST_RECT_Y = 146;
    private final static int TEST_RECT_WIDTH = 20;
    private final static int TEST_RECT_HEIGHT = 60;
    private final static double FOUR_STACK_HUE_THRESHOLD = 70;
    private final static double ONE_STACK_HUE_THRESHOLD = 58;
    private RingNumber result;
    Paint rectPaint;
    Paint textPaint;
    android.graphics.Rect drawRectangle;
    float textLineSize;

    public RingNumber getResult() {
        return result;
    }
    private boolean initialFrameDone;
    private Mat ring;
    private Mat ringHSV;
    private boolean initialDrawDone;

    @Override
    public void init(int width, int height, CameraCalibration calibration) {

    }

    public void processFirstFrame(Mat frame) {
        Rect rect = new Rect(TEST_RECT_X, TEST_RECT_Y, TEST_RECT_WIDTH, TEST_RECT_HEIGHT);

        ring = frame.submat(rect);
        ringHSV = new Mat(ring.cols(), ring.rows(), ring.type());
    }

    @Override
    public Object processFrame(Mat frame, long captureTimeNanos) {
        if (!initialFrameDone) {
            processFirstFrame(frame);
            initialFrameDone = true;
        }
        Imgproc.cvtColor(ring, ringHSV, Imgproc.COLOR_BGR2HSV);

        double avgHueValue = Core.mean(ringHSV).val[0];

        if (avgHueValue > FOUR_STACK_HUE_THRESHOLD) {
            result = RingNumber.FOUR;
        } else if (avgHueValue > ONE_STACK_HUE_THRESHOLD) {
            result = RingNumber.ONE;
        } else {
            result = RingNumber.ZERO;
        }
        result.avgHueValue = avgHueValue;

        return result;
    }

    public void drawFirstFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity) {
        rectPaint = new Paint();
        rectPaint.setColor(Color.RED);
        rectPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setTextSize(40 * scaleCanvasDensity);
        textPaint.setColor(Color.GREEN);

        int left = Math.round(TEST_RECT_X * scaleBmpPxToCanvasPx);
        int top = Math.round(TEST_RECT_Y * scaleBmpPxToCanvasPx);
        int right = left + Math.round(TEST_RECT_WIDTH * scaleBmpPxToCanvasPx);
        int bottom = top + Math.round(TEST_RECT_HEIGHT * scaleBmpPxToCanvasPx);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        textLineSize = (fm.descent - fm.ascent) * scaleBmpPxToCanvasPx;

        drawRectangle = new android.graphics.Rect(left, top, right, bottom);
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        if (!initialDrawDone) {
            drawFirstFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity);
            initialDrawDone = true;
        }
        canvas.drawRect(drawRectangle, rectPaint);
        canvas.drawText(userContext.toString(), 0, textLineSize, textPaint);
        canvas.drawText(String.format("%.2f", ((RingNumber) userContext).avgHueValue),
                0, textLineSize * 2, textPaint);
    }
}