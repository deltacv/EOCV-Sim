/*
 * Copyright (c) 2022 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.eocvsim.virtualreflect.jvm

import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import io.github.deltacv.eocvsim.virtualreflect.Visibility
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class JvmVirtualField(
    val instance: Any?,
    val field: Field
) : VirtualField {

    override val name: String = field.name
    override val type: Class<*> = field.type

    override val isFinal get() = Modifier.isFinal(field.modifiers)

    override val visibility get() = when {
        Modifier.isPublic(field.modifiers) -> Visibility.PUBLIC
        Modifier.isProtected(field.modifiers) -> Visibility.PROTECTED
        Modifier.isPrivate(field.modifiers) -> Visibility.PRIVATE
        else -> Visibility.PACKAGE_PRIVATE
    }

    private var hasLabel: Boolean? = null
    private var cachedLabel: String? = null

    override val label: String?
        get() = if(hasLabel == null) {
            val labelAnnotations = this.field.getDeclaredAnnotationsByType(Label::class.java)
            if(labelAnnotations.isEmpty()) {
                hasLabel = false
                null
            } else {
                hasLabel = true
                cachedLabel = labelAnnotations[0].name

                cachedLabel
            }
        } else if(hasLabel == true) {
            cachedLabel
        } else null

    override fun get(): Any? = field.get(instance)

    override fun set(value: Any?) {
        field.set(instance, value)
    }

}
