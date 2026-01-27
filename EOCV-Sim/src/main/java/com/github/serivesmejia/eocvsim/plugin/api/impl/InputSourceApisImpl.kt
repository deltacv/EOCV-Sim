package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.input.source.HttpSource
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import com.github.serivesmejia.eocvsim.input.source.VideoSource
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.InputSourceApi
import io.github.deltacv.eocvsim.plugin.api.InputSourceManagerApi

class InputSourceApiImpl(owner: EOCVSimPlugin, val internalInputSource: com.github.serivesmejia.eocvsim.input.InputSource) : InputSourceApi(owner) {
    override val isPaused by liveApiField { internalInputSource.paused }

    override val type by apiField {
        when(internalInputSource) {
            is CameraSource -> Type.Camera(internalInputSource.webcamName, internalInputSource.size)
            is ImageSource -> Type.Image(internalInputSource.imgPath, internalInputSource.size)
            is VideoSource -> Type.Video(internalInputSource.videoPath, internalInputSource.size)
            is HttpSource -> Type.Http(internalInputSource.url)
            else -> throw IllegalStateException("Unknown input source type: ${internalInputSource::class.java}")
        }
    }

    override fun disableApi() { }
}

class InputSourceManagerApiImpl(owner: EOCVSimPlugin, val internalInputSourceManager: InputSourceManager) : InputSourceManagerApi(owner) {
    override val allSources: List<InputSourceApi> by liveApiField {
        internalInputSourceManager.sources.values.map {
            InputSourceApiImpl(owner, it)
        }
    }
    override val currentSource: InputSourceApi? by liveNullableApiField {
        internalInputSourceManager.currentInputSource?.let { InputSourceApiImpl(owner, it) }
    }

    override fun addInputSource(name: String, type: InputSourceApi.Type) = apiImpl {
        val source = when(type) {
            is InputSourceApi.Type.Camera -> CameraSource(type.cameraName, type.size)
            is InputSourceApi.Type.Image -> ImageSource(type.filePath, type.size)
            is InputSourceApi.Type.Video -> VideoSource(type.filePath, type.size)
            is InputSourceApi.Type.Http -> HttpSource(type.url)
        }

        internalInputSourceManager.addInputSource(name, source)

        InputSourceApiImpl(owner, source)
    }

    override fun getInputSource(name: String) = apiImpl {
        val source = internalInputSourceManager.sources[name] ?: return@apiImpl null // not found
        InputSourceApiImpl(owner, source)
    }

    override fun disableApi() { }
}