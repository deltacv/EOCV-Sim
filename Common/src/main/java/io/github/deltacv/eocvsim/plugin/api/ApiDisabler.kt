package io.github.deltacv.eocvsim.plugin.api

/**
 * Utility object to disable APIs
 *
 * Exposes the internalDisableApi() method of
 * each Api in an indirect way to avoid misuse.
 */
object ApiDisabler {
    fun disableApis(vararg apis: Api) {
        apis.forEach { it.internalDisableApi() }
    }
}