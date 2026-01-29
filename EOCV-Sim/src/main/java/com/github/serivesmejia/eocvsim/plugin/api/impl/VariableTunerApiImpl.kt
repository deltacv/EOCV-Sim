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

package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunerManager
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.TunableFieldApi
import io.github.deltacv.eocvsim.plugin.api.VariableTunerApi
import io.github.deltacv.eocvsim.virtualreflect.VirtualField

class TunableFieldApiImpl(owner: EOCVSimPlugin, val internalTunableField: TunableField<*>) : TunableFieldApi(owner) {
    override val field: VirtualField by liveApiField { internalTunableField.reflectionField }

    override fun setFieldValue(index: Int, value: Any) = apiImpl {
        internalTunableField.setFieldValue(index, value)
    }

    override fun disableApi() { }
}

class VariableTunerApiImpl(owner: EOCVSimPlugin, val internalTunerManager: TunerManager) : VariableTunerApi(owner) {
    override fun newTunableFieldInstanceOf(
        virtualField: VirtualField,
        pipeline: Any
    ) = apiImpl {
        val tunableField = internalTunerManager.newTunableFieldInstanceFor<Any>(virtualField, pipeline)
        TunableFieldApiImpl(owner, tunableField)
    }

    override fun getTunableFieldWithLabel(label: String): TunableFieldApi? = apiImpl {
        val tunableField = internalTunerManager.getCurrentTunableFieldWithLabel(label) ?: return@apiImpl null

        TunableFieldApiImpl(owner, tunableField)
    }

    override fun disableApi() { }
}