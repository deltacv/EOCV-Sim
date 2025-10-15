package io.github.deltacv.eocvsim.plugin

import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder
import java.io.File

val PLUGIN_FOLDER = (EOCVSimFolder + File.separator + "plugins").apply { mkdir() }
val EMBEDDED_PLUGIN_FOLDER = (PLUGIN_FOLDER + File.separator + "embedded").apply { mkdir() }
val PLUGIN_CACHING_FOLDER = (PLUGIN_FOLDER + File.separator + "caching").apply { mkdir() }
val FILESYSTEMS_FOLDER = (PLUGIN_FOLDER + File.separator + "filesystems").apply { mkdir() }