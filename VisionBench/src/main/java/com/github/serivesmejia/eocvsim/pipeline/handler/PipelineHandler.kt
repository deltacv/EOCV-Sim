/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.pipeline.handler

import com.github.serivesmejia.eocvsim.input.InputSource
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

interface PipelineHandler {

    fun preInit()

    fun init()

    fun processFrame(currentInputSource: InputSource?)

    fun onException()

    fun onChange(beforePipeline: OpenCvPipeline?, newPipeline: OpenCvPipeline, telemetry: Telemetry)

}
