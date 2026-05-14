/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.eocvsim.input

import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import io.github.deltacv.common.util.loggerForThis
import io.github.deltacv.eocvsim.input.control.CameraSourceControlMap
import io.github.deltacv.vision.external.source.VisionSourceBase
import io.github.deltacv.vision.external.util.ThrowableHandler
import io.github.deltacv.vision.external.util.Timestamped
import org.opencv.core.Mat
import org.opencv.core.Size

class VisionInputSource(
    throwableHandler: ThrowableHandler? = null,
    private val inputSourceProvider: (Size) -> InputSource,
) : VisionSourceBase(throwableHandler) {

    val logger by loggerForThis()

    private var inputSource: InputSource? = null

    override fun check(): Int {
        val testSource = inputSourceProvider(Size(0.0, 0.0))
        val state = testSource.init()
        testSource.close()
        return if(state) 0 else 1
    }

    override fun getControlMap() = if(inputSource is CameraSource) {
        CameraSourceControlMap(inputSource!! as CameraSource)
    } else throw IllegalStateException("Controls are not available for source ${inputSource?.name ?: ""}")

    override fun close(): Boolean {
        inputSource?.close()
        return true
    }

    override fun startSource(size: Size): Boolean {
        inputSource = inputSourceProvider(size)
        return inputSource!!.init()
    }

    override fun stopSource(): Boolean {
        inputSource?.close()
        return true;
    }

    private val emptyMat = Mat()

    override fun pullFrame(): Timestamped<Mat> {
        return try {
            val frame = inputSource!!.update();
            Timestamped(frame, inputSource!!.captureTimeNanos)
        } catch(e: Exception) {
            Timestamped(emptyMat, 0)
        }
    }

}
