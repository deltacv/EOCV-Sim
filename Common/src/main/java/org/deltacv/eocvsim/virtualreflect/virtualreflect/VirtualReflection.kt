/*
 * Copyright (c) 2022 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.virtualreflect

/**
 * Interface for virtual reflection, which allows to get a [VirtualReflectContext] from a class or an instance
 */
interface VirtualReflection {

    fun contextOf(c: Class<*>): VirtualReflectContext?

    fun contextOf(value: Any): VirtualReflectContext?

}
