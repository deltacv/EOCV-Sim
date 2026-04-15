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

package com.github.serivesmejia.eocvsim.input

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.component.visualizer.pipeline.SourceSelectorPanel
import com.github.serivesmejia.eocvsim.input.source.ImageSource
import com.github.serivesmejia.eocvsim.input.source.NullSource
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.common.util.loggerForThis
import kotlinx.coroutines.Job
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.swing.SwingUtilities

class InputSourceManager(val eocvSim: EOCVSim) {

    companion object {
        private val BLACK = Scalar(0.0, 0.0, 0.0, 255.0)
    }

    @Volatile var lastMatFromSource: Mat? = null
    @Volatile var currentInputSource: InputSource? = null

    val sources = mutableMapOf<String, InputSource>()

    val inputSourceLoader = InputSourceLoader()

    val onSourcesListChange = EventHandler("InputSourceManager-OnSourcesListChange")

    private var defaultSource = ""

    private val logger by loggerForThis()

    fun init() {
        logger.info("Initializing...")

        if (lastMatFromSource == null) {
            lastMatFromSource = Mat(Size(640.0, 480.0), 24) // 24 is CV_8UC4 (RGBA)
            lastMatFromSource!!.setTo(BLACK)
        }

        val size = Size(640.0, 480.0)

        createDefaultImgInputSource("/images/ug_4.jpg", "ug_eocvsim_4.jpg", "Ultimate Goal 4 Ring", size)
        createDefaultImgInputSource("/images/ug_1.jpg", "ug_eocvsim_1.jpg", "Ultimate Goal 1 Ring", size)
        createDefaultImgInputSource("/images/ug_0.jpg", "ug_eocvsim_0.jpg", "Ultimate Goal 0 Ring", size)

        if (sources.isEmpty()) {
            logger.warn("No input sources found, creating default null source")

            val nullSource = NullSource().apply {
                isDefault = true
            }

            addInputSource("Default", nullSource)
        } else {
            setInputSource("Ultimate Goal 4 Ring", true)
        }

        inputSourceLoader.loadInputSourcesFromFile()

        for ((name, source) in inputSourceLoader.loadedInputSources) {
            logger.info("Loaded input source $name")
            addInputSource(name, source)
        }
    }

    private fun createDefaultImgInputSource(resourcePath: String, fileName: String, sourceName: String, imgSize: Size) {
        try {
            val `is` = InputSource::class.java.getResourceAsStream(resourcePath)
            val f = SysUtil.copyFileIsTemp(`is`, fileName, true).file

            val src = ImageSource(f.absolutePath, imgSize).apply {
                isDefault = true
                createdOn = sources.size.toLong()
            }

            addInputSource(sourceName, src)
        } catch (e: IOException) {
            logger.error("Error while creating default image input source", e)
        }
    }

    fun update(isPaused: Boolean) {
        val currentSource = currentInputSource ?: return

        try {
            currentSource.setPaused(isPaused)

            val m = currentSource.update()

            if (m != null && !m.empty()) {
                lastMatFromSource?.apply {
                    setTo(BLACK) // clear previous mat
                    m.copyTo(this)
                    // add an extra alpha channel because that's what eocv returns for some reason... (more realistic simulation lol)
                    Imgproc.cvtColor(this, this, Imgproc.COLOR_RGB2RGBA)
                }
            }
        } catch (ex: Exception) {
            logger.error("Error while processing current source", ex)
            logger.warn("Changing to default source")

            setInputSource(defaultSource)
        }
    }

    @JvmOverloads
    fun addInputSource(name: String, inputSource: InputSource?, dispatchedByUser: Boolean = false) {
        if (inputSource == null) return

        if (sources.containsKey(name)) return

        inputSource.eocvSim = eocvSim

        eocvSim.visualizer.sourceSelectorPanel?.allowSourceSwitching = false

        inputSource.name = name
        sources[name] = inputSource

        if (inputSource.createdOn == -1L) {
            inputSource.createdOn = System.currentTimeMillis()
        }

        if (!inputSource.isDefault) {
            inputSourceLoader.saveInputSource(name, inputSource)
            inputSourceLoader.saveInputSourcesToFile()
        }

        onSourcesListChange.run()

        eocvSim.visualizer.sourceSelectorPanel?.let { panel ->
            SwingUtilities.invokeLater {
                val sourceSelector = panel.sourceSelector

                val currentSourceIndex = sourceSelector.selectedIndex

                if (dispatchedByUser) {
                    val index = panel.getIndexOf(name)

                    sourceSelector.selectedIndex = index

                    requestSetInputSource(name)

                    eocvSim.onMainUpdate.once {
                        eocvSim.pipelineManager.requestSetPaused(false)
                        pauseIfImageTwoFrames()
                    }
                } else {
                    sourceSelector.selectedIndex = currentSourceIndex
                }

                panel.allowSourceSwitching = true
            }
        }

        logger.info("Adding InputSource $inputSource (${inputSource.javaClass.simpleName})")
    }

