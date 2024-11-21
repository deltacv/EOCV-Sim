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

package io.github.deltacv.eocvsim.plugin.security.superaccess

import com.github.serivesmejia.eocvsim.util.extension.fileHash
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.github.serivesmejia.eocvsim.util.serialization.PolymorphicAdapter
import com.google.gson.GsonBuilder
import com.moandjiezana.toml.Toml
import io.github.deltacv.eocvsim.gui.dialog.SuperAccessRequest
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import io.github.deltacv.eocvsim.plugin.loader.PluginManager.Companion.GENERIC_LAWYER_YEET
import io.github.deltacv.eocvsim.plugin.loader.PluginManager.Companion.GENERIC_SUPERACCESS_WARN
import io.github.deltacv.eocvsim.plugin.loader.PluginParser
import io.github.deltacv.eocvsim.plugin.security.Authority
import io.github.deltacv.eocvsim.plugin.security.AuthorityFetcher
import io.github.deltacv.eocvsim.plugin.security.MutablePluginSignature
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.File
import java.lang.Exception
import java.lang.Thread.sleep
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.zip.ZipFile
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

object SuperAccessDaemon {
    val logger by loggerForThis()

    val gson = GsonBuilder()
        .registerTypeHierarchyAdapter(SuperAccessMessage::class.java, PolymorphicAdapter<SuperAccessMessage>("message"))
        .registerTypeHierarchyAdapter(SuperAccessResponse::class.java, PolymorphicAdapter<SuperAccessResponse>("response"))
        .create()

    @get:Synchronized
    @set:Synchronized
    private var uniqueId = 0

    sealed class SuperAccessMessage {
        data class Request(var pluginPath: String, var signature: MutablePluginSignature, var reason: String) : SuperAccessMessage()
        data class Check(var pluginPath: String) : SuperAccessMessage()

        var id = uniqueId++
    }

    sealed class SuperAccessResponse(val id: Int) {
        class Success(id: Int) : SuperAccessResponse(id)
        class Failure(id: Int) : SuperAccessResponse(id)
    }

    val SUPERACCESS_FILE = PluginManager.PLUGIN_CACHING_FOLDER + File.separator + "superaccess.txt"

    private val access = ConcurrentHashMap<String, Boolean>()

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

        WsClient(args[0].toIntOrNull() ?: throw IllegalArgumentException("Port is not a valid int"), args[1].toBoolean()).connect()
    }

    class WsClient(port: Int, val autoacceptTrusted: Boolean) : WebSocketClient(URI("ws://localhost:$port")) {

        private val executor = Executors.newFixedThreadPool(4)

        override fun onOpen(p0: ServerHandshake?) {
            logger.info("SuperAccessDaemon connection opened.")
            logger.info("Autoaccept on trusted: $autoacceptTrusted")
        }

        override fun onMessage(msg: String) {
            val message = gson.fromJson(msg, SuperAccessMessage::class.java)

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
                (gson.toJson(SuperAccessResponse.Failure(message.id)))
                return@handleRequest
            }

            if(SUPERACCESS_FILE.exists() && SUPERACCESS_FILE.readLines().contains(pluginFile.fileHash())) {
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
                val fetch = AuthorityFetcher.fetchAuthority(parser.pluginAuthor)
                untrusted = fetch != null // the plugin is claiming to be made by the authority, but it's not signed by them
            }

            val reason = message.reason

            val name = "${parser.pluginName} v${parser.pluginVersion} by ${parser.pluginAuthor}"

            var warning = "<html>$GENERIC_SUPERACCESS_WARN"
            if(reason.trim().isNotBlank()) {
                warning += "<br><br><i>$reason</i>"
            }

            // helper function to grant access, avoid code duplication
            fun grant() {
                SUPERACCESS_FILE.appendText(pluginFile.fileHash() + "\n")
                accessGranted(message.id, message.pluginPath)
            }

            warning += if(validAuthority != null) {
                "<br><br>This plugin has been digitally signed by <b>${validAuthority.name}</b>.<br>It is a trusted authority in the EOCV-Sim ecosystem."
            } else if(untrusted) {
                "<br><br>This plugin claims to be made by <b>${parser.pluginAuthor}</b>, but it has not been digitally signed by them.<br><h2>Beware of potential security risks.</h2>"
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

            if(SUPERACCESS_FILE.exists() && SUPERACCESS_FILE.readLines().contains(pluginFile.fileHash())) {
                accessGranted(message.id, message.pluginPath)
            } else {
                accessDenied(message.id, message.pluginPath)
            }
        }

        private fun accessGranted(id: Int, pluginPath: String) {
            access[pluginPath] = true
            send(gson.toJson(SuperAccessResponse.Success(id)))
        }

        private fun accessDenied(id: Int, pluginPath: String) {
            access[pluginPath] = false
            send(gson.toJson(SuperAccessResponse.Failure(id)))
        }

        override fun onClose(p0: Int, p1: String?, p2: Boolean) {
            logger.info("SuperAccessDaemon connection closed: $p0, $p1, $p2")
            exitProcess(-1)
        }

        override fun onError(p0: Exception?) {
            logger.error("Error in SuperAccessDaemon", p0)
        }
    }

    private fun parsePlugin(file: File): PluginParser? {
        ZipFile(file).use {
            val pluginToml = it.getEntry("plugin.toml")
            if(pluginToml != null) {
                return PluginParser(Toml().read(it.getInputStream(pluginToml)))
            }
        }

        return null
    }
}