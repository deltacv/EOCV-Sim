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
import io.github.deltacv.eocvsim.virtualreflect.VirtualReflection
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