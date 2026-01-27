package io.github.deltacv.eocvsim.plugin.loader

import com.github.serivesmejia.eocvsim.config.ConfigLoader
import com.github.serivesmejia.eocvsim.util.extension.plus
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.security.PluginSignature
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import net.lingala.zip4j.ZipFile
import java.io.File
import java.nio.file.Path

/**
 * A PluginLoader that wraps an already-instantiated
 * plugin, without loading from a JAR.
 */
class EmbeddedPluginLoader<T: EOCVSimPlugin>(
    override val pluginInfo: PluginInfo,
    override val pluginClass: Class<T>,
    val pluginApiProvider: EOCVSimApiProvider,
    val pluginInstantiator: () -> T
) : PluginLoader() {

    override val pluginSource: PluginSource = PluginSource.EMBEDDED

    override val pluginFile: File get() {
        // find the jar where the plugin class is located
        return pluginClass.protectionDomain.codeSource.location.file.let { File(it) }
    }

    override var loaded: Boolean = false
        private set

    override var enabled: Boolean = false
        private set

    override var shouldEnable: Boolean = true // Embedded plugins are always enabled by config

    override var plugin: EOCVSimPlugin? = null
        private set

    override val signature = PluginSignature(false, null, System.currentTimeMillis())

    override val classpath: List<File>
        get() {
            //return the current classpath of the java runtime
            // all jars
            return System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map { File(it) }
                .filter { it.exists() && it.isFile && it.extension == "jar" }
        }

    /**
     * The file system for the plugin
     */
    override lateinit var fileSystem: SandboxFileSystem
        private set

    val fileSystemZip by lazy { PluginManager.FILESYSTEMS_FOLDER + File.separator + "${hash()}-fs" }
    val fileSystemZipPath: Path by lazy { fileSystemZip.toPath() }

    override val hasSuperAccess get() = pluginInfo.superAccess

    constructor(
        pluginInfo: PluginInfo,
        pluginClass: Class<T>,
        pluginApiProvider: EOCVSimApiProvider,
    ) : this(
        pluginInfo,
        pluginClass,
        pluginApiProvider,
        pluginInstantiator = {
            try {
                // instantiate with default constructor
                val constructor = pluginClass.getDeclaredConstructor()
                constructor.newInstance()
            } catch(_: NoSuchMethodException) {
                throw InvalidPluginException("Plugin class must have a default constructor or use the other constructor of EmbeddedPluginLoader")
            } catch (e: Error) {
                throw InvalidPluginException("Failed to instantiate plugin class: ${e.message}")
            }
        }
    )

    private fun setupFs() {
        if(!fileSystemZip.exists()) {
            val zip = ZipFile(fileSystemZip) // kinda wack but uh, yeah...
            zip.addFile(ConfigLoader.CONFIG_SAVEFILE)
            zip.removeFile(ConfigLoader.CONFIG_SAVEFILE.name)
            zip.close()
        }

        fileSystem = SandboxFileSystem(fileSystemZipPath, hash())
    }

    override fun load() {
        setupFs()

        val ctx = PluginContext(pluginApiProvider, this)

        PluginContext.globalContextMap[pluginClass.name] = ctx
        plugin = pluginInstantiator()

        if (loaded) return
        plugin!!.onLoad()
        loaded = true
    }

    override fun enable() {
        if (!loaded || enabled) return
        plugin!!.enabled = true
        plugin!!.onEnable()
        enabled = true
    }

    override fun disable() {
        if (!enabled) return
        plugin!!.enabled = false
        plugin!!.onDisable()
        kill()
    }

    override fun kill() {
        if (!loaded) return
        fileSystem.close()
        enabled = false
        loaded = false
    }

    override fun requestSuperAccess(reason: String): Boolean {
        // Embedded plugins either always have or never have super access
        return hasSuperAccess
    }
}
