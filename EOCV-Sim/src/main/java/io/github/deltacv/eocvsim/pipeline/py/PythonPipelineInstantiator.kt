package io.github.deltacv.eocvsim.pipeline.py

import com.github.serivesmejia.eocvsim.pipeline.DefaultPipelineInstantiator
import com.github.serivesmejia.eocvsim.pipeline.PipelineInstantiator
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import io.github.deltacv.eocvsim.virtualreflect.py.PyVirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class PythonPipelineInstantiator(
    val name: String,
    val sourceCode: String
) : PipelineInstantiator {

    @Suppress("UNCHECKED_CAST")
    override fun instantiate(clazz: Class<out OpenCvPipeline>, telemetry: Telemetry): OpenCvPipeline {
        if(!ReflectUtil.hasSuperclass(clazz, PythonPipeline::class.java)) {
            throw IllegalArgumentException("Pipeline class ${clazz.typeName} does not extend from PythonPipeline")
        }

        return clazz.getConstructor(
            String::class.java, String::class.java, Telemetry::class.java
        ).newInstance(name, sourceCode, telemetry)
    }

    override fun nameOf(clazz: Class<out OpenCvPipeline>) = name

    override fun virtualReflectOf(clazz: Class<out OpenCvPipeline>) =
        if(ReflectUtil.hasSuperclass(clazz, PythonPipeline::class.java)) {
            PyVirtualReflection
        } else DefaultPipelineInstantiator.virtualReflectOf(clazz)

}