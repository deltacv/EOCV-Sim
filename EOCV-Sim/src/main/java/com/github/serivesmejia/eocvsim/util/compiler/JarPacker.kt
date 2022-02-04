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

package com.github.serivesmejia.eocvsim.util.compiler

import com.github.serivesmejia.eocvsim.util.SysUtil
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry

object JarPacker {

    private fun pack(outputJar: File, inputClasses: File,
                     resourceFilesRoot: File? = null,
                     resourceFiles: List<File>? = null,
                     manifest: Manifest = Manifest()) {

        FileOutputStream(outputJar).use { outStream ->
            JarOutputStream(outStream, manifest).use { jarOutStream ->
                for (classFile in SysUtil.filesUnder(inputClasses, ".class")) {
                    putFileInJar(jarOutStream, inputClasses, classFile)
                }

                if(resourceFiles != null && resourceFilesRoot != null) {
                    for(resFile in resourceFiles) {
                        putFileInJar(jarOutStream, resourceFilesRoot, resFile)
                    }
                }
            }
        }

    }

    fun packClassesUnder(outputJar: File,
                         inputClasses: File,
                         manifest: Manifest = Manifest()) = pack(outputJar, inputClasses, manifest = manifest)

    fun packResAndClassesUnder(outputJar: File,
                               inputClasses: File,
                               resourceFilesRoot: File,
                               resourceFiles: List<File>,
                               manifest: Manifest = Manifest()) =
        pack(outputJar, inputClasses, resourceFilesRoot, resourceFiles, manifest)

    private fun putFileInJar(jar: JarOutputStream, rootFile: File, file: File) {
        if(!file.exists()) return

        val ze = JarEntry(SysUtil.getRelativePath(rootFile, file).path.replace(File.separator, "/"))
        ze.time = file.lastModified()

        jar.putNextEntry(ze)
        SysUtil.copyStream(file, jar)
        jar.closeEntry()
    }

}