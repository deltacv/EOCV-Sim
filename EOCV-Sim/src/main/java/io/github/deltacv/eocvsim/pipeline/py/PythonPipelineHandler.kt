package io.github.deltacv.eocvsim.pipeline.py

import com.github.serivesmejia.eocvsim.pipeline.DefaultPipelineHandler
import com.github.serivesmejia.eocvsim.pipeline.PipelineHandler
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import io.github.deltacv.eocvsim.virtualreflect.py.PyVirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class PythonPipelineHandler(
    val name: String,
    val sourceCode: String
) : PipelineHandler {

    @Suppress("UNCHECKED_CAST")
    override fun instantiate(clazz: Class<out OpenCvPipeline>, telemetry: Telemetry): OpenCvPipeline {
        if(!ReflectUtil.hasSuperclass(clazz, PythonPipeline::class.java)) {
            return DefaultPipelineHandler.instantiate(clazz, telemetry)
        }

        return clazz.getConstructor(
            String::class.java, String::class.java, Telemetry::class.java
        ).newInstance(name, sourceCode, telemetry)
    }

    override fun nameOf(clazz: Class<out OpenCvPipeline>) = name

    override fun virtualReflectOf(clazz: Class<out OpenCvPipeline>) =
        if(ReflectUtil.hasSuperclass(clazz, PythonPipeline::class.java)) {
            PyVirtualReflection
        } else DefaultPipelineHandler.virtualReflectOf(clazz)

    override fun sourceOf(clazz: Class<out OpenCvPipeline>) =
        if(ReflectUtil.hasSuperclass(clazz, PythonPipeline::class.java)) {
            PipelineSource.PYTHON_RUNTIME
        } else DefaultPipelineHandler.sourceOf(clazz)
}

fun PipelineManager.addPythonPipeline(
    name: String, sourceCode: String,
    updateGui: Boolean = true
) = addPipelineClass(PythonPipeline::class.java, PythonPipelineHandler(name, sourceCode), refreshGuiPipelineList = updateGui)

fun PipelineManager.removePythonPipeline(
    name: String,
    updateGui: Boolean = true
) = removePipeline(name, PipelineSource.PYTHON_RUNTIME, refreshGuiPipelineList = updateGui)