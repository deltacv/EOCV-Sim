package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

abstract class TunableFieldApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val field: VirtualField

    abstract fun setFieldValue(index: Int, value: Any)
}

abstract class VariableTunerApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract fun newTunableFieldInstanceOf(virtualField: VirtualField, pipeline: Any): TunableFieldApi?
    abstract fun getTunableFieldWithLabel(label: String): TunableFieldApi?
}