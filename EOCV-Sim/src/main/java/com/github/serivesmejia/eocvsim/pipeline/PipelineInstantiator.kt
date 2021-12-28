package com.github.serivesmejia.eocvsim.pipeline

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline
import java.lang.reflect.Constructor

interface PipelineInstantiator {
    fun instantiate(clazz: Class<out OpenCvPipeline>, telemetry: Telemetry): OpenCvPipeline
}

object DefaultPipelineInstantiator : PipelineInstantiator {

    override fun instantiate(clazz: Class<out OpenCvPipeline>, telemetry: Telemetry): OpenCvPipeline {
        var nextPipeline: OpenCvPipeline
        var constructor: Constructor<*>

        try { //instantiate pipeline if it has a constructor of a telemetry parameter
            constructor = clazz.getConstructor(Telemetry::class.java)
            nextPipeline = constructor.newInstance(telemetry) as OpenCvPipeline
        } catch (ex: NoSuchMethodException) { //instantiating with a constructor of no params
            constructor = clazz.getConstructor()
            nextPipeline = constructor.newInstance() as OpenCvPipeline
        }

        return nextPipeline
    }

}