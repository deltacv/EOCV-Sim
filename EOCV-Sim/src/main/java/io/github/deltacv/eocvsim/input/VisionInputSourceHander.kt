package io.github.deltacv.eocvsim.input

import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import com.github.serivesmejia.eocvsim.input.source.VideoSource
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import io.github.deltacv.vision.external.source.ViewportAndSourceHander
import io.github.deltacv.vision.external.source.VisionSource
import io.github.deltacv.vision.external.source.VisionSourceHander
import org.opencv.core.Size
import org.openftc.easyopencv.OpenCvViewport
import java.io.File
import java.lang.IllegalArgumentException
import java.net.URLConnection

class VisionInputSourceHander(val stopNotifier: EventHandler, val viewport: OpenCvViewport) : ViewportAndSourceHander {

    private fun isImage(path: String): Boolean {
        val mimeType: String = URLConnection.getFileNameMap().getContentTypeFor(path)
        return mimeType.startsWith("image")
    }

    private fun isVideo(path: String): Boolean {
        val mimeType: String = URLConnection.getFileNameMap().getContentTypeFor(path)
        return mimeType.startsWith("video")
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

        stopNotifier.doOnce {
            source.stop()
        }

        return source
    }

    override fun viewport() = viewport

}