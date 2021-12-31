package com.github.serivesmejia.eocvsim.pipeline

import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline
import java.lang.reflect.Constructor

interface PipelineInstantiator {
    fun instantiate(clazz: Class<out OpenCvPipeline>, telemetry: Telemetry): OpenCvPipeline

    fun nameOf(clazz: Class<out OpenCvPipeline>): String?

    fun virtualReflectOf(clazz: Class<out OpenCvPipeline>): VirtualReflection
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

    override fun nameOf(clazz: Class<out OpenCvPipeline>) = clazz.simpleName

    override fun virtualReflectOf(clazz: Class<out OpenCvPipeline>) = JvmVirtualReflection

}