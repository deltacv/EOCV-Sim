/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.input.source

import com.github.serivesmejia.eocvsim.input.InputSource
import org.opencv.core.Mat
import javax.swing.filechooser.FileFilter

class NullSource : InputSource() {

    override fun init(): Boolean = true

    override fun reset() {}

    override fun close() {}

    @Transient private val emptyMat = Mat()

    override fun update(): Mat = emptyMat

    override fun onPause() {}

    override fun onResume() {}

    override fun internalCloneSource(): InputSource = NullSource()

    override val fileFilters: FileFilter? = null
    override val captureTimeNanos: Long get() = System.nanoTime()


}

