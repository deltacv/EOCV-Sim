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

package io.github.deltacv.eocvsim.plugin.security

import com.github.serivesmejia.eocvsim.util.extension.hashString
import io.github.deltacv.common.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.eocvsim.plugin.loader.InvalidPluginException
import java.io.File
import java.security.PublicKey
import java.security.Signature
import java.util.Base64
import java.util.zip.ZipFile

data class PluginSignature(
    val verified: Boolean,
    val authority: Authority?,
    val timestamp: Long
)

data class MutablePluginSignature(
    var verified: Boolean,
    var authority: MutableAuthority?,
    var timestamp: Long
)

fun PluginSignature.toMutable() = MutablePluginSignature(verified, authority?.toMutable(), timestamp)

object PluginSignatureVerifier {
    private val emptyResult = PluginSignature(false, null, 0L)

    val logger by loggerForThis()

    fun verify(pluginFile: File): PluginSignature {
        logger.info("Verifying plugin signature of ${pluginFile.name}")

        ZipFile(pluginFile).use { zip ->
            val signatureEntry = zip.getEntry("signature.toml")
            val pluginEntry = zip.getEntry("plugin.toml")

            if (signatureEntry == null) {
                logger.warn("Plugin JAR does not contain a signature.toml file")
                return emptyResult
            }

            if(pluginEntry == null) {
                logger.warn("Plugin JAR does not contain a plugin.toml file")
                return emptyResult
            }

            val signatureToml = zip.getInputStream(signatureEntry).bufferedReader()
            val signature = Toml().read(signatureToml)

            val authorityName = signature.getString("authority")
            if (authorityName == null) {
                logger.warn("signature.toml does not contain an authority field")
                return emptyResult
            }

            val authorityPublic = signature.getString("public")
            if (authorityPublic == null) {
                logger.warn("signature.toml does not contain a public (key) field")
                return emptyResult
            }

            val authority = AuthorityFetcher.fetchAuthority(authorityName)
            if (authority == null) {
                logger.warn("Failed to fetch authority $authorityName")
                return emptyResult
            }

            if(Base64.getEncoder().encodeToString(authority.publicKey.encoded) != authorityPublic) {
                logger.warn("Authority public key does not match the one in the signature")
                return emptyResult
            }

            val signatureData = signature.getTable("signatures") ?: run {
                logger.warn("signature.toml does not contain a signatures table")
                return emptyResult
            }

            // Get the public key from the authority
            val publicKey = authority.publicKey

            val signatures = mutableMapOf<String, String>()

            // Verify each signature
            for ((path, sign) in signatureData.toMap()) {
                if(sign !is String) {
                    logger.warn("Signature for class $path is not a string")
                    return emptyResult
                }

                signatures[path] = sign
            }

            // Now, verify each class in the JAR file
            val classEntries = zip.entries().toList().filter { entry -> entry.name.endsWith(".class") }

            if(classEntries.count() != signatures.count()) {
                logger.warn("Amount of classes in the JAR does not match the amount of signatures")
                return emptyResult
            }

            val signatureStatus = mutableMapOf<String, Boolean>()

            for(sign in signatures) {
                signatureStatus[sign.key] = false
            }

            for (classEntry in classEntries) {
                val className = classEntry.name.removeSuffix(".class").replace('/', '.') // Convert to fully qualified class name

                // Fetch the class bytecode
                val classData = zip.getInputStream(classEntry).readBytes()

                // Hash the class name to get the signature key
                val classHash = className.hashString

                // Verify the signature of the class
                if (!verifySignature(classData, signatures[classHash]!!, publicKey)) {
                    throw InvalidPluginException("Signature verification failed for class $className. Please try to re-download the plugin or discard it immediately.")
                } else {
                    signatureStatus[classHash] = true
                }
            }

            if(signatureStatus.containsValue(false)) {
                throw InvalidPluginException("Some classes in the plugin are not signed. Please try to re-download the plugin or discard it immediately.")
            }

            logger.info("Plugin ${pluginFile.absolutePath} has been verified, signed by $authorityName")


            return PluginSignature(true, authority, System.currentTimeMillis())
        }
    }

    private fun verifySignature(classData: ByteArray, signatureString: String, publicKey: PublicKey): Boolean {
        // Extract the actual signature bytes from the string (you'll need to implement this)
        val signatureBytes = decodeSignature(signatureString) // Implement this method
        return try {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(classData)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            logger.error("Error during signature verification", e)
            false
        }
    }

    private fun decodeSignature(signatureString: String): ByteArray {
        // Remove any whitespace or newline characters that may exist in the string
        val cleanSignature = signatureString.replace("\\s".toRegex(), "")

        // Decode the Base64-encoded string into a byte array
        return Base64.getDecoder().decode(cleanSignature)
    }
}