/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.TunableFieldApi
import io.github.deltacv.eocvsim.plugin.api.VariableTunerApi
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class TunableFieldApiImpl(owner: EOCVSimPlugin, val internalTunableField: TunableField<*>) : TunableFieldApi(owner) {
    override val field: VirtualField by liveApiField { internalTunableField.reflectionField }

    override fun setFieldValue(index: Int, value: Any) = apiImpl<Unit> {
        internalTunableField.tunableValues.getOrNull(index)?.setAnyFromGui(value)
    }

    override fun disableApi() { }
}

class VariableTunerApiImpl(owner: EOCVSimPlugin, val internalTunerManager: TunerManager) : VariableTunerApi(owner) {
    override fun newTunableFieldInstanceOf(
        virtualField: VirtualField,
        pipeline: Any
    ) = apiImpl {
        val tunableField = internalTunerManager.newTunableFieldInstanceFor(virtualField, pipeline)
        if (tunableField == null) return@apiImpl null
        TunableFieldApiImpl(owner, tunableField)
    }

    override fun getTunableFieldWithLabel(label: String): TunableFieldApi? = apiImpl {
        val tunableField = internalTunerManager.getCurrentTunableFieldWithLabel(label) ?: return@apiImpl null

        TunableFieldApiImpl(owner, tunableField)
    }

    override fun disableApi() { }
}
