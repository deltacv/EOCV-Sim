package com.github.serivesmejia.eocvsim.tuner.field.numeric

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.field.NumericField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class FloatField(
    instance: Any,
    reflectionField: VirtualField,
    eocvSim: EOCVSim
) : NumericField<Float>(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS_DECIMAL) {

    init {
        _value = initialFieldValue as? Float ?: 0.0f
    }

    override fun createNumber(value: Double): Float = value.toFloat()

    class Acceptor : TunableFieldAcceptor {
        override fun accept(clazz: Class<*>) =
            clazz == Float::class.java || clazz == java.lang.Float::class.java
    }
}
