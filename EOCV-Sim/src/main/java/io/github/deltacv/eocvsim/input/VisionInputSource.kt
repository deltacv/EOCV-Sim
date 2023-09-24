package io.github.deltacv.eocvsim.input

import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.vision.external.source.VisionSourceBase
import io.github.deltacv.vision.external.util.ThrowableHandler
import io.github.deltacv.vision.external.util.Timestamped
import org.opencv.core.Mat
import org.opencv.core.Size

class VisionInputSource(
    private val inputSource: InputSource,
    throwableHandler: ThrowableHandler? = null
) : VisionSourceBase(throwableHandler) {

    val logger by loggerForThis()

    override fun init(): Int {
        return 0
    }

    override fun close(): Boolean {
        inputSource.close()
        inputSource.reset()
        return true
    }

    override fun startSource(size: Size?): Boolean {
        inputSource.setSize(size)
        inputSource.init()
        return true
    }

    override fun stopSource(): Boolean {
        inputSource.close()
        return true;
    }

    private val emptyMat = Mat()

    override fun pullFrame(): Timestamped<Mat> {
        return try {
            val frame = inputSource.update();
            Timestamped(frame, inputSource.captureTimeNanos)
        } catch(e: Exception) {
            Timestamped(emptyMat, 0)
        }
    }

}