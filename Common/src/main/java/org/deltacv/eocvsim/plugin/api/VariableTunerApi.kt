/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.api

import org.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.deltacv.eocvsim.virtualreflect.VirtualField
/**
 * Represents a single tunable field exposed by the variable tuner.
 *
 * A tunable field is backed by a [VirtualField] and allows updating its value
 * at runtime. Implementations are expected to apply changes immediately to
 * the underlying pipeline instance.
 */
abstract class TunableFieldApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * The underlying virtual field being tuned.
     *
     * This describes the field metadata (name, type, annotations, etc.)
     * and is used to identify the tunable field.
     */
    abstract val field: VirtualField

    /**
     * Sets the value of this field.
     *
     * @param index index of the target instance or element, if applicable
     * @param value new value to assign to the field
     *
     * @throws Exception if the value is incompatible or cannot be applied
     */
    abstract fun setFieldValue(index: Int, value: Any)
}

/**
 * API responsible for managing tunable fields for pipelines.
 *
 * This API creates and tracks [TunableFieldApi] instances based on
 * reflected pipeline fields and provides lookup utilities for
 * already-registered tunables.
 */
abstract class VariableTunerApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Creates a new tunable field instance for the given [VirtualField].
     *
     * The returned [TunableFieldApi] is bound to the provided pipeline instance
     * and allows modifying the field value at runtime.
     *
     * @param virtualField virtual field metadata describing the tunable field
     * @param pipeline pipeline instance that owns the field
     * @return a new tunable field instance, or null if the field is not tunable
     */
    abstract fun newTunableFieldInstanceOf(
        virtualField: VirtualField,
        pipeline: Any
    ): TunableFieldApi?

    /**
     * Retrieves an existing tunable field by its label, specified
     * by [VirtualField.label].
     *
     * @see [org.deltacv.eocvsim.virtualreflect.jvm.Label]
     * @param label label identifying the tunable field
     * @return the matching tunable field, or null if none exists
     */
    abstract fun getTunableFieldWithLabel(label: String): TunableFieldApi?
}
