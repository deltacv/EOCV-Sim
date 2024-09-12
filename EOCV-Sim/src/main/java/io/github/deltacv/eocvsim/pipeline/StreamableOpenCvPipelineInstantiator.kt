package io.github.deltacv.eocvsim.pipeline

import com.github.serivesmejia.eocvsim.pipeline.instantiator.DefaultPipelineInstantiator
import com.github.serivesmejia.eocvsim.pipeline.instantiator.PipelineInstantiator
import io.github.deltacv.eocvsim.stream.ImageStreamer
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

class StreamableOpenCvPipelineInstantiator(
    val imageStreamer: ImageStreamer
) : PipelineInstantiator {

    override fun instantiate(clazz: Class<*>, telemetry: Telemetry) =
        DefaultPipelineInstantiator.instantiate(clazz, telemetry).apply {
            if(this is StreamableOpenCvPipeline) {
                this.streamer = imageStreamer
            }
        }

    override fun virtualReflectOf(pipeline: OpenCvPipeline) = JvmVirtualReflection

    override fun variableTunerTarget(pipeline: OpenCvPipeline) = pipeline

}