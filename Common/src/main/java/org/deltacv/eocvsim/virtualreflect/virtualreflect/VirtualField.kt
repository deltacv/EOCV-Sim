/*
 * Copyright (c) 2022 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.virtualreflect

/**
 * Represents a field of a class, but is not necessarily backed by an actual Java Field.
 */
interface VirtualField {

    val name: String
    val type: Class<*>

    val isFinal: Boolean

    val visibility: Visibility

    val label: String?

    fun get(): Any?
    fun set(value: Any?)
    
}

enum class Visibility {
    PUBLIC, PROTECTED, PRIVATE, PACKAGE_PRIVATE
}
