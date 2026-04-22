/*
 * Copyright (c) 2026 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.tuner

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import com.github.serivesmejia.eocvsim.tuner.field.numeric.*
import com.github.serivesmejia.eocvsim.tuner.field.*
import com.github.serivesmejia.eocvsim.tuner.field.cv.*

/**
 * A manual registry for tunable fields, eliminating the need for classpath scanning
 * and complex reflection constructor invocation.
 */
object TunableFieldRegistry {

    private val exactFields = mutableMapOf<Class<*>, (Any, VirtualField) -> TunableField<*>>()
    private val acceptors = mutableListOf<Pair<TunableFieldAcceptor, (Any, VirtualField) -> TunableField<*>>>()

    init {
        registerField(Int::class.javaObjectType) { target, f -> IntegerField(target, f) }
        registerField(Double::class.javaObjectType) { target, f -> DoubleField(target, f) }
        registerField(Float::class.javaObjectType) { target, f -> FloatField(target, f) }
        registerField(Long::class.javaObjectType) { target, f -> LongField(target, f) }

        registerField(String::class.java) { target, f -> StringField(target, f) }
        registerField(Boolean::class.javaObjectType) { target, f -> BooleanField(target, f) }

        registerField(org.opencv.core.Scalar::class.java) { target, f -> ScalarField(target, f) }
        registerField(org.opencv.core.Point::class.java) { target, f -> PointField(target, f) }
        registerField(org.opencv.core.Rect::class.java) { target, f -> RectField(target, f) }

        registerAcceptor(EnumField.Acceptor()) { target, f -> EnumField(target, f) }
    }

    fun registerField(type: Class<*>, constructor: (Any, VirtualField) -> TunableField<*>) {
        exactFields[type] = constructor
    }

    fun registerAcceptor(acceptor: TunableFieldAcceptor, constructor: (Any, VirtualField) -> TunableField<*>) {
        acceptors.add(acceptor to constructor)
    }

    fun getTunableFieldFor(field: VirtualField, pipeline: Any): TunableField<*>? {
        if (field.isFinal) return null

        var type = field.type
        if (type.isPrimitive) {
            type = ReflectUtil.wrap(type)
        }

        var constructor = exactFields[type]
        if (constructor == null) {
            constructor = acceptors.find { it.first.accept(type) }?.second
        }

        return constructor?.invoke(pipeline, field)
    }


    fun hasTunableFieldFor(type: Class<*>): Boolean {
        var t = type
        if (t.isPrimitive) {
            t = ReflectUtil.wrap(t)
        }
        if (exactFields.containsKey(t)) return true
        return acceptors.any { it.first.accept(t) }
    }

    fun reset() {
        exactFields.clear()
        acceptors.clear()
    }
}