    fun deleteInputSource(sourceName: String) {
        val src = sources[sourceName] ?: return
        if (src.isDefault) return

        sources.remove(sourceName)

        inputSourceLoader.deleteInputSource(sourceName)
        inputSourceLoader.saveInputSourcesToFile()
    }

    fun setInputSource(sourceName: String?, makeDefault: Boolean): Boolean {
        val result = setInputSource(sourceName)

        if (result && makeDefault) {
            defaultSource = sourceName ?: ""
        }

        return result
    }

    fun setInputSource(sourceName: String?): Boolean {
        val src = if (sourceName == null) {
            NullSource()
        } else {
            sources[sourceName]
        }

        src?.reset()

        if (src != null) {
            if (!InputSourceInitializer.initializeWithTimeout(src, this)) {
                eocvSim.visualizer.asyncPleaseWaitDialog(
                    "Error while loading requested source", "Falling back to previous source",
                    "Close", Dimension(300, 150), true, true
                )

                logger.error("Error while loading requested source ($sourceName) reported by itself (init method returned false)")

                return false
            }
        }

        currentInputSource?.reset()
        currentInputSource = src

        // if pause on images option is turned on by user
        if (eocvSim.configManager.config.pauseOnImages) {
            pauseIfImage()
        }

        logger.info("Set InputSource to ${currentInputSource.toString()} (${src?.javaClass?.simpleName})")

        return true
    }

    fun cleanSourceIfDirty() {
        currentInputSource?.cleanIfDirty()
    }

    fun isNameInUse(name: String) = sources.containsKey(name)

    fun tryName(name: String): String {
        var sourceName = name
        var count = 0

        while (isNameInUse(sourceName)) {
            count++
            sourceName = "$name ($count)"
        }

        return sourceName
    }

    fun pauseIfImage() {
        val source = currentInputSource ?: return

        // if the new input source is an image, we will pause the next frame
        // to execute one shot analysis on images and save resources.
        if (SourceType.fromClass(source.javaClass) == SourceType.IMAGE) {
            eocvSim.onMainUpdate.once {
                eocvSim.pipelineManager.setPaused(
                    true,
                    PipelineManager.PauseReason.IMAGE_ONE_ANALYSIS
                )
            }
        }
    }

    fun pauseIfImageTwoFrames() {
        // if the new input source is an image, we will pause the next frame
        // to execute one shot analysis on images and save resources.
        eocvSim.onMainUpdate.once { pauseIfImage() }
    }

    fun requestSetInputSource(name: String?) {
        eocvSim.onMainUpdate.once { setInputSource(name) }
    }

    fun showApwdIfNeeded(sourceName: String?, job: Job?): Visualizer.AsyncPleaseWaitDialog? {
        val type = getSourceType(sourceName)
        if (type == SourceType.CAMERA || type == SourceType.VIDEO || type == SourceType.HTTP) {
            val apwd = eocvSim.visualizer.asyncPleaseWaitDialog(
                "Opening source...", null, "Cancel",
                Dimension(300, 150), true
            )

            apwd.onCancel {
                job?.cancel(CancellationException())
            }
            return apwd
        }

        return null
    }

    fun getDefaultInputSource() = defaultSource

    fun getSourceType(sourceName: String?): SourceType {
        if (sourceName == null) return SourceType.UNKNOWN

        val source = sources[sourceName] ?: return SourceType.UNKNOWN
        return SourceType.fromClass(source.javaClass)
    }

    val sortedInputSources: List<InputSource> get() {
        val sourcesList = ArrayList(sources.values)
        sourcesList.sort()

        return sourcesList
    }
}
