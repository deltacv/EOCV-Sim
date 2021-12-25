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

import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import java.awt.FlowLayout
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileFilter

class FileSelector(columns: Int = 18,
                   mode: DialogFactory.FileChooser.Mode,
                   vararg fileFilters: FileFilter?) : JPanel(FlowLayout()) {

    constructor(columns: Int) : this(columns, DialogFactory.FileChooser.Mode.FILE_SELECT)

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
            DialogFactory.createFileChooser(frame, mode, *fileFilters).addCloseListener { returnVal: Int, selectedFile: File?, selectedFileFilter: FileFilter? ->
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
