package com.github.serivesmejia.eocvsim.input.source;

import com.github.serivesmejia.eocvsim.input.source.CameraSource;
import com.google.gson.*;

import java.lang.reflect.Type;

public class CameraSourceAdapter implements JsonSerializer<CameraSource> {

    @Override
    public JsonElement serialize(CameraSource src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        if(src.webcamName != null) {
            obj.addProperty("webcamName", src.webcamName);
        } else {
            obj.addProperty("webcamIndex", src.webcamIndex);
        }

        obj.add("size", context.serialize(src.size));
        obj.addProperty("createdOn", src.getCreationTime());

        return obj;
    }

}
