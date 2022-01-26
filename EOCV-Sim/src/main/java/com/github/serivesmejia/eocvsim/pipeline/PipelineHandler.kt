package com.github.serivesmejia.eocvsim.pipeline

import com.github.serivesmejia.eocvsim.pipeline.compiler.isFromRuntimeBuild
import io.github.deltacv.eocvsim.pipeline.PipelineSource
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline
import java.lang.reflect.Constructor

interface PipelineHandler {
    fun instantiate(clazz: Class<out OpenCvPipeline>, telemetry: Telemetry): OpenCvPipeline

    fun nameOf(clazz: Class<out OpenCvPipeline>): String?
    fun virtualReflectOf(clazz: Class<out OpenCvPipeline>): VirtualReflection

    fun sourceOf(clazz: Class<out OpenCvPipeline>): PipelineSource
}

object DefaultPipelineHandler : PipelineHandler {

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

    override fun sourceOf(clazz: Class<out OpenCvPipeline>) = if(clazz.isFromRuntimeBuild) {
        PipelineSource.COMPILED_ON_RUNTIME
    } else PipelineSource.CLASSPATH

}