package io.github.deltacv.eocvsim.ipc

import io.github.deltacv.eocvsim.ipc.message.sim.PrintMessage
import io.github.deltacv.eocvsim.ipc.security.DestroyableString
import io.github.deltacv.eocvsim.ipc.security.PassToken
import kotlin.system.measureTimeMillis

fun main() {
    println(measureTimeMillis {
        println(PassToken(DestroyableString.random()).hash)
    })

    val client = IpcClient()
    client.connectBlocking()

    client.broadcast(
        PrintMessage("hello world from ipc websocket client!").onResponse {
            println(it)
        }
    )
}