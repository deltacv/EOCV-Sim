package com.github.serivesmejia.eocvsim.pipeline.util

import com.github.serivesmejia.eocvsim.util.Log
import org.openftc.easyopencv.OpenCvPipeline
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class PipelineSnapshot(holdingPipeline: OpenCvPipeline) {

    companion object {
        private val TAG = "PipelineSnapshot"
    }

    val pipelineFieldValues: Map<Field, Any>
    val pipelineClass = holdingPipeline::class.java

    init {
        val fieldValues = mutableMapOf<Field, Any>()

        for(field in pipelineClass.declaredFields) {
            if(Modifier.isFinal(field.modifiers) || !Modifier.isPublic(field.modifiers))
                continue

            fieldValues[field] = field.get(holdingPipeline)
        }

        pipelineFieldValues = fieldValues.toMap()

        Log.info(TAG, "Taken snapshot of pipeline ${pipelineClass.name}")
    }

    fun transferTo(otherPipeline: OpenCvPipeline) {
        if(pipelineClass.name != otherPipeline::class.java.name) return

        for((field, value) in pipelineFieldValues) {
            try {
                field.set(otherPipeline, value)
            } catch(e: Exception) {
                Log.warn(
                    TAG,
                    "Failed to set field ${field.name} from snapshot of ${pipelineClass.name}. " +
                    "Retrying with by name lookup logic..."
                )

                try {
                    val byNameField = otherPipeline::class.java.getDeclaredField(field.name)
                    byNameField.set(otherPipeline, value)
                } catch(e: Exception) {
                    Log.warn(
                        TAG, "Definitely failed to set field ${field.name} from snapshot of ${pipelineClass.name}. " +
                        "Did the source code change?", e
                    )
                }
            }
        }
    }

}
