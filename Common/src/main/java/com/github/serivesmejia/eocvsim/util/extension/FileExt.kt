/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.extension

import java.io.File
import java.security.MessageDigest

val appData: File = File(System.getProperty("user.home") + File.separator)

/**
 * Operator function to concatenate a string to a file path
 */
operator fun File.plus(str: String): File {
    return File(this.absolutePath, str)
}

fun File.fileHash(algorithm: String = "SHA-256"): String {
    val messageDigest = MessageDigest.getInstance(algorithm)
    messageDigest.update(readBytes())
    return byteArrayToHex(messageDigest.digest())
}

private val hex = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

fun byteArrayToHex(bytes: ByteArray): String {
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        sb.append(hex[(b.toInt() and 0xF0) shr 4])
        sb.append(hex[b.toInt() and 0x0F])
    }
    return sb.toString()
}
