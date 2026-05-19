/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.ReflectUtil
import org.deltacv.eocvsim.virtualreflect.VirtualField
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

