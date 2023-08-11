package com.qualcomm.robotcore.eventloop.opmode

import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.pipeline.handler.SpecificPipelineHandler
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class OpModePipelineHandler : SpecificPipelineHandler<OpMode>(
        { it is OpMode }
) {

    override fun preInit() {
        pipeline?.telemetry = telemetry
        pipeline?.hardwareMap = HardwareMap(null, null)
    }

    override fun init() {
    }

    override fun processFrame(currentInputSource: InputSource?) {
    }

    override fun onChange(pipeline: OpenCvPipeline, telemetry: Telemetry) {
        this.pipeline?.requestOpModeStop()

        super.onChange(pipeline, telemetry)
    }

}