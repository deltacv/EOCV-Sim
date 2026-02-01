package io.github.deltacv.common.util

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun loggerFor(clazz: KClass<*>) = lazy {
    LoggerFactory.getLogger(clazz.java)!!
}

fun Any.loggerForThis() = lazy {
    LoggerFactory.getLogger(this::class.java)!!
}

fun loggerOf(name: String) = lazy {
    LoggerFactory.getLogger(name)!!
}