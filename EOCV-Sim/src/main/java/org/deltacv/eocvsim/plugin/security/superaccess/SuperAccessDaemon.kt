/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.security.superaccess

import com.github.serivesmejia.eocvsim.util.extension.fileHash
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.serialization.JacksonJsonSupport
import org.deltacv.common.util.loggerForThis
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.deltacv.common.util.serialization.Toml
import org.deltacv.eocvsim.gui.dialog.SuperAccessRequest
import org.deltacv.eocvsim.plugin.loader.PluginInfo
import org.deltacv.eocvsim.plugin.loader.PluginManager
import org.deltacv.eocvsim.plugin.loader.PluginManager.Companion.GENERIC_LAWYER_YEET
import org.deltacv.eocvsim.plugin.loader.PluginManager.Companion.GENERIC_SUPERACCESS_WARN
import org.deltacv.eocvsim.plugin.security.Authority
import org.deltacv.eocvsim.plugin.security.AuthorityFetcher
import org.deltacv.eocvsim.plugin.security.MutablePluginSignature
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.File
import java.lang.Exception
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipFile
import javax.swing.SwingUtilities
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

object SuperAccessDaemon {
    val logger by loggerForThis()

    val mapper = JacksonJsonSupport.ipcMapper

    @get:Synchronized
    @set:Synchronized
    private var uniqueId = 0

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    sealed class SuperAccessMessage {
        data class Request(var pluginPath: String, var signature: MutablePluginSignature, var reason: String) : SuperAccessMessage()
        data class Check(var pluginPath: String) : SuperAccessMessage()

        var id = uniqueId++
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    sealed class SuperAccessResponse(val id: Int) {
        class Success(id: Int) : SuperAccessResponse(id)
        class Failure(id: Int) : SuperAccessResponse(id)
    }

    val SUPERACCESS_FILE = PluginManager.PLUGIN_CACHING_FOLDER + File.separator + "superaccess.txt"

    private val access = ConcurrentHashMap<String, Boolean>()
    private val fileLock = ReentrantLock()

    @JvmStatic
    fun main(args: Array<String>) {
        if(args.size < 2) {
            logger.error("Usage: <port> <autoAcceptOnTrusted (true/false)>")
            exitProcess(-1)
        }
        if(args.size > 2) {
            logger.warn("Ignoring extra arguments.")
        }

        System.setProperty("sun.java2d.d3d", "false")
        System.setProperty("apple.awt.UIElement", "true")
        System.setProperty("apple.awt.application.appearance", "system")
        System.setProperty("apple.awt.application.name", "EasyOpenCV Simulator - SuperAccess")

        // start websocket client, listen for EOCV-Sim's requests
        WsClient(args[0].toIntOrNull() ?: throw IllegalArgumentException("Port is not a valid int"), args[1].toBoolean()).connect()
    }

    class WsClient(port: Int, val autoacceptTrusted: Boolean) : WebSocketClient(URI("ws://localhost:$port")) {

        private val executor = Executors.newFixedThreadPool(4)

        override fun onOpen(p0: ServerHandshake?) {
            logger.info("SuperAccessDaemon connection opened.")
            logger.info("Autoaccept on trusted: $autoacceptTrusted")
        }

        override fun onMessage(msg: String) {
            val message = mapper.readValue(msg, SuperAccessMessage::class.java)

            executor.submit {
                when (message) {
                    is SuperAccessMessage.Request -> {
                        handleRequest(message)
                    }

                    is SuperAccessMessage.Check -> {
                        handleCheck(message)
                    }
                }
            }
        }

        private fun handleRequest(message: SuperAccessMessage.Request) {
            val pluginFile = File(message.pluginPath)

            val parser = parsePlugin(pluginFile) ?: run {
                logger.error("Failed to parse plugin at ${message.pluginPath}")
                send(mapper.writeValueAsString(SuperAccessResponse.Failure(message.id)))
                return@handleRequest
            }

            val hasAccess = fileLock.withLock {
                SUPERACCESS_FILE.exists() && SUPERACCESS_FILE.readLines().contains(pluginFile.fileHash())
            }

            if(hasAccess) {
                accessGranted(message.id, message.pluginPath)
                return
            }

            logger.info("Requesting SuperAccess for ${message.pluginPath}")

            var validAuthority: Authority? = null
            var untrusted = false

            if(message.signature.authority != null) {
                val declaredAuthority = message.signature.authority!!
                val authorityName = declaredAuthority.name

                val fetchedAuthorityKey = AuthorityFetcher.fetchAuthority(authorityName)?.publicKey

                if(fetchedAuthorityKey == null) {
                    logger.error("Failed to fetch authority $authorityName")
                } else {
                    if(fetchedAuthorityKey.encoded.contentEquals(declaredAuthority.publicKey)) {
                        logger.info("Authority key matches the fetched key for $authorityName")
                        validAuthority = Authority(authorityName, fetchedAuthorityKey)
                    } else {
                        logger.warn("Authority key does not match the fetched key for $authorityName")
                        untrusted = true
                    }
                }
            } else {
                val fetch = AuthorityFetcher.fetchAuthority(parser.author)
                untrusted = fetch != null // the plugin is claiming to be made by the authority, but it's not signed by them
            }

            val reason = message.reason

            val name = parser.nameWithVersionAndAuthor

            var warning = "<html>$GENERIC_SUPERACCESS_WARN"
            if(reason.trim().isNotBlank()) {
                warning += "<br><br><i>$reason</i>"
            }

            // helper function to grant access, avoid code duplication
            fun grant() {
                fileLock.withLock {
                    if (!SUPERACCESS_FILE.exists()) {
                        SUPERACCESS_FILE.createNewFile()
                    }
                    // Re-check in case another thread granted access in the meantime
                    if (!SUPERACCESS_FILE.readLines().contains(pluginFile.fileHash())) {
                        SUPERACCESS_FILE.appendText(pluginFile.fileHash() + "\n")
                    }
                }
                accessGranted(message.id, message.pluginPath)
            }

            warning += if(validAuthority != null) {
                "<br><br>This plugin has been digitally signed by <b>${validAuthority.name}</b>.<br>It is a trusted authority in the EOCV-Sim ecosystem."
            } else if(untrusted) {
                "<br><br>This plugin claims to be made by <b>${parser.author}</b>, but it has not been digitally signed by them.<br><h2>Beware of potential security risks.</h2>"
            } else {
                GENERIC_LAWYER_YEET
            }

            warning += "</html>"

            if(validAuthority != null && autoacceptTrusted) {
                grant()
                logger.info("Granted automatic SuperAccess to $name. The plugin has been signed by ${validAuthority.name}")
                return
            }

            SwingUtilities.invokeLater {
                SuperAccessRequest(name, warning, validAuthority == null) { granted ->
                    if(granted) {
                        grant()
                    } else {
                        accessDenied(message.id, message.pluginPath)
                    }
                }
            }
        }

        private fun handleCheck(message: SuperAccessMessage.Check) {
            if(access.containsKey(message.pluginPath)) {
                if(access[message.pluginPath] == true) {
                    accessGranted(message.id, message.pluginPath)
                    return
                } else {
                    accessDenied(message.id, message.pluginPath)
                    return
                }
            }

            val pluginFile = File(message.pluginPath)

            val hasAccess = fileLock.withLock {
                SUPERACCESS_FILE.exists() && SUPERACCESS_FILE.readLines().contains(pluginFile.fileHash())
            }

            if(hasAccess) {
                accessGranted(message.id, message.pluginPath)
            } else {
                accessDenied(message.id, message.pluginPath)
            }
        }

        private fun accessGranted(id: Int, pluginPath: String) {
            access[pluginPath] = true
            send(mapper.writeValueAsString(SuperAccessResponse.Success(id)))
        }

        private fun accessDenied(id: Int, pluginPath: String) {
            access[pluginPath] = false
            send(mapper.writeValueAsString(SuperAccessResponse.Failure(id)))
        }

        override fun onClose(p0: Int, p1: String?, p2: Boolean) {
            logger.info("SuperAccessDaemon connection closed: $p0, $p1, $p2")
            exitProcess(-1)
        }

        override fun onError(p0: Exception?) {
            logger.error("Error in SuperAccessDaemon", p0)
        }
    }

    private fun parsePlugin(file: File): PluginInfo? {
        ZipFile(file).use {
            val pluginToml = it.getEntry("plugin.toml")
            if(pluginToml != null) {
                return PluginInfo.fromToml(Toml().read(it.getInputStream(pluginToml)))
            }
        }

        return null
    }
}
