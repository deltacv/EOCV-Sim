package com.github.serivesmejia.eocvsim.pipeline.instantiator

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

interface PipelineInstantiator {

    fun instantiate(clazz: Class<*>, telemetry: Telemetry): OpenCvPipeline

}