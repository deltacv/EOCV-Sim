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

package com.github.serivesmejia.eocvsim.gui

import com.github.serivesmejia.eocvsim.gui.util.GuiUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.awt.image.BufferedImage
import java.util.NoSuchElementException
import javax.swing.ImageIcon

object Icons {

    private val bufferedImages = HashMap<String, Image>()

    private val icons = HashMap<String, ImageIcon>()
    private val resizedIcons = HashMap<String, ImageIcon>()

    private val futureIcons = mutableListOf<FutureIcon>()

    private var colorsInverted = false

    val logger by loggerForThis()

    init {
        addFutureImage("ico_eocvsim", "/images/icon/ico_eocvsim.png", false)

        addFutureImage("ico_img", "/images/icon/ico_img.png")
        addFutureImage("ico_cam", "/images/icon/ico_cam.png")
        addFutureImage("ico_vid", "/images/icon/ico_vid.png")

        addFutureImage("ico_config", "/images/icon/ico_config.png")
        addFutureImage("ico_slider", "/images/icon/ico_slider.png")
        addFutureImage("ico_textbox", "/images/icon/ico_textbox.png")
        addFutureImage("ico_colorpick", "/images/icon/ico_colorpick.png")

        addFutureImage("ico_gears", "/images/icon/ico_gears.png")
        addFutureImage("ico_hammer", "/images/icon/ico_hammer.png")

        addFutureImage("ico_colorpick_pointer", "/images/icon/ico_colorpick_pointer.png")
    }

    fun getImage(name: String): ImageIcon {
        for(futureIcon in futureIcons.toTypedArray()) {
            if(futureIcon.name == name) {
                logger.trace("Loading future icon $name")
                addImage(futureIcon.name, futureIcon.resourcePath, futureIcon.allowInvert)

                futureIcons.remove(futureIcon)
            }
        }

        if(!icons.containsKey(name)) {
            throw NoSuchElementException("Image $name is not loaded into memory")
        }
        return icons[name]!!
    }

    fun lazyGetImageResized(name: String, width: Int, height: Int) = lazy {
        getImageResized(name, width, height)
    }

    fun getImageResized(name: String, width: Int, height: Int): ImageIcon {
        //determines the icon name from the:
        //name, widthxheight, is inverted or is original
        val resIconName = "$name-${width}x${height}${
            if(colorsInverted) {
                "-inverted"
            } else {
                ""
            }
        }"

        val icon = if(resizedIcons.contains(resIconName)) {
            resizedIcons[resIconName]
        } else {
            resizedIcons[resIconName] = GuiUtil.scaleImage(getImage(name), width, height)
            resizedIcons[resIconName]
        }

        return icon!!
    }

    fun addFutureImage(name: String, path: String, allowInvert: Boolean = true) = futureIcons.add(
        FutureIcon(name, path, allowInvert)
    )

    fun addImage(name: String, path: String, allowInvert: Boolean = true) {
        val buffImg = GuiUtil.loadBufferedImage(path)
        if(colorsInverted && allowInvert) {
            GuiUtil.invertBufferedImageColors(buffImg)
        }

        bufferedImages[name] = Image(buffImg, allowInvert)
        icons[name] = ImageIcon(buffImg)
    }

    fun setDark(dark: Boolean) {
        if(dark) {
            if(!colorsInverted) {
                invertAll()
                colorsInverted = true
            }
        } else {
            if(colorsInverted) {
                invertAll()
                colorsInverted = false
            }
        }
    }

    private fun invertAll() {
        for((_, image) in bufferedImages) {
            if(image.allowInvert) {
                GuiUtil.invertBufferedImageColors(image.img)
            }
        }
    }

    data class Image(val img: BufferedImage, val allowInvert: Boolean)

    data class FutureIcon(val name: String, val resourcePath: String, val allowInvert: Boolean)

}