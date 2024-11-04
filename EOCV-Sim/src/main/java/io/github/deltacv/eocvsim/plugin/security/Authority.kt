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

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.moandjiezana.toml.Toml
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import java.io.File
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit

data class Authority(
    val name: String,
    val publicKey: PublicKey
)

data class MutableAuthority(
    var name: String,
    var publicKey: ByteArray
)

fun Authority.toMutable() = MutableAuthority(name, publicKey.encoded)

data class CachedAuthority(
    val authority: Authority,
    val fetchedTime: Long // Timestamp when the key was fetched
)

object AuthorityFetcher {

    const val AUTHORITY_SERVER_URL = "https://raw.githubusercontent.com/deltacv/Authorities/refs/heads/master"

    private val AUTHORITIES_FILE = PluginManager.PLUGIN_CACHING_FOLDER + File.separator + "authorities.toml"

    private val TTL_DURATION_MS = TimeUnit.HOURS.toMillis(8)

    private val logger by loggerForThis()
    private val cache = mutableMapOf<String, CachedAuthority>()

    fun fetchAuthority(name: String): Authority? {
        validateCache()

        // Check if the authority is cached and the file is valid
        cache[name]?.let { cachedAuthority ->
            logger.info("Returning cached authority for $name")
            return cachedAuthority.authority
        }

        // Load authorities from file if it exists
        if (AUTHORITIES_FILE.exists()) {
            try {
                val authoritiesToml = Toml().read(AUTHORITIES_FILE)
                val authorityData = authoritiesToml.getTable(name)
                val authorityPublicKey = authorityData?.getString("public")
                val fetchedTime = authorityData?.getLong("timestamp")

                // Check if the fetched time is valid and within the TTL
                if (authorityPublicKey != null && fetchedTime != null &&
                    System.currentTimeMillis() - fetchedTime < TTL_DURATION_MS) {

                    val authority = Authority(name, parsePublicKey(authorityPublicKey))
                    cache[name] = CachedAuthority(authority, fetchedTime)

                    return authority
                }
            } catch (e: Exception) {
                logger.error("Failed to read authorities file", e)
                AUTHORITIES_FILE.delete()
            }
        }

        // Fetch the authority from the server
        val authorityUrl = "${AUTHORITY_SERVER_URL.trim('/')}/$name"
        return try {
            val authorityPublicKey = URL(authorityUrl).readText()
            val pem = parsePem(authorityPublicKey)

            val authority = Authority(name, parsePublicKey(pem))

            // Cache the fetched authority
            cache[name] = CachedAuthority(authority, System.currentTimeMillis())

            // write to the authorities file
            saveAuthorityToFile(name, pem)

            authority
        } catch (e: Exception) {
            logger.error("Failed to fetch authority from server", e)
            null
        }
    }

    private fun validateCache() {
        val currentTime = System.currentTimeMillis()

        if(!AUTHORITIES_FILE.exists()) {
            SysUtil.saveFileStr(AUTHORITIES_FILE, "timestamp = $currentTime\n")
        }

        val authoritiesToml = Toml().read(AUTHORITIES_FILE)
        val timestamp = authoritiesToml.getLong("timestamp")

        if(currentTime - timestamp > TTL_DURATION_MS) {
            AUTHORITIES_FILE.delete()
            logger.info("Authorities file has expired, clearing cache")
            cache.clear()
        }
    }

    private fun saveAuthorityToFile(name: String, publicKey: String) {
        try {
            val sb = StringBuilder()

            // Load existing authorities if the file exists
            if (AUTHORITIES_FILE.exists()) {
                val existingToml = AUTHORITIES_FILE.readText()

                sb.append(existingToml)
            }

            // Append new authority information
            sb.appendLine("[$name]")
            sb.appendLine("public = \"$publicKey\"")
            sb.appendLine("timestamp = ${System.currentTimeMillis()}")

            // Write the updated content to the file
            AUTHORITIES_FILE.appendText(sb.toString())
        } catch (e: Exception) {
            logger.error("Failed to save authority to file", e)
        }
    }

    fun parsePem(pem: String) =
        pem.trim().lines().drop(1).dropLast(1).joinToString("")

    private fun parsePublicKey(keyPEM: String): PublicKey {
        // Decode the Base64 encoded string
        val encoded = Base64.getDecoder().decode(keyPEM)

        // Generate the public key
        val keySpec = X509EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }
}