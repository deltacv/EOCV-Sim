/*
 * Copyright (c) 2024 Sebastian Erives
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

package io.github.deltacv.eocvsim.plugin.security.superaccess

import com.github.serivesmejia.eocvsim.util.JavaProcess
import com.github.serivesmejia.eocvsim.util.loggerForThis
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

class SuperAccessDaemonClient {

    val logger by loggerForThis()

    private val startLock = ReentrantLock()
    val startCondition = startLock.newCondition()

    // create a new WebSocket server
    private val server = WsServer(startLock, startCondition)

    fun init() {
        server.start()

        startLock.withLock {
            startCondition.await()
        }

        logger.info("SuperAccessDaemonClient initialized")
    }

    fun sendRequest(request: SuperAccessDaemon.SuperAccessMessage.Request, onResponse: (Boolean) -> Unit) {
        if(server.connections.isEmpty()) {
            onResponse(false)
            return
        }

        server.broadcast(SuperAccessDaemon.gson.toJson(request))

        server.addResponseReceiver(request.id) { response ->
            if(response is SuperAccessDaemon.SuperAccessResponse.Success) {
                onResponse(true)
            } else if(response is SuperAccessDaemon.SuperAccessResponse.Failure) {
                onResponse(false)
            }
        }
    }

    fun checkAccess(file: File): Boolean {
        val lock = ReentrantLock()
        val condition = lock.newCondition()

        val check = SuperAccessDaemon.SuperAccessMessage.Check(file.absolutePath)
        server.broadcast(SuperAccessDaemon.gson.toJson(check))

        var hasAccess = false

        server.addResponseReceiver(check.id) { response ->
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
            condition.await(3, java.util.concurrent.TimeUnit.SECONDS)
        }

        return hasAccess
    }

    private class WsServer(
        val startLock: ReentrantLock,
        val startCondition: Condition,
    ) : WebSocketServer(InetSocketAddress(0)) {
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
            val hostString = conn.localSocketAddress.hostString
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
            val response = SuperAccessDaemon.gson.fromJson(msg, SuperAccessDaemon.SuperAccessResponse::class.java)

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
                        null, listOf(port.toString())
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