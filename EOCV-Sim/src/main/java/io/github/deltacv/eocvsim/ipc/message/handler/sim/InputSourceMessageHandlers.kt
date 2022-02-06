package io.github.deltacv.eocvsim.ipc.message.handler.sim

import com.github.serivesmejia.eocvsim.input.SourceType
import io.github.deltacv.eocvsim.input.InputSourceData
import io.github.deltacv.eocvsim.input.InputSourceType
import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.response.sim.InputSourcesListResponse
import io.github.deltacv.eocvsim.ipc.message.sim.InputSourcesListMessage
import io.github.deltacv.eocvsim.ipc.message.sim.SetInputSourceMessage

@IpcMessageHandler.Register(InputSourcesListMessage::class)
class InputSourcesListMessageHandler : IpcMessageHandler<InputSourcesListMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<InputSourcesListMessage>) {
        ctx.eocvSim.onMainUpdate.doOnce {
            val sources = mutableListOf<InputSourceData>()

            for((name, _) in ctx.eocvSim.inputSourceManager.sources) {
                val type = ctx.eocvSim.inputSourceManager.getSourceType(name)!!

                sources.add(InputSourceData(name, when(type) {
                    SourceType.IMAGE -> InputSourceType.IMAGE
                    SourceType.CAMERA -> InputSourceType.CAMERA
                    SourceType.VIDEO -> InputSourceType.VIDEO
                    SourceType.UNKNOWN -> continue
                }))
            }

            ctx.respond(InputSourcesListResponse(sources.toTypedArray()))
        }
    }

}

class SetInputSourceMessageHandler : IpcMessageHandler<SetInputSourceMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<SetInputSourceMessage>) {
        ctx.eocvSim.onMainUpdate.doOnce {
            ctx.eocvSim.inputSourceManager.setInputSource(ctx.message.name)

            ctx.respond(IpcOkResponse())
        }
    }

}