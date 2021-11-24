package com.github.serivesmejia.eocvsim.input.camera

import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio

class OpenCvWebcam(override val index: Int) : Webcam {

    // OpenCV's VideoCapture (not to be confused with OpenIMAJ's, called the same)
    val videoCapture = VideoCapture(index)

    override val isOpen: Boolean
        get() = videoCapture.isOpened

    override var resolution: Size
        get() {
            val width = videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH).toDouble()
            val height = videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT).toDouble()
            return Size(width, height)
        }
        set(value) {
            videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, value.width)
            videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, value.height)
        }

    override val name = "Webcam $index"

    override fun open() {
        videoCapture.open(index)
    }

    override fun read(mat: Mat) {
        videoCapture.read(mat)
    }

    override fun close() {
        videoCapture.release()
    }
}