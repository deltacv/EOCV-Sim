package org.firstinspires.ftc.teamcode;

import android.graphics.Canvas;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.opencv.core.Mat;
import org.openftc.easyopencv.OpenCvPipeline;
import org.openftc.easyopencv.TimestampedOpenCvPipeline;

public class AprilTagProcessorPipeline extends TimestampedOpenCvPipeline {

    AprilTagProcessor processor = new AprilTagProcessor.Builder()
            .setOutputUnits(DistanceUnit.METER, AngleUnit.DEGREES)
            .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
            .setDrawAxes(true)
            .setDrawTagOutline(true)
            .setDrawCubeProjection(true)
            .build();

    @Override
    public void init(Mat firstFrame) {
        processor.init(firstFrame.width(), firstFrame.height(), null);
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
