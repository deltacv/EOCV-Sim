/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.plugin.api

/**
 * Utility object to disable APIs
 *
 * Exposes the internalDisableApi() method of
 * each Api in an indirect way to avoid misuse.
 */
object ApiDisabler {
    /**
     * Disables the given APIs.
     * @param apis The APIs to disable.
     */
    fun disableApis(vararg apis: Api) {
        apis.forEach { it.internalDisableApi() }
    }
}
