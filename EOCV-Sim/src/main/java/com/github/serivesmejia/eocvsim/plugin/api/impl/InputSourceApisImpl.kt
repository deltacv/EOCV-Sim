/*
 * Copyright (c) 2026 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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

class InputSourceApiImpl(owner: EOCVSimPlugin, val internalInputSource: com.github.serivesmejia.eocvsim.input.InputSource) : InputSourceApi(owner) {
    override val isPaused by liveApiField { internalInputSource.paused }

    override val data by apiField {
        when(internalInputSource) {
            is CameraSource -> New.Camera(internalInputSource.webcamName, internalInputSource.size)
            is ImageSource -> New.Image(internalInputSource.imgPath, internalInputSource.size)
            is VideoSource -> New.Video(internalInputSource.videoPath, internalInputSource.size)
            is HttpSource -> New.Http(internalInputSource.url)
            else -> throw IllegalStateException("Unknown input source type: ${internalInputSource::class.java}")
        }
    }

    override val name: String by apiField { internalInputSource.name }
    override val creationTime by apiField { internalInputSource.creationTime }

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

    override fun setInputSource(name: String) = apiImpl {
        internalInputSourceManager.setInputSource(name)
    }

    override fun addInputSource(name: String, aNew: InputSourceApi.New) = apiImpl {
        val source = when(aNew) {
            is InputSourceApi.New.Camera -> CameraSource(aNew.cameraName, aNew.size)
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