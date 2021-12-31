package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.sim.PythonPipelineSourceMessage

@IpcMessageHandler.Register(
    PythonPipelineSourceMessage::class
)
class PythonPipelineSourceMessageHandler : IpcMessageHandler<PythonPipelineSourceMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<PythonPipelineSourceMessage>) {
        ctx.eocvSim.pipelineManager.pipelines
    }

}