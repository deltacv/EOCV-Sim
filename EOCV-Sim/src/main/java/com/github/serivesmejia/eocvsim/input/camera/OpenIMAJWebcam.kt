package com.github.serivesmejia.eocvsim.input.camera

import com.github.serivesmejia.eocvsim.util.cv.CvUtil
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import org.opencv.core.*
import org.openftc.easyopencv.MatRecycler
import org.openimaj.image.ImageUtilities
import org.openimaj.video.capture.Device
import org.openimaj.video.capture.VideoCapture
import java.awt.image.BufferedImage
import java.util.concurrent.ArrayBlockingQueue

class OpenIMAJWebcam(private val device: Device, resolution: Size) : Webcam {

    companion object {
        @JvmStatic val availableWebcams: MutableList<Device> get() = VideoCapture.getVideoDevices()
    }

    var videoCapture: VideoCapture? = null
        private set

    override val isOpen get() = videoCapture != null
    override var resolution = resolution
        set(value) {
            if(videoCapture != null) {
                throw IllegalStateException("Cannot change resolution while a video stream is running")
            }
            field = value
        }

    override val index = 0
    override val name: String get() = device.nameStr

    private var recycler: MatRecycler? = null
    private val matQueue = EvictingBlockingQueue<MatRecycler.RecyclableMat>(ArrayBlockingQueue(2))

    private var img: BufferedImage? = null
    private var frameMat: Mat? = null

    init {
        matQueue.setEvictAction {
            it.returnMat()
        }
    }

    override fun open() {
        if(videoCapture != null) {
            throw IllegalStateException("Webcam is already open, close it first before calling open() again")
        }

        videoCapture = VideoCapture(resolution.width.toInt(), resolution.height.toInt(), device)

        recycler?.releaseAll()
        frameMat = Mat(resolution.height.toInt(), resolution.width.toInt(), CvType.CV_8UC3)

        img?.flush()
        img = BufferedImage(resolution.width.toInt(), resolution.height.toInt(), BufferedImage.TYPE_3BYTE_BGR)
    }

    override fun read(mat: Mat) {
        throwIfNotOpened()
        val frame = videoCapture!!.nextFrame

        val img = ImageUtilities.createBufferedImageForDisplay(frame, img)
        CvUtil.bufferedImageToMat(img, frameMat!!)
        
        frameMat!!.copyTo(mat)
    }

    override fun close() {
        throwIfNotOpened()
        videoCapture!!.stopCapture()
        videoCapture = null
    }

    private fun throwIfNotOpened() {
        if(videoCapture == null) {
            throw IllegalStateException("The camera is not opened");
        }
    }

}