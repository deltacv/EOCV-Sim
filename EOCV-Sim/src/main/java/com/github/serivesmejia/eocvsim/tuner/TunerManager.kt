/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner

import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel
import com.github.serivesmejia.eocvsim.tuner.exception.CancelTunableFieldAddingException
import io.github.deltacv.common.util.loggerForThis
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
import io.github.deltacv.eocvsim.virtualreflect.jvm.JvmVirtualReflection

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.util.orchestration.dependency
import com.github.serivesmejia.eocvsim.util.orchestration.PhaseOrchestrableBase

class TunerManager : PhaseOrchestrableBase(), KoinComponent {

    private val pipelineManager: PipelineManager by dependency<PipelineManager>(inject())
    private val visualizer: Visualizer by inject()

    val logger by loggerForThis()

    val fields = mutableListOf<TunableField<*>>()

    var reflect: VirtualReflection = JvmVirtualReflection

    private var firstInit = true

    override suspend fun init() {
        pipelineManager.onPipelineChange.attach { reset() }
        refreshFields()
    }

    private fun refreshFields() {
        pipelineManager.reflectTarget?.let { target ->
            addFieldsFrom(target)
            visualizer.updateTunerFields(createTunableFieldPanels())

            val iterator = fields.iterator()
            while (iterator.hasNext()) {
                val field = iterator.next()
                try {
                    field.init()
                } catch (e: CancelTunableFieldAddingException) {
                    logger.info("Field ${field.fieldName} was removed due to \"${e.message}\"")
                    iterator.remove()
                }
            }
        }
    }

    override suspend fun run() {
        for (field in fields.toList()) { // toList to avoid concurrent modification issues
            try {
                field.update()
            } catch (ex: Exception) {
                logger.error("Error while updating field ${field.fieldName}", ex)
            }

            if (field.fieldPanel?.hasRequestedAllConfigReeval() == true) {
                for (f in fields) {
                    f.fieldPanel?.panelOptions?.reevaluateConfig()
                }
            }
        }
    }

    override suspend fun destroy() {
        reset()
    }

    fun reset() {
        fields.clear()
        refreshFields()
    }

    fun newTunableFieldInstanceFor(field: VirtualField, pipeline: Any): TunableField<*>? {
        return TunableFieldRegistry.getTunableFieldFor(field, pipeline)
    }

    fun addFieldsFrom(pipeline: Any) {
        val reflectContext = reflect.contextOf(pipeline) ?: return
        val virtualFields = reflectContext.fields

        for (field in virtualFields) {
            val tunableField = newTunableFieldInstanceFor(field, pipeline)
            if (tunableField != null) {
                fields.add(tunableField)
            }
        }
    }

    fun getCurrentTunableFieldWithLabel(label: String): TunableField<*>? {
        val labeledField = fields.find { it.reflectionField.label == label }
        labeledField?.isIgnoreGuiUpdates = true
        return labeledField
    }

    fun reevaluateConfigs() {
        for (field in fields) {
            field.fieldPanel?.panelOptions?.reevaluateConfig()
        }
    }

    private fun createTunableFieldPanels(): List<TunableFieldPanel> {
        return fields.map { TunableFieldPanel(it) }
    }

}

