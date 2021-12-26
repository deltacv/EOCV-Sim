package io.github.deltacv.eocvsim.ipc.message.response

open class IpcOkResponse : IpcMessageResponse() {
    override val status = true
}

open class IpcErrorResponse : IpcMessageResponse() {
    override val status = false
}