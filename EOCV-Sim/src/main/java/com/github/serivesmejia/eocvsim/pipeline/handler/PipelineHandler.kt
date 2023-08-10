package com.github.serivesmejia.eocvsim.pipeline.util

import com.github.serivesmejia.eocvsim.input.InputSource
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

interface PipelineHandler {

    fun preInit(pipeline: OpenCvPipeline, telemetry: Telemetry)

    fun init()

    fun processFrame(currentInputSource: InputSource?)

    fun onChange()

}