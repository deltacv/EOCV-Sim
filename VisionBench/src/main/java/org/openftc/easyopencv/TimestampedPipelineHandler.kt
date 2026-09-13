/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv

import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.pipeline.handler.PipelineHandler
import com.github.serivesmejia.eocvsim.pipeline.handler.SpecificPipelineHandler
import org.firstinspires.ftc.robotcore.external.Telemetry

class TimestampedPipelineHandler : SpecificPipelineHandler<TimestampedOpenCvPipeline>(
        { it is TimestampedOpenCvPipeline }
) {
    override fun preInit() {
    }

    override fun init() {
        pipeline?.setTimestamp(0)
    }

    override fun processFrame(currentInputSource: InputSource?) {
        pipeline?.setTimestamp(currentInputSource?.captureTimeNanos ?: 0L)
    }

    override fun onException() {
    }
}
