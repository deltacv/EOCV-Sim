package io.github.deltacv.eocvsim.ipc

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.Log
import com.google.gson.Gson
import io.github.deltacv.eocvsim.ipc.message.AuthMessage
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse
import io.github.deltacv.eocvsim.ipc.security.PassToken
import io.github.deltacv.eocvsim.ipc.security.secureRandomString
import io.github.deltacv.eocvsim.ipc.serialization.ipcGson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
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
            if(messageObject !is AuthMessage) {
                conn.close(1013, "Didn't authenticate with an AuthMessage before messaging started")
                return
            }
        }

        Log.trace(TAG, "Received message ${messageObject} with id ${messageObject.id} from ${conn.localSocketAddress.hostString}")

        val handler = handlerFor(messageObject::class.java)
        handler?.let {
            it.internalHandle(IpcTransactionContext(messageObject, conn, this, gson))

            Log.trace(TAG, "Handler ${handler::class.java.typeName} executed for message with id ${messageObject.id} from ${conn.localSocketAddress.hostString}")
        }
    }

    override fun onError(conn: WebSocket, ex: Exception) {
        Log.info(TAG, "Exception with client ${conn.localSocketAddress.hostString}", ex)
        authorisedConnections.remove(conn)
    }

    override fun onStart() {
        if(usePassToken) {
            val str = secureRandomString()
            passToken = PassToken(str)
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(StringSelection(str.toString()), null)
        }

        Log.info(TAG, "Opened IPC websocket at ${this.address.hostString ?: "localhost"} port ${this.address.port}")
    }

    fun auth(ctx: IpcTransactionContext<AuthMessage>) {
        if(passToken == ctx.message.passToken) {
            authorisedConnections.add(ctx.wsCtx)
            Log.info(TAG, "Client ${ctx.wsCtx.localSocketAddress.hostString} successfully authenticated")
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
                    Log.trace(TAG,"Handler class ${handlerClass.typeName} doesn't implement a constructor with no parameters, it cannot be instantiated")
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