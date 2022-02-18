package io.github.deltacv.eocvsim.ipc.message.response

open class IpcOkResponse(var info: String = "")  : IpcMessageResponse() {
    override val status = true

    override fun toString(): String {
        return "IpcOkResponse(type=\"${this::class.java.typeName}\", info=\"$info\")"
    }
}

open class IpcErrorResponse(
    var reason: String = "",
    var exception: Exception? = null
) : IpcMessageResponse() {
    override val status = false

    override fun toString(): String {
        return "IpcErrorResponse(type=\"${this::class.java.typeName}\", reason=\"$reason\", exception=\"$exception\")"
    }
}