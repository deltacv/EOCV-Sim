package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.handler.dsl.IpcMessageHandlerDsl
import io.github.deltacv.eocvsim.ipc.message.response.IpcBooleanResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.IsStreamingMessage
import io.github.deltacv.eocvsim.ipc.message.sim.StartStreamingMessage
import io.github.deltacv.eocvsim.ipc.message.sim.StopStreamingMessage


@IpcMessageHandler.Register(IsStreamingMessage::class)
class IsStreamingMessageHandler : IpcMessageHandlerDsl<IsStreamingMessage>({
    respondWithBool { eocvSim.pipelineManager.streamData != null }
})

@IpcMessageHandler.Register(StartStreamingMessage::class)
class StartStreamingMessageHandler : IpcMessageHandlerDsl<StartStreamingMessage>({

    handle {
        eocvSim.pipelineManager.onUpdate.doOnce {
            try {
                with(message) {
                    eocvSim.pipelineManager.startPipelineStream(
                        opcode, width, height
                    )
                }

                ok()
            } catch(e: IllegalStateException) {
                error(e.message ?: "", e)
            }
        }
    }

})

@IpcMessageHandler.Register(StopStreamingMessage::class)
class StopStreamingMessageHandler : IpcMessageHandlerDsl<StopStreamingMessage>({
    handle {
        eocvSim.pipelineManager.onUpdate.doOnce {
            eocvSim.pipelineManager.stopPipelineStream()
            ok()
        }
    }
})