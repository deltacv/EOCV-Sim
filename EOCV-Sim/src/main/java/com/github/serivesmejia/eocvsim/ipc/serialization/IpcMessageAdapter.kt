package com.github.serivesmejia.eocvsim.ipc.serialization

import com.github.serivesmejia.eocvsim.ipc.messages.IpcMessage
import com.google.gson.*
import java.lang.reflect.Type

object IpcMessageAdapter : JsonSerializer<IpcMessage>, JsonDeserializer<IpcMessage> {
    override fun serialize(src: IpcMessage, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {

    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): IpcMessage {

    }
}