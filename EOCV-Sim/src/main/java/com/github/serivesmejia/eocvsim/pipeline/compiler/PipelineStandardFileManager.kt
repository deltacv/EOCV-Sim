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

package com.github.serivesmejia.eocvsim.pipeline.compiler

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.compiler.DelegatingStandardFileManager
import com.github.serivesmejia.eocvsim.util.loggerFor
import java.io.File
import java.util.*
import javax.tools.StandardJavaFileManager
import javax.tools.StandardLocation

class PipelineStandardFileManager(delegate: StandardJavaFileManager) : DelegatingStandardFileManager(delegate) {

    var sourcePath: Iterable<File>
        set(value) = delegate.setLocation(StandardLocation.SOURCE_PATH, value)
        get() = delegate.getLocation(StandardLocation.SOURCE_PATH)

    companion object {
        val logger by loggerFor(PipelineStandardFileManager::class)

        val classpath by lazy {
            val classpathList = arrayListOf<File>()

            logger.trace("Scanning classpath files...")
            
            for(file in SysUtil.getClasspathFiles()) {
                val files = SysUtil.filesUnder(file, ".jar")
                files.forEach {
                    logger.trace("Found classpath file ${it.absolutePath}")
                }

                classpathList.addAll(files)
            }

            classpathList.toList()
        }
    }

    init {
        delegate.setLocation(StandardLocation.CLASS_PATH, classpath)
        delegate.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(CompiledPipelineManager.CLASSES_OUTPUT_FOLDER))
        delegate.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singletonList(CompiledPipelineManager.SOURCES_OUTPUT_FOLDER))
    }

}
