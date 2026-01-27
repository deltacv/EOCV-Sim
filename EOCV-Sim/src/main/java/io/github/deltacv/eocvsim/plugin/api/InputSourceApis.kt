package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.opencv.core.Size

abstract class InputSourceApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val isPaused: Boolean
    abstract val type: Type

    sealed class Type {
        data class Image(val filePath: String, val size: Size) : Type()
        data class Video(val filePath: String, val size: Size) : Type()
        data class Camera(val cameraName: String, val size: Size) : Type()
        data class Http(val url: String) : Type()
    }
}

abstract class InputSourceManagerApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val allSources: List<InputSourceApi>
    abstract val currentSource: InputSourceApi?

    abstract fun addInputSource(name: String, type: InputSourceApi.Type): InputSourceApi
    abstract fun getInputSource(name: String): InputSourceApi?
}