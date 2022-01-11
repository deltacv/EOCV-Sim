package io.github.deltacv.eocvsim.ipc.serialization

import com.google.gson.GsonBuilder
import io.github.deltacv.eocvsim.ipc.message.IpcMessage
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse

val ipcGson get() = GsonBuilder()
    .registerTypeHierarchyAdapter(IpcMessage::class.java, IpcMessageAdapter)
    .registerTypeHierarchyAdapter(IpcMessageResponse::class.java, IpcMessageResponseAdapter)
    .registerTypeAdapter(Any::class.java, AnyAdapter)
    .create()