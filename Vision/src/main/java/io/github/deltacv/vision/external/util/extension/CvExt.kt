/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

@file:JvmName("CvExt")

package io.github.deltacv.vision.external.util.extension

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
