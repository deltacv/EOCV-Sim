package io.github.deltacv.eocvsim.ipc.message

import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse

abstract class IpcMessageBase : IpcMessage {

    companion object {
        private var idCount = -1

        fun nextId(): Int {
            idCount++
            return idCount
        }
    }

    @Transient
    private val onResponseCallbacks = mutableListOf<(IpcMessageResponse) -> Unit>()

    override val id = nextId()

    override fun receiveResponse(response: IpcMessageResponse) {
        for(callback in onResponseCallbacks) {
            callback(response)
        }
    }

    @JvmName("onResponseTyped")
    inline fun <reified R: IpcMessageResponse> onResponse(crossinline callback: (R) -> Unit): IpcMessageBase {
        onResponse {
            if(it is R) {
                callback(it)
            }
        }

        return this
    }

    override fun onResponse(callback: (IpcMessageResponse) -> Unit): IpcMessageBase {
        onResponseCallbacks.add(callback)
        return this
    }

    override fun toString() = "IpcMessageBase(type=\"${this::class.java.typeName}\", id=$id)"

}