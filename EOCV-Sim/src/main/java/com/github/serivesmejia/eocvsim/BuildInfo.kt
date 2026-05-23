/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim

import com.github.serivesmejia.eocvsim.util.serialization.JacksonJsonSupport

/**
 * Runtime build metadata loaded from the generated JSON resource.
 *
 * The Gradle build writes `EOCVSimBuildInfo.json` into the main resources
 * output, and this object reads it lazily at runtime so no source files need
 * to be regenerated on no-op builds.
 */
object BuildInfo {

    private const val RESOURCE_PATH = "/EOCVSimBuildInfo.json"

    private data class BuildInfoDto(
        val versionString: String,
        val standardVersionString: String,
        val buildDate: String,
        val isDev: Boolean,
        val packagePlatform: String,
        val opencvVersion: String,
        val paperVisionVersion: String
    )

    private val metadata: BuildInfoDto by lazy(LazyThreadSafetyMode.PUBLICATION) {
        loadMetadata()
    }

    private fun loadMetadata(): BuildInfoDto {
        val stream = BuildInfo::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: throw IllegalStateException("Missing build metadata resource '$RESOURCE_PATH'. Was the project built with the generated JSON resource enabled?")

        stream.use {
            return try {
                JacksonJsonSupport.persistenceMapper.readValue(it, BuildInfoDto::class.java)
            } catch (ex: Exception) {
                throw IllegalStateException("Failed to read build metadata from '$RESOURCE_PATH'", ex)
            }
        }
    }

    val VERSION_STRING: String
        get() = metadata.versionString

    val STANDARD_VERSION_STRING: String
        get() = metadata.standardVersionString

    val BUILD_DATE: String
        get() = metadata.buildDate

    val IS_DEV: Boolean
        get() = metadata.isDev

    val PACKAGE_PLATFORM: String
        get() = metadata.packagePlatform

    val OPENCV_VERSION: String
        get() = metadata.opencvVersion

    val PAPER_VISION_VERSION: String
        get() = metadata.paperVisionVersion
}

