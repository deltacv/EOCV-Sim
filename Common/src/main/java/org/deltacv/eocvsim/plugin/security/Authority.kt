/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.security

import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.LockFile
import org.deltacv.common.util.loggerForThis
import org.deltacv.common.util.serialization.Toml
import org.deltacv.eocvsim.plugin.PLUGIN_CACHING_FOLDER
import java.io.File
import java.net.URI
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

data class Authority(
    val name: String,
    val publicKey: PublicKey
)

class MutableAuthority(
    var name: String = "",
    var publicKey: ByteArray = byteArrayOf()
)

fun Authority.toMutable() = MutableAuthority(name, publicKey.encoded)

data class CachedAuthority(
    val authority: Authority,
    val fetchedTime: Long // Timestamp when the key was fetched
)

object AuthorityFetcher {

    const val AUTHORITY_SERVER_URL = "https://deltacv.org/authorities/"

    private val AUTHORITIES_FILE = PLUGIN_CACHING_FOLDER + File.separator + "authorities.toml"
    private val AUTHORITIES_LOCK_FILE = LockFile(PLUGIN_CACHING_FOLDER + File.separator + "authorities.lock")

    private val AUTHORITIES_LOCK_FILE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(3)

    private val TTL_DURATION_MS = TimeUnit.HOURS.toMillis(8)

    private val logger by loggerForThis()
    private val cache = mutableMapOf<String, CachedAuthority>()

    fun fetchAuthority(name: String): Authority? {
        try {
            validateCache()

            // Check if the authority is cached and the file is valid
            cache[name]?.let { cachedAuthority ->
                logger.info("Returning cached authority for $name")
                return cachedAuthority.authority
            }
        } catch (e: Exception) {
            logger.error("Failed to validate cache", e)
        }

        // Load authorities from file if it exists
        if (AUTHORITIES_FILE.exists() && tryLockAuthoritiesFile()) {
            try {
                val authoritiesToml = readAuthoritiesFile() ?: return null
                val timestamp = authoritiesToml.getLong("timestamp")

                if (System.currentTimeMillis() - (timestamp ?: 0L) > TTL_DURATION_MS) {
                    AUTHORITIES_FILE.delete()
                } else {
                    val authorityData = authoritiesToml.getTable(name)
                    val authorityPublicKey = authorityData?.getString("public")
                    val fetchedTime = authorityData?.getLong("timestamp")

                    // Check if the fetched time is valid and within the TTL
                    if (authorityPublicKey != null && fetchedTime != null &&
                        System.currentTimeMillis() - fetchedTime < TTL_DURATION_MS
                    ) {
                        val authority = Authority(name, parsePublicKey(authorityPublicKey))
                        cache[name] = CachedAuthority(authority, fetchedTime)

                        return authority
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to read authorities file", e)
                AUTHORITIES_FILE.delete()
            } finally {
                AUTHORITIES_LOCK_FILE.unlock()
            }
        }

        // Fetch the authority from the server
        val authorityUrl = "${AUTHORITY_SERVER_URL.trim('/')}/${name}"

        return try {
            logger.info("Fetching authority from URL: $authorityUrl")

            val authorityPublicKey = URI.create(authorityUrl).toURL().readText()
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
        if(!tryLockAuthoritiesFile()) {
            return
        }

        try {
            val currentTime = System.currentTimeMillis()

            if(!AUTHORITIES_FILE.exists()) {
                AUTHORITIES_FILE.writeText("timestamp = $currentTime\n")
                return
            }

            val authoritiesToml = readAuthoritiesFile()
            val timestamp = authoritiesToml?.getLong("timestamp")

            if(timestamp != null && currentTime - timestamp > TTL_DURATION_MS) {
                AUTHORITIES_FILE.delete()
                logger.info("Authorities file has expired, clearing cache")
                cache.clear()
            }
        } catch (e: Exception) {
            logger.error("Authorities cache is corrupted, clearing it", e)
            AUTHORITIES_FILE.delete()
            cache.clear()
        } finally {
            AUTHORITIES_LOCK_FILE.unlock()
        }
    }

    private fun readAuthoritiesFile(): Toml? {
        if (!AUTHORITIES_FILE.exists()) {
            return null
        }

        return try {
            Toml().read(AUTHORITIES_FILE)
        } catch (e: Exception) {
            logger.warn("Authorities cache is invalid, deleting it and recreating later", e)
            AUTHORITIES_FILE.delete()
            null
        }
    }

    private fun saveAuthorityToFile(name: String, publicKey: String) {
        if(!tryLockAuthoritiesFile()) {
            return
        }

        try {
            val currentTime = System.currentTimeMillis()
            val existingAuthorities = linkedMapOf<String, Any?>()

            if (AUTHORITIES_FILE.exists()) {
                val existingToml = readAuthoritiesFile() ?: Toml()
                val existingMap = existingToml.toMap().filterKeys { it != "timestamp" }
                existingMap.forEach { (key, value) ->
                    if (value is Map<*, *>) {
                        existingAuthorities[key] = linkedMapOf<String, Any?>().apply {
                            value.forEach { (nestedKey, nestedValue) -> put(nestedKey.toString(), nestedValue) }
                        }
                    } else {
                        existingAuthorities[key] = value
                    }
                }

                val topLevelTimestamp = existingToml.getLong("timestamp")
                if (topLevelTimestamp != null) {
                    existingAuthorities["timestamp"] = topLevelTimestamp
                }
            }

            existingAuthorities["timestamp"] = existingAuthorities["timestamp"] as? Number ?: currentTime
            val authorityTable = linkedMapOf<String, Any?>()
            authorityTable["public"] = publicKey
            authorityTable["timestamp"] = currentTime
            existingAuthorities[name] = authorityTable

            AUTHORITIES_FILE.writeText(buildAuthoritiesToml(existingAuthorities))
        } catch (e: Exception) {
            logger.error("Failed to save authority to file", e)
        } finally {
            AUTHORITIES_LOCK_FILE.unlock()
        }
    }

    private fun buildAuthoritiesToml(authorities: Map<String, Any?>): String {
        val sb = StringBuilder()
        val topLevelTimestamp = authorities["timestamp"]
        if (topLevelTimestamp != null) {
            sb.appendLine("timestamp = $topLevelTimestamp")
        }

        for ((name, value) in authorities.filterKeys { it != "timestamp" }) {
            if (value !is Map<*, *>) continue

            sb.appendLine()
            sb.appendLine("[$name]")
            for ((key, entryValue) in value) {
                val fieldName = key.toString()
                when (entryValue) {
                    is Number -> sb.appendLine("$fieldName = $entryValue")
                    is Boolean -> sb.appendLine("$fieldName = ${entryValue.toString().lowercase(Locale.ROOT)}")
                    is String -> sb.appendLine("$fieldName = \"$entryValue\"")
                    null -> sb.appendLine("$fieldName = \"\"")
                    else -> sb.appendLine("$fieldName = \"$entryValue\"")
                }
            }
        }

        return sb.toString().trimEnd() + "\n"
    }

    private fun tryLockAuthoritiesFile(): Boolean {
        val time = System.currentTimeMillis()

        logger.info("Trying to lock authorities file")

        while(!AUTHORITIES_LOCK_FILE.tryLock(false) && (System.currentTimeMillis() - time) < AUTHORITIES_LOCK_FILE_TIMEOUT_MS) {
            Thread.sleep(100)
        }

        if(!AUTHORITIES_LOCK_FILE.isLocked) {
            logger.warn("Failed to lock authorities file")
        }

        return AUTHORITIES_LOCK_FILE.isLocked
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
