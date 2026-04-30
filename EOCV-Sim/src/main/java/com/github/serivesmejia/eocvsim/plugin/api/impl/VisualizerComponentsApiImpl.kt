package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.gui.component.visualizer.TelemetryPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.PipelineSelectorPanel
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.SourceSelectorPanel
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.PipelineSelectorPanelApi
import io.github.deltacv.eocvsim.plugin.api.SourceSelectorPanelApi
import io.github.deltacv.eocvsim.plugin.api.TelemetryPanelApi
import io.github.deltacv.eocvsim.plugin.api.VisualizerComponentsFactoryApi

class VisualizerComponentsFactoryApiImpl(owner: EOCVSimPlugin) : VisualizerComponentsFactoryApi(owner) {
    override fun createPipelineSelectorPanel() = apiImpl { PipelineSelectorPanelApiImpl(owner, PipelineSelectorPanel()) }
    override fun createSourceSelectorPanel() = apiImpl { SourceSelectorPanelApiImpl(owner, SourceSelectorPanel()) }
    override fun createTelemetryPanel() = apiImpl { TelemetryPanelApiImpl(owner, TelemetryPanel()) }

    override fun disableApi() { }
}

class PipelineSelectorPanelApiImpl(owner: EOCVSimPlugin, val internalPanel: PipelineSelectorPanel) : PipelineSelectorPanelApi(owner) {

    override val jPanel by apiField(internalPanel)

    override var isInteractionEnabled: Boolean
        get() = apiImpl { internalPanel.pipelineSelectorScroll.isEnabled }
        set(value) = apiImpl { internalPanel.pipelineSelectorScroll.isEnabled = value }

    override var allowSwitching: Boolean
        get() = apiImpl { internalPanel.allowPipelineSwitching }
        set(value) = apiImpl { internalPanel.allowPipelineSwitching = value }

    override val selectedPipelineName: String? by liveApiField { internalPanel.pipelineSelector.selectedValue }
    override val selectedPipelineIndex by liveApiField { internalPanel.pipelineSelector.selectedIndex }

    override fun refresh() = apiImpl {
        internalPanel.updatePipelinesList()
    }

    override fun disableApi() { }

}

class SourceSelectorPanelApiImpl(owner: EOCVSimPlugin, val internalPanel: SourceSelectorPanel) : SourceSelectorPanelApi(owner) {

    override val jPanel by apiField(internalPanel)

    override var isInteractionEnabled: Boolean
        get() = apiImpl { internalPanel.sourceSelectorScroll.isEnabled }
        set(value) = apiImpl { internalPanel.sourceSelectorScroll.isEnabled = value }

    override var allowSwitching: Boolean
        get() = apiImpl { internalPanel.allowSourceSwitching }
        set(value) = apiImpl { internalPanel.allowSourceSwitching = value }

    override val selectedSourceName: String? by liveApiField { internalPanel.sourceSelector.selectedValue }
    override val selectedSourceIndex by liveApiField { internalPanel.sourceSelector.selectedIndex }

    override fun refresh() = apiImpl {
        internalPanel.updateSourcesList()
    }

    override fun disableApi() { }

}

class TelemetryPanelApiImpl(owner: EOCVSimPlugin, val internalPanel: TelemetryPanel) : TelemetryPanelApi(owner) {
    override val jPanel by apiField(internalPanel)

    override fun update(text: String, captionSeparator: String, itemSeparator: String) = apiImpl {
        internalPanel.updateTelemetry(text, captionSeparator, itemSeparator)
    }

    override fun clear() = apiImpl {
        internalPanel.telemetryList.removeAll()
    }

    override fun disableApi() { }

}