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

import com.github.serivesmejia.eocvsim.input.SourceType
import io.github.deltacv.eocvsim.input.InputSourceData
import io.github.deltacv.eocvsim.input.InputSourceType
import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.handler.dsl.IpcMessageHandlerDsl
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcStringResponse
import io.github.deltacv.eocvsim.ipc.message.response.sim.InputSourcesListResponse
import io.github.deltacv.eocvsim.ipc.message.sim.GetCurrentInputSourceMessage
import io.github.deltacv.eocvsim.ipc.message.sim.InputSourcesListMessage
import io.github.deltacv.eocvsim.ipc.message.sim.SetInputSourceMessage

@IpcMessageHandler.Register(InputSourcesListMessage::class)
class InputSourcesListMessageHandler : IpcMessageHandlerDsl<InputSourcesListMessage>({

    handle {
        eocvSim.onMainUpdate.doOnce {
            val sources = mutableListOf<InputSourceData>()

            for((name, _) in eocvSim.inputSourceManager.sources) {
                val type = eocvSim.inputSourceManager.getSourceType(name)!!

                sources.add(InputSourceData(name, when(type) {
                    SourceType.IMAGE -> InputSourceType.IMAGE
                    SourceType.CAMERA -> InputSourceType.CAMERA
                    SourceType.VIDEO -> InputSourceType.VIDEO
                    SourceType.UNKNOWN -> continue
                }))
            }

            respond(InputSourcesListResponse(sources.toTypedArray()))
        }
    }

})

@IpcMessageHandler.Register(GetCurrentInputSourceMessage::class)
class GetCurrentInputSourceMessageHandler : IpcMessageHandlerDsl<GetCurrentInputSourceMessage>({
    respondWith { IpcStringResponse(eocvSim.inputSourceManager.currentInputSource.name) }
})

@IpcMessageHandler.Register(SetInputSourceMessage::class)
class SetInputSourceMessageHandler : IpcMessageHandlerDsl<SetInputSourceMessage>({

    handle {
        eocvSim.onMainUpdate.doOnce {
            eocvSim.inputSourceManager.setInputSource(message.name)
            ok()
        }
    }

})