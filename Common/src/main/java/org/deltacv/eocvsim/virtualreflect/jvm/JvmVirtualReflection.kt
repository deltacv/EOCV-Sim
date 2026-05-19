/*
 * Copyright (c) 2022 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.virtualreflect.jvm

import org.deltacv.eocvsim.virtualreflect.VirtualReflectContext
import org.deltacv.eocvsim.virtualreflect.VirtualReflection
import java.lang.ref.WeakReference
import java.util.*

object JvmVirtualReflection : VirtualReflection {

    private val cache = WeakHashMap<Any, WeakReference<JvmVirtualReflectContext>>()

    override fun contextOf(c: Class<*>) = cacheContextOf(null, c)

    override fun contextOf(value: Any) = cacheContextOf(value, value::class.java)

    private fun cacheContextOf(value: Any?, clazz: Class<*>): VirtualReflectContext {
        if(!cache.containsKey(value) || cache[value]?.get() == null) {
            cache[value] = WeakReference(JvmVirtualReflectContext(value, clazz))
        }

        return cache[value]!!.get()!!
    }

}
