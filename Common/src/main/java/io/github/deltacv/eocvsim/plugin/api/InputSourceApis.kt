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

package io.github.deltacv.eocvsim.plugin.api

import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import org.opencv.core.Size

/**
 * Represents a single input source used by the application.
 *
 * An input source provides image data to the pipeline and may originate from
 * different backends such as files, cameras, or network streams.
 *
 * Each input source is immutable in its origin configuration and is identified
 * by a unique name managed by the [InputSourceManagerApi].
 *
 * @param owner the plugin that owns this API instance
 */
abstract class InputSourceApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Whether this input source is currently paused.
     *
     * A paused source remains selected but does not actively provide new frames.
     */
    abstract val isPaused: Boolean

    /**
     * Descriptor describing how this input source was created.
     *
     * This contains the immutable origin data such as file paths, camera names,
     * URLs, and expected frame size.
     */
    abstract val data: New

    /**
     * Human-readable name of this input source.
     *
     * Names are unique within the owning [InputSourceManagerApi].
     */
    abstract val name: String

    /**
     * Timestamp (in milliseconds since epoch) indicating when this input source
     * was created.
     */
    abstract val creationTime: Long

    /**
     * Enumeration of supported input source types.
     */
    enum class Type {
        IMAGE,
        VIDEO,
        CAMERA,
        HTTP
    }

    /**
     * Sealed hierarchy describing the creation parameters for an input source.
     *
     * This is used when creating new sources and for introspection of existing
     * ones.
     */
    sealed class New(val type: Type) {

        /**
         * Image file input source.
         *
         * @param filePath path to the image file
         * @param size resolution of the image
         */
        data class Image(
            val filePath: String,
            val size: Size
        ) : New(Type.IMAGE)

        /**
         * Video file input source.
         *
         * @param filePath path to the video file
         * @param size resolution of the video frames
         */
        data class Video(
            val filePath: String,
            val size: Size
        ) : New(Type.VIDEO)

        /**
         * Camera input source.
         *
         * @param cameraName identifier of the camera device
         * @param size requested capture resolution
         */
        data class Camera(
            val cameraName: String,
            val size: Size
        ) : New(Type.CAMERA)

        /**
         * HTTP stream input source.
         *
         * @param url URL of the HTTP stream
         */
        data class Http(
            val url: String
        ) : New(Type.HTTP)
    }
}

/**
 * Manages the lifecycle and selection of input sources.
 *
 * This API is responsible for creating, storing, selecting, and retrieving
 * [InputSourceApi] instances. Only one input source may be active at a time.
 *
 * @param owner the plugin that owns this API instance
 */
abstract class InputSourceManagerApi(owner: EOCVSimPlugin) : Api(owner) {

    /**
     * Returns all registered input sources.
     */
    abstract val allSources: List<InputSourceApi>

    /**
     * Returns the currently active input source, or `null` if none is selected.
     */
    abstract val currentSource: InputSourceApi?

    /**
     * Sets the active input source by name.
     *
     * @param name the name of the input source
     * @return `true` if the source exists and was selected, `false` otherwise
     */
    abstract fun setInputSource(name: String): Boolean

    /**
     * Retrieves an input source by name.
     *
     * @param name the name of the input source
     * @return the input source, or `null` if no source with that name exists
     */
    abstract fun getInputSource(name: String): InputSourceApi?

    /**
     * Creates and registers a new input source.
     *
     * @param name unique name for the input source
     * @param aNew creation descriptor defining the source type and parameters
     * @return the newly created input source
     */
    abstract fun addInputSource(
        name: String,
        aNew: InputSourceApi.New
    ): InputSourceApi
}