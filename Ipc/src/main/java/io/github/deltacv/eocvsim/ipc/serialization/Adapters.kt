package io.github.deltacv.eocvsim.ipc.serialization

import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse

object IpcMessageAdapter : PolymorphicAdapter<IpcMessage>("message")

object IpcMessageResponseAdapter : PolymorphicAdapter<IpcMessageResponse>("messageResponse")