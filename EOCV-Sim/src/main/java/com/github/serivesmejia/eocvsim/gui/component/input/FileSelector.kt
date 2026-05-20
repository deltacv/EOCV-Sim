/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.input

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.FlowLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileFilter

class FileSelector(columns: Int = 18,
                   mode: DialogFactory.FileChooser.Mode,
                   vararg fileFilters: FileFilter?) : JPanel(FlowLayout()), KoinComponent {

    private val dialogFactory: DialogFactory by inject()

    constructor(columns: Int, vararg fileFilters: FileFilter?) : this(columns, DialogFactory.FileChooser.Mode.FILE_SELECT, *fileFilters)

    constructor(columns: Int, mode: DialogFactory.FileChooser.Mode) : this(columns, mode, null)

    @JvmField val onFileSelect = EventHandler("OnFileSelect")

    val dirTextField = JTextField(columns)
    val selectDirButton = JButton("Select file...")

    var lastSelectedFile: File? = null
        set(value) {
            dirTextField.text = value?.absolutePath ?: ""
            field = value
            onFileSelect.run()
        }

    var lastSelectedFileFilter: FileFilter? = null
        private set

    init {
        dirTextField.isEditable = false

        selectDirButton.addActionListener {
            val frame = SwingUtilities.getWindowAncestor(this)
            dialogFactory.createFileChooser(frame, mode, "", *fileFilters).addCloseListener { returnVal: Int, selectedFile: File?, selectedFileFilter: FileFilter? ->
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    lastSelectedFileFilter = selectedFileFilter
                    lastSelectedFile = selectedFile
                }
            }
        }

        add(dirTextField)
        add(selectDirButton)
    }

}

