/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.plugin.api.impl

import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.input.source.HttpSource
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import com.github.serivesmejia.eocvsim.input.source.VideoSource
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.api.InputSourceApi
import io.github.deltacv.eocvsim.plugin.api.InputSourceManagerApi
import org.opencv.core.Size

class InputSourceApiImpl(owner: EOCVSimPlugin, val internalInputSource: com.github.serivesmejia.eocvsim.input.InputSource) : InputSourceApi(owner) {
    override val isPaused by liveApiField { internalInputSource.isPaused }

    override val data by apiField {
        when(internalInputSource) {
            is CameraSource -> New.Camera(
                internalInputSource.camera?.name ?: "",
                Size(internalInputSource.videoMode?.width?.toDouble() ?: 0.0,
                    internalInputSource.videoMode?.height?.toDouble() ?: 0.0
                )
            )
            is ImageSource -> New.Image(internalInputSource.imgPath, internalInputSource.sourceSize)
            is VideoSource -> New.Video(internalInputSource.videoPath, internalInputSource.sourceSize)
            is HttpSource -> New.Http(internalInputSource.url)
            else -> throw IllegalStateException("Unknown input source type: ${internalInputSource::class.java}")
        }
    }

    override val name: String by apiField { internalInputSource.name }
    override val creationTime by apiField { internalInputSource.creationTime }



    override fun disableApi() { }
}

class InputSourceManagerApiImpl(owner: EOCVSimPlugin, val internalInputSourceManager: InputSourceManager) : InputSourceManagerApi(owner) {
    override val allSources: List<InputSourceApi> by liveApiField<List<InputSourceApi>> {
        internalInputSourceManager.sources.values.map {
            InputSourceApiImpl(owner, it)
        }
    }
    override val currentSource: InputSourceApi? by liveNullableApiField<InputSourceApi> {
        internalInputSourceManager.currentInputSource?.let { InputSourceApiImpl(owner, it) }
    }

    override fun setInputSource(name: String) = apiImpl {
        internalInputSourceManager.setInputSource(name)
    }

    override fun addInputSource(name: String, aNew: InputSourceApi.New) = apiImpl {
        val source = when(aNew) {
            is InputSourceApi.New.Camera -> TODO() // CameraSource(aNew.cameraName, aNew.size)
            is InputSourceApi.New.Image -> ImageSource(aNew.filePath, aNew.size)
            is InputSourceApi.New.Video -> VideoSource(aNew.filePath, aNew.size)
            is InputSourceApi.New.Http -> HttpSource(aNew.url)
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
