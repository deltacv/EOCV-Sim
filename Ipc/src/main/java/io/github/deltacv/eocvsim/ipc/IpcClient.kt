package io.github.deltacv.eocvsim.ipc

import io.github.deltacv.eocvsim.ipc.message.AuthMessage
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.security.PassToken
import io.github.deltacv.eocvsim.ipc.serialization.ipcGson
import io.github.deltacv.eocvsim.util.loggerForThis
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

class IpcClient(
    val port: Int = 11026,
    val passToken: PassToken? = null
) : WebSocketClient(URI("ws://localhost:$port")) {

    val logger by loggerForThis()

    private val gson = ipcGson

    private val binaryHandlers = mutableMapOf<Byte, MutableList<(Int, ByteBuffer) -> Unit>>()
    private val awaitingResponseMessages = mutableMapOf<Int, IpcMessage>()

    fun broadcast(message: IpcMessage) {
        send(gson.toJson(message))
        logger.trace("Sent $message to server at port $port and id ${message.id}")

        awaitingResponseMessages[message.id] = message
    }

    fun binaryHandler(opcode: Byte, callback: (Int, ByteBuffer) -> Unit) {
        val list = binaryHandlers[opcode] ?: mutableListOf()
        list.add(callback)

        binaryHandlers[opcode] = list
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        logger.info("Connected to server at port $port with ${handshakedata.httpStatusMessage}")

        if(passToken != null) {
            logger.info("Authenticating to port $port...")

            broadcast(AuthMessage(passToken).onResponse {
                if(it is IpcOkResponse) {
                    logger.info("Authentication to port $port successful")
                } else if(it is IpcErrorResponse) {
                    logger.warn("Authentication to port $port failed: ${it.reason}")
                }
            })
        }
    }

    override fun onMessage(message: String) {
        val response = gson.fromJson(message, IpcMessageResponse::class.java) ?: return

        for((id, messageObj) in awaitingResponseMessages) {
            if(id == response.id) {
                logger.trace("Server at port $port responded to id $id with $response")
                messageObj.receiveResponse(response)
                break
            }
        }

        awaitingResponseMessages.remove(response.id)
    }

    override fun onMessage(bytes: ByteBuffer) {
        // following small spec
        val opcode = bytes.get() // first byte indicates the opcode (determines who will take this data)

        val id = bytes.int // second to fifth bytes correspond to a signed java int
                           // optionally contains an id (can be left to 0 if not needed)

        val callbacks = binaryHandlers[opcode]
        if(callbacks == null) {
            bytes.clear()
            return
        }

        for(callback in callbacks) {
            callback(id,
                bytes // remaining bytes are just user data,
                      // so we just pass the bytebuffer which currently
                      // is at the position where the data starts
            )
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        logger.warn("Disconnected from server at port $port with code $code: $reason")
    }

    override fun onError(ex: Exception) {
        logger.error("Exception threw in client connected to port $port", ex)
    }

}