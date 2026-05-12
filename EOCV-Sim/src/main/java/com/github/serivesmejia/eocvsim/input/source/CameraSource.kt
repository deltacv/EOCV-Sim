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

import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import com.google.gson.annotations.Expose
import org.koin.core.component.KoinComponent
import org.opencv.core.Mat
import org.opencv.core.Size
import org.openftc.easyopencv.MatRecycler
import org.slf4j.LoggerFactory
import org.wpilib.vision.camera.CvSink
import org.wpilib.vision.camera.UsbCamera
import org.wpilib.vision.camera.VideoMode
import javax.swing.filechooser.FileFilter

class CameraSource : InputSource, KoinComponent {

    companion object {
        @JvmStatic var currentWebcamIndex = -1
    }

    override val hasSlowInitialization: Boolean get() = true

    @Transient var webcamIndex: Int = 0

    @Expose @JvmField var webcamName: String = ""

    @Transient var camera: UsbCamera? = null
        private set
    @Transient private var cvSink: CvSink? = null

    @Transient private var lastFramePaused: MatRecycler.RecyclableMat? = null
    @Transient private var lastFrame: MatRecycler.RecyclableMat? = null

    @Transient private var initialized = false

    @Transient var isLegacyByIndex = false

    @Expose @JvmField @Volatile var size: Size = Size()

    @Transient private var matRecycler = MatRecycler(4)
    @Transient private var capTimeNanos: Long = 0
    @Transient private val logger = LoggerFactory.getLogger(javaClass)

    constructor() : super() {
        createdOn = System.currentTimeMillis()
    }

    constructor(webcamName: String?, size: Size?) : super() {
        this.webcamName = webcamName ?: ""
        this.size = size ?: Size()
        createdOn = System.currentTimeMillis()
    }

    constructor(webcamIndex: Int, size: Size?) : super() {
        this.webcamIndex = webcamIndex
        this.size = size ?: Size()
        isLegacyByIndex = true
        createdOn = System.currentTimeMillis()
    }

    override fun setSize(size: Size) {
        this.size = size
    }

    override fun getSize(): Size = size

    override fun init(): Boolean {
        if (initialized) return false
        initialized = true

        val cam = if (webcamName.isNotEmpty()) {
            // find by name
            val infos = UsbCamera.enumerateUsbCameras()
            val info = infos.firstOrNull { it.name == webcamName }
            if (info == null) {
                logger.error("Could not find webcam \"$webcamName\"")
                return false
            }
            UsbCamera(webcamName, info.dev)
        } else {
            UsbCamera("camera$webcamIndex", webcamIndex)
        }

        camera = cam

        if (size.width > 0 && size.height > 0) {
            cam.setResolution(size.width.toInt(), size.height.toInt())
        }

        // pick highest fps mode available at requested resolution
        val mode = cam.videoMode
        cam.videoMode = VideoMode(mode.pixelFormat, mode.width, mode.height, mode.fps)

        cvSink = CvSink("eocvsim_sink_$webcamIndex").also {
            it.source = cam
        }

        // test frame
        val testMat = matRecycler.takeMatOrNull()!!
        val grabbed = cvSink!!.grabFrame(testMat)
        if (grabbed == 0L) {
            logger.error("Unable to open camera $webcamIndex: ${cvSink!!.error}")
            testMat.returnMat()
            return false
        }

        if (testMat.empty()) {
            logger.error("Unable to open camera $webcamIndex, returned Mat was empty.")
            testMat.returnMat()
            return false
        }

        matRecycler.returnMat(testMat)
        currentWebcamIndex = webcamIndex

        return true
    }

    override fun reset() {
        if (!initialized) return

        cvSink?.close()
        cvSink = null
        camera?.close()
        camera = null

        if (lastFrame?.isCheckedOut == true) lastFrame?.returnMat()
        if (lastFramePaused?.isCheckedOut == true) lastFramePaused?.returnMat()

        initialized = false
    }

    override fun close() {
        cvSink?.close()
        cvSink = null
        camera?.close()
        camera = null
        currentWebcamIndex = -1
    }

    @Transient private var lastNewFrame: MatRecycler.RecyclableMat? = null

    override fun update(): Mat? {
        lastNewFrame?.returnMat()
        lastNewFrame = null

        if (isPaused) return lastFramePaused

        if (lastFramePaused != null) {
            lastFramePaused?.release()
            lastFramePaused?.returnMat()
            lastFramePaused = null
        }

        if (lastFrame == null) lastFrame = matRecycler.takeMatOrNull()
        if (cvSink == null) return lastFrame

        val newFrame = matRecycler.takeMatOrNull()!!
        lastNewFrame = newFrame

        val grabbed = cvSink!!.grabFrameNoTimeout(newFrame)
        capTimeNanos = System.nanoTime()

        if (grabbed == 0L || newFrame.empty()) {
            newFrame.returnMat()
            lastNewFrame = null
            return lastFrame
        }

        if (size.area() == 0.0) size = newFrame.size()

        newFrame.copyTo(lastFrame)
        newFrame.release()
        newFrame.returnMat()
        lastNewFrame = null

        return lastFrame
    }

    override fun onPause() {
        if (lastFramePaused == null) lastFramePaused = matRecycler.takeMatOrNull()
        cvSink?.grabFrameNoTimeout(lastFramePaused!!)

        cvSink?.close()
        cvSink = null
        camera?.close()
        camera = null
        currentWebcamIndex = -1
    }

    override fun onResume() {
        InputSourceInitializer.runWithTimeout(this) {
            init()
        }
    }

    override fun internalCloneSource(): InputSource = if (isLegacyByIndex) {
        CameraSource(webcamIndex, size)
    } else {
        CameraSource(webcamName, size)
    }

    override val fileFilters: FileFilter? get() = null
    override val captureTimeNanos: Long get() = capTimeNanos

    override fun toString() = "CameraSource($webcamName, $webcamIndex, $size)"
}