/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.pipeline.instantiator

import org.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

object DefaultPipelineInstantiator : PipelineInstantiator {

    override fun instantiate(clazz: Class<*>, telemetry: Telemetry) = try {
        //instantiate pipeline if it has a constructor of a telemetry parameter
        val constructor = clazz.getConstructor(Telemetry::class.java)
        constructor.newInstance(telemetry) as OpenCvPipeline
    } catch (ex: NoSuchMethodException) {
        //instantiating with a constructor of no params
        val constructor = clazz.getConstructor()
        constructor.newInstance() as OpenCvPipeline
    }

    override fun virtualReflectOf(pipeline: OpenCvPipeline) = JvmVirtualReflection

    override fun variableTunerTarget(pipeline: OpenCvPipeline) = pipeline

}
