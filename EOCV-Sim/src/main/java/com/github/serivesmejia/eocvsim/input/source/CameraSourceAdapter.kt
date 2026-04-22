package com.github.serivesmejia.eocvsim.input.source

import com.google.gson.*
import java.lang.reflect.Type

class CameraSourceAdapter : JsonSerializer<CameraSource> {

    override fun serialize(src: CameraSource, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()

        if (src.webcamName.isNotEmpty()) {
            obj.addProperty("webcamName", src.webcamName)
        } else {
            obj.addProperty("webcamIndex", src.webcamIndex)
        }

        obj.add("size", context.serialize(src.size))
        obj.addProperty("createdOn", src.creationTime)


        return obj
    }

}
