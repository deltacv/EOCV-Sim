/*
 * Copyright (c) 2024 Sebastian Erives
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

package io.github.deltacv.eocvsim.plugin.loader

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.removeFromEnd
import io.github.deltacv.eocvsim.plugin.pluginPackageBlacklist
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class PluginClassLoader(private val pluginJar: File, val pluginContext: PluginContext) : ClassLoader() {

    private val zipFile = ZipFile(pluginJar)
    private val loadedClasses = mutableMapOf<String, Class<*>>()

    init {
        FileSystems.getDefault()
    }

    private fun loadClass(entry: ZipEntry): Class<*> {
        val name = entry.name.removeFromEnd(".class").replace('/', '.')

        zipFile.getInputStream(entry).use { inStream ->
            ByteArrayOutputStream().use { outStream ->
                SysUtil.copyStream(inStream, outStream)
                val bytes = outStream.toByteArray()

                val clazz = defineClass(name, bytes, 0, bytes.size)
                loadedClasses[name] = clazz

                return clazz
            }
        }
    }

    override fun findClass(name: String) = loadedClasses[name] ?: loadClass(name, false)

    fun loadClassStrict(name: String): Class<*> {
        for(blacklistedPackage in pluginPackageBlacklist) {
            if(name.contains(blacklistedPackage)) {
                throw IllegalAccessError("Plugins are not authorized to use $name")
            }
        }

        return loadClass(zipFile.getEntry(name.replace('.', '/') + ".class"))
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        var clazz = loadedClasses[name]

        if(clazz == null) {
            try {
                clazz = loadClassStrict(name)
                if(resolve) resolveClass(clazz)
            } catch(e: Exception) {
                // fallback to the system classloader
                clazz = Class.forName(name)
            }
        }

        return clazz!!
    }

    override fun getResourceAsStream(name: String): InputStream? {
        val entry = zipFile.getEntry(name)

        if(entry != null) {
            try {
                return zipFile.getInputStream(entry)
            } catch (e: IOException) { }
        }

        return super.getResourceAsStream(name)
    }

    override fun toString() = "PluginClassLoader@\"${pluginJar.name}\""

}