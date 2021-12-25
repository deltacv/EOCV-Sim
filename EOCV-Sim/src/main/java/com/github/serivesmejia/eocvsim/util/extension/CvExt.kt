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
@file:JvmName("CvExt")

package com.github.serivesmejia.eocvsim.util.extension

import com.qualcomm.robotcore.util.Range
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

fun Scalar.cvtColor(code: Int): Scalar {
    val mat = Mat(5, 5, CvType.CV_8UC3);
    mat.setTo(this)
    Imgproc.cvtColor(mat, mat, code);

    val newScalar = Scalar(mat.get(1, 1))
    mat.release()

    return newScalar
}

fun Size.aspectRatio() = height / width
fun Mat.aspectRatio() = size().aspectRatio()

fun Size.clipTo(size: Size): Size {
    width = Range.clip(width, 0.0, size.width)
    height = Range.clip(height, 0.0, size.height)
    return this
}