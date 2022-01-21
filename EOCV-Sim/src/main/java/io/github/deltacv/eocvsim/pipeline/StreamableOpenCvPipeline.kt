package io.github.deltacv.eocvsim.pipeline

import io.github.deltacv.eocvsim.ipc.IpcImageStreamer
import org.opencv.core.Mat
import org.openftc.easyopencv.OpenCvPipeline

abstract class StreamableOpenCvPipeline : OpenCvPipeline() {

    internal var streamer: IpcImageStreamer? = null

    fun streamFrame(id: Short, image: Mat, cvtCode: Int? = null) = streamer?.sendFrame(id, image, cvtCode)

}