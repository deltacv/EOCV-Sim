/*
 * Copyright (c) 2022 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.eocvsim.virtualreflect

/**
 * Context for virtual reflection, which makes for a multi-platform way to reflect on classes, fields, and methods.
 */
interface VirtualReflectContext {

    /**
     * The name of the class this context is reflecting on, including package name
     */
    val name: String

    /**
     * The simple name of the class this context is reflecting on, without package name
     */
    val simpleName: String

    /**
     * The fields of the class this context is reflecting on
     */
    val fields: Array<VirtualField>

    /**
     * Gets a field by its name, or null if it doesn't exist
     */
    fun getField(name: String): VirtualField?

    /**
     * Gets a field by its label, or null if it doesn't exist
     * @see io.github.deltacv.eocvsim.virtualreflect.jvm.Label
     */
    fun getLabeledField(label: String): VirtualField?

}
