/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.tuner

interface TunableFieldAcceptor {
    fun accept(clazz: Class<*>): Boolean
}
