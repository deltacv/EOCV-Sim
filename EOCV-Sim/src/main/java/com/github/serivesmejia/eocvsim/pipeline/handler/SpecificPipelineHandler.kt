package com.github.serivesmejia.eocvsim.pipeline.handler

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

abstract class SpecificPipelineHandler<P: OpenCvPipeline>(
        val typeChecker: (OpenCvPipeline) -> Boolean
) : PipelineHandler {

    var pipeline: P? = null
        private set

    var telemetry: Telemetry? = null
        private set

    @Suppress("UNCHECKED_CAST")
    override fun onChange(pipeline: OpenCvPipeline, telemetry: Telemetry) {
        if(typeChecker(pipeline)) {
            this.pipeline = pipeline as P
            this.telemetry = telemetry
        } else {
            this.pipeline = null
            this.telemetry = null // "don't get paid enough to handle this shit"
                                  // - OpModePipelineHandler, probably
        }
    }

}