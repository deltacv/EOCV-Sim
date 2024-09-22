/*
 * Copyright (c) 2023 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.pipeline.instantiator.processor

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.pipeline.instantiator.PipelineInstantiator
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
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

    override fun virtualReflectOf(pipeline: OpenCvPipeline) = JvmVirtualReflection

    override fun variableTunerTarget(pipeline: OpenCvPipeline) = (pipeline as ProcessorPipeline).processor

}