/*
 * Copyright (c) 2026 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.output

import com.github.serivesmejia.eocvsim.config.ConfigManager
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.FileFilters
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.common.util.loggerForThis
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import javax.swing.filechooser.FileFilter

class RecordingManager : KoinComponent {

    val configManager: ConfigManager by inject()
    val pipelineManager: PipelineManager by inject()
    val visualizer: Visualizer by inject()
    val dialogFactory: DialogFactory by inject()
    val onMainUpdate: EventHandler by inject(named("onMainLoop"))

    private val logger by loggerForThis()

    var currentRecordingSession: VideoRecordingSession? = null
        private set

    fun isCurrentlyRecording() = currentRecordingSession != null

    fun startRecordingSession() {
        if (currentRecordingSession == null) {
            currentRecordingSession = VideoRecordingSession(
                configManager.config.videoRecordingFps.fps.toDouble(), configManager.config.videoRecordingSize
            )

            currentRecordingSession!!.startRecordingSession()

            logger.info("Recording session started")

            pipelineManager.pipelineOutputPosters.add(currentRecordingSession!!.matPoster)
        }
    }

    fun stopRecordingSession() {
        currentRecordingSession?.let { itVideo ->
            visualizer.pipelineSelectorPanel.buttonsPanel.pipelineRecordBtt.isEnabled = false

            itVideo.stopRecordingSession()
            pipelineManager.pipelineOutputPosters.remove(itVideo.matPoster)

            logger.info("Recording session stopped")

            dialogFactory.createFileChooser(
                visualizer.frame, DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT, "", FileFilters.recordedVideoFilter
            ).addCloseListener { _: Int, file: File?, _: FileFilter? ->
                onMainUpdate.once {
                    if (file != null) {
                        var correctedFile = file
                        val extension = SysUtil.getExtensionByStringHandling(file.name)
                        if (!extension.isPresent || extension.get() != "avi") {
                            correctedFile = File(file.absolutePath + ".avi")
                        }


                        itVideo.saveTo(correctedFile)
                    }

                    visualizer.pipelineSelectorPanel.buttonsPanel.pipelineRecordBtt.isEnabled = true
                    currentRecordingSession = null
                }
            }
        }
    }

}
