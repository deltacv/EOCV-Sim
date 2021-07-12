package org.firstinspires.ftc.teamcode

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.opencv.core.Mat
import org.openftc.easyopencv.TimestampedOpenCvPipeline

class TimestampedPipelineTest(val telemetry: Telemetry) : TimestampedOpenCvPipeline() {

    override fun processFrame(input: Mat, captureTimeNanos: Long): Mat {
        telemetry.addData("cap time nanos", captureTimeNanos)
        telemetry.update()

        return input
    }

}