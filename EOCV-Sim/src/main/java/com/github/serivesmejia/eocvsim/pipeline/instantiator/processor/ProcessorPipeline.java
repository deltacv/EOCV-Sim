package com.github.serivesmejia.eocvsim.pipeline.instantiator.processor;

import android.graphics.Canvas;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Mat;
import org.openftc.easyopencv.TimestampedOpenCvPipeline;

@Disabled
class ProcessorPipeline extends TimestampedOpenCvPipeline {

    private VisionProcessor processor;

    public ProcessorPipeline(VisionProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void init(Mat mat) {
        processor.init(mat.width(), mat.height(), null);
    }

    @Override
    public Mat processFrame(Mat input, long captureTimeNanos) {
        requestViewportDrawHook(processor.processFrame(input, captureTimeNanos));
        return input;
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        processor.onDrawFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity, userContext);
    }

}
