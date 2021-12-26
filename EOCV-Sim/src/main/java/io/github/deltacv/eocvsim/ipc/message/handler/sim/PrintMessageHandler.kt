package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.sim.PrintMessage

@IpcMessageHandler.Register(PrintMessage::class)
class PrintMessageHandler : IpcMessageHandler<PrintMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<PrintMessage>) {
        println(ctx.message.printMessage)
        ctx.respond(IpcOkResponse())
    }

}