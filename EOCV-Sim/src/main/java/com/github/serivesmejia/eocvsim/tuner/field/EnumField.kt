package com.github.serivesmejia.eocvsim.tuner.field

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.TunableEnum
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableFieldAcceptor
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class EnumField(instance: Any,
                reflectionField: VirtualField) : TunableField<Enum<*>>(instance, reflectionField, AllowMode.TEXT) {


    val values = reflectionField.type.enumConstants

    private val initialValue = initialFieldValue as Enum<*>

    private var currentValue = initialValue

    private val tunableValue by lazy { TunableEnum(currentValue, values as Array<Any>, { currentValue }, { updateEnum(it) }) }

    override val tunableValues by lazy { listOf(tunableValue) }

    private fun updateEnum(newValue: Enum<*>) {
        currentValue = newValue
        setPipelineFieldValue(newValue)
    }

    override fun init() {
        reflectionField.set(currentValue)
    }

    override fun refreshPipelineObject() {
        val curr = reflectionField.get() as? Enum<*> ?: return
        currentValue = curr
    }

    override val value: Enum<*>
        get() = currentValue

    class Acceptor : TunableFieldAcceptor {
        override fun accept(clazz: Class<*>) = clazz.isEnum
    }

}