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

package io.github.deltacv.eocvsim.ipc

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.google.gson.Gson
import io.github.deltacv.eocvsim.ipc.message.AuthMessage
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.security.PassToken
import io.github.deltacv.eocvsim.ipc.security.secureRandomString
import io.github.deltacv.eocvsim.ipc.serialization.ipcGson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class IpcServer(
    val eocvSim: EOCVSim,
    port: Int = 11026,
    val onlyAllowLocalHost: Boolean = true,
    val usePassToken: Boolean = true
) : WebSocketServer(InetSocketAddress(port)) {

    val logger by loggerForThis()

    lateinit var passToken: PassToken
        private set

    private val handlers = mutableMapOf<Class<out IpcMessage>, IpcMessageHandler<*>>()
    private val handlerClasses get() = eocvSim.classpathScan.scanResult.ipcMessageHandlerClasses

    private val gson = ipcGson

    private val authorisedConnections = mutableListOf<WebSocket>()

    private val daemonBuffers = mutableMapOf<Int, WeakReference<ByteBuffer>>()

    fun broadcastBinary(opcode: Byte, id: Short, data: ByteArray) {
        // 1 byte for opcode + 2 bytes for short id + n bytes for data
        val requiredSize = 1 + 2 + data.size

        val buffer: ByteBuffer

        synchronized(daemonBuffers) {
            buffer = daemonBuffers[requiredSize]?.get()
                ?: ByteBuffer.allocate(requiredSize)

            daemonBuffers.remove(requiredSize)
        }

        buffer.clear()

        buffer.put(opcode)
        buffer.putShort(id)
        buffer.put(data)

        buffer.clear()
        broadcast(buffer)

        synchronized(daemonBuffers) {
            daemonBuffers[requiredSize] = WeakReference(buffer)
        }
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val hostString = conn.localSocketAddress.hostString

        if(onlyAllowLocalHost && hostString != "127.0.0.1" && hostString != "localhost" && hostString != "0.0.0.0") {
            conn.close(1013, "Ipc does not allow connections incoming from non-localhost addresses")
        }

        logger.info("Client $hostString connected to IPC server")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        logger.info("Client ${conn.localSocketAddress.hostString} disconnected from IPC server with code $code: $reason")
        authorisedConnections.remove(conn)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val messageObject = gson.fromJson(message, IpcMessage::class.java)

        if(usePassToken && !authorisedConnections.contains(conn)) {
            if(messageObject !is AuthMessage) {
                conn.close(1013, "Didn't authenticate with an AuthMessage before messaging started")
                return
            }
        }

        logger.trace("Received message $messageObject with id $messageObject.id from ${conn.localSocketAddress.hostString}")

        val handler = handlerFor(messageObject::class.java)
        handler?.let {
            val ctx = IpcTransactionContext(messageObject, conn, this, gson)

            try {
                it.internalHandle(ctx)
                logger.trace("Handler ${handler::class.java.typeName} executed for message with id ${messageObject.id} from ${conn.localSocketAddress.hostString}")
            } catch(e: Exception) {
                ctx.respond(IpcErrorResponse("Exception thrown while processing message", e))
                logger.error("Handler ${handler::class.java.typeName} throwed an exception while processing message with id ${messageObject.id} from ${conn.localSocketAddress.hostString}", e)
            }
        }
    }

    override fun onError(conn: WebSocket, ex: Exception) {
        logger.error("Exception with client ${conn.localSocketAddress.hostString}", ex)
        authorisedConnections.remove(conn)
    }

    override fun onStart() {
        if(usePassToken) {
            val str = secureRandomString()
            passToken = PassToken(str)
        }

        logger.info("Opened IPC websocket at ${this.address.hostString ?: "localhost"} port ${this.address.port}")
    }

    fun auth(ctx: IpcTransactionContext<AuthMessage>) {
        if(passToken == ctx.message.passToken) {
            authorisedConnections.add(ctx.wsCtx)
            logger.info("Client ${ctx.wsCtx.localSocketAddress.hostString} successfully authenticated")
        } else {
            ctx.wsCtx.close(1013, "Passtoken is incorrect")
            return
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <M: IpcMessage> handlerFor(messageClass: Class<M>): IpcMessageHandler<M>? {
        return if(handlers.containsKey(messageClass)) {
            handlers[messageClass]!! as IpcMessageHandler<M>
        } else {
            val handlerClass = handlerClasses[messageClass]

            if(handlerClass != null) {
                try {
                    handlers[messageClass] = handlerClass.getConstructor().newInstance() as IpcMessageHandler<M>
                    handlers[messageClass]!! as IpcMessageHandler<M>
                } catch(ignored: NoSuchMethodException) {
                    logger.trace("Handler class ${handlerClass.typeName} doesn't implement a constructor with no parameters, it cannot be instantiated")
                    null
                }
            } else null
        }
    }

    class IpcTransactionContext<M: IpcMessage>(
        val message: M,
        val wsCtx: WebSocket,
        val server: IpcServer,
        private val gson: Gson
    ) {

        val eocvSim get() = server.eocvSim

        fun respond(response: IpcMessageResponse) {
            response.id = message.id
            wsCtx.send(gson.toJson(response))
        }

    }

}