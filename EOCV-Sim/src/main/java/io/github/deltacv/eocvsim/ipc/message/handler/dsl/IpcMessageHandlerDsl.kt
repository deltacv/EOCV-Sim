package io.github.deltacv.eocvsim.ipc.message.handler.dsl

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcBooleanResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse

abstract class IpcMessageHandlerDsl<M: IpcMessage>(private val block: IpcMessageHandlerDsl<M>.() -> Unit) : IpcMessageHandler<M>() {

    private val handlerBlocks = mutableListOf<IpcServer.IpcTransactionContext<M>.() -> Unit>()

    private var hasBeenBuilt = false

    fun handle(block: IpcServer.IpcTransactionContext<M>.() -> Unit) {
        handlerBlocks.add(block)
    }

    fun respondWith(supplier: IpcServer.IpcTransactionContext<M>.() -> IpcMessageResponse) = handle {
        respond(supplier(this))
    }

    fun respondWithOk(infoSupplier: (IpcServer.IpcTransactionContext<M>.() -> String)? = null) = respondWith {
        IpcOkResponse(infoSupplier?.invoke(this) ?: "")
    }

    fun respondWithBool(boolSupplier: IpcServer.IpcTransactionContext<M>.() -> Boolean) = respondWith {
        IpcBooleanResponse(boolSupplier(this))
    }

    override fun handle(ctx: IpcServer.IpcTransactionContext<M>) {
        if(!hasBeenBuilt) {
            block()
            hasBeenBuilt = true
        }

        for(handlerBlock in handlerBlocks) {
            handlerBlock.invoke(ctx)
        }
    }

}