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

import io.github.deltacv.eocvsim.virtualreflect.VirtualReflectContext
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import java.lang.reflect.Field

class JvmVirtualReflectContext(
    val instance: Any? = null,
    val clazz: Class<*>
) : VirtualReflectContext {

    override val name = clazz.name
    override val simpleName = clazz.simpleName

    private val cachedVirtualFields = mutableMapOf<Field, JvmVirtualField>()

    override fun getFields(): Array<VirtualField> = clazz.fields.map { virtualFieldFor(it) }.toTypedArray()

    override fun getField(name: String): VirtualField? {
        val field = clazz.getField(name) ?: return null
        return virtualFieldFor(field)
    }

    private fun virtualFieldFor(field: Field): JvmVirtualField {
        if(!cachedVirtualFields.containsKey(field)) {
            cachedVirtualFields[field] = JvmVirtualField(instance, field)
        }

        return cachedVirtualFields[field]!!
    }

}