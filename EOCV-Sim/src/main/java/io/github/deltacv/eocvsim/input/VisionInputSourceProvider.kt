/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.eocvsim.input

import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import com.github.serivesmejia.eocvsim.input.source.VideoSource
import io.github.deltacv.vision.external.source.ViewportVisionSourceProvider
import io.github.deltacv.vision.external.source.VisionSource
import io.github.deltacv.vision.internal.opmode.OpModeNotifier
import io.github.deltacv.vision.internal.opmode.OpModeState
import io.github.deltacv.vision.internal.opmode.RedirectToOpModeThrowableHandler
import org.opencv.core.Mat
import org.opencv.core.Size
import org.openftc.easyopencv.OpenCvViewport
import org.wpilib.vision.camera.UsbCamera
import org.wpilib.vision.camera.VideoMode
import java.io.File
import javax.imageio.ImageIO

class VisionInputSourceProvider(
    val notifier: OpModeNotifier,
    val viewport: OpenCvViewport,
    val inputSourceManager: InputSourceManager
) : ViewportVisionSourceProvider {

    private fun isImage(path: String) = try {
        ImageIO.read(File(path)) != null
    } catch (_: Exception) {
        false
    }

    private fun isVideo(path: String): Boolean {
        val capture = org.opencv.videoio.VideoCapture(path)
        val mat = Mat()

        capture.read(mat)
        val ok = !mat.empty()

        capture.release()
        return ok
    }

    private fun defaultMode(index: Int): VideoMode {
        val cam = UsbCamera("$index", index)

        val mode = try {
            cam.videoMode
        } catch (_: Exception) {
            // fallback safe mode if enumeration fails
            VideoMode(0, 640, 480, 30)
        }

        cam.close()
        return mode
    }

    override fun get(name: String): VisionSource {

        val source = VisionInputSource(
            when {
                File(name).exists() -> {
                    when {
                        isImage(name) -> ImageSource(name, Size())
                        isVideo(name) -> VideoSource(name, Size())
                        else -> throw IllegalArgumentException(
                            "File $name is neither image nor video"
                        )
                    }
                }

                else -> {
                    val index = name.toIntOrNull()
                        ?: if (name == "default" || name == "Webcam 1") 0 else null

                    if (index == null) {
                        inputSourceManager.sources[name]
                            ?: throw IllegalArgumentException("Input source $name not found")
                    } else {
                        // 🔥 FIX: use real VideoMode instead of Size hack
                        CameraSource(index, defaultMode(index))
                    }
                }
            },
            RedirectToOpModeThrowableHandler(notifier)
        )

        notifier.onStateChange {
            when (notifier.state) {
                OpModeState.STOPPED -> {
                    source.stop()
                    removeListener()
                }
                else -> {}
            }
        }

        return source
    }

    override fun viewport() = viewport
}
