package com.github.serivesmejia.eocvsim.tuner.field.numeric

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.field.NumericField
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor

class DoubleField(
    instance: Any,
    reflectionField: VirtualField
) : NumericField<Double>(instance, reflectionField, AllowMode.ONLY_NUMBERS_DECIMAL, reflectionField.get() as? Double ?: 0.0) {


    override fun createNumber(value: Double): Double = value

    class Acceptor : TunableFieldAcceptor {
        override fun accept(clazz: Class<*>) =
            clazz == Double::class.java || clazz == java.lang.Double::class.java
    }
}
