/*
 * Copyright (c) 2022 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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