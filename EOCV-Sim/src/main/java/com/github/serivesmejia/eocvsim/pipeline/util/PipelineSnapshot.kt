/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.pipeline.util

import io.github.deltacv.common.util.loggerForThis
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflectContext
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflectContext
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection
import org.openftc.easyopencv.OpenCvPipeline
import java.util.*

class PipelineSnapshot(val virtualReflectContext: VirtualReflectContext, filter: ((VirtualField) -> Boolean)? = null) {

    val logger by loggerForThis()

    val holdingPipelineName = virtualReflectContext.simpleName

    val pipelineClass get() = (virtualReflectContext as JvmVirtualReflectContext).clazz

    val pipelineFieldValues: Map<VirtualField, Any?>

    init {
        val fieldValues = mutableMapOf<VirtualField, Any?>()

        for(field in virtualReflectContext.fields) {
            if(field.isFinal || field.isFinal)
                continue

            if(filter?.invoke(field) == false) continue

            fieldValues[field] = field.get()
        }

        pipelineFieldValues = fieldValues.toMap()

        logger.info("Taken snapshot of pipeline ${pipelineClass.name}")
    }

    fun transferTo(otherPipeline: OpenCvPipeline,
                   lastInitialPipelineSnapshot: PipelineSnapshot? = null) {
        if(pipelineClass.name != otherPipeline::class.java.name) return

        val changedList = if(lastInitialPipelineSnapshot != null)
            getChangedFieldsComparedTo(PipelineSnapshot(JvmVirtualReflection.contextOf(otherPipeline)), lastInitialPipelineSnapshot)
        else Collections.emptyList()

        fieldValuesLoop@
        for((field, value) in pipelineFieldValues) {
            for(changedField in changedList) {
                if(changedField.name == field.name && changedField.type == field.type) {
                    logger.trace(
                        "Skipping field ${field.name} since its value was changed in code, compared to the initial state of the pipeline"
                    )

                    continue@fieldValuesLoop
                }
            }

            try {
                field.set(value)
            } catch(e: Exception) {
                logger.trace(
                    "Failed to set field ${field.name} from snapshot of ${pipelineClass.name}. " +
                    "Retrying with by name lookup logic..."
                )

                try {
                    val byNameField = otherPipeline::class.java.getDeclaredField(field.name)
                    byNameField.set(otherPipeline, value)
                } catch(e: Exception) {
                    logger.warn(
                        "Definitely failed to set field ${field.name} from snapshot of ${pipelineClass.name}. Did the source code change?",
                        e
                    )
                }
            }
        }
    }

    fun getField(name: String): Pair<VirtualField, Any?>? {
        for((field, value) in pipelineFieldValues) {
            if(field.name == name) {
                return Pair(field, value)
            }
        }

        return null
    }

    private fun getChangedFieldsComparedTo(
        pipelineSnapshotA: PipelineSnapshot,
        pipelineSnapshotB: PipelineSnapshot
    ): List<VirtualField> = pipelineSnapshotA.run {
        if(holdingPipelineName != pipelineSnapshotB.holdingPipelineName && pipelineClass != pipelineSnapshotB.pipelineClass)
            return Collections.emptyList()

        val changedList = mutableListOf<VirtualField>()

        for((field, value) in pipelineFieldValues) {
            val (otherField, otherValue) = pipelineSnapshotB.getField(field.name) ?: continue
            if (field.type != otherField.type) continue

            if(otherValue != value) {
                changedList.add(field)
            }
        }

        return changedList
    }

}

