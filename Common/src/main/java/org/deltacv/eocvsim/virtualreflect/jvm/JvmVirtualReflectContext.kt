/*
 * Copyright (c) 2022 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.virtualreflect.jvm

import org.deltacv.eocvsim.virtualreflect.VirtualReflectContext
import org.deltacv.eocvsim.virtualreflect.VirtualField
import java.lang.reflect.Field

class JvmVirtualReflectContext(
    val instance: Any? = null,
    val clazz: Class<*>
) : VirtualReflectContext {

    override val name: String = clazz.name
    override val simpleName: String = clazz.simpleName

    private val cachedVirtualFields = mutableMapOf<Field, JvmVirtualField>()

    override val fields: Array<VirtualField> = clazz.fields.map { virtualFieldFor(it) }.toTypedArray()

    override fun getField(name: String): VirtualField? {
        val field = clazz.getField(name) ?: return null
        return virtualFieldFor(field)
    }

    override fun getLabeledField(label: String): VirtualField? {
        var labeledField: VirtualField? = null

        for(field in fields) {
            if(field.label == label) {
                labeledField = field
                break
            }
        }

        return labeledField
    }

    private fun virtualFieldFor(field: Field): JvmVirtualField {
        if(!cachedVirtualFields.containsKey(field)) {
            cachedVirtualFields[field] = JvmVirtualField(instance, field)
        }

        return cachedVirtualFields[field]!!
    }

}
