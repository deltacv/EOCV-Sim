package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage

@IpcMessageHandler.Register(
    ChangePipelineMessage::class
)
class ChangePipelineMessageHandler : IpcMessageHandler<ChangePipelineMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<ChangePipelineMessage>) {
        ctx.eocvSim.pipelineManager.onUpdate.doOnce {
            val result = ctx.eocvSim.pipelineManager.changePipeline(ctx.message.pipelineName, ctx.message.pipelineSource, force = ctx.message.force)

            if(result) {
                ctx.respond(IpcOkResponse())
            } else {
                ctx.respond(IpcErrorResponse("Requested pipeline wasn't found"))
            }
        }
    }

}