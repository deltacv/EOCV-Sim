/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.api

import org.deltacv.eocvsim.plugin.EOCVSimPlugin
import javax.swing.JPanel

/**
 * Factory API for creating complex visualizer components as raw JPanel instances.
 *
 * This API provides methods to create UI panels for pipeline selection, source selection,
 * and telemetry display. These panels are automatically wired to relevant events and hooks
 * for seamless integration with the EOCV-Sim environment.
 *
 * @param owner The plugin that owns this API instance.
 */
abstract class VisualizerComponentsFactoryApi(owner: EOCVSimPlugin) : Api(owner) {
    /**
     * Creates a new instance of the JPanel for selecting pipelines.
     *
     * The panel displays a scrollable list of all available pipelines managed by
     * [PipelineManagerApi]. It allows users to select a pipeline and is automatically
     * wired to handle relevant events, such as pipeline addition, removal, or selection.
     *
     * @return A new instance of [PipelineSelectorPanelApi].
     */
    abstract fun createPipelineSelectorPanel(): PipelineSelectorPanelApi

    /**
     * Creates a new instance of the JPanel for selecting input sources.
     *
     * The panel displays a scrollable list of all available input sources managed by
     * [InputSourceManagerApi]. It allows users to select an input source and is automatically
     * wired to handle relevant events, such as source addition, removal, or selection.
     *
     * @return A new instance of [SourceSelectorPanelApi].
     */
    abstract fun createSourceSelectorPanel(): SourceSelectorPanelApi

    /**
     * Creates a new instance of the JPanel for displaying telemetry data.
     *
     * The panel displays a scrollable list of telemetry messages, with each message
     * formatted according to the specified separators. It is designed to provide
     * real-time updates and clear functionality.
     *
     * @return A new instance of [TelemetryPanelApi].
     */
    abstract fun createTelemetryPanel(): TelemetryPanelApi
}

/**
 * Represents the panel for selecting pipelines.
 *
 * This panel provides a user interface for browsing and selecting pipelines.
 * It supports enabling/disabling user interaction and switching between pipelines.
 * The panel is automatically updated to reflect the latest state of the pipelines.
 *
 * @param owner The plugin that owns this API instance.
 */
abstract class PipelineSelectorPanelApi(owner: EOCVSimPlugin) : Api(owner) {
    /**
     * The JPanel instance representing the UI component.
     */
    abstract val jPanel: JPanel

    /**
     * Indicates whether user interaction with the panel is enabled.
     */
    abstract var isInteractionEnabled: Boolean

    /**
     * Indicates whether switching between pipelines is allowed.
     */
    abstract var allowSwitching: Boolean

    /**
     * Gets the name of the currently selected pipeline, or null if no pipeline is selected.
     */
    abstract val selectedPipelineName: String?

    /**
     * Gets the index of the currently selected pipeline, or -1 if no pipeline is selected.
     */
    abstract val selectedPipelineIndex: Int

    /**
     * Refreshes the panel to reflect the latest state of the pipelines.
     */
    abstract fun refresh()
}

/**
 * Represents the panel for selecting input sources.
 *
 * This panel provides a user interface for browsing and selecting input sources.
 * It supports enabling/disabling user interaction and switching between sources.
 * The panel is automatically updated to reflect the latest state of the sources.
 *
 * @param owner The plugin that owns this API instance.
 */
abstract class SourceSelectorPanelApi(owner: EOCVSimPlugin) : Api(owner) {
    /**
     * The JPanel instance representing the UI component.
     */
    abstract val jPanel: JPanel

    /**
     * Indicates whether user interaction with the panel is enabled.
     */
    abstract var isInteractionEnabled: Boolean

    /**
     * Indicates whether switching between sources is allowed.
     */
    abstract var allowSwitching: Boolean

    /**
     * Gets the name of the currently selected source, or null if no source is selected.
     */
    abstract val selectedSourceName: String?

    /**
     * Gets the index of the currently selected source, or -1 if no source is selected.
     */
    abstract val selectedSourceIndex: Int

    /**
     * Refreshes the panel to reflect the latest state of the sources.
     */
    abstract fun refresh()
}

/**
 * Represents the panel for displaying telemetry data.
 *
 * This panel provides a user interface for displaying telemetry messages.
 * It supports real-time updates and clearing of telemetry data. The panel
 * is designed to handle large volumes of data efficiently.
 *
 * @param owner The plugin that owns this API instance.
 */
abstract class TelemetryPanelApi(owner: EOCVSimPlugin) : Api(owner) {
    /**
     * The JPanel instance representing the UI component.
     */
    abstract val jPanel: JPanel

    /**
     * Updates the telemetry panel with the given text, using the specified separators.
     *
     * @param text The telemetry data to display.
     * @param captionSeparator The separator between captions and their values.
     * @param itemSeparator The separator between different telemetry items.
     */
    abstract fun update(text: String, captionSeparator: String = " : ", itemSeparator: String = " | ")

    /**
     * Clears all telemetry data from the panel.
     */
    abstract fun clear()
}
