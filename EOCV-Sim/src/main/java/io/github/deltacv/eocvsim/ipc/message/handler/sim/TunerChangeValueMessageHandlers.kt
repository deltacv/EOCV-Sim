/*
 * Copyright (c) 2022 Sebastian Erives
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

package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.handler.dsl.IpcMessageHandlerDsl
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValueMessage
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValuesMessage

@IpcMessageHandler.Register(TunerChangeValueMessage::class)
class TunerChangeValueMessageHandler : IpcMessageHandlerDsl<TunerChangeValueMessage>({

    handle {
        eocvSim.pipelineManager.onUpdate.doOnce {
            val field = eocvSim.tunerManager.getTunableFieldWithLabel(message.label)
                ?: return@doOnce

            field.setFieldValue(message.index, message.value)

            eocvSim.pipelineManager.requestSetPaused(false)
        }
    }

})

@IpcMessageHandler.Register(TunerChangeValuesMessage::class)
class TunerChangeValuesMessageHandler : IpcMessageHandlerDsl<TunerChangeValuesMessage>({

    handle {
        eocvSim.pipelineManager.onUpdate.doOnce {
            val field = eocvSim.tunerManager.getTunableFieldWithLabel(message.label)
                ?: return@doOnce

            for((i, value) in message.values.withIndex()) {
                field.setFieldValue(i, value)
            }

            eocvSim.pipelineManager.requestSetPaused(false)
        }
    }

})