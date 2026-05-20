/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.serialization

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * Shared Jackson mappers for the simulator.
 *
 * `persistenceMapper` is configured for the config/input-source files and only
 * serializes fields, avoiding getter-based schema drift from computed properties.
 * `ipcMapper` keeps the same Kotlin support but is meant for internal message payloads.
 */
object JacksonJsonSupport {

    @JvmField
    val persistenceMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(VideoModeJackson.module())
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(SerializationFeature.INDENT_OUTPUT)

    @JvmField
    val ipcMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(SerializationFeature.INDENT_OUTPUT)
}
