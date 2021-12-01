/*
 * Copyright (c) 2021 Sebastian Erives
 * Some code extracted from EasyOpenCV written by the OpenFTC team (c) 2019 where indicated
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

import org.opencv.core.Core
import org.opencv.core.Mat

abstract class WebcamBase(rotation: WebcamRotation) : Webcam {

    override var rotation = rotation
        set(value) {
            assertNotOpen("change rotation of camera")
            field = value
        }

    private val frame = Mat()

    override fun read(mat: Mat) {
        assertOpen()

        val rotateCode = mapRotationToRotateCode(rotation)

        if(rotateCode != -1) {
            internalRead(frame)
            Core.rotate(frame, mat, rotateCode)
        } else {
            internalRead(mat)
        }
    }

    protected abstract fun internalRead(mat: Mat)

    protected fun assertOpen() {
        if(!isOpen) {
            throw IllegalStateException("Camera \"$name\" is not open")
        }
    }

    protected fun assertNotOpen(operation: String) {
        if(isOpen) {
            throw IllegalStateException("Can't $operation while camera \"$name\" is open")
        }
    }

    // extracted from https://github.com/OpenFTC/EasyOpenCV/blob/526c42cefb13f2a36fe8bae350ef9c80f656dc3a/easyopencv/src/main/java/org/openftc/easyopencv/OpenCvWebcamImpl.java#L393
    protected open fun mapRotationToRotateCode(rotation: WebcamRotation): Int {
        /*
         * The camera sensor in a webcam is mounted in the logical manner, such
         * that the raw image is upright when the webcam is used in its "normal"
         * orientation. However, if the user is using it in any other orientation,
         * we need to manually rotate the image.
         */
        if (rotation === WebcamRotation.SIDEWAYS_LEFT) {
            return Core.ROTATE_90_COUNTERCLOCKWISE
        }
        return if (rotation === WebcamRotation.SIDEWAYS_RIGHT) {
            Core.ROTATE_90_CLOCKWISE
        } else if (rotation === WebcamRotation.UPSIDE_DOWN) {
            Core.ROTATE_180
        } else {
            -1
        }
    }

}