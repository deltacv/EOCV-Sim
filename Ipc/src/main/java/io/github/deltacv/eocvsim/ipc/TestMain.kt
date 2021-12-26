package io.github.deltacv.eocvsim.ipc

import io.github.deltacv.eocvsim.ipc.message.sim.PrintMessage

fun main() {
    val client = IpcClient()
    client.connectBlocking()

    client.broadcast(
        PrintMessage("hello world from ipc websocket client!").onResponse {
            println(it)
        }
    )
}