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