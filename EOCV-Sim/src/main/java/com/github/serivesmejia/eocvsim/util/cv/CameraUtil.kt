package com.github.serivesmejia.eocvsim.util.cv

import com.github.serivesmejia.eocvsim.input.camera.OpenCvWebcam
import com.github.serivesmejia.eocvsim.input.camera.Webcam
import com.github.serivesmejia.eocvsim.util.Log
import org.opencv.core.Size
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.Videoio

object CameraUtil {

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

    const val TAG = "CameraUtil"

    // adapted from https://www.learnpythonwithrune.org/find-all-possible-webcam-resolutions-with-opencv-in-python/
    // with a predefined list of commonResolutions because pulling from wikipedia is dumb lol
    @JvmStatic fun getResolutionsOf(index: Int): Array<Size> {
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
        return resolutions.toTypedArray()
    }

   @JvmStatic @JvmOverloads
   fun findWebcamsOpenCv(emptyCamerasBeforeGivingUp: Int = 4): List<Webcam> {
       val webcams = mutableListOf<OpenCvWebcam>()

       var capture: VideoCapture? = null
       var currentIndex = 0

       var emptyCameras = 0

        do {
            if(capture != null && capture.isOpened) {
                capture.release()
            }

            capture = VideoCapture(currentIndex)
            capture.open(currentIndex)

            if(capture.isOpened) {
                webcams.add(OpenCvWebcam(currentIndex))
                emptyCameras = 0
            } else {
                emptyCameras++
            }

            currentIndex++
        } while((capture != null && capture.isOpened) || emptyCameras <= emptyCamerasBeforeGivingUp)

        return webcams
    }

}