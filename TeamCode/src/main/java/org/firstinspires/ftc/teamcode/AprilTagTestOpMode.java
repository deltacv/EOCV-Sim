package org.firstinspires.ftc.teamcode;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.opencv.core.Mat;
import org.openftc.easyopencv.OpenCvPipeline;
import org.openftc.easyopencv.TimestampedOpenCvPipeline;

public class AprilTagProcessorTest extends TimestampedOpenCvPipeline {

    AprilTagProcessor processor = new AprilTagProcessor.Builder()
            .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
            .setDrawCubeProjection(true)
            .setDrawTagID(true)
            .setOutputUnits(DistanceUnit.CM, AngleUnit.DEGREES)
            .build();

    @Override
    public void init(Mat firstFrame) {
        processor.init(firstFrame.width(), firstFrame.height(), null);
    }

    @Override
    public Mat processFrame(Mat input, long captureTimeNanos) {
        processor.processFrame(input, captureTimeNanos);
        return input;
    }

}
