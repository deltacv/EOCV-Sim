package com.github.serivesmejia.eocvsim.input.source

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import io.github.deltacv.common.util.loggerForThis
import org.koin.core.component.KoinComponent
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import org.wpilib.util.PixelFormat
import org.wpilib.vision.camera.CvSink
import org.wpilib.vision.camera.UsbCamera
import org.wpilib.vision.camera.VideoMode
import javax.swing.filechooser.FileFilter

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE
)
class CameraSource : InputSource, KoinComponent {

    companion object {
        @JvmStatic var currentWebcamIndex = -1
    }

    override val hasSlowInitialization: Boolean get() = true

    @Transient var webcamIndex: Int = 0
    @JsonProperty @JvmField var webcamName: String = ""

    @JsonProperty @JvmField var videoMode: VideoMode? = null

    @Transient var camera: UsbCamera? = null
        private set

    @Transient private var cvSink: CvSink? = null

    @Transient private var lastFrame = Mat()

    @Transient private var initialized = false

    @Transient var isLegacyByIndex = false

    @Transient private var matRecycler = MatRecycler(3)
    @Transient private var capTimeNanos: Long = 0
    private val logger by loggerForThis()

    constructor() : super()

    constructor(webcamName: String?, videoMode: VideoMode?) : super() {
        this.webcamName = webcamName ?: ""
        this.videoMode = videoMode
    }

    constructor(webcamIndex: Int, videoMode: VideoMode?) : super() {
        this.webcamIndex = webcamIndex
        this.videoMode = videoMode
        this.isLegacyByIndex = true
    }

    override fun setSize(size: Size) {
        // deprecated concept now; derived from VideoMode
    }

    override fun getSize(): Size =
        videoMode?.let { Size(it.width.toDouble(), it.height.toDouble()) } ?: Size()

    override fun init(): Boolean {
        if (initialized) return false
        initialized = true

        val cam = if (webcamName.isNotEmpty()) {
            val infos = UsbCamera.enumerateUsbCameras()
            val info = infos.firstOrNull { it.name == webcamName }
                ?: run {
                    logger.error("Camera not found: $webcamName")
                    return false
                }

            UsbCamera(webcamName, info.dev)
        } else {
            UsbCamera("$webcamIndex", webcamIndex)
        }

        camera = cam

        val desiredMode = videoMode

        if (desiredMode != null) {
            cam.videoMode = desiredMode
        } else {
            val mode = cam.videoMode
            cam.videoMode = mode
        }

        val mode = cam.videoMode

        logger.info(
            "Camera started: $webcamName ${mode?.stringify()}"
        )

        cvSink = CvSink("eocvsim_sink_$webcamIndex", PixelFormat.BGR).also {
            it.source = cam
        }

        val test = matRecycler.takeMatOrNull() ?: return false
        val ok = cvSink!!.grabFrame(test, 5.0)

        if (ok == 0L || test.empty()) {
            test.returnMat()
            logger.error("Failed to open camera: ${cvSink!!.error}")
            return false
        }

        test.returnMat()

        currentWebcamIndex = webcamIndex
        return true
    }

    override fun reset() {
        if (!initialized) return

        cvSink?.close()
        cvSink = null

        camera?.close()
        camera = null

        lastFrame.release()

        initialized = false
    }

    override fun close() {
        cvSink?.close()
        camera?.close()
        currentWebcamIndex = -1
    }

    override fun update(): Mat {
        if (isPaused) return lastFrame

        val grabTime = cvSink?.grabFrame(lastFrame, 3.0) ?: 0L

        if(lastFrame.empty()) {
            return lastFrame
        }

        capTimeNanos = grabTime
        Imgproc.cvtColor(lastFrame, lastFrame, Imgproc.COLOR_BGR2RGBA)
        return lastFrame
    }

    override fun onPause() {
        cvSink?.grabFrame(lastFrame, 2.0)
        cvSink?.close()
        camera?.close()
        currentWebcamIndex = -1
    }

    override fun onResume() {
        InputSourceInitializer.runWithTimeout(this) { init() }
    }

    override fun internalCloneSource(): InputSource =
        if (isLegacyByIndex) {
            CameraSource(webcamIndex, videoMode)
        } else {
            CameraSource(webcamName, videoMode)
        }

    override val fileFilters: FileFilter? get() = null
    override val captureTimeNanos: Long get() = capTimeNanos

    override fun toString() =
        "CameraSource($webcamName, $webcamIndex, ${videoMode?.stringify()})"

    private fun VideoMode.stringify() = "${width}x${height}@${fps} ${pixelFormat}"
}