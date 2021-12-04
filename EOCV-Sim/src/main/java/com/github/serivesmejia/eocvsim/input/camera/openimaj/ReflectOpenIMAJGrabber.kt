package com.github.serivesmejia.eocvsim.input.camera.openimaj

import org.bridj.Pointer
import org.openimaj.video.capture.VideoCapture

class ReflectOpenIMAJGrabber(private val videoCapture: VideoCapture) {

    private val clazz = VideoCapture::class.java

    private val grabberField by lazy { clazz.getDeclaredField("grabber") }

    private val grabberClazz by lazy { grabber::class.java }

    private val grabber by lazy {
        grabberField.isAccessible = true
        val grabber = grabberField.get(videoCapture)
        grabberField.isAccessible = false

        grabber
    }

    private val getImageMethod by lazy { grabberClazz.getDeclaredMethod("getImage") }

    @Suppress("UNCHECKED_CAST")
    val image get() = getImageMethod.invoke(grabber) as Pointer<Byte>

}