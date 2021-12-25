package com.github.serivesmejia.eocvsim.ipc.message.response

abstract class IpcMessageResponse {
    var id = 0

    abstract val status: Boolean
}