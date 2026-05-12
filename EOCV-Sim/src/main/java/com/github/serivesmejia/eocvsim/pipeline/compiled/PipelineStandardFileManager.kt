/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.pipeline.compiled

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.compiler.DelegatingStandardFileManager
import io.github.deltacv.common.util.loggerFor
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
            logger.trace("Classpath: {}", System.getProperty("java.class.path"))

            //add all jars in the libs folder...hardcoded for JavaPackager, ugh
            classpathList.addAll(
                SysUtil.filesUnder(File(System.getProperty("user.dir") + File.separator + "libs"), ".jar")
            )

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

