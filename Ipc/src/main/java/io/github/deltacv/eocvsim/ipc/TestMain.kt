package io.github.deltacv.eocvsim.ipc

import io.github.deltacv.eocvsim.ipc.message.sim.PrintMessage
import io.github.deltacv.eocvsim.ipc.security.PassToken

fun main() {
    val client = IpcClient(passToken = PassToken("eL31fCp8AHci%ZzWDtex8P2CzhpB5MGj5U&5uj#Obgnibm!r#rwq&#tWL&%lpWDJdKOJLh\$dH-#=GrQJyWkCB#sOaEnI6z#E@at\$LDtw#%g7P"))
    client.connectBlocking()

    client.broadcast(
        PrintMessage("hello world from ipc websocket client!").onResponse {
            println(it)
        }
    )
}