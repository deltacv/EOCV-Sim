package com.github.serivesmejia.eocvsim.ipc

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.ipc.message.handler.IpcMessageHandler
import com.github.serivesmejia.eocvsim.ipc.message.IpcMessage
import com.github.serivesmejia.eocvsim.ipc.message.response.IpcMessageResponse
import com.github.serivesmejia.eocvsim.ipc.serialization.IpcMessageAdapter
import com.github.serivesmejia.eocvsim.ipc.serialization.IpcMessageResponseAdapter
import com.github.serivesmejia.eocvsim.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.javalin.Javalin
import io.javalin.websocket.WsMessageContext

class IpcServer(val eocvSim: EOCVSim) {

    companion object {
        const val TAG = "IpcServer"
    }

    val javalin: Javalin = Javalin.create()

    private val handlers = mutableMapOf<Class<out IpcMessage>, IpcMessageHandler<*>>()
    private val handlerClasses get() = eocvSim.classpathScan.scanResult.ipcMessageHandlerClasses

    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(IpcMessage::class.java, IpcMessageAdapter)
        .registerTypeHierarchyAdapter(IpcMessageResponse::class.java, IpcMessageResponseAdapter)
        .create()

    fun start() {
        javalin.ws("/eocvsim/") { ws ->
            ws.onConnect {
                Log.info(TAG, "Client ${it.session.remoteAddress.hostString} connected to IPC server")
            }
            ws.onClose {
                Log.info(TAG, "Client ${it.session.remoteAddress.hostString}  disconnected from IPC server")
            }
            ws.onError {
                Log.info(TAG, "Exception with client ${it.session.remoteAddress.hostString}", it.error())
            }

            ws.onMessage { ctx ->
                val message = gson.fromJson(ctx.message(), IpcMessage::class.java)
                val handler = handlerFor(message::class.java)

                handler?.internalHandle(IpcTransactionContext(message, ctx, gson))
            }
        }.start()

        Log.info(TAG, "Opened IPC websocket at ${javalin.jettyServer()!!.serverHost ?: "localhost"} port ${javalin.port()}")
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
        val wsCtx: WsMessageContext,
        private val gson: Gson
    ) {

        fun respond(response: IpcMessageResponse) {
            response.id = message.id
            wsCtx.send(gson.toJson(response))
        }

    }

}