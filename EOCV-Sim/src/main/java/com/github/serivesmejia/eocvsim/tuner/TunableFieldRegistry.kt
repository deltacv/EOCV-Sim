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

    private val exactFields = mutableMapOf<Class<*>, (Any, VirtualField, EOCVSim) -> TunableField<*>>()
    private val acceptors = mutableListOf<Pair<TunableFieldAcceptor, (Any, VirtualField, EOCVSim) -> TunableField<*>>>()

    init {
        registerField(Int::class.javaObjectType) { target, f, sim -> IntegerField(target, f, sim) }
        registerField(Double::class.javaObjectType) { target, f, sim -> DoubleField(target, f, sim) }
        registerField(Float::class.javaObjectType) { target, f, sim -> FloatField(target, f, sim) }
        registerField(Long::class.javaObjectType) { target, f, sim -> LongField(target, f, sim) }

        registerField(String::class.java) { target, f, sim -> StringField(target, f, sim) }
        registerField(Boolean::class.javaObjectType) { target, f, sim -> BooleanField(target, f, sim) }

        registerField(org.opencv.core.Scalar::class.java) { target, f, sim -> ScalarField(target, f, sim) }
        registerField(org.opencv.core.Point::class.java) { target, f, sim -> PointField(target, f, sim) }
        registerField(org.opencv.core.Rect::class.java) { target, f, sim -> RectField(target, f, sim) }

        registerAcceptor(EnumField.Acceptor()) { target, f, sim -> EnumField(target, f, sim) }
    }

    fun registerField(type: Class<*>, constructor: (Any, VirtualField, EOCVSim) -> TunableField<*>) {
        exactFields[type] = constructor
    }

    fun registerAcceptor(acceptor: TunableFieldAcceptor, constructor: (Any, VirtualField, EOCVSim) -> TunableField<*>) {
        acceptors.add(acceptor to constructor)
    }

    fun getTunableFieldFor(field: VirtualField, pipeline: Any, eocvSim: EOCVSim): TunableField<*>? {
        if (field.isFinal) return null

        var type = field.type
        if (type.isPrimitive) {
            type = ReflectUtil.wrap(type)
        }

        var constructor = exactFields[type]
        if (constructor == null) {
            constructor = acceptors.find { it.first.accept(type) }?.second
        }

        return constructor?.invoke(pipeline, field, eocvSim)
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
