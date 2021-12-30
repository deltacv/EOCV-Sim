package io.github.deltacv.eocvsim.pipeline.js

import com.github.serivesmejia.eocvsim.pipeline.PipelineInstantiator
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class JavascriptPipelineInstantiator(
    val name: String,
    val sourceCode: String
) : PipelineInstantiator {

    @Suppress("UNCHECKED_CAST")
    override fun instantiate(clazz: Class<out OpenCvPipeline>, telemetry: Telemetry): OpenCvPipeline {
        if(!ReflectUtil.hasSuperclass(clazz, JavascriptPipeline::class.java)) {
            throw IllegalArgumentException("Pipeline class ${clazz.typeName} does not extend from JavascriptPipeline")
        }

        return clazz.getConstructor(
            String::class.java, String::class.java, Telemetry::class.java
        ).newInstance(name, sourceCode, telemetry)
    }

}