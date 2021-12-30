package io.github.deltacv.eocvsim.ipc

import com.github.serivesmejia.eocvsim.util.Log
import io.github.deltacv.eocvsim.ipc.message.AuthMessage
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcErrorResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse
import io.github.deltacv.eocvsim.ipc.security.PassToken
import io.github.deltacv.eocvsim.ipc.serialization.ipcGson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class IpcClient(
    val port: Int = 11026,
    val passToken: PassToken? = null
) : WebSocketClient(URI("ws://localhost:$port")) {

    companion object {
        const val TAG = "IpcClient"
    }

    private val gson = ipcGson

    private val awaitingResponseMessages = mutableMapOf<Int, IpcMessage>()

    fun broadcast(message: IpcMessage) {
        send(gson.toJson(message))
        Log.trace(TAG, "Sent $message to server at port $port and id ${message.id}")

        awaitingResponseMessages[message.id] = message
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        Log.info(TAG, "Connected to server at port $port with ${handshakedata.httpStatusMessage}")

        if(passToken != null) {
            Log.info(TAG, "Authenticating to port $port...")

            broadcast(AuthMessage(passToken).onResponse {
                if(it is IpcOkResponse) {
                    Log.info(TAG, "Authentication to port $port successful")
                } else if(it is IpcErrorResponse) {
                    Log.info(TAG, "Authentication to port $port failed: ${it.reason}")
                }
            })
        }
    }

    override fun onMessage(message: String) {
        val response = gson.fromJson(message, IpcMessageResponse::class.java) ?: return

        for((id, messageObj) in awaitingResponseMessages) {
            if(id == response.id) {
                Log.trace(TAG, "Server at port $port responded to id $id with $response")
                messageObj.receiveResponse(response)
                break
            }
        }

        awaitingResponseMessages.remove(response.id)
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        Log.info(TAG, "Disconnected from server at port $port with code $code: $reason")
    }

    override fun onError(ex: Exception) {
        Log.error(TAG, "Exception threw in client connected to port $port", ex)
    }

}