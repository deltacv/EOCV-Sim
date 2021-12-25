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

package com.github.serivesmejia.eocvsim.gui.component.input

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.util.ValidCharactersDocumentFilter
import com.github.serivesmejia.eocvsim.gui.util.extension.SwingExt.documentFilter
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.opencv.core.Size
import java.awt.Color
import java.awt.FlowLayout
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.roundToInt

class SizeFields(initialSize: Size = EOCVSim.DEFAULT_EOCV_SIZE,
                 allowDecimalValues: Boolean = false,
                 allowNegativeValues: Boolean = false,
                 descriptiveText: String = "Size: ",
                 middleText: String = " x ") : JPanel(FlowLayout()) {

    constructor(initialSize: Size, allowDecimalValues: Boolean, descriptiveText: String)
            : this(initialSize, allowDecimalValues, false, descriptiveText, " x ")

    val widthTextField = JTextField(4)
    val heightTextField = JTextField(4)

    private val widthValidator: ValidCharactersDocumentFilter
    private val heightValidator: ValidCharactersDocumentFilter

    @get:Synchronized
    val lastValidWidth: Double
        get() = widthValidator.lastValid

    @get:Synchronized
    val currentWidth: Double
        get() = widthTextField.text.toDouble()

    @get:Synchronized
    val lastValidHeight: Double
        get() = heightValidator.lastValid

    @get:Synchronized
    val currentHeight: Double
        get() = heightTextField.text.toDouble()

    @get:Synchronized
    val lastValidSize: Size
        get() = Size(lastValidWidth, lastValidHeight)

    @get:Synchronized
    val currentSize: Size
        get() = Size(currentWidth, currentHeight)

    private val validChars = ArrayList<Char>()

    val valid: Boolean
        get() = widthValidator.valid && heightValidator.valid && widthTextField.text != "" && heightTextField.text != ""

    @JvmField val onChange = EventHandler("SizeFields-OnChange")

    init {
        //add all valid characters for non decimal numeric fields
        Collections.addAll(validChars, '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        if(allowDecimalValues) {
            validChars.add('.')
        }
        if(allowNegativeValues) {
            validChars.add('-')
        }

        widthValidator = ValidCharactersDocumentFilter(validChars.toTypedArray())
        heightValidator = ValidCharactersDocumentFilter(validChars.toTypedArray())

        widthTextField.documentFilter = widthValidator
        widthTextField.document.addDocumentListener(BorderChangerListener(widthTextField, widthValidator, onChange))
        widthTextField.text = "${ if(allowDecimalValues) { initialSize.width } else { initialSize.width.roundToInt() } }"

        heightTextField.documentFilter = heightValidator
        heightTextField.document.addDocumentListener(BorderChangerListener(heightTextField, heightValidator, onChange))
        heightTextField.text = "${ if(allowDecimalValues) { initialSize.height } else { initialSize.height.roundToInt() }  }"

        val sizeLabel = JLabel(descriptiveText)
        sizeLabel.horizontalAlignment = JLabel.LEFT
        add(sizeLabel)

        add(widthTextField)

        val xLabel = JLabel(middleText)
        xLabel.horizontalAlignment = JLabel.CENTER
        add(xLabel)

        add(heightTextField)
    }

    private class BorderChangerListener(val field: JTextField, val validator: ValidCharactersDocumentFilter, val onChange: EventHandler? = null): DocumentListener {

        val initialBorder = field.border
        val redBorder = LineBorder(Color(255, 79, 79), 2)

        override fun insertUpdate(e: DocumentEvent?) = change()
        override fun removeUpdate(e: DocumentEvent?) = change()
        override fun changedUpdate(e: DocumentEvent?) = change()
        fun change() {
            if(validator.valid && field.text != "") {
                field.border = initialBorder
            } else {
                field.border = redBorder
            }

            onChange?.run()
        }
    }

}