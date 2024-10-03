package io.github.deltacv.eocvsim.input.control

import com.github.serivesmejia.eocvsim.input.source.CameraSource
import io.github.deltacv.steve.WebcamProperty
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference
import java.util.concurrent.TimeUnit

class CameraSourceExposureControl(
    cameraSource: CameraSource
) : ExposureControl {

    val control = cameraSource.webcamPropertyControl

    override fun getMode() = if(control.getPropertyAuto(WebcamProperty.EXPOSURE)) {
        ExposureControl.Mode.Auto
    } else ExposureControl.Mode.Manual

    override fun setMode(mode: ExposureControl.Mode): Boolean {
        control.setPropertyAuto(WebcamProperty.EXPOSURE, mode == ExposureControl.Mode.Auto)
        return control.getPropertyAuto(WebcamProperty.EXPOSURE)
    }

    override fun isModeSupported(mode: ExposureControl.Mode) =
        mode == ExposureControl.Mode.Auto || mode == ExposureControl.Mode.Manual

    override fun getMinExposure(resultUnit: TimeUnit): Long {
        val bounds = control.getPropertyBounds(WebcamProperty.EXPOSURE)
        return TimeUnit.SECONDS.convert(bounds.min.toLong(), resultUnit)
    }

    override fun getMaxExposure(resultUnit: TimeUnit): Long {
        val bounds = control.getPropertyBounds(WebcamProperty.EXPOSURE)
        return TimeUnit.SECONDS.convert(bounds.max.toLong(), resultUnit)
    }

    override fun getExposure(resultUnit: TimeUnit): Long {
        return TimeUnit.SECONDS.convert(control.getProperty(WebcamProperty.EXPOSURE).toLong(), resultUnit)
    }

    override fun getCachedExposure(
        resultUnit: TimeUnit,
        refreshed: MutableReference<Boolean>,
        permittedStaleness: Long,
        permittedStalenessUnit: TimeUnit
    ) = getExposure(resultUnit)

    override fun setExposure(duration: Long, durationUnit: TimeUnit): Boolean {
        val seconds = TimeUnit.SECONDS.convert(duration, durationUnit).toInt()

        control.setProperty(WebcamProperty.EXPOSURE, seconds)
        return control.getProperty(WebcamProperty.EXPOSURE) == seconds
    }

    override fun isExposureSupported() = control.isPropertySupported(WebcamProperty.EXPOSURE)

    override fun getAePriority(): Boolean {
        throw UnsupportedOperationException("AE priority is not supported by EOCV-Sim")
    }

    override fun setAePriority(priority: Boolean): Boolean {
        throw UnsupportedOperationException("AE priority is not supported by EOCV-Sim")
    }

}