/*
 * Copyright (c) 2024 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.input.SourceType
import com.github.serivesmejia.eocvsim.util.loggerForThis
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

class InputSourceDropTarget(val eocvSim: EOCVSim) : DropTarget() {

    val logger by loggerForThis()

    @Suppress("UNCHECKED_CAST")
    override fun drop(evt: DropTargetDropEvent) {
        try {
            evt.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE)
            val droppedFiles = evt.transferable.getTransferData(
                DataFlavor.javaFileListFlavor
            ) as List<File>

            for(file in droppedFiles) {
                val sourceType = SourceType.isFileUsableForSource(file)

                if(sourceType != SourceType.UNKNOWN) {
                    DialogFactory.createSourceDialog(eocvSim, sourceType, file)
                    break
                }
            }
        } catch (e: Exception) {
            logger.warn("Drag n' drop failed", e)
        } finally {
            evt.dropComplete(true)
        }
    }

}