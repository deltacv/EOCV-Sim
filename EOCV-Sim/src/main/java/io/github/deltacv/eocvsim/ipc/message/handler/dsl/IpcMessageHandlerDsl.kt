/*
 * Copyright (c) 2022 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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