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

package com.github.serivesmejia.eocvsim.input.camera

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.openimaj.image.MBFImage
import org.openimaj.video.capture.Device
import org.openimaj.video.capture.VideoCapture
import kotlin.system.measureTimeMillis

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

    private var frameMat: Mat? = null
    private var pixels: ByteArray? = null

    override fun open() {
        assertNotOpen("open camera")

        videoCapture = VideoCapture(resolution.width.toInt(), resolution.height.toInt(), device)

        frameMat = Mat(resolution.height.toInt(), resolution.width.toInt(), CvType.CV_8UC3)

        val pixelsArraySize = resolution.width.toInt() * resolution.height.toInt() * 3
        if(pixels == null || pixels!!.size != pixelsArraySize) {
            pixels = ByteArray(pixelsArraySize)
        }

        //img?.flush()
        //img = BufferedImage(resolution.width.toInt(), resolution.height.toInt(), BufferedImage.TYPE_3BYTE_BGR)
    }

    override fun internalRead(mat: Mat) {
        val milis = measureTimeMillis {
            val frame = videoCapture!!.nextFrame
            frame.copyTo(frameMat!!)
            frameMat!!.copyTo(mat)
        }
        println("took $milis ms to do shitty convert to mat")
    }

    private fun MBFImage.copyTo(mat: Mat) {
        val r = getBand(0).pixels
        val g = getBand(1).pixels
        val b = getBand(2).pixels

        val scanlineStride = width * 3
        val pixelStride = 3

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels!![y * scanlineStride + x * pixelStride + 2] = (r[y][x] * 255.0).toInt().toByte()
                pixels!![y * scanlineStride + x * pixelStride + 1] = (g[y][x] * 255.0).toInt().toByte()
                pixels!![y * scanlineStride + x * pixelStride + 0] = (b[y][x] * 255.0).toInt().toByte()
            }
        }

        mat.put(0, 0, pixels!!)
    }

    override fun close() {
        assertOpen()

        videoCapture!!.stopCapture()
        videoCapture = null
    }

}