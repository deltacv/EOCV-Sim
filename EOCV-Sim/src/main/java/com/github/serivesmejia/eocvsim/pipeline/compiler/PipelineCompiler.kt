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

import com.github.serivesmejia.eocvsim.util.*
import com.github.serivesmejia.eocvsim.util.compiler.JarPacker
import com.github.serivesmejia.eocvsim.util.compiler.compiler
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.Field
import java.nio.charset.Charset
import java.util.*
import javax.tools.*

class PipelineCompiler(
    private val sourcesInputPath: File, private val sourceFiles: List<File>,
    private val resInputPath: File? = null, private val resFiles: List<File>? = null
): DiagnosticListener<JavaFileObject> {

    companion object {
        val logger by loggerFor(PipelineCompiler::class)

        val IS_USABLE by lazy {
            val usable = COMPILER != null

            // Send a warning message to console
            // will only be sent once (that's why it's done here)
            if(!usable) {
                logger.warn("Unable to compile Java source code in this JVM (the ToolProvider wasn't able to provide a compiler)")
                logger.warn("For the user, this probably means that the sim is running in a JRE which doesn't include the javac compiler executable")
                logger.warn("To be able to compile pipelines on runtime, make sure the sim is running on a JDK that includes the javac executable (any JDK probably does)")
            }

            usable
        }

        val COMPILER = compiler

        val INDENT = "      "
    }

    private var diagnosticBuilders = mutableMapOf<String, StringBuilder>()

    val latestDiagnostic: String
        get() {
            val diagnostic = StringBuilder()
            diagnostic.appendLine("Using the ${COMPILER!!.name} compiler").appendLine()
            for((_, builder) in diagnosticBuilders) {
                diagnostic.appendLine(builder)
                diagnostic.appendLine("")
            }

            return diagnostic.toString().trim()
        }

    val args = arrayListOf(
        "-source", "1.8",
        "-target", "1.8",
        "-g",
        "-encoding", "UTF-8",
        "-Xlint:unchecked",
        "-Xlint:deprecation",
        "-XDuseUnsharedTable=true"
    )

    constructor(inputPath: File) : this(inputPath, SysUtil.filesUnder(inputPath, ".java"))

    fun compile(outputJar: File): PipelineCompileResult {
        val javac = COMPILER!!.javaCompiler
        
        val fileManager = PipelineStandardFileManager(javac.getStandardFileManager(this, Locale.getDefault(), Charset.defaultCharset()))
        fileManager.sourcePath = Collections.singleton(sourcesInputPath)

        val javaFileObjects = fileManager.getJavaFileObjects(*sourceFiles.toTypedArray())

        if(javaFileObjects.iterator().hasNext()) {
            SysUtil.deleteFilesUnder(CompiledPipelineManager.CLASSES_OUTPUT_FOLDER)

            val task = javac.getTask(
                PrintWriter(System.out),
                fileManager,
                this,
                args,
                null,
                javaFileObjects
            )

            if(task.call()) {
                val outputClasses = fileManager.getLocation(StandardLocation.CLASS_OUTPUT).iterator().next()

                if(resInputPath == null || resFiles == null) {
                    JarPacker.packClassesUnder(outputJar, outputClasses)
                } else {
                    JarPacker.packResAndClassesUnder(outputJar, outputClasses, resInputPath, resFiles)
                }

                return PipelineCompileResult(PipelineCompileStatus.SUCCESS, latestDiagnostic)
            }

            return PipelineCompileResult(PipelineCompileStatus.FAILED, latestDiagnostic)
        } else {
            return PipelineCompileResult(PipelineCompileStatus.NO_SOURCE, "No source files")
        }
    }

    override fun report(diagnostic: Diagnostic<out JavaFileObject>) {
        // kinda stupid but eclipse compiler wraps exceptions in a "ExceptionDiagnostic" which is protected
        // and we can't access the exception directly in any way, so we have to use reflection. ExceptionDiagnostic
        // only returns the exception message, so we have to get the exception and rethrow it to get the full information on what went wrong
        if(diagnostic::class.java.typeName == "org.eclipse.jdt.internal.compiler.tool.ExceptionDiagnostic") {
            val eField = ReflectUtil.getDeclaredFieldDeep(diagnostic::class.java, "exception")!!
            eField.isAccessible = true

            val value = eField.get(diagnostic)

            if(value is Exception)
                throw value
        }
        
        val locale = Locale.getDefault()
        val source = diagnostic.source

        val builder = if(source != null) {
            val relativeFile = SysUtil.getRelativePath(sourcesInputPath, File(source.name))

            val builder = diagnosticBuilders[relativeFile.path] ?: StringBuilder()

            if (!diagnosticBuilders.containsKey(relativeFile.path)) {
                builder.appendLine("> ${relativeFile.path}")
                diagnosticBuilders[relativeFile.path] = builder
            }
            builder
        } else {
            val builder = diagnosticBuilders["other"] ?: StringBuilder()

            if (!diagnosticBuilders.containsKey("other")) {
                builder.appendLine("> Other")
                diagnosticBuilders["other"] = builder
            }

            builder
        }

        val formattedMessage = diagnostic.getMessage(locale).replace("\n", "\n$INDENT")

        builder.appendLine(String.format(locale, "$INDENT(%d:%d): %s: %s",
            diagnostic.lineNumber, diagnostic.columnNumber, diagnostic.kind, formattedMessage
        ))
    }

}

enum class PipelineCompileStatus {
    SUCCESS,
    FAILED,
    NO_SOURCE
}

data class PipelineCompileResult(val status: PipelineCompileStatus, val message: String)