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

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

class EnumComboBox<T : Enum<T>>(
    descriptiveText: String = "Select a value:",
    private val clazz: Class<T>,
    values: Array<T>,
    private val nameSupplier: (T) -> String = { it.name },
    private val enumSupplier: (String) -> T = {
        java.lang.Enum.valueOf(clazz, it) as T
    }
) : JPanel() {

    val descriptiveLabel = JLabel(descriptiveText)
    val comboBox = JComboBox<String>()

    var selectedEnum: T?
        set(value) {
            value?.let {
                comboBox.selectedItem = nameSupplier(it)
            }
        }
        get() {
            comboBox.selectedItem?.let {
                return enumSupplier(comboBox.selectedItem.toString())
            }
            return null
        }

    val onSelect = EventHandler("EnumComboBox-OnSelect")

    init {
        if(descriptiveText.trim() != "") {
            descriptiveLabel.horizontalAlignment = JLabel.LEFT
            add(descriptiveLabel)
        }

        for(value in values) {
            comboBox.addItem(nameSupplier(value))
        }
        add(comboBox)

        comboBox.addActionListener { onSelect.run() }
    }


    fun removeEnumOption(enum: T) {
        comboBox.removeItem(nameSupplier(enum))
    }

}
