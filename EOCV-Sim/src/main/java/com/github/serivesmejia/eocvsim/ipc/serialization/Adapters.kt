package com.github.serivesmejia.eocvsim.ipc.serialization

import com.github.serivesmejia.eocvsim.ipc.message.IpcMessage
import com.github.serivesmejia.eocvsim.ipc.message.response.IpcMessageResponse

object IpcMessageAdapter : PolymorphicAdapter<IpcMessage>("message")

object IpcMessageResponseAdapter : PolymorphicAdapter<IpcMessageResponse>("messageResponse")