package io.github.deltacv.eocvsim.ipc.message.handler.sim

import com.github.serivesmejia.eocvsim.pipeline.PipelineSource
import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.PythonPipelineSourceMessage
import io.github.deltacv.eocvsim.pipeline.py.addPythonPipeline
import io.github.deltacv.eocvsim.pipeline.py.removePythonPipeline

@IpcMessageHandler.Register(
    PythonPipelineSourceMessage::class
)
class PythonPipelineSourceMessageHandler : IpcMessageHandler<PythonPipelineSourceMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<PythonPipelineSourceMessage>) {
        val pipelineManager = ctx.eocvSim.pipelineManager

        pipelineManager.onUpdate.doOnce {
            pipelineManager.removePythonPipeline(ctx.message.pipelineName, false)

            ctx.eocvSim.visualizer.pipelineSelectorPanel?.allowPipelineSwitching = false
            pipelineManager.addPythonPipeline(ctx.message.pipelineName, ctx.message.pythonSource, true)

            if(
                pipelineManager.currentPipelineData?.displayName == ctx.message.pipelineName
                && pipelineManager.currentPipelineData?.source == PipelineSource.PYTHON_RUNTIME
            ) {
                pipelineManager.changePipeline(
                    pipelineManager.currentPipelineData!!.displayName,
                    PipelineSource.PYTHON_RUNTIME
                )
            } else {
                ctx.eocvSim.visualizer.pipelineSelectorPanel?.selectedIndex = pipelineManager.currentPipelineIndex
            }

            ctx.eocvSim.visualizer.pipelineSelectorPanel.allowPipelineSwitching = true

            ctx.respond(IpcOkResponse())
        }
    }

}