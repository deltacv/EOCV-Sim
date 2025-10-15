package io.github.deltacv.eocvsim.plugin.loader

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.config.ConfigLoader
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import io.github.deltacv.eocvsim.plugin.security.PluginSignature
import com.github.serivesmejia.eocvsim.gui.dialog.PluginOutput
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.extension.plus
import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * A PluginLoader that wraps an already-instantiated plugin
 * without loading from a JAR.
 */
class EmbeddedPluginLoader<T: EOCVSimPlugin>(
    val eocvSim: EOCVSim,
    override val pluginName: String,
    override val pluginVersion: String,
    override val pluginDescription: String = "",
    override val pluginAuthor: String = "",
    override val pluginAuthorEmail: String = "",
    private val superAccess: Boolean = true,
    override val pluginClass: Class<T>,
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

    override lateinit var plugin: EOCVSimPlugin
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
    val fileSystemZipPath by lazy { fileSystemZip.toPath() }

    override val hasSuperAccess: Boolean
        get() = superAccess

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

        val ctx = PluginContext(eocvSim, this)

        PluginContext.globalContextMap[pluginClass.name] = ctx
        plugin = pluginInstantiator()

        if (loaded) return
        plugin.onLoad()
        loaded = true
    }

    override fun enable() {
        if (!loaded || enabled) return
        plugin.enabled = true
        plugin.onEnable()
        enabled = true
    }

    override fun disable() {
        if (!enabled) return
        plugin.enabled = false
        plugin.onDisable()
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

    /**
     * Get the hash of the plugin based off the plugin name and author
     * @return the hash
     */
    override fun hash() = "${pluginName}${PluginOutput.SPECIAL}${pluginAuthor}".hashString

}
