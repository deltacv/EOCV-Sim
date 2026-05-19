/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.workspace.util.template

import com.github.serivesmejia.eocvsim.util.SysUtil
import org.deltacv.common.util.loggerForThis
import com.github.serivesmejia.eocvsim.workspace.util.WorkspaceTemplate
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.IOException

object DefaultWorkspaceTemplate : WorkspaceTemplate() {

    val logger by loggerForThis()

    val templateZipResource = javaClass.getResourceAsStream("/templates/default_workspace.zip")

    override fun extractTo(folder: File): Boolean {
        if(!folder.isDirectory) return false

        val templateZipFile = SysUtil.copyFileIsTemp(
            templateZipResource, "default_workspace.zip", true
        ).file

        return try {
            logger.info("Extracting template to ${folder.absolutePath}")

            ZipFile(templateZipFile).extractAll(folder.absolutePath)

            logger.info("Successfully extracted template")
            true
        } catch(ex: IOException) {
            logger.warn("Failed to extract workspace template to ${folder.absolutePath}", ex)
            false
        }
    }

}

