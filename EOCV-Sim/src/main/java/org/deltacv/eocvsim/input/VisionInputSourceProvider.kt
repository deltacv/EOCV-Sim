/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.input

import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.input.source.CameraSource
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import com.github.serivesmejia.eocvsim.input.source.VideoSource
import org.deltacv.vision.external.source.ViewportVisionSourceProvider
import org.deltacv.vision.external.source.VisionSource
import org.deltacv.vision.internal.opmode.OpModeNotifier
import org.deltacv.vision.internal.opmode.OpModeState
import org.deltacv.vision.internal.opmode.RedirectToOpModeThrowableHandler
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

    private fun defaultMode(index: Int, size: Size): VideoMode {
        val cam = UsbCamera("$index", index)

        val mode = try {
            val modes = cam.enumerateVideoModes()
            if (size.width > 0 && size.height > 0) {
                modes.firstOrNull { it.width == size.width.toInt() && it.height == size.height.toInt() }
                    ?: modes.firstOrNull()
                    ?: cam.videoMode
            } else {
                cam.videoMode
            }
        } catch (_: Exception) {
            VideoMode(0, 640, 480, 30)
        } finally {
            cam.close()
        }

        return mode
    }

    override fun get(name: String): VisionSource {
        val source = VisionInputSource(
            RedirectToOpModeThrowableHandler(notifier)
        ) { size ->
            when {
                File(name).exists() -> {
                    when {
                        isImage(name) -> ImageSource(name, size)
                        isVideo(name) -> VideoSource(name, size)
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
                        CameraSource(index, defaultMode(index, size))
                    }
                }
            }
        }

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
