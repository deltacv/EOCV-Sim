package com.github.serivesmejia.eocvsim.ipc.message.handler

import com.github.serivesmejia.eocvsim.ipc.IpcServer
import com.github.serivesmejia.eocvsim.ipc.message.IpcMessage
import kotlin.reflect.KClass

abstract class IpcMessageHandler<M: IpcMessage> {

    @Suppress("UNCHECKED_CAST")
    internal fun internalHandle(ctx: IpcServer.IpcTransactionContext<*>) {
        handle(ctx as IpcServer.IpcTransactionContext<M>)
    }

    abstract fun handle(ctx: IpcServer.IpcTransactionContext<M>)

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Register(
        val handledMessage: KClass<out IpcMessage>
    )

}

