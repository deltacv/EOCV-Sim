package io.github.deltacv.eocvsim.ipc.message.handler

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import kotlin.reflect.KClass

abstract class IpcMessageHandler<M: IpcMessage> {

    @Suppress("UNCHECKED_CAST")
    internal fun internalHandle(ctx: IpcServer.IpcTransactionContext<*>) {
        handle(ctx as IpcServer.IpcTransactionContext<M>)
    }

    abstract fun handle(ctx: IpcServer.IpcTransactionContext<M>)

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS)
    annotation class Register(
        val handledMessage: KClass<out IpcMessage>
    )

}

