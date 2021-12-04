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

import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio

class OpenCvWebcam @JvmOverloads constructor(
    override val index: Int,
    resolution: Size = Size(320.0, 240.0),
    rotation: WebcamRotation = WebcamRotation.UPRIGHT,
    fps: Double = 30.0
) : WebcamBase(rotation) {

    // OpenCV's VideoCapture (not to be confused with OpenIMAJ's, called the same)
    val videoCapture = VideoCapture(index)

    override val isOpen: Boolean
        get() = videoCapture.isOpened

    override var resolution: Size
        get() {
            val width = videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH)
            val height = videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT)
            return Size(width, height)
        }
        set(value) {
            assertNotOpen("change resolution")
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, value.width)
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, value.height)
        }

    override var fps = fps
        get() = videoCapture.get(Videoio.CAP_PROP_FPS)
        set(value) {
            assertNotOpen("change fps")
            videoCapture.set(Videoio.CAP_PROP_FPS, value)
            field = value
        }

    override val name = "Webcam $index"

    init {
        this.resolution = resolution
    }

    override fun open() {
        assertNotOpen("open camera")
        videoCapture.open(index)
    }

    override fun internalRead(mat: Mat) {
        videoCapture.read(mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB)
    }

    override fun close() {
        assertOpen()
        videoCapture.release()
    }
}