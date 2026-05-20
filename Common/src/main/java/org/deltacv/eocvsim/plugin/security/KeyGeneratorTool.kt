/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.security

import java.io.File
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.util.Base64

object KeyGeneratorTool {
    @JvmStatic
    fun main(args: Array<String>) {
        // Generate RSA key pair
        val keyPair: KeyPair = generateKeyPair()

        // Save keys to files
        saveKeyToFile("private_key.pem", keyPair.private)
        saveKeyToFile("public_key.pem", keyPair.public)

        println("Keys generated and saved to files 'private_key.pem' and 'public_key.pem'")
    }
}

fun generateKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048) // Use 2048 bits for key size
    return keyPairGenerator.generateKeyPair()
}

fun saveKeyToFile(filename: String, key: java.security.Key) {
    val encodedKey = Base64.getEncoder().encodeToString(key.encoded)
    val keyType = if (key is PrivateKey) "PRIVATE" else "PUBLIC"

    val pemFormat = "-----BEGIN ${keyType} KEY-----\n$encodedKey\n-----END ${keyType} KEY-----"

    FileWriter(File(filename)).use { writer ->
        writer.write(pemFormat)
    }
}
