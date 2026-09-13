/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner.field.numeric

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.field.NumericField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor
import org.deltacv.eocvsim.virtualreflect.VirtualField

class LongField(
    instance: Any,
    reflectionField: VirtualField
) : NumericField<Long>(instance, reflectionField, AllowMode.ONLY_NUMBERS, reflectionField.get() as? Long ?: 0L) {


    override fun createNumber(value: Double): Long = value.toLong()

    class Acceptor : TunableFieldAcceptor {
        override fun accept(clazz: Class<*>) =
            clazz == Long::class.java || clazz == java.lang.Long::class.java
    }
}

