/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.input

import com.github.serivesmejia.eocvsim.input.source.*
import java.io.File

enum class SourceType(val stubInstance: InputSource?, val coolName: String) {

    IMAGE(ImageSource(), "Image"),
    CAMERA(CameraSource(), "Camera"),
    VIDEO(VideoSource(), "Video"),
    HTTP(HttpSource(), "HTTP"),
    UNKNOWN(null, "Unknown");

    val klazz: Class<out InputSource>? = stubInstance?.javaClass

    companion object {
        @JvmStatic
        fun fromClass(clazz: Class<out InputSource>): SourceType {
            for (sourceType in entries) {
                if (sourceType.klazz == clazz) {
                    return sourceType
                }
            }
            return UNKNOWN
        }

        @JvmStatic
        fun fromCoolName(coolName: String): SourceType {
            for (sourceType in entries) {
                if (sourceType.coolName.equals(coolName, ignoreCase = true)) {
                    return sourceType
                }
            }
            return UNKNOWN
        }

        @JvmStatic
        fun isFileUsableForSource(file: File): SourceType {
            for (type in entries) {
                val filters = type.stubInstance?.fileFilters
                if (filters != null && filters.accept(file)) {
                    return type
                }
            }
            return UNKNOWN
        }
    }

}

