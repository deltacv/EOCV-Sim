/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.security.superaccess

import com.github.serivesmejia.eocvsim.util.JavaProcess
import com.github.serivesmejia.eocvsim.util.serialization.JacksonJsonSupport
import org.deltacv.common.util.loggerForThis
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.File
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias ResponseReceiver = (SuperAccessDaemon.SuperAccessResponse) -> Unit
typealias ResponseCondition = (SuperAccessDaemon.SuperAccessResponse) -> Boolean

private data class AccessCache(
    val file: String,
    val hasAccess: Boolean,
    val timestamp: Long
)

class SuperAccessDaemonClient(
    val cacheTTLMillis: Long = 3_000,
    val autoacceptOnTrusted: Boolean
) {

    val logger by loggerForThis()

    private val startLock = ReentrantLock()
    val startCondition = startLock.newCondition()

    private val cacheLock = Any()
    private val accessCache = mutableMapOf<String, AccessCache>()

    // create a new WebSocket server
    private var server: WsServer? = null
    
    private var isInitialized = false

    fun initIfNeeded() {
        if (isInitialized) return
        
        server = WsServer(startLock, startCondition, autoacceptOnTrusted)
        server!!.start()

        startLock.withLock {
            startCondition.await()
        }

        logger.info("SuperAccessDaemonClient initialized")
        isInitialized = true
    }

    fun sendRequest(request: SuperAccessDaemon.SuperAccessMessage.Request, onResponse: (Boolean) -> Unit) {
        initIfNeeded()
        
        if(server!!.connections.isEmpty()) {
            onResponse(false)
            return
        }

        server!!.broadcast(JacksonJsonSupport.ipcMapper.writeValueAsString(request))

        server!!.addResponseReceiver(request.id) { response ->
            val result = when (response) {
                is SuperAccessDaemon.SuperAccessResponse.Success -> {
                    onResponse(true)
                    true
                }

                is SuperAccessDaemon.SuperAccessResponse.Failure -> {
                    onResponse(false)
                    false
                }
            }

            synchronized(cacheLock) {
                val newCache = AccessCache(request.pluginPath, result, System.currentTimeMillis())
                accessCache[request.pluginPath] = newCache
            }
        }
    }

    fun checkAccess(file: File): Boolean {
        synchronized(cacheLock) {
            if (accessCache.containsKey(file.absolutePath)) {
                val currentCache = accessCache[file.absolutePath]!!

                if (System.currentTimeMillis() - currentCache.timestamp < cacheTTLMillis) {
                    return currentCache.hasAccess
                }
            }
        }
        
        initIfNeeded()

        val lock = ReentrantLock()
        val condition = lock.newCondition()

        val check = SuperAccessDaemon.SuperAccessMessage.Check(file.absolutePath)
        server!!.broadcast(JacksonJsonSupport.ipcMapper.writeValueAsString(check))

        var hasAccess = false

        server!!.addResponseReceiver(check.id) { response ->
            if(response is SuperAccessDaemon.SuperAccessResponse.Success) {
                hasAccess = true

                lock.withLock {
                    condition.signalAll()
                }
            } else if(response is SuperAccessDaemon.SuperAccessResponse.Failure) {
                hasAccess = false

                lock.withLock {
                    condition.signalAll()
                }
            }
        }

        lock.withLock {
            condition.await(5, java.util.concurrent.TimeUnit.SECONDS)
        }

        synchronized(cacheLock) {
            val newCache = AccessCache(file.absolutePath, hasAccess, System.currentTimeMillis())
            accessCache[file.absolutePath]  = newCache
        }

        return hasAccess
    }

    private class WsServer(
        val startLock: ReentrantLock,
        val startCondition: Condition,
        val autoacceptOnTrusted: Boolean
    ) : WebSocketServer(InetSocketAddress("127.0.0.1", 0)) {
        // create an executor with 1 thread
        private val executor = Executors.newSingleThreadExecutor()

        val logger by loggerForThis()

        private val responseReceiverLock = Any()
        private val responseReceiver = mutableMapOf<ResponseCondition, ResponseReceiver>()

        private val pendingRequests = mutableMapOf<Int, ResponseReceiver>()
        private var processRestarts = 0

        // Notify all pending requests if the process dies
        private fun notifyPendingRequestsOfFailure() {
            pendingRequests.forEach { key, value ->
                value(SuperAccessDaemon.SuperAccessResponse.Failure(key))
            }
            pendingRequests.clear()
        }

        override fun onOpen(conn: WebSocket, p1: ClientHandshake?) {
            val hostString = conn.remoteSocketAddress.hostString
            if(hostString != "127.0.0.1" && hostString != "localhost" && hostString != "0.0.0.0") {
                logger.warn("Connection from ${conn.remoteSocketAddress} refused, only localhost connections are allowed")
                conn.close(1013, "Ipc does not allow connections incoming from non-localhost addresses")
            }

            logger.info("SuperAccessDaemon is here.")

            processRestarts = 0

            startLock.withLock {
                startCondition.signalAll()
            }
        }

        override fun onClose(
            p0: WebSocket?,
            p1: Int,
            p2: String?,
            p3: Boolean
        ) {
            logger.info("SuperAccessDaemon is gone.")
            notifyPendingRequestsOfFailure() // Notify all waiting clients
        }

        override fun onMessage(ws: WebSocket, msg: String) {
            val response = JacksonJsonSupport.ipcMapper.readValue(msg, SuperAccessDaemon.SuperAccessResponse::class.java)

            synchronized(responseReceiverLock) {
                for ((condition, receiver) in responseReceiver.toMap()) {
                    if (condition(response)) {
                        receiver(response)
                    }
                }
            }

            pendingRequests.remove(response.id)
        }

        override fun onError(p0: WebSocket?, p1: Exception?) {
            logger.error("SuperAccessDaemon error", p1)
        }

        override fun onStart() {
            fun startProcess() {
                executor.submit {
                    logger.info("Starting SuperAccessDaemon on port $port")

                    val exitCode = JavaProcess.exec(
                        SuperAccessDaemon::class.java,
                        JavaProcess.SLF4JIOReceiver(logger),
                        listOf("-Dlog4j.configurationFile=log4j2_nofile.xml"),
                        listOf(port.toString(), autoacceptOnTrusted.toString())
                    )

                    if(processRestarts == 6) {
                        logger.error("SuperAccessDaemon restarted too many times, aborting.")
                        return@submit
                    }

                    logger.error("SuperAccessDaemon exited with code $exitCode, restarting...")
                    startProcess() // restart the process

                    processRestarts++
                }
            }

            startProcess()
        }

        fun addResponseReceiver(id: Int, receiver: ResponseReceiver) {
            pendingRequests[id] = receiver

            synchronized(responseReceiverLock) {
                responseReceiver[{ it.id == id }] = receiver
            }
        }
    }

}
