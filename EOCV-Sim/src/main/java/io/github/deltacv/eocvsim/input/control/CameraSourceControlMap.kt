package io.github.deltacv.eocvsim.input.control

import com.github.serivesmejia.eocvsim.input.source.CameraSource
import io.github.deltacv.vision.external.source.CameraControlMap
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.CameraControl
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl

class CameraSourceControlMap(
    val source: CameraSource
) : CameraControlMap {

    @Suppress("UNCHECKED_CAST")
    override fun <T : CameraControl> get(classType: Class<T>) =
        when(classType) {
            ExposureControl::class.java -> CameraSourceExposureControl(source) as T
            else -> null
        }

}