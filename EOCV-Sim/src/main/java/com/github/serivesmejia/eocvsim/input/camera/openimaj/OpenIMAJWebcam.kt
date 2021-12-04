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

package com.github.serivesmejia.eocvsim.input.camera.openimaj

import com.github.serivesmejia.eocvsim.input.camera.WebcamBase
import com.github.serivesmejia.eocvsim.input.camera.WebcamRotation
import com.github.serivesmejia.eocvsim.util.Log
import com.github.serivesmejia.eocvsim.util.fps.FpsLimiter
import org.bridj.Pointer
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import org.openimaj.video.capture.Device
import org.openimaj.video.capture.VideoCapture
import java.util.concurrent.ArrayBlockingQueue
import kotlin.experimental.and
import kotlin.system.measureTimeMillis

class OpenIMAJWebcam @JvmOverloads constructor(
    private val device: Device,
    resolution: Size = Size(320.0, 240.0),
    rotation: WebcamRotation = WebcamRotation.UPRIGHT,
    fps: Double = 30.0
) : WebcamBase(rotation) {

    companion object {
        @JvmStatic
        val availableWebcams: MutableList<Device>
            get() = VideoCapture.getVideoDevices()

        const val TAG = "OpenIMAJWebcam"
    }

    override val isOpen get() = videoCapture != null
    override var resolution = resolution
        set(value) {
            assertNotOpen("change resolution")
            field = value
        }

    override val index get() = availableWebcams.indexOf(device)
    override val name: String get() = device.nameStr

    override var fps = fps
        set(value) {
            assertNotOpen("change fps")
            field = value
        }

    var videoCapture: VideoCapture? = null
        private set

    private var stream: WebcamStream? = null
    private var streamThread: Thread? = null

    private var frameMat: Mat? = null
    private val closeLock = Any()

    override fun open() {
        assertNotOpen("open camera")

        try {
            val width = resolution.width.toInt()
            val height = resolution.height.toInt()

            videoCapture = VideoCapture(width, height, fps, device)
            frameMat = Mat(height, width, CvType.CV_8UC3)

            // creating webcam stream and starting it in another thread
            stream = WebcamStream(this, closeLock)
            streamThread = Thread(stream!!, "WebcamStream-\"$name\"-Thread")

            streamThread!!.start()
        } catch(e: Exception) {
            Log.error(TAG, "Error while opening camera", e)

            videoCapture = null
            streamThread?.interrupt()
        }
    }

    override fun internalRead(mat: Mat) {
        val queue = stream!!.queue

        if (!queue.isEmpty()) {
            val webcamFrameMat = queue.poll()

            webcamFrameMat.copyTo(frameMat)
            webcamFrameMat.copyTo(mat)

            webcamFrameMat.returnMat()
        } else {
            frameMat!!.copyTo(mat)
        }
    }

    override fun close() {
        assertOpen()

        synchronized(closeLock) {
            videoCapture!!.stopCapture()
            videoCapture = null
        }
    }

    private class WebcamStream(
        val webcam: OpenIMAJWebcam,
        val lock: Any,
        matQueueSize: Int = 2,
    ) : Runnable {

        private val grabber = ReflectOpenIMAJGrabber(webcam.videoCapture!!)
        private val fpsLimiter = FpsLimiter(webcam.fps)

        private val width = webcam.resolution.width.toInt()
        private val height = webcam.resolution.height.toInt()

        val queue = EvictingBlockingQueue(ArrayBlockingQueue<MatRecycler.RecyclableMat>(matQueueSize))

        private val recycler = MatRecycler(matQueueSize + 2, height, width, CvType.CV_8UC3)
        private val pixels = ByteArray(width * height * 3)

        override fun run() {
            queue.setEvictAction {
                it.returnMat()
            }

            while (!Thread.interrupted() && webcam.isOpen && webcam.videoCapture!!.hasNextFrame()) {
                synchronized(lock) {
                    val err = grabber.nextFrame()

                    if (err == -1) {
                        Log.warn(TAG, "Timed out waiting for next frame of \"${webcam.name}\"")
                        return@synchronized
                    } else if (err < -1) {
                        throw RuntimeException("Error occurred getting next frame (code: $err)")
                    }

                    val image = grabber.image ?: return@synchronized
                    val mat = recycler.takeMat()

                    val buffer = (image as Pointer<*>).getByteBuffer((width * height * 3).toLong())
                    buffer.get(pixels)

                    mat.put(0, 0, pixels)

                    queue.offer(mat)
                }

                fpsLimiter.sync()
            }

            recycler.releaseAll()
        }

    }

}