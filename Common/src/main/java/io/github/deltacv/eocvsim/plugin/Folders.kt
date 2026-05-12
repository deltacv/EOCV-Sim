/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.eocvsim.plugin

import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import java.io.File

val PLUGIN_FOLDER get() = (EOCVSimFolder + File.separator + "plugins").apply { mkdir() }
val PLUGIN_CACHING_FOLDER get() = (PLUGIN_FOLDER + File.separator + "caching").apply { mkdir() }
val EMBEDDED_PLUGIN_FOLDER get() = (PLUGIN_CACHING_FOLDER + File.separator + "embedded").apply { mkdir() }
val FILESYSTEMS_FOLDER get() = (PLUGIN_FOLDER + File.separator + "filesystems").apply { mkdir() }
