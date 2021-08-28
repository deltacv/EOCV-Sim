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

package com.github.serivesmejia.eocvsim.pipeline.compiler.util

import com.github.serivesmejia.eocvsim.pipeline.compiler.CompiledPipelineManager
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.zeroBased
import java.io.File

object PackageStatementRemover {

    fun processFiles(sourceFiles: List<File>): Pair<List<File>, File> {
        val files = arrayListOf<File>()

        SysUtil.deleteFilesUnder(CompiledPipelineManager.FLAT_SRC_FOLDER)

        for(file in sourceFiles) {
            val destination = File(CompiledPipelineManager.FLAT_SRC_FOLDER.absolutePath + File.separator + file.name)
            if(destination.exists()) {
                throw IllegalStateException("Duplicate class file, name: ${file.name}. Make sure all the files in all the packages have a unique name")
            }

            SysUtil.saveFileStr(
                destination,
                stripPackageDeclaration(
                    SysUtil.loadFileStr(file)
                )
            )

            files.add(destination)
        }

        return Pair(files, CompiledPipelineManager.FLAT_SRC_FOLDER)
    }

    val PACKAGE_DECLARATION_REGEX = Regex("package ([\\w&&\\D||\\n]([\\w\\.]*[\\w])?);")

    fun stripPackageDeclaration(src: String): String {
        val match = PACKAGE_DECLARATION_REGEX.find(src)

        return if(match != null) {
            val builder = StringBuilder()
            val newLines = match.value.lines().size - 1

            builder.append(src.subSequence(0, match.range.first))
            repeat(newLines) {
                builder.appendLine()
            }
            builder.append(src.subSequence(match.range.last + 1, src.length.zeroBased))

            builder.toString()
        } else src
    }

}