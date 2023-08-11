package com.github.serivesmejia.eocvsim.pipeline.handler

import com.github.serivesmejia.eocvsim.input.InputSource
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

interface PipelineHandler {

    fun preInit()

    fun init()

    fun processFrame(currentInputSource: InputSource?)

    fun onChange(pipeline: OpenCvPipeline, telemetry: Telemetry)

}