package com.github.serivesmejia.eocvsim.util

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun Any.loggerFor(clazz: KClass<*>) = lazy { LoggerFactory.getLogger(clazz.java) }
fun Any.loggerForThis() = lazy { LoggerFactory.getLogger(this::class.java) }

fun Any.loggerOf(name: String) = lazy { LoggerFactory.getLogger(name) }