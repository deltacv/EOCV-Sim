package com.github.serivesmejia.eocvsim.input.source

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer
import com.github.serivesmejia.eocvsim.config.ConfigManager
import io.github.deltacv.common.util.loggerForThis
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
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

    @JsonProperty @JvmField var cameraPortIndex: Int = -1
    @JsonProperty @JvmField var exactPortMatch: Boolean = false
    @JsonProperty @JvmField var vendorId: Int? = null
    @JsonProperty @JvmField var productId: Int? = null
    @Transient var webcamName: String = ""

    @JsonProperty @JvmField var videoMode: VideoMode? = null

    @Transient var camera: UsbCamera? = null
        private set

    @Transient private var cvSink: CvSink? = null

    @Transient private var lastFrame = Mat()

    @Transient private var initialized = false

    @Transient var isLegacyByIndex = false

    @Transient private var capTimeNanos: Long = 0
    private val configManager: ConfigManager by inject()
    private val logger by loggerForThis()

    constructor() : super()

    constructor(webcamName: String?, videoMode: VideoMode?) : super() {
        this.webcamName = webcamName ?: ""
        this.videoMode = videoMode
    }

    constructor(webcamName: String?, vendorId: Int?, productId: Int?, videoMode: VideoMode?) : super() {
        this.webcamName = webcamName ?: ""
        this.vendorId = vendorId
        this.productId = productId
        this.videoMode = videoMode
    }

    constructor(
        webcamName: String?,
        cameraPortIndex: Int,
        exactPortMatch: Boolean,
        vendorId: Int?,
        productId: Int?,
        videoMode: VideoMode?
    ) : super() {
        this.webcamName = webcamName ?: ""
        this.cameraPortIndex = cameraPortIndex
        this.exactPortMatch = exactPortMatch
        this.vendorId = vendorId
        this.productId = productId
        this.videoMode = videoMode
    }

    constructor(cameraPortIndex: Int, videoMode: VideoMode?) : super() {
        this.cameraPortIndex = cameraPortIndex
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

        val matchedInfo = when {
            exactPortMatch -> {
                val infos = UsbCamera.enumerateUsbCameras()
                infos.firstOrNull {
                    cameraPortIndex >= 0 &&
                        it.dev == cameraPortIndex &&
                        vendorId != null && productId != null &&
                        it.vendorId == vendorId &&
                        it.productId == productId
                } ?: run {
                    logger.error("Camera not found on the same connection: $cameraPortIndex")
                    return false
                }
            }

            cameraPortIndex >= 0 -> {
                val infos = UsbCamera.enumerateUsbCameras()
                infos.firstOrNull { it.dev == cameraPortIndex }
                    ?: run {
                        logger.error("Camera not found on port: $cameraPortIndex")
                        return false
                    }
            }

            vendorId != null && productId != null -> {
                val infos = UsbCamera.enumerateUsbCameras()
                infos.firstOrNull {
                    it.vendorId == vendorId && it.productId == productId
                } ?: run {
                    logger.error("Camera not found by VID/PID: ${vendorId}:${productId}")
                    return false
                }
            }

            webcamName.isNotEmpty() -> {
                val infos = UsbCamera.enumerateUsbCameras()
                infos.firstOrNull { it.name == webcamName }
                    ?: run {
                        logger.error("Camera not found: $webcamName")
                        return false
                    }
            }

            else -> null
        }

        val cam = if (matchedInfo != null) {
            webcamName = matchedInfo.name
            UsbCamera(matchedInfo.name, matchedInfo.dev)
        } else {
            UsbCamera("$cameraPortIndex", cameraPortIndex)
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
            "Camera started: ${matchedInfo?.name ?: webcamName.ifEmpty { "Camera $cameraPortIndex" }} ${mode?.stringify()}"
        )

        cvSink = CvSink("eocvsim_sink_$cameraPortIndex", PixelFormat.BGR).also {
            it.source = cam
        }

        val ok = cvSink!!.grabFrame(lastFrame, configManager.config.webcamOpenTimeoutSec)

        if (ok == 0L || lastFrame.empty()) {
            logger.error("Failed to open camera: ${cvSink!!.error}")
            return false
        }

        currentWebcamIndex = cameraPortIndex
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

        val grabTime = cvSink?.grabFrame(lastFrame, configManager.config.webcamNewFrameTimeoutSec) ?: 0L

        if(lastFrame.empty()) {
            return lastFrame
        }

        capTimeNanos = grabTime
        Imgproc.cvtColor(lastFrame, lastFrame, Imgproc.COLOR_BGR2RGBA)
        return lastFrame
    }

    override fun onPause() {
        cvSink?.grabFrame(lastFrame, configManager.config.webcamNewFrameTimeoutSec)
        cvSink?.close()
        camera?.close()
        currentWebcamIndex = -1
    }

    override fun onResume() {
        InputSourceInitializer.runWithTimeout(this) { init() }
    }

    override fun internalCloneSource(): InputSource =
        if (isLegacyByIndex) {
            CameraSource(cameraPortIndex, videoMode)
        } else {
            CameraSource(webcamName, cameraPortIndex, exactPortMatch, vendorId, productId, videoMode)
        }

    override val fileFilters: FileFilter? get() = null
    override val captureTimeNanos: Long get() = capTimeNanos

    override fun toString() =
        "CameraSource($webcamName, port=$cameraPortIndex, exactPortMatch=$exactPortMatch, vid=$vendorId, pid=$productId, ${videoMode?.stringify()})"

    private fun VideoMode.stringify() = "${width}x${height}@${fps} $pixelFormat"
}