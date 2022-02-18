package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.handler.dsl.IpcMessageHandlerDsl
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage

@IpcMessageHandler.Register(
    ChangePipelineMessage::class
)
class ChangePipelineMessageHandler : IpcMessageHandlerDsl<ChangePipelineMessage>({

    handle {
        eocvSim.pipelineManager.onUpdate.doOnce {
            val result = eocvSim.pipelineManager.changePipeline(
                message.pipelineName,
                message.pipelineSource,
                force = message.force
            )

            if(result) ok()
            else error("Requested pipeline wasn't found")
        }
    }

})