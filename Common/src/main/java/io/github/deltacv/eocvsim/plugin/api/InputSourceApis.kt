package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.opencv.core.Size

abstract class InputSourceApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val isPaused: Boolean
    abstract val data: New
    abstract val name: String
    abstract val creationTime: Long

    enum class Type {
        IMAGE,
        VIDEO,
        CAMERA,
        HTTP
    }

    sealed class New(val type: Type) {
        data class Image(val filePath: String, val size: Size) : New(Type.IMAGE)
        data class Video(val filePath: String, val size: Size) : New(Type.VIDEO)
        data class Camera(val cameraName: String, val size: Size) : New(Type.CAMERA)
        data class Http(val url: String) : New(Type.HTTP)
    }
}

abstract class InputSourceManagerApi(owner: EOCVSimPlugin) : Api(owner) {
    abstract val allSources: List<InputSourceApi>
    abstract val currentSource: InputSourceApi?

    abstract fun setInputSource(name: String): Boolean
    abstract fun getInputSource(name: String): InputSourceApi?

    abstract fun addInputSource(name: String, aNew: InputSourceApi.New): InputSourceApi
}