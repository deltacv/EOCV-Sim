package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.ChangePipelineMessage
import io.github.deltacv.eocvsim.ipc.message.sim.PipelineSource

@IpcMessageHandler.Register(
    ChangePipelineMessage::class
)
class ChangePipelineMessageHandler : IpcMessageHandler<ChangePipelineMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<ChangePipelineMessage>) {
        ctx.eocvSim.pipelineManager.onUpdate.doOnce {
            val result = ctx.eocvSim.pipelineManager.changePipeline(ctx.message.pipelineName, when(ctx.message.pipelineSource) {
                PipelineSource.CLASSPATH -> com.github.serivesmejia.eocvsim.pipeline.PipelineSource.CLASSPATH
                PipelineSource.COMPILED_ON_RUNTIME -> com.github.serivesmejia.eocvsim.pipeline.PipelineSource.COMPILED_ON_RUNTIME
                PipelineSource.PYTHON_RUNTIME -> com.github.serivesmejia.eocvsim.pipeline.PipelineSource.PYTHON_RUNTIME
            }, ctx.message.force)

            if(result) {
                ctx.respond(IpcOkResponse())
            } else {
                ctx.respond(IpcErrorResponse("Requested pipeline wasn't found"))
            }
        }
    }

}