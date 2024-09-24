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
import io.github.deltacv.eocvsim.sandbox.restrictions.MethodCallByteCodeChecker
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicLoadingMethodBlacklist
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicLoadingPackageBlacklist
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicLoadingPackageWhitelist
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.FileSystems
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * ClassLoader for loading classes from a plugin jar file
 * @param pluginJar the jar file of the plugin
 * @param pluginContext the plugin context
 */
class PluginClassLoader(private val pluginJar: File, val pluginContextProvider: () -> PluginContext) : ClassLoader() {

    private val zipFile = try {
        ZipFile(pluginJar)
    } catch(e: Exception) {
        throw IOException("Failed to open plugin JAR file", e)
    }

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

                if(!pluginContextProvider().hasSuperAccess)
                    MethodCallByteCodeChecker(bytes, dynamicLoadingMethodBlacklist)

                val clazz = defineClass(name, bytes, 0, bytes.size)
                loadedClasses[name] = clazz

                return clazz
            }
        }
    }

    override fun findClass(name: String) = loadedClasses[name] ?: loadClass(name, false)

    /**
     * Load a class from the plugin jar file
     * @param name the name of the class to load
     * @return the loaded class
     * @throws IllegalAccessError if the class is blacklisted
     */
    fun loadClassStrict(name: String): Class<*> {
        if(!pluginContextProvider().hasSuperAccess) {
            for (blacklistedPackage in dynamicLoadingPackageBlacklist) {
                if (name.contains(blacklistedPackage)) {
                    throw IllegalAccessError("Plugins are blacklisted to use $name")
                }
            }
        }

        return loadClass(zipFile.getEntry(name.replace('.', '/') + ".class") ?: throw ClassNotFoundException(name))
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        var clazz = loadedClasses[name]

        if(clazz == null) {
            var inWhitelist = false

            for(whiteListedPackage in dynamicLoadingPackageWhitelist) {
                if(name.contains(whiteListedPackage)) {
                    inWhitelist = true
                    break
                }
            }

            if(!inWhitelist && !pluginContextProvider().hasSuperAccess) {
                throw IllegalAccessError("Plugins are not whitelisted to use $name")
            }

            clazz = try {
                Class.forName(name)
            } catch (e: ClassNotFoundException) {
                loadClassStrict(name)
            }

            if(resolve) resolveClass(clazz)
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

    override fun getResource(name: String): URL? {
        // Try to find the resource inside the plugin JAR
        val entry = zipFile.getEntry(name)

        if (entry != null) {
            try {
                // Construct a URL for the resource inside the plugin JAR
                return URL("jar:file:${pluginJar.absolutePath}!/$name")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback to the parent classloader if not found in the plugin JAR
        return super.getResource(name)
    }

    override fun toString() = "PluginClassLoader@\"${pluginJar.name}\""

}