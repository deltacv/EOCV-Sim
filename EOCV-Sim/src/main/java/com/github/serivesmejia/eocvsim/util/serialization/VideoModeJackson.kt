package com.github.serivesmejia.eocvsim.util.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import org.wpilib.util.PixelFormat
import org.wpilib.vision.camera.VideoMode

/**
 * Keeps persisted CameraSource.videoMode JSON stable while allowing reconstruction
 * of WPILib VideoMode, which has no Jackson-friendly default constructor.
 */
object VideoModeJackson {

    fun module(): SimpleModule = SimpleModule("VideoModeModule")
        .addSerializer(VideoMode::class.java, VideoModeSerializer)
        .addDeserializer(VideoMode::class.java, VideoModeDeserializer)

    private object VideoModeSerializer : JsonSerializer<VideoMode>() {
        override fun serialize(value: VideoMode, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeStartObject()
            gen.writeStringField("pixelFormat", value.pixelFormat.toString())
            gen.writeNumberField("width", value.width)
            gen.writeNumberField("height", value.height)
            gen.writeNumberField("fps", value.fps)
            gen.writeEndObject()
        }
    }

    private object VideoModeDeserializer : JsonDeserializer<VideoMode>() {
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): VideoMode {
            val node = parser.codec.readTree<com.fasterxml.jackson.databind.JsonNode>(parser)

            val pixelFormatValue = parsePixelFormat(node.get("pixelFormat"))
            val width = node.get("width")?.asInt() ?: 640
            val height = node.get("height")?.asInt() ?: 480
            val fps = node.get("fps")?.asInt() ?: 30

            return VideoMode(pixelFormatValue, width, height, fps)
        }

        private fun parsePixelFormat(node: com.fasterxml.jackson.databind.JsonNode?): Int {
            if (node == null || node.isNull) return 0
            if (node.isInt) return node.asInt()

            if (node.isTextual) {
                val text = node.asText().trim()
                if (text.isEmpty()) return 0

                val enumConstant = PixelFormat.entries.firstOrNull {
                    it.name.equals(text, ignoreCase = true) ||
                        it.toString().equals(text, ignoreCase = true)
                } ?: return 0

                return enumConstantToInt(enumConstant)
            }

            return 0
        }

        private fun enumConstantToInt(value: PixelFormat): Int {
            return try {
                val method = value.javaClass.getMethod("getValue")
                (method.invoke(value) as Number).toInt()
            } catch (_: Exception) {
                (value as Enum<*>).ordinal
            }
        }
    }
}

