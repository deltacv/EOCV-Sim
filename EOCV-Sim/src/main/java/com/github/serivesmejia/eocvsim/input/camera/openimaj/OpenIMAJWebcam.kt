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
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.openftc.easyopencv.MatRecycler
import org.openimaj.video.capture.Device
import org.openimaj.video.capture.VideoCapture
import java.util.concurrent.ArrayBlockingQueue

class OpenIMAJWebcam @JvmOverloads constructor(
    private val device: Device,
    resolution: Size = Size(320.0, 240.0),
    rotation: WebcamRotation = WebcamRotation.UPRIGHT
) : WebcamBase(rotation) {

    companion object {
        @JvmStatic val availableWebcams: MutableList<Device> get() = VideoCapture.getVideoDevices()
    }

    var videoCapture: VideoCapture? = null
        private set

    override val isOpen get() = videoCapture != null
    override var resolution = resolution
        set(value) {
            assertNotOpen("change resolution")
            field = value
        }

    override val index = 0
    override val name: String get() = device.nameStr

    private var stream: WebcamStream? = null
    private var streamThread: Thread? = null

    private var frameMat: Mat? = null

    override fun open() {
        assertNotOpen("open camera")

        val width = resolution.width.toInt()
        val height = resolution.height.toInt()

        videoCapture = VideoCapture(width, height, device)
        frameMat = Mat(height, width, CvType.CV_8UC3)

        // creating webcam stream and starting it in another thread
        stream = WebcamStream(this)
        streamThread = Thread(stream!!, "Thread-WebcamStream-\"$name\"")

        streamThread!!.start()
    }

    override fun internalRead(mat: Mat) {
        val queue = stream!!.queue

        if(!queue.isEmpty()) {
            val webcamFrameMat = queue.poll()

            webcamFrameMat.copyTo(frameMat)
            webcamFrameMat.copyTo(mat)

            webcamFrameMat.returnMat()
        } else {
            frameMat!!.copyTo(mat)
        }
    }

    private class WebcamStream(
        val webcam: OpenIMAJWebcam,
        matQueueSize: Int = 2,
    ) : Runnable {

        private val width = webcam.resolution.width.toInt()
        private val height = webcam.resolution.height.toInt()

        val queue = EvictingBlockingQueue(ArrayBlockingQueue<MatRecycler.RecyclableMat>(matQueueSize))
        private val recycler = MatRecycler(matQueueSize + 2, height, width, CvType.CV_8UC3)

        private val pixels = ByteArray(width * height * 3)

        private val scanlineStride = width * 3
        private val pixelStride = 3

        private val grabber = ReflectOpenIMAJGrabber(webcam.videoCapture!!)

        override fun run() {
            queue.setEvictAction {
                it.returnMat()
            }

            while(!Thread.interrupted() && webcam.isOpen) {
                val mat = recycler.takeMat()
                val image = grabber.image
                val imageBytes = image.

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        pixels[y * scanlineStride + x * pixelStride + 2] = (r[y][x] * 255.0).toInt().toByte()
                        pixels[y * scanlineStride + x * pixelStride + 1] = (g[y][x] * 255.0).toInt().toByte()
                        pixels[y * scanlineStride + x * pixelStride + 0] = (b[y][x] * 255.0).toInt().toByte()
                    }
                }

                mat.put(0, 0, pixels)
                queue.offer(mat)
            }

            recycler.releaseAll()
        }

    }

    override fun close() {
        assertOpen()

        videoCapture!!.stopCapture()
        videoCapture = null
    }

}