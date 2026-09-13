/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.compiler

import org.deltacv.common.util.loggerOf
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler
import javax.tools.JavaCompiler
import javax.tools.ToolProvider

private val logger by loggerOf("CompilerProvider")

val compiler by lazy {
    val toolProviderCompiler = try {
        ToolProvider.getSystemJavaCompiler()
    } catch(e: Exception) {
        logger.warn("ToolProvider threw an exception on getSystemJavaCompiler()", e)
        null
    }

    try {
        if (toolProviderCompiler == null) {
            Compiler("Eclipse", EclipseCompiler())
        } else {
            Compiler("JDK", toolProviderCompiler)
        }
    } catch(e: Exception) {
        logger.warn("Unexpected exception while providing a java compiler", e)
        null
    }
}

data class Compiler(val name: String, val javaCompiler: JavaCompiler)
