package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcBooleanResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.IsStreamingMessage
import io.github.deltacv.eocvsim.ipc.message.sim.StartStreamingMessage
import io.github.deltacv.eocvsim.ipc.message.sim.StopStreamingMessage


@IpcMessageHandler.Register(
    IsStreamingMessage::class
)
class IsStreamingMessageHandler : IpcMessageHandler<IsStreamingMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<IsStreamingMessage>) {
        ctx.respond(IpcBooleanResponse(ctx.eocvSim.pipelineManager.streamData != null))
    }

}

@IpcMessageHandler.Register(
    StartStreamingMessage::class
)
class StartStreamingMessageHandler : IpcMessageHandler<StartStreamingMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<StartStreamingMessage>) {
        ctx.eocvSim.pipelineManager.onUpdate.doOnce {
            try {
                with(ctx.message) {
                    ctx.eocvSim.pipelineManager.startPipelineStream(
                        opcode, width, height
                    )
                }

                ctx.respond(IpcOkResponse())
            } catch(e: IllegalStateException) {
                ctx.respond(IpcErrorResponse(e.message ?: "", e))
            }
        }
    }

}

@IpcMessageHandler.Register(
    StopStreamingMessage::class
)
class StopStreamingMessageHandler : IpcMessageHandler<StopStreamingMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<StopStreamingMessage>) {
        ctx.eocvSim.pipelineManager.onUpdate.doOnce {
            ctx.eocvSim.pipelineManager.stopPipelineStream()
        }
    }

}