package com.github.serivesmejia.eocvsim.input.camera

import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import org.openimaj.image.MBFImage
import org.openimaj.video.VideoDisplay
import org.openimaj.video.VideoDisplayListener
import org.openimaj.video.capture.Device
import org.openimaj.video.capture.VideoCapture
import java.util.concurrent.ArrayBlockingQueue

class OpenIMAJWebcam(val device: Device, resolution: Size) : Webcam, VideoDisplayListener<MBFImage> {

    companion object {
        val availableWebcams get() = VideoCapture.getVideoDevices()
    }

    var videoCapture: VideoCapture? = null
        private set

    override val isOpen: Boolean
        get() = TODO("Not yet implemented")
    override var resolution = resolution
        set(value) {
            field = value
        }

    override val index = 0
    override val name get() = device.nameStr

    private var recycler: MatRecycler? = null
    private val matQueue = EvictingBlockingQueue<MatRecycler.RecyclableMat>(ArrayBlockingQueue(2))

    init {
        matQueue.setEvictAction {
            it.returnMat()
        }
    }

    override fun open() {
        videoCapture = VideoCapture(resolution.width.toInt(), resolution.height.toInt(), device)

        recycler?.releaseAll()
        recycler = MatRecycler(3, resolution.height.toInt(), resolution.width.toInt(), CvType.CV_8UC4)
    }

    private val lastImage = Mat()

    override fun read(mat: Mat) {
        val frame = videoCapture!!.nextFrame

        val frameMat = recycler!!.takeMat()
        mat.put(0, 0, frame.toPackedARGBPixels())

        val matList = listOf(mat)
        Core.mixChannels(matList, matList, ARGB2BGRA_PAIRS) // reorder ARGB to BGRA
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR) // remove the useless alpha channel

        frameMat.copyTo(mat)
    }

    override fun close() {
        videoCapture!!.stopCapture()
        videoCapture = null
    }

    override fun afterUpdate(display: VideoDisplay<MBFImage>?) {
    }

    override fun beforeUpdate(frame: MBFImage) {
        val mat = recycler!!.takeMat()
        mat.put(0, 0, frame.toPackedARGBPixels())

        val matList = listOf(mat)
        Core.mixChannels(matList, matList, ARGB2BGRA_PAIRS) // reorder ARGB to BGRA
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR) // remove the useless alpha channel

        matQueue.put(mat)
    }

}

private val ARGB2BGRA_PAIRS = MatOfInt(
        0, 3, // A to last
        1, 2, // R to third
        2, 1, // G to second
        3, 0  // B to first
)