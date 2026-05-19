/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.workspace.util

import org.deltacv.common.util.loggerForThis
import java.io.File

abstract class WorkspaceTemplate {

    fun extractToIfEmpty(folder: File): Boolean {
        val logger by loggerForThis()

        if(folder.isDirectory && folder.listFiles()!!.isEmpty()) {
            return try {
                extractTo(folder)
            } catch(e: Exception) {
                logger.error("Error while extracting workspace template to $folder", e)
                false
            }
        }

        return false
    }

    abstract fun extractTo(folder: File): Boolean

}
