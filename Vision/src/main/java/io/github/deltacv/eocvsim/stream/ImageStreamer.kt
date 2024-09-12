package io.github.deltacv.eocvsim.stream

import org.opencv.core.Mat

interface ImageStreamer {

    fun sendFrame(id: Int, image: Mat, cvtCode: Int? = null)

}