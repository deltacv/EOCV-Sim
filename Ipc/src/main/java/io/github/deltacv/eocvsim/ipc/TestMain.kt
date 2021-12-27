package io.github.deltacv.eocvsim.ipc

import io.github.deltacv.eocvsim.ipc.message.sim.PrintMessage
import io.github.deltacv.eocvsim.ipc.security.PassToken

fun main() {
    val client = IpcClient(passToken = PassToken("NhpY_ql3MW4X7Autkql5JK7YoCDgjSz&cfQN4qWt5#Hk7MLR7olEYs-&bERngC0d%zaREu8NbuSe=Or9Ys5f&@e#KNtP"))
    client.connectBlocking()

    client.broadcast(
        PrintMessage("hello world from ipc websocket client!").onResponse {
            println(it)
        }
    )
}