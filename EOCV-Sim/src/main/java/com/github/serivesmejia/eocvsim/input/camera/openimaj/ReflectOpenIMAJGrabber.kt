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

import org.bridj.Pointer
import org.opencv.core.Size
import org.openimaj.video.capture.VideoCapture

class ReflectOpenIMAJGrabber(private val videoCapture: VideoCapture) {

    private val clazz = VideoCapture::class.java

    private val grabberField by lazy { clazz.getDeclaredField("grabber") }

    private val grabberClazz by lazy { grabber::class.java }

    private val grabber by lazy {
        grabberField.isAccessible = true
        grabberField.get(videoCapture)
    }

    private val nextFrameMethod by lazy {
        grabberClazz.getDeclaredMethod("nextFrame").apply { isAccessible = true }
    }
    private val getImageMethod by lazy {
        grabberClazz.getDeclaredMethod("getImage").apply { isAccessible = true }
    }

    private val getWidthMethod by lazy {
        grabberClazz.getDeclaredMethod("getWidth").apply { isAccessible = true }
    }
    private val getHeightMethod by lazy {
        grabberClazz.getDeclaredMethod("getHeight").apply { isAccessible = true }
    }

    @Suppress("UNCHECKED_CAST")
    val image: Pointer<Byte>? get() {
        val img = getImageMethod.invoke(grabber)
        return if(img is Pointer<*>) img as Pointer<Byte> else null
    }

    val resolution get() = Size(
        (getWidthMethod.invoke(grabber) as Int).toDouble(),
        (getHeightMethod.invoke(grabber) as Int).toDouble()
    )

    fun nextFrame() = nextFrameMethod.invoke(grabber) as Int

}