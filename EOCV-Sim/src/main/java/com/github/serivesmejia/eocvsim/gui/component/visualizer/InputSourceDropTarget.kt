/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.DialogFactory
import com.github.serivesmejia.eocvsim.input.SourceType
import io.github.deltacv.common.util.loggerForThis
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

class InputSourceDropTarget : DropTarget(), KoinComponent {

    val logger by loggerForThis()

    private val dialogFactory: DialogFactory by inject()

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
                    dialogFactory.createSourceDialog(sourceType, file)
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
