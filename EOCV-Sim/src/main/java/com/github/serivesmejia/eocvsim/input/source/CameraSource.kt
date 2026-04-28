/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.input.source

import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.util.WebcamDriver
import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import com.github.serivesmejia.eocvsim.util.StrUtil

import com.google.gson.annotations.Expose
import io.github.deltacv.steve.Webcam
import io.github.deltacv.steve.WebcamRotation
import io.github.deltacv.steve.opencv.OpenCvWebcam
import io.github.deltacv.steve.opencv.OpenCvWebcamBackend
import io.github.deltacv.steve.openpnp.OpenPnpBackend
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import org.slf4j.LoggerFactory
import javax.swing.filechooser.FileFilter

class CameraSource : InputSource {

    companion object {
        // for global use, -1 means no webcam currently in use
        @JvmStatic var currentWebcamIndex = -1
    }

    override val hasSlowInitialization: Boolean get() = true

    @delegate:Transient
    private val configManager: ConfigManager by inject()

    @Transient var webcamIndex: Int = 0

    @Expose @JvmField var webcamName: String = ""

    @Transient private var camera: Webcam? = null

    @Transient private var lastFramePaused: MatRecycler.RecyclableMat? = null
    @Transient private var lastFrame: MatRecycler.RecyclableMat? = null

    @Transient private var initialized = false

    @Transient var isLegacyByIndex = false

    @Expose @JvmField @Volatile var size: Size = Size()
    @Expose @JvmField @Volatile var rotation: WebcamRotation = WebcamRotation.UPRIGHT


    @Transient private var matRecycler = MatRecycler(4)

    @Transient private var capTimeNanos: Long = 0

    @Transient private val logger = LoggerFactory.getLogger(javaClass)

    constructor() : super() {
        createdOn = System.currentTimeMillis()
    }


    constructor(webcamName: String?, size: Size?, rotation: WebcamRotation? = WebcamRotation.UPRIGHT) : super() {
        this.webcamName = webcamName ?: ""
        this.size = size ?: Size()
        this.rotation = rotation ?: WebcamRotation.UPRIGHT
        createdOn = System.currentTimeMillis()
    }


    constructor(webcamIndex: Int, size: Size?, rotation: WebcamRotation? = WebcamRotation.UPRIGHT) : super() {
        this.webcamIndex = webcamIndex
        this.size = size ?: Size()
        this.rotation = rotation ?: WebcamRotation.UPRIGHT
        isLegacyByIndex = true
        createdOn = System.currentTimeMillis()
    }



    fun getWebcamPropertyControl() = camera?.propertyControl

    override fun setSize(size: Size) {
        this.size = size
    }

    override fun getSize(): Size = size


    override fun init(): Boolean {
        if (initialized) return false
        initialized = true

        if (rotation == WebcamRotation.UPRIGHT) rotation = WebcamRotation.UPRIGHT // already defaulted


        if (webcamName.isNotEmpty()) {

            when (configManager.config.preferredWebcamDriver) {
                WebcamDriver.OpenPnp -> Webcam.backend = OpenPnpBackend
                WebcamDriver.OpenIMAJ -> {
                    configManager.config.preferredWebcamDriver = WebcamDriver.OpenPnp

                    Webcam.backend = OpenPnpBackend
                }
                else -> {}
            }

            val webcams = Webcam.availableWebcams
            var foundWebcam = false

            for (device in webcams) {
                val name = device.name
                val similarity = StrUtil.similarity(name, webcamName)

                if (name == webcamName || similarity > 0.6) {
                    logger.info("\"$name\" compared to \"$webcamName\", similarity $similarity")
                    camera = device
                    foundWebcam = true
                    break
                }
            }

            if (!foundWebcam) {
                logger.error("Could not find webcam $webcamName")
                return false
            }
        } else {
            Webcam.backend = OpenCvWebcamBackend
            camera = OpenCvWebcam(webcamIndex, size, rotation)

        }

        camera?.resolution = size
        camera?.rotation = rotation


        try {
            camera?.open()
        } catch (ex: Exception) {
            logger.error("Error while opening camera $webcamIndex", ex)
            return false
        }

        if (camera?.isOpen != true) {
            logger.error("Unable to open camera $webcamIndex, isOpen() returned false.")
            return false
        }

        val newFrame = matRecycler.takeMatOrNull()

        camera?.read(newFrame)

        if (newFrame!!.empty()) {
            logger.error("Unable to open camera $webcamIndex, returned Mat was empty.")
            newFrame.release()
            return false
        }

        matRecycler.returnMat(newFrame)
        currentWebcamIndex = webcamIndex

        return true
    }

    override fun reset() {
        if (!initialized) return
        if (camera?.isOpen == true) camera?.close()

        if (lastFrame?.isCheckedOut == true) lastFrame?.returnMat()
        if (lastFramePaused?.isCheckedOut == true) lastFramePaused?.returnMat()

        initialized = false
    }

    override fun close() {
        if (camera?.isOpen == true) camera?.close()
        currentWebcamIndex = -1
    }

    @Transient private var lastNewFrame: MatRecycler.RecyclableMat? = null

    override fun update(): Mat? {
        lastNewFrame?.returnMat()
        lastNewFrame = null

        if (isPaused) {
            return lastFramePaused
        } else if (lastFramePaused != null) {
            lastFramePaused?.release()
            lastFramePaused?.returnMat()
            lastFramePaused = null
        }

        if (lastFrame == null) lastFrame = matRecycler.takeMatOrNull()
        if (camera == null) return lastFrame

        val newFrame = matRecycler.takeMatOrNull()
        lastNewFrame = newFrame

        camera?.read(newFrame)
        capTimeNanos = System.nanoTime()

        if (newFrame == null || newFrame.empty()) {

            newFrame?.returnMat()
            return lastFrame
        }

        if (size.area() == 0.0) size = lastFrame!!.size()

        newFrame.copyTo(lastFrame)

        newFrame.release()
        newFrame.returnMat()

        lastNewFrame = null

        return lastFrame
    }

    override fun onPause() {
        if (lastFrame != null) lastFrame?.release()
        if (lastFramePaused == null) lastFramePaused = matRecycler.takeMatOrNull()

        camera?.read(lastFramePaused!!)
        lastFramePaused?.let { Imgproc.cvtColor(it, it, Imgproc.COLOR_BGR2RGB) }

        update()

        camera?.close()
        currentWebcamIndex = -1
    }

    override fun onResume() {
        InputSourceInitializer.runWithTimeout(this) {

            camera?.open()
            camera?.isOpen == true
        }
    }

    override fun internalCloneSource(): InputSource = if (isLegacyByIndex) {
        CameraSource(webcamIndex, size, rotation)
    } else {
        CameraSource(webcamName, size, rotation)
    }


    override val fileFilters: FileFilter? get() = null
    override val captureTimeNanos: Long get() = capTimeNanos


    override fun toString(): String {
        return "CameraSource($webcamName, $webcamIndex, ${size})"
    }

}
