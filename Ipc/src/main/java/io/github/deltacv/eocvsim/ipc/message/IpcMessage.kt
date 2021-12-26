package io.github.deltacv.eocvsim.ipc.message

import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse

interface IpcMessage {

    val id: Int

    fun receiveResponse(response: IpcMessageResponse)
    fun onResponse(callback: (IpcMessageResponse) -> Unit): IpcMessage

}

