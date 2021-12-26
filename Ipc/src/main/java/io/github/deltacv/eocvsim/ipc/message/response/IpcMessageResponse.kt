package io.github.deltacv.eocvsim.ipc.message.response

abstract class IpcMessageResponse {
    var id = 0

    abstract val status: Boolean

    override fun toString(): String {
        return "IpcMessageResponse(type=\"${this::class.java.typeName}\", status=\"${if(status) "OK" else "ERROR"}\")"
    }
}