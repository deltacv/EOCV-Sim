package io.github.deltacv.eocvsim.input

import com.github.serivesmejia.eocvsim.input.source.CameraSource
import io.github.deltacv.vision.external.source.ViewportAndSourceHander
import io.github.deltacv.vision.external.source.VisionSource
import io.github.deltacv.vision.external.source.VisionSourceHander
import org.opencv.core.Size
import org.openftc.easyopencv.OpenCvViewport

class VisionInputSourceHander(val viewport: OpenCvViewport) : ViewportAndSourceHander {

    override fun hand(name: String): VisionSource {
        return VisionInputSource(CameraSource(0, Size(640.0, 480.0)))
    }

    override fun viewport() = viewport

}