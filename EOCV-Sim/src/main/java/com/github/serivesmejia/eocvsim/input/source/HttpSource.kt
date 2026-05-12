/*
 * Copyright (c) 2025 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.input.source

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import com.github.serivesmejia.eocvsim.config.ConfigManager
import io.github.deltacv.common.util.loggerForThis
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.wpilib.vision.camera.CvSink
import org.wpilib.vision.camera.HttpCamera
import javax.swing.filechooser.FileFilter

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
class HttpSource @JvmOverloads constructor (
    @field:JsonProperty @JvmField var url: String = ""
) : InputSource(), KoinComponent {

    override val hasSlowInitialization: Boolean get() = true

    @Transient private var camera: HttpCamera? = null

    @Transient private var cvSink: CvSink? = null

    @Transient private var lastFrame = Mat()
    @Transient private var initialized = false
    @Transient private var capTimeNanos: Long = 0
    private val configManager: ConfigManager by inject()
    private val logger by loggerForThis()

    override fun init(): Boolean {
        try {
            camera = HttpCamera("eocvsim_http_source", url)
        } catch (e: Exception) {
            logger.error("Error while initializing HTTP camera", e)
            return false
        }

        try {
            cvSink = CvSink("eocvsim_http_sink").also {
                it.source = camera
            }
        } catch (e: Exception) {
            logger.error("Error while setting up HTTP camera sink", e)
            return false
        }

        val ok = cvSink!!.grabFrame(lastFrame, configManager.config.webcamOpenTimeoutSec)
        if (ok == 0L || lastFrame.empty()) {
            logger.error("Failed to grab frame from HTTP camera: ${cvSink!!.error}")
            return false
        }

        logger.info("HttpSource initialized")
        initialized = true
        return true
    }


    override fun update(): Mat? {
        if (!initialized) return null

        val ok = cvSink?.grabFrame(lastFrame, configManager.config.webcamNewFrameTimeoutSec) ?: 0L

        if (ok == 0L || lastFrame.empty()) {
            return null
        }
        capTimeNanos = System.nanoTime()
        Imgproc.cvtColor(lastFrame, lastFrame, Imgproc.COLOR_BGR2RGBA)
        return lastFrame
    }

    override fun reset() {
        if (!initialized) return
        cvSink?.close()
        cvSink = null
        camera?.close()
        camera = null
        lastFrame.release()
        initialized = false
    }

    override fun close() {
        cvSink?.close()
        camera?.close()
    }

    override fun onPause() {
        cvSink?.close()
        camera?.close()
    }

    override fun onResume() {
        InputSourceInitializer.runWithTimeout(this) { init() }
    }

    override fun internalCloneSource(): InputSource = HttpSource(url)

    override val fileFilters: FileFilter? get() = null
    override val captureTimeNanos: Long get() = capTimeNanos

    override fun toString(): String {
        return "HttpSource($url)"
    }
}

