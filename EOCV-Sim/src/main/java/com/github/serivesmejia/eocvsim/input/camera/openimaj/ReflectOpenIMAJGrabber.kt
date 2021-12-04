package com.github.serivesmejia.eocvsim.input.camera.openimaj

import org.bridj.Pointer
import org.openimaj.video.capture.VideoCapture

class ReflectOpenIMAJGrabber(private val videoCapture: VideoCapture) {

    private val clazz = VideoCapture::class.java

    private val grabberField by lazy { clazz.getDeclaredField("grabber") }

    private val grabberClazz by lazy { grabber::class.java }

    private val grabber by lazy {
        grabberField.isAccessible = true
        grabberField.get(videoCapture)
    }

    private val nextFrameMethod by lazy {
        grabberClazz.getDeclaredMethod("nextFrame").apply { isAccessible = true }
    }
    private val getImageMethod by lazy {
        grabberClazz.getDeclaredMethod("getImage").apply { isAccessible = true }
    }

    @Suppress("UNCHECKED_CAST")
    val image get() = getImageMethod.invoke(grabber)

    fun nextFrame() = nextFrameMethod.invoke(grabber) as Int

}