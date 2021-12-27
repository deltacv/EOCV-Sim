package io.github.deltacv.eocvsim.ipc.security

class PassToken(private var pass: String) {

    @Transient private var _hash: String? = null

    val hash: String get() {
        // manually doing lazy initialization because gson is weird
        if(_hash == null) {
            _hash = pass.sha512()
        }

        return _hash!!
    }

    fun matches(other: String) = hash == other.sha512()

    override fun equals(other: Any?) = other is PassToken && other.hash == hash

    override fun toString() = "PassToken(hash=$hash)"

}