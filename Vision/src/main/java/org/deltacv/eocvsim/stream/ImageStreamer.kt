/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.eocvsim.stream

import org.opencv.core.Mat

interface ImageStreamer {
    fun sendFrame(id: Int, image: Mat, cvtCode: Int? = null)
}
