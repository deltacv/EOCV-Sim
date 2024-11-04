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
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicLoadingExactMatchBlacklist
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicLoadingMethodBlacklist
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicLoadingPackageBlacklist
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicLoadingPackageWhitelist
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * ClassLoader for loading classes from a plugin jar file
 * @param pluginJar the jar file of the plugin
 * @param classpath additional classpath that the plugin may require
 * @param pluginContextProvider a function that provides the PluginContext for the plugin
 */
class PluginClassLoader(
    private val pluginJar: File,
    val classpath: List<File>,
    val pluginContextProvider: () -> PluginContext
) : ClassLoader() {

    private var additionalZipFiles = mutableListOf<WeakReference<ZipFile>>()

    private val zipFile = try {
        ZipFile(pluginJar)
    } catch (e: Exception) {
        throw IOException("Failed to open plugin JAR file", e)
    }

    private val loadedClasses = mutableMapOf<String, Class<*>>()

    private fun loadClass(entry: ZipEntry, zipFile: ZipFile = this.zipFile): Class<*> {
        val name = entry.name.removeFromEnd(".class").replace('/', '.')

        zipFile.getInputStream(entry).use { inStream ->
            ByteArrayOutputStream().use { outStream ->
                SysUtil.copyStream(inStream, outStream)
                val bytes = outStream.toByteArray()

                if (!pluginContextProvider().hasSuperAccess)
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
     */
    fun loadClassStrict(name: String): Class<*> {
        return loadClass(zipFile.getEntry(name.replace('.', '/') + ".class") ?: throw ClassNotFoundException(name))
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        var clazz = loadedClasses[name]

        try {
            if (clazz == null) {
                if (!pluginContextProvider().hasSuperAccess) {
                    var inWhitelist = false

                    for (whiteListedPackage in dynamicLoadingPackageWhitelist) {
                        if (name.contains(whiteListedPackage)) {
                            inWhitelist = true
                            break
                        }
                    }

                    if (!inWhitelist && !pluginContextProvider().hasSuperAccess) {
                        throw IllegalAccessError("Plugins are not whitelisted to use $name")
                    }

                    blacklistLoop@
                    for (blacklistedPackage in dynamicLoadingPackageBlacklist) {
                        // If the class is whitelisted, skip the blacklist check
                        if (inWhitelist) continue@blacklistLoop

                        if (name.contains(blacklistedPackage)) {
                            throw IllegalAccessError("Plugins are blacklisted to use $name")
                        }
                    }

                    for (blacklistedClass in dynamicLoadingExactMatchBlacklist) {
                        if (name == blacklistedClass) {
                            throw IllegalAccessError("Plugins are blacklisted to use $name")
                        }
                    }
                }

                clazz = Class.forName(name)
            }
        } catch(e: Throwable) {
            clazz = try {
                loadClassStrict(name)
            } catch(_: Throwable) {
                val classpathClass = classFromClasspath(name)

                if(classpathClass != null) {
                    classpathClass
                } else {
                    throw e
                }
            }
        }

        if (resolve) resolveClass(clazz)
        return clazz!!
    }

    override fun getResourceAsStream(name: String): InputStream? {
        val entry = zipFile.getEntry(name)

        if (entry != null) {
            try {
                return zipFile.getInputStream(entry)
            } catch (_: IOException) {
            }
        } else {
            // Try to find the resource inside the classpath
            resourceAsStreamFromClasspath(name)?.let { return it }
        }

        return super.getResourceAsStream(name)
    }

    override fun getResource(name: String): URL? {
        // Try to find the resource inside the plugin JAR
        val entry = zipFile.getEntry(name)

        if (entry != null) {
            // Construct a URL for the resource inside the plugin JAR
            return URL("jar:file:${pluginJar.absolutePath}!/$name")
        } else {
            resourceFromClasspath(name)?.let { return it }
        }

        // Fallback to the parent classloader if not found in the plugin JAR
        return super.getResource(name)
    }

    /**
     * Get a resource from the classpath specified in the constructor
     */
    fun resourceAsStreamFromClasspath(name: String): InputStream? {
        for (file in classpath) {
            if (file == pluginJar) continue

            val zipFile = ZipFile(file)

            val entry = zipFile.getEntry(name)

            if (entry != null) {
                try {
                    additionalZipFiles.add(WeakReference(zipFile))
                    return zipFile.getInputStream(entry)
                } catch (_: Exception) {
                    zipFile.close()
                }
            } else {
                zipFile.close()
            }
        }

        return null
    }

    /**
     * Get a resource from the classpath specified in the constructor
     */
    fun resourceFromClasspath(name: String): URL? {
        for (file in classpath) {
            if (file == pluginJar) continue

            val zipFile = ZipFile(file)

            try {
                val entry = zipFile.getEntry(name)

                if (entry != null) {
                    try {
                        return URL("jar:file:${file.absolutePath}!/$name")
                    } catch (_: Exception) {
                    }
                }
            } finally {
                zipFile.close()
            }
        }

        return null
    }

    /**
     * Load a class from the classpath specified in the constructor
     */
    fun classFromClasspath(className: String): Class<*>? {
        for (file in classpath) {
            if (file == pluginJar) continue

            val zipFile = ZipFile(file)

            try {
                val entry = zipFile.getEntry(className.replace('.', '/') + ".class")

                if (entry != null) {
                    return loadClass(entry, zipFile = zipFile)
                }
            } finally {
                zipFile.close()
            }
        }

        return null
    }

    fun close() {
        zipFile.close()
        for(ref in additionalZipFiles) {
            ref.get()?.close()
        }
    }

    override fun toString() = "PluginClassLoader@\"${pluginJar.name}\""

}