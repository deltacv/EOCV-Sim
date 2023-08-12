package com.qualcomm.robotcore.eventloop.opmode

import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.pipeline.handler.SpecificPipelineHandler
import com.qualcomm.robotcore.hardware.HardwareMap
import io.github.deltacv.eocvsim.input.VisionInputSourceHander
import io.github.deltacv.vision.external.source.ThreadSourceHander
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline
import org.openftc.easyopencv.OpenCvViewport

class OpModePipelineHandler(private val viewport: OpenCvViewport) : SpecificPipelineHandler<OpMode>(
        { it is OpMode }
) {

    override fun preInit() {
        ThreadSourceHander.register(VisionInputSourceHander(viewport))

        pipeline?.telemetry = telemetry
        pipeline?.hardwareMap = HardwareMap()
    }

    override fun init() { }


    override fun processFrame(currentInputSource: InputSource?) {
    }

    override fun onChange(pipeline: OpenCvPipeline, telemetry: Telemetry) {
        this.pipeline?.requestOpModeStop()

        super.onChange(pipeline, telemetry)
    }

}