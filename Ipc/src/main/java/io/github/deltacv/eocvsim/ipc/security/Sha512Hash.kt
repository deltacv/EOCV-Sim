package io.github.deltacv.eocvsim.ipc.security

import java.math.BigInteger
import java.security.MessageDigest

private val md = MessageDigest.getInstance("SHA-512");

fun String.sha512(): String {
    return encodeToByteArray().sha512()
}

fun ByteArray.sha512(): String {
    val messageDigest = md.digest(this)

    // Convert byte array into signum representation
    val no = BigInteger(1, messageDigest)

    // Convert message digest into hex value
    var hashtext = no.toString(16)

    // Add preceding 0s to make it 32 bit
    while (hashtext.length < 32) {
        hashtext = "0$hashtext"
    }

    // return the HashText
    return hashtext
}