package com.github.serivesmejia.eocvsim.input.camera

import org.opencv.core.Mat
import org.opencv.core.Size

interface Webcam {

    val isOpen: Boolean
    var resolution: Size

    val index: Int
    val name: String

    fun open()

    fun read(mat: Mat)

    fun close()

}