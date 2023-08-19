package com.github.serivesmejia.eocvsim.pipeline.instantiator.processor

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.instantiator.PipelineInstantiator
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.vision.VisionProcessor
import org.openftc.easyopencv.OpenCvPipeline

object ProcessorInstantiator : PipelineInstantiator {

    override fun instantiate(clazz: Class<*>, telemetry: Telemetry): OpenCvPipeline {
        if(!ReflectUtil.hasSuperclass(clazz, VisionProcessor::class.java))
            throw IllegalArgumentException("Class $clazz does not extend VisionProcessor")

        val processor = try {
            //instantiate pipeline if it has a constructor of a telemetry parameter
            val constructor = clazz.getConstructor(Telemetry::class.java)
            constructor.newInstance(telemetry) as VisionProcessor
        } catch (ex: NoSuchMethodException) {
            //instantiating with a constructor of no params
            val constructor = clazz.getConstructor()
            constructor.newInstance() as VisionProcessor
        }

        return ProcessorPipeline(processor)
    }

}