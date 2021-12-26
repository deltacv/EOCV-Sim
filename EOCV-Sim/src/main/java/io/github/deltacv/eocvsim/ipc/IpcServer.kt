package io.github.deltacv.eocvsim.ipc

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.Log
import com.google.gson.Gson
import io.github.deltacv.eocvsim.ipc.message.AuthMessage
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.security.DestroyableString
import io.github.deltacv.eocvsim.ipc.security.PassToken
import io.github.deltacv.eocvsim.ipc.serialization.ipcGson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class IpcServer(
    val eocvSim: EOCVSim,
    port: Int = 11026,
    val onlyAllowLocalHost: Boolean = true,
    val usePassToken: Boolean = true
) : WebSocketServer(InetSocketAddress(port)) {

    companion object {
        const val TAG = "IpcServer"
    }

    lateinit var passToken: PassToken
        private set

    private val handlers = mutableMapOf<Class<out IpcMessage>, IpcMessageHandler<*>>()
    private val handlerClasses get() = eocvSim.classpathScan.scanResult.ipcMessageHandlerClasses

    private val gson = ipcGson

    private val authorisedConnections = mutableListOf<WebSocket>()

    init {
        if(usePassToken) {
            passToken = PassToken(DestroyableString.random())
        }
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val hostString = conn.localSocketAddress.hostString

        if(onlyAllowLocalHost && hostString != "127.0.0.1" && hostString != "localhost" && hostString != "0.0.0.0") {
            conn.close(1013, "Ipc does not allow connections incoming from non-localhost addresses")
        }

        Log.info(TAG, "Client $hostString connected to IPC server")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.info(TAG, "Client ${conn.localSocketAddress.hostString} disconnected from IPC server with code $code: $reason")
        authorisedConnections.remove(conn)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val messageObject = gson.fromJson(message, IpcMessage::class.java)

        if(usePassToken && !authorisedConnections.contains(conn)) {
            if(messageObject is AuthMessage) {
                if(passToken.matches(messageObject.passToken)) {
                    authorisedConnections.add(conn)
                } else {
                    conn.close(1013, "Passtoken is incorrect")
                    return
                }
            } else {
                conn.close(1013, "Didn't authenticate with an AuthMessage before messaging started")
                return
            }
        }

        val handler = handlerFor(messageObject::class.java)

        handler?.internalHandle(IpcTransactionContext(messageObject, conn, this, gson))
    }

    override fun onError(conn: WebSocket, ex: Exception) {
        Log.info(TAG, "Exception with client ${conn.localSocketAddress.hostString}", ex)
        authorisedConnections.remove(conn)
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
        val server: IpcServer,
        private val gson: Gson
    ) {

        fun respond(response: IpcMessageResponse) {
            response.id = message.id
            wsCtx.send(gson.toJson(response))
        }

    }

}