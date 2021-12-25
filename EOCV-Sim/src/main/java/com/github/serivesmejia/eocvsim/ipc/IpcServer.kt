package com.github.serivesmejia.eocvsim.ipc

import com.github.serivesmejia.eocvsim.ipc.messages.IpcMessage
import com.github.serivesmejia.eocvsim.ipc.serialization.IpcMessageAdapter
import com.github.serivesmejia.eocvsim.util.Log
import com.google.gson.GsonBuilder
import io.javalin.Javalin

class IpcServer {

    companion object {
        const val TAG = "IpcServer"
    }

    val javalin = Javalin.create()

    private val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(IpcMessage::class.java, IpcMessageAdapter)
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

            ws.onMessage {
                val message = gson.fromJson(it.message(), IpcMessage::class.java)
                message.onReceive()
            }
        }.start()

        Log.info(TAG, "Opened IPC websocket at ${javalin.jettyServer().serverHost} port ${javalin.port()}")
    }

}