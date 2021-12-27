package io.github.deltacv.eocvsim.ipc.message.response

open class IpcOkResponse(var info: String = "")  : IpcMessageResponse() {
    override val status = true

    override fun toString(): String {
        return "IpcOkResponse(info=\"$info\")"
    }
}

open class IpcErrorResponse(var reason: String = "") : IpcMessageResponse() {
    override val status = false

    override fun toString(): String {
        return "IpcErrorResponse(reason=\"$reason\")"
    }
}