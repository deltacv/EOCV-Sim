/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.pipeline.instantiator

import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

interface PipelineInstantiator {

    val isUsable: Boolean
        get() = true

    fun instantiate(clazz: Class<*>, telemetry: Telemetry): OpenCvPipeline

    fun virtualReflectOf(pipeline: OpenCvPipeline): VirtualReflection
    fun variableTunerTarget(pipeline: OpenCvPipeline): Any?

}
