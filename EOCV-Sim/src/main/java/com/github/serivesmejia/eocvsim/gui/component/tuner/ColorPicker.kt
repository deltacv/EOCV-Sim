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

package com.github.serivesmejia.eocvsim.gui.component.tuner

import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import io.github.deltacv.vision.external.gui.SwingOpenCvViewport
import org.opencv.core.Scalar
import java.awt.Cursor
import java.awt.Point
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent


class ColorPicker(private val viewport: SwingOpenCvViewport) {

    companion object {
        private val size = if(SysUtil.OS == SysUtil.OperatingSystem.WINDOWS) {
            200
        } else { 35 }

        val colorPickIco = EOCVSimIconLibrary.icoColorPickPointer.resized(size, size).image

        val colorPickCursor = Toolkit.getDefaultToolkit().createCustomCursor(
            colorPickIco, Point(0, 0), "Color Pick Pointer"
        )
    }

    var isPicking = false
        private set

    var hasPicked = false
        private set

    val onPick = EventHandler("ColorPicker-OnPick")
    val onCancel = EventHandler("ColorPicker-OnCancel")

    private var initialCursor: Cursor? = null

    var colorRgb = Scalar(0.0, 0.0, 0.0)
        private set

    val clickListener = object: MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            //if clicked with primary button...
            if(e.button == MouseEvent.BUTTON1) {
                // The pixel color at location x, y
                val color = Robot().getPixelColor(e.xOnScreen, e.yOnScreen)

                //wrap Java's color to OpenCV's Scalar since we're EOCV-Sim not JavaCv-Sim right?
                colorRgb = Scalar(
                    color.red.toDouble(), color.green.toDouble(), color.blue.toDouble()
                )

                hasPicked = true
                onPick.run() //run all oick listeners
            } else {
                onCancel.run()
            }

            stopPicking()
        }
    }

    fun startPicking() {
        if(isPicking) return
        isPicking = true
        hasPicked = false

        viewport.component.addMouseListener(clickListener)

        initialCursor = viewport.component.cursor
        viewport.component.cursor = colorPickCursor
    }

    fun stopPicking() {
        if(!isPicking) return
        isPicking = false

        if(!hasPicked) {
            onPick.removeAllListeners()
            onCancel.run()
        }

        viewport.component.removeMouseListener(clickListener)
        viewport.component.cursor = initialCursor
    }

}
