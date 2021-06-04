package com.github.serivesmejia.eocvsim.util.compiler

import com.github.serivesmejia.eocvsim.util.SysUtil
import java.io.File
import java.io.FileOutputStream
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

        val ze = ZipEntry(SysUtil.getRelativePath(rootFile, file).path)
        ze.time = file.lastModified()

        jar.putNextEntry(ze)
        SysUtil.copyStream(file, jar)
        jar.closeEntry()
    }

}