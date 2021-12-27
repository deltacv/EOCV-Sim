package io.github.deltacv.eocvsim.ipc.message.handler

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.message.AuthMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse

@IpcMessageHandler.Register(AuthMessage::class)
class AuthMessageHandler : IpcMessageHandler<AuthMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<AuthMessage>) {
        if(!ctx.server.usePassToken) {
            ctx.respond(IpcErrorResponse("No authentication needed"))
        } else {
            ctx.server.auth(ctx)
        }
    }

}