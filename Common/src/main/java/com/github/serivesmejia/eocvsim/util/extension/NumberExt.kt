/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util.extension

/**
 * Clip a number to be at least 0
 */
fun Int.clipUpperZero(): Int {
    return if(this > 0) {
        this
    } else {
        0
    }
}

/**
 * Clip something like a List size to be an index
 */
val Int.zeroBased get() = (this - 1).clipUpperZero()
