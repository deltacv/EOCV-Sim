/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.gui.util

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.gui.dialog.FileAlreadyExists
import com.github.serivesmejia.eocvsim.util.SysUtil
import io.github.deltacv.common.util.loggerForThis
import io.github.deltacv.vision.external.util.CvUtil
import org.opencv.core.Mat
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Component
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

object GuiUtil {

    private val logger = LoggerFactory.getLogger(GuiUtil::class.java)

    @JvmStatic
    fun loadBufferedImage(path: String): BufferedImage {
        return ImageIO.read(javaClass.getResourceAsStream(path) ?: throw IOException("Resource not found: $path"))
    }

    @JvmStatic
    fun saveBufferedImage(file: File, bufferedImage: BufferedImage, format: String = "jpg") {
        ImageIO.write(bufferedImage, format, file)
    }

    @JvmStatic
    fun catchSaveBufferedImage(file: File, bufferedImage: BufferedImage, format: String = "jpg") {
        try {
            saveBufferedImage(file, bufferedImage, format)
        } catch (e: IOException) {
            logger.error("Failed to save buffered image", e)
        }
    }

    @JvmStatic
    fun invertBufferedImageColors(input: BufferedImage) {
        for (x in 0 until input.width) {
            for (y in 0 until input.height) {
                val rgba = input.getRGB(x, y)
                var col = Color(rgba, true)

                if (col.alpha <= 0) continue

                col = Color(
                    255 - col.red,
                    255 - col.green,
                    255 - col.blue
                )

                input.setRGB(x, y, col.rgb)
            }
        }
    }

    @JvmStatic
    fun saveBufferedImageFileChooser(parent: Component?, bufferedImage: BufferedImage, dialogFactory: DialogFactory) {
        val jpegFilter = FileNameExtensionFilter("JPEG (*.jpg)", "jpg", "jpeg")
        val pngFilter = FileNameExtensionFilter("PNG (*.png)", "png")

        val validExts = arrayOf("jpg", "jpeg", "png")

        dialogFactory.createFileChooser(parent, DialogFactory.FileChooser.Mode.SAVE_FILE_SELECT, jpegFilter, pngFilter)
            .addCloseListener { mode, selectedFile, selectedFileFilter ->
                if (mode == JFileChooser.APPROVE_OPTION && selectedFile != null) {
                    val extension = SysUtil.getExtensionByStringHandling(selectedFile.name)

                    var saveImage: Boolean

                    if (!selectedFile.exists()) {
                        saveImage = true
                    } else {
                        val userChoice = dialogFactory.createFileAlreadyExistsDialog()
                        saveImage = userChoice == FileAlreadyExists.UserChoice.REPLACE
                    }

                    if (saveImage) {
                        var fileToSave = selectedFile
                        var ext = ""

                        if (selectedFileFilter is FileNameExtensionFilter) {
                            ext = selectedFileFilter.extensions[0]
                            fileToSave = File(selectedFile.absolutePath + "." + ext)
                            catchSaveBufferedImage(fileToSave, bufferedImage, ext)
                        } else if (extension.isPresent && validExts.contains(extension.get())) {
                            ext = extension.get()
                            catchSaveBufferedImage(fileToSave, bufferedImage, ext)
                        } else {
                            fileToSave = File(selectedFile.absolutePath + ".jpg")
                            catchSaveBufferedImage(fileToSave, bufferedImage)
                        }
                    }
                }
            }
    }

    @JvmStatic
    fun saveMatFileChooser(parent: Component?, mat: Mat, dialogFactory: DialogFactory) {
        val clonedMat = mat.clone()
        val img = CvUtil.matToBufferedImage(clonedMat)
        clonedMat.release()

        saveBufferedImageFileChooser(parent, img, dialogFactory)
    }

    class NoSelectionModel : DefaultListSelectionModel() {
        override fun setAnchorSelectionIndex(anchorIndex: Int) {}
        override fun setLeadAnchorNotificationEnabled(flag: Boolean) {}
        override fun setLeadSelectionIndex(leadIndex: Int) {}
        override fun setSelectionInterval(index0: Int, index1: Int) {}
    }

    @JvmStatic
    fun isToListModel(inputStream: InputStream, charset: Charset): ListModel<String> {
        val listModel = DefaultListModel<String>()
        val isStr = SysUtil.loadIsStr(inputStream, charset)
        val lines = isStr.split("\n").toTypedArray()

        for (i in lines.indices) {
            listModel.add(i, lines[i])
        }

        return listModel
    }
}
