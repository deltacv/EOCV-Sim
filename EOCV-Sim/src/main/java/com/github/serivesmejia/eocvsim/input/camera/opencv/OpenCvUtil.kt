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

package com.github.serivesmejia.eocvsim.input.camera.opencv

import org.opencv.core.Size
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio

object OpenCvUtil {

    val commonResolutions = listOf(
        Size(176.0, 144.0),
        Size(320.0, 240.0),
        Size(640.0, 360.0),
        Size(640.0, 480.0),
        Size(960.0, 540.0),
        Size(1024.0, 768.0),
        Size(1280.0, 720.0),
        Size(1280.0, 1024.0)
    )

    // adapted from https://www.learnpythonwithrune.org/find-all-possible-webcam-resolutions-with-opencv-in-python/
    // with a predefined list of commonResolutions because pulling from wikipedia is dumb lol
    @JvmStatic fun getResolutionsOf(index: Int): List<Size> {
        val camera = VideoCapture(index)

        val resolutions = mutableListOf<Size>()

        for(size in commonResolutions) {
            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, size.width)
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, size.height)

            val currentSize = Size(
                camera.get(Videoio.CAP_PROP_FRAME_WIDTH),
                camera.get(Videoio.CAP_PROP_FRAME_HEIGHT),
            )

            if(currentSize.width == 0.0 || currentSize.height == 0.0) {
                continue
            }

            if(!resolutions.contains(currentSize)) {
                resolutions.add(currentSize)
            }
        }

        camera.release()
        return resolutions
    }

}