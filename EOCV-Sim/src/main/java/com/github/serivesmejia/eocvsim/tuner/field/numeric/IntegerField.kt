/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner.field.numeric

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.field.NumericField
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor

class IntegerField(
    instance: Any,
    reflectionField: VirtualField
) : NumericField<Int>(instance, reflectionField, AllowMode.ONLY_NUMBERS, reflectionField.get() as? Int ?: 0) {


    override fun createNumber(value: Double): Int = value.toInt()

    class Acceptor : TunableFieldAcceptor {
        override fun accept(clazz: Class<*>) =
            clazz == Int::class.java || clazz == java.lang.Integer::class.java
    }
}

