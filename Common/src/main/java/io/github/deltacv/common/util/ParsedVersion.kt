package io.github.deltacv.common.util

/**
 * ParsedVersion class to parse and compare versions
 * @param version the version string to parse
 */
class ParsedVersion(val version: String) {

    private val splitVersion = version.split(".")

    /**
     * Major version number
     */
    val major = splitVersion.getOrNull(0)?.toIntOrNull() ?: throw IllegalArgumentException("Version $version does not have a valid major value")

    /**
     * Minor version number
     */
    val minor = splitVersion.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException("Version $version does not have a valid minor value")

    /**
     * Patch version number
     * Will default to 0 if not present
     */
    val patch = splitVersion.getOrNull(2)?.toIntOrNull() ?: 0

    /**
     * Compare this version to another ParsedVersion
     * @param o the other ParsedVersion to compare to
     * @return 0 if equal, -1 if this version is lower, 1 if this version is higher
     * @see Comparable
     */
    operator fun compareTo(o: ParsedVersion) = when {
        major != o.major -> major.compareTo(o.major)
        minor != o.minor -> minor.compareTo(o.minor)
        else -> patch.compareTo(o.patch)
    }

    /**
     * Convert this ParsedVersion to a string
     */
    override fun toString(): String {
        return "$major.$minor.$patch"
    }

}