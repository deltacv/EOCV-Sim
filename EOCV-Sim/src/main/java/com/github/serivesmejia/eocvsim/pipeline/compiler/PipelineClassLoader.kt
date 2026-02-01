package com.github.serivesmejia.eocvsim.pipeline.compiler

import com.github.serivesmejia.eocvsim.util.ClasspathScan
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.extension.removeFromEnd
import io.github.deltacv.eocvsim.sandbox.restrictions.MethodCallByteCodeChecker
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicCodeMethodBlacklist
import io.github.deltacv.eocvsim.sandbox.restrictions.dynamicCodePackageBlacklist
import org.openftc.easyopencv.OpenCvPipeline
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@Suppress("UNCHECKED_CAST")
class PipelineClassLoader(pipelinesJar: File) : ClassLoader() {

    private val zipFile = ZipFile(pipelinesJar)
    private val loadedClasses = mutableMapOf<String, Class<*>>()

    var pipelineClasses: List<Class<*>>
        private set

    init {
        val scanResult = ClasspathScan().scan(pipelinesJar.absolutePath, this)
        this.pipelineClasses = scanResult.pipelineClasses.toList()
    }

    private fun loadPipelineClass(entry: ZipEntry): Class<*> {
        val name = entry.name.removeFromEnd(".class").replace('/', '.')

        zipFile.getInputStream(entry).use { inStream ->
            ByteArrayOutputStream().use { outStream ->
                SysUtil.copyStream(inStream, outStream)
                val bytes = outStream.toByteArray()

                // Bytecode-level deny list (methods)
                MethodCallByteCodeChecker(bytes, dynamicCodeMethodBlacklist)

                val clazz = defineClass(name, bytes, 0, bytes.size)
                loadedClasses[name] = clazz
                return clazz
            }
        }
    }

    override fun findClass(name: String): Class<*> =
        loadedClasses[name] ?: throw ClassNotFoundException(name)

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        loadedClasses[name]?.let { return it }

        val clazz = try {
            // 2) Try pipeline JAR first
            val entry = zipFile.getEntry(name.replace('.', '/') + ".class")
            if (entry != null) {
                loadPipelineClass(entry)
            } else {
                // 3) Fallback to parent / system classloader
                Class.forName(name)
            }
        } catch (e: Throwable) {
            throw e
        }

        if (resolve) resolveClass(clazz)
        return clazz
    }

    override fun getResourceAsStream(name: String): InputStream? {
        val entry = zipFile.getEntry(name)
        if (entry != null) {
            try {
                return zipFile.getInputStream(entry)
            } catch (_: IOException) {}
        }
        return super.getResourceAsStream(name)
    }
}

val OpenCvPipeline.isFromRuntimeCompilation: Boolean
    get() = this::class.java.classLoader is PipelineClassLoader
