package com.github.serivesmejia.eocvsim.tuner.field.numeric

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.field.NumericField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class LongField(
    instance: Any,
    reflectionField: VirtualField,
    eocvSim: EOCVSim
) : NumericField<Long>(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS) {

    init {
        _value = initialFieldValue as? Long ?: 0L
    }

    override fun createNumber(value: Double): Long = value.toLong()

    class Acceptor : TunableFieldAcceptor {
        override fun accept(clazz: Class<*>) =
            clazz == Long::class.java || clazz == java.lang.Long::class.java
    }
}
