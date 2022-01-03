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

package com.github.serivesmejia.eocvsim.workspace.util.template

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.github.serivesmejia.eocvsim.workspace.util.VSCodeLauncher
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
            templateZipResource, "default_workspace.zip", false
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
