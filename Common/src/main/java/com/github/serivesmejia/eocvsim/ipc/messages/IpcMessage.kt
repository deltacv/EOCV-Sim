package com.github.serivesmejia.eocvsim.ipc.messages

interface IpcMessage<R: IpcMessageResponse> {

    fun onResponse(response: R)

}

interface IpcMessageResponse