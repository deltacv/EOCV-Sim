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

import com.github.serivesmejia.eocvsim.gui.dialog.PluginOutput
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.eocvsim.plugin.loader.PluginParser
import picocli.CommandLine
import java.io.File
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import java.util.Base64
import java.util.Scanner
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipOutputStream
import kotlin.math.log
import kotlin.system.exitProcess

class PluginSigningTool : Runnable {

    val logger by loggerForThis()

    @CommandLine.Option(names = ["-p", "--plugin"], description = ["The plugin JAR file to sign"], required = true)
    var pluginFile: String? = null

    @CommandLine.Option(names = ["-a", "--authority"], description = ["The authority to sign the plugin with"], required = true)
    var authority: String? = null

    @CommandLine.Option(names = ["-k", "--key"], description = ["The private key file to sign the plugin with"], required = true)
    var privateKeyFile: String? = null

    override fun run() {
        logger.info("Signing plugin $pluginFile with authority $authority")

        val authority = AuthorityFetcher.fetchAuthority(authority ?: throw IllegalArgumentException("Authority is required"))
        if (authority == null) {
            exitProcess(1)
        }

        // Load the private key
        val privateKey = loadPrivateKey(privateKeyFile ?: throw IllegalArgumentException("Private key file is required"))

        val publicKey = authority.publicKey // Assuming Authority has a publicKey property
        if (!isPrivateKeyMatchingAuthority(publicKey, privateKey)) {
            logger.error("Private key does not match to the authority's public key.")
            exitProcess(1)
        } else {
            logger.info("Private key matches to the authority's public key.")
        }

        signPlugin(File(pluginFile), privateKey, authority)

        logger.info("Plugin signed successfully and saved")
    }

    private fun loadPrivateKey(privateKeyPath: String): PrivateKey {
        val keyBytes = File(privateKeyPath).readBytes()
        val keyString = String(keyBytes)
        val decodedKey = Base64.getDecoder().decode(AuthorityFetcher.parsePem(keyString))

        val keySpec = PKCS8EncodedKeySpec(decodedKey)
        val keyFactory = KeyFactory.getInstance("RSA")

        return keyFactory.generatePrivate(keySpec)
    }

    private fun signPlugin(jarFile: File, privateKey: PrivateKey, authority: Authority) {
        // Generate signatures for each class in the JAR
        val signatures = mutableMapOf<String, String>()

        ZipFile(jarFile).use { zip ->
            val classEntries = zip.entries().asSequence().filter { it.name.endsWith(".class") }

            for (classEntry in classEntries) {
                val className = classEntry.name.removeSuffix(".class").replace('/', '.')

                val classData = zip.getInputStream(classEntry).readBytes()

                // Sign the class data
                val signature = signClass(classData, privateKey)
                signatures[className] = signature

                logger.info("Signed class $className")
            }
        }

        logger.info("Signed all classes, creating signature.toml in jar")

        // Create signature.toml
        createSignatureToml(signatures, authority, jarFile)
    }

    private fun signClass(classData: ByteArray, privateKey: PrivateKey): String {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(classData)
        val signatureBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signatureBytes)
    }

    private fun createSignatureToml(
        signatures: Map<String, String>,
        authority: Authority, jarFile: File
    ) {
        val toml = StringBuilder()
        toml.appendLine("authority = \"${authority.name}\"")
        toml.appendLine("public = \"${Base64.getEncoder().encodeToString(authority.publicKey.encoded)}\"")
        toml.appendLine("timestamp = ${System.currentTimeMillis()}")
        toml.appendLine("[signatures]")

        for ((className, signature) in signatures) {
            toml.appendLine("${className.hashString} = \"$signature\"")
        }

        saveTomlToJar(toml.toString(), jarFile)
    }


    private fun saveTomlToJar(tomlContent: String, jarFile: File) {
        // Create a temporary file for the new JAR
        val tempJarFile = File(jarFile.parentFile, "${jarFile.name}.tmp")

        ZipOutputStream(FileOutputStream(tempJarFile)).use { zipOut ->
            // First, copy existing entries from the original JAR
            jarFile.inputStream().use { jarIn ->
                val jarInput = java.util.zip.ZipInputStream(jarIn)
                var entry: ZipEntry?
                while (jarInput.nextEntry.also { entry = it } != null) {
                    if(entry!!.name == "signature.toml") {
                        continue
                    }

                    // Write each existing entry to the new JAR
                    zipOut.putNextEntry(ZipEntry(entry!!.name))
                    jarInput.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }

            // Now add the new signature.toml file
            val tomlEntry = ZipEntry("signature.toml")
            zipOut.putNextEntry(tomlEntry)
            zipOut.write(tomlContent.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        // Replace the original JAR with the new one
        if (!jarFile.delete()) {
            throw IOException("Failed to delete original JAR file: ${jarFile.absolutePath}")
        }
        if (!tempJarFile.renameTo(jarFile)) {
            throw IOException("Failed to rename temporary JAR file to original name: ${tempJarFile.absolutePath}")
        }
    }

    private fun isPrivateKeyMatchingAuthority(publicKey: PublicKey, privateKey: PrivateKey): Boolean {
        // encrypt and decrypt a sample message to check if the keys match
        val sampleMessage = PluginOutput.SPECIAL

        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(sampleMessage.toByteArray())

        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decrypted = cipher.doFinal(encrypted)

        return sampleMessage == String(decrypted)
    }
}

fun main(args: Array<String>) {
    if(args.isEmpty()) {
        val scanner = Scanner(System.`in`)

        val tool = PluginSigningTool()
        println("Enter the plugin JAR file path:")
        tool.pluginFile = scanner.next()

        println("Enter the authority to sign the plugin with:")
        tool.authority = scanner.next()

        println("Enter the private key file path:")
        tool.privateKeyFile = scanner.next()

        tool.run()
    } else {
        CommandLine(PluginSigningTool()).execute(*args)
    }
}