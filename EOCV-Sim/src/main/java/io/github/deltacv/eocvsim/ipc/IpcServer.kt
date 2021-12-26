package io.github.deltacv.eocvsim.ipc

import com.github.serivesmejia.eocvsim.EOCVSim
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.serialization.IpcMessageAdapter
import io.github.deltacv.eocvsim.ipc.serialization.IpcMessageResponseAdapter
import com.github.serivesmejia.eocvsim.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.deltacv.eocvsim.ipc.serialization.ipcGson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.URI

class IpcServer(val eocvSim: EOCVSim, port: Int = 11026) : WebSocketServer(InetSocketAddress(port)) {

    companion object {
        const val TAG = "IpcServer"
    }

    private val handlers = mutableMapOf<Class<out IpcMessage>, IpcMessageHandler<*>>()
    private val handlerClasses get() = eocvSim.classpathScan.scanResult.ipcMessageHandlerClasses

    private val gson = ipcGson

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        Log.info(TAG, "Client ${conn.localSocketAddress.hostString} connected to IPC server")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.info(TAG, "Client ${conn.localSocketAddress.hostString} disconnected from IPC server with code $code: $reason")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val messageObject = gson.fromJson(message, IpcMessage::class.java)
        val handler = handlerFor(messageObject::class.java)

        println(message)

        handler?.internalHandle(IpcTransactionContext(messageObject, conn, gson))
    }

    override fun onError(conn: WebSocket, ex: Exception) {
        Log.info(TAG, "Exception with client ${conn.localSocketAddress.hostString}", ex)
    }

    override fun onStart() {
        Log.info(TAG, "Opened IPC websocket at ${this.address.hostString ?: "localhost"} port ${this.address.port}")
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
                    Log.warn(TAG,"Handler class ${handlerClass.typeName} doesn't implement a constructor with no parameters, it cannot be instantiated")
                    null
                }
            } else null
        }
    }

    class IpcTransactionContext<M: IpcMessage>(
        val message: M,
        val wsCtx: WebSocket,
        private val gson: Gson
    ) {

        fun respond(response: IpcMessageResponse) {
            response.id = message.id
            wsCtx.send(gson.toJson(response))
        }

    }

}