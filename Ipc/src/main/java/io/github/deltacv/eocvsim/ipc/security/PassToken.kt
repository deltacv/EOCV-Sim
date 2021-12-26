package io.github.deltacv.eocvsim.ipc.security

class PassToken(private val pass: DestroyableString) {

    val hash by lazy { pass.sha512() }

    fun matches(other: String) = hash == other.sha512()

}