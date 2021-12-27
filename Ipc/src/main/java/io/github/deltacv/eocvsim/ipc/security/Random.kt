package io.github.deltacv.eocvsim.ipc.security

import java.nio.CharBuffer
import java.security.SecureRandom

private val secureRandom by lazy { SecureRandom() }
private val allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUWXYZabcdefghijklmnopqrstuwxyz0123456789-_+=$&#@#%!?"

fun secureRandomString(length: Int? = null): String {
    val characters = length ?: secureRandom.nextInt(240) + 16
    val builder = StringBuilder()

    repeat(characters) {
        builder.append(
            allowedCharacters[
                    secureRandom.nextInt(allowedCharacters.length - 1)
            ]
        )
    }

    return builder.toString()
}