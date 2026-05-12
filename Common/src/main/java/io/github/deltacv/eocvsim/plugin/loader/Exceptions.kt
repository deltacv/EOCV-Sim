/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.eocvsim.plugin.loader

import kotlin.RuntimeException

class InvalidPluginException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class UnsupportedPluginException(message: String) : RuntimeException(message)
