/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.input

import com.github.serivesmejia.eocvsim.util.event.EventHandler
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

class EnumComboBox<T : Enum<T>> @JvmOverloads constructor(
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
                return enumSupplier(comboBox.selectedItem!!.toString())
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

