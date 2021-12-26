package io.github.deltacv.eocvsim.ipc

import com.github.serivesmejia.eocvsim.util.Log
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.serialization.ipcGson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class IpcClient(val port: Int = 11026) : WebSocketClient(URI("ws://localhost:$port")) {

    companion object {
        const val TAG = "IpcClient"
    }

    private val gson = ipcGson

    private val awaitingResponseMessages = mutableMapOf<Int, IpcMessage>()

    fun broadcast(message: IpcMessage) {
        Log.trace(TAG, "Sent $message to server at port $port")
        send(gson.toJson(message))

        awaitingResponseMessages[message.id] = message
    }

    override fun onOpen(handshakedata: ServerHandshake) {
        Log.info(TAG, "Connected to server at port $port with ${handshakedata.httpStatusMessage}")
    }

    override fun onMessage(message: String) {
        val response = gson.fromJson(message, IpcMessageResponse::class.java) ?: return

        for((id, message) in awaitingResponseMessages) {
            if(id == response.id) {
                message.receiveResponse(response)
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