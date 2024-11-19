package com.github.serivesmejia.eocvsim.util.extension

import java.security.MessageDigest

/**
 * Remove a string from the end of this string
 * @param rem the string to remove
 * @return the string without the removed string at the end
 */
fun String.removeFromEnd(rem: String): String {
    if(endsWith(rem)) {
        return substring(0, length - rem.length).trim()
    }
    return trim()
}

val Any.hashString get() = Integer.toHexString(hashCode())!!

val String.hashString: String get() {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val hash = messageDigest.digest(toByteArray())
    return byteArrayToHex(hash)
}