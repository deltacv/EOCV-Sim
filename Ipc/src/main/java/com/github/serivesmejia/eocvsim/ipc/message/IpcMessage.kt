package com.github.serivesmejia.eocvsim.ipc.message

import com.github.serivesmejia.eocvsim.ipc.message.response.IpcMessageResponse

interface IpcMessage {

    val id: Int

    fun receiveResponse(response: IpcMessageResponse)
    fun onResponse(callback: (IpcMessageResponse) -> Unit)

}

