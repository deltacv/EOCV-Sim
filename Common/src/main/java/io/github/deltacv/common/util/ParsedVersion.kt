package io.github.deltacv.common.util

class ParsedVersion(val version: String) {

    private val splitVersion = version.split(".")

    val major = splitVersion.getOrNull(0)?.toIntOrNull() ?: throw IllegalArgumentException("Version $version does not have a valid major value")
    val minor = splitVersion.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException("Version $version does not have a valid minor value")
    val patch = splitVersion.getOrNull(2)?.toIntOrNull() ?: 0

    operator fun compareTo(o: ParsedVersion) = when {
        major != o.major -> major.compareTo(o.major)
        minor != o.minor -> minor.compareTo(o.minor)
        else -> patch.compareTo(o.patch)
    }


    override fun toString(): String {
        return "$major.$minor.$patch"
    }

}