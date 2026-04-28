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
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.fps.FpsLimiter
import com.google.gson.annotations.Expose
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio
import org.openftc.easyopencv.MatRecycler
import org.slf4j.LoggerFactory
import javax.swing.filechooser.FileFilter

class VideoSource @JvmOverloads constructor(
    @Expose @JvmField var videoPath: String = "",
    @Expose @JvmField var size: Size = Size()

) : InputSource() {

    override val hasSlowInitialization: Boolean get() = true

    @Transient private var video: VideoCapture? = null

    @Transient private val fpsLimiter = FpsLimiter(30.0)

    @Transient private var lastFramePaused: MatRecycler.RecyclableMat? = null
    @Transient private var lastFrame: MatRecycler.RecyclableMat? = null

    @Transient private var initialized = false

    @Transient private var matRecycler: MatRecycler? = null

    @Transient private var lastFramePosition = 0.0

    @Transient private var capTimeNanos: Long = 0

    @Transient private val logger = LoggerFactory.getLogger(javaClass)

    override fun init(): Boolean {
        if (initialized) return false
        initialized = true

        video = VideoCapture()
        video!!.open(videoPath)

        if (!video!!.isOpened) {
            logger.error("Unable to open video $videoPath")
            return false
        }

        if (matRecycler == null) matRecycler = MatRecycler(4)

        val newFrame = matRecycler!!.takeMatOrNull()
        newFrame!!.release()

        video!!.read(newFrame)

        if (newFrame.empty()) {
            logger.error("Unable to open video $videoPath, returned Mat was empty.")
            return false
        }

        fpsLimiter.maxFPS = video!!.get(Videoio.CAP_PROP_FPS)

        newFrame.release()
        matRecycler!!.returnMat(newFrame)

        return true
    }

    override fun reset() {
        if (!initialized) return

        if (video?.isOpened == true) video!!.release()

        if (lastFrame?.isCheckedOut == true) lastFrame!!.returnMat()
        if (lastFramePaused?.isCheckedOut == true) lastFramePaused!!.returnMat()

        matRecycler?.releaseAll()

        video = null
        initialized = false
    }

    override fun close() {
        if (video?.isOpened == true) video!!.release()
        if (lastFrame?.isCheckedOut == true) lastFrame!!.returnMat()

        lastFramePaused?.let {
            it.returnMat()
            lastFramePaused = null
        }
    }

    override fun update(): Mat? {
        if (isPaused) {
            return lastFramePaused
        } else if (lastFramePaused != null) {
            lastFramePaused!!.returnMat()
            lastFramePaused = null
        }

        try {
            fpsLimiter.sync()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        if (lastFrame == null) lastFrame = matRecycler?.takeMatOrNull()
        if (video == null) return lastFrame

        val newFrame = matRecycler?.takeMatOrNull() ?: return lastFrame

        video!!.read(newFrame)
        capTimeNanos = System.nanoTime()

        if (newFrame.empty()) {
            newFrame.returnMat()
            
            reset()
            init()
            return lastFrame
        }

        if (size.area() == 0.0) size = lastFrame!!.size()

        Imgproc.cvtColor(newFrame, lastFrame, Imgproc.COLOR_BGR2RGB)
        Imgproc.resize(lastFrame, lastFrame, size, 0.0, 0.0, Imgproc.INTER_AREA)

        matRecycler?.returnMat(newFrame)

        return lastFrame
    }

    override fun onPause() {
        lastFrame?.release()

        if (lastFramePaused == null) lastFramePaused = matRecycler?.takeMatOrNull()

        video?.read(lastFramePaused)

        lastFramePaused?.let {
            Imgproc.cvtColor(it, it, Imgproc.COLOR_BGR2RGB)
            Imgproc.resize(it, it, size, 0.0, 0.0, Imgproc.INTER_AREA)
        }

        update()

        lastFramePosition = video?.get(Videoio.CAP_PROP_POS_FRAMES) ?: 0.0

        video?.release()
        video = null
    }

    override fun onResume() {
        video = VideoCapture()
        video!!.open(videoPath)
        video!!.set(Videoio.CAP_PROP_POS_FRAMES, lastFramePosition)
    }

    override fun internalCloneSource(): InputSource = VideoSource(videoPath, size)

    override fun getSize(): Size = size

    
    override fun setSize(size: Size) {
        this.size = size
    }

    override val fileFilters: FileFilter get() = FileFilters.videoMediaFilter
    override val captureTimeNanos: Long get() = capTimeNanos


    override fun toString(): String {
        return "VideoSource($videoPath, $size)"

    }

}
