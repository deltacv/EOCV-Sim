package io.github.deltacv.eocvsim.input

import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import com.github.serivesmejia.eocvsim.input.source.VideoSource
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import io.github.deltacv.vision.external.source.ViewportAndSourceHander
import io.github.deltacv.vision.external.source.VisionSource
import io.github.deltacv.vision.external.source.VisionSourceHander
import io.github.deltacv.vision.internal.opmode.OpModeNotifier
import io.github.deltacv.vision.internal.opmode.OpModeState
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.videoio.VideoCapture
import org.openftc.easyopencv.OpenCvViewport
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.URLConnection
import javax.imageio.ImageIO

class VisionInputSourceHander(val notifier: OpModeNotifier, val viewport: OpenCvViewport) : ViewportAndSourceHander {

    private fun isImage(path: String) = try {
        ImageIO.read(File(path)) != null
    } catch(ex: IOException) { false }

    private fun isVideo(path: String): Boolean {
        val capture = VideoCapture(path)
        val mat = Mat()

        capture.read(mat)

        val isVideo = !mat.empty()

        capture.release()

        return isVideo
    }

    override fun hand(name: String): VisionSource {
        val source = VisionInputSource(if(File(name).exists()) {
            if(isImage(name)) {
                ImageSource(name)
            } else if(isVideo(name)) {
                VideoSource(name, null)
            } else throw IllegalArgumentException("File is not an image nor a video")
        } else {
            val index = name.toIntOrNull()
                    ?: if(name == "default" || name == "Webcam 1") 0
                    else throw IllegalArgumentException("Unknown source $name")

            CameraSource(index, Size(640.0, 480.0))
        })

        notifier.onStateChange {
            when(notifier.state) {
                OpModeState.STOPPED -> {
                    source.stop()
                    it.removeThis()
                }
                else -> {}
            }
        }

        return source
    }

    override fun viewport() = viewport

}