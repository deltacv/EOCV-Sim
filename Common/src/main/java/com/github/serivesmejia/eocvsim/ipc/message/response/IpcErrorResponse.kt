package com.github.serivesmejia.eocvsim.ipc.message.response

open class IpcErrorResponse(val cause: String) : IpcMessageResponse() {
    override val status = false
}