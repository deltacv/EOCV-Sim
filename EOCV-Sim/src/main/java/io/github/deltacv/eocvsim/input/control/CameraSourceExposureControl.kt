package io.github.deltacv.eocvsim.input.control

import com.github.serivesmejia.eocvsim.input.source.CameraSource
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl
import org.firstinspires.ftc.robotcore.internal.collections.MutableReference
import org.wpilib.vision.camera.VideoProperty
import java.util.concurrent.TimeUnit

class CameraSourceExposureControl(
    private val cameraSource: CameraSource
) : ExposureControl {

    private val camera
        get() = cameraSource.camera ?: error("Camera not initialized")

    /**
     * CSCore exposure property.
     *
     * Usually:
     * - exposure_absolute (Windows / DirectShow)
     * - exposure (Linux / V4L)
     *
     * Try absolute first.
     */
    private val exposureProperty: VideoProperty
        get() {
            val abs = camera.getProperty("exposure_absolute")

            return if (abs.kind != VideoProperty.Kind.kNone) {
                abs
            } else {
                camera.getProperty("exposure")
            }
        }

    override fun getMode(): ExposureControl.Mode {
        val prop = camera.getProperty("exposure_auto")

        if (prop.kind == VideoProperty.Kind.kNone) {
            return ExposureControl.Mode.Unknown
        }

        return if (prop.get() != 0) {
            ExposureControl.Mode.Auto
        } else {
            ExposureControl.Mode.Manual
        }
    }

    override fun setMode(mode: ExposureControl.Mode): Boolean {
        when (mode) {
            ExposureControl.Mode.Auto -> {
                camera.setExposureAuto()
            }

            ExposureControl.Mode.Manual -> {
                // Keep current exposure value when switching to manual
                val current = exposureProperty.get()
                camera.setExposureManual(current)
            }

            else -> return false
        }

        return getMode() == mode
    }

    override fun isModeSupported(mode: ExposureControl.Mode): Boolean {
        return mode == ExposureControl.Mode.Auto ||
                mode == ExposureControl.Mode.Manual
    }

    override fun getMinExposure(resultUnit: TimeUnit): Long {
        return convertExposure(
            exposureProperty.min.toLong(),
            resultUnit
        )
    }

    override fun getMaxExposure(resultUnit: TimeUnit): Long {
        return convertExposure(
            exposureProperty.max.toLong(),
            resultUnit
        )
    }

    override fun getExposure(resultUnit: TimeUnit): Long {
        return convertExposure(
            exposureProperty.get().toLong(),
            resultUnit
        )
    }

    @Deprecated("")
    override fun getCachedExposure(
        resultUnit: TimeUnit,
        refreshed: MutableReference<Boolean>,
        permittedStaleness: Long,
        permittedStalenessUnit: TimeUnit
    ): Long {
        refreshed.value = true
        return getExposure(resultUnit)
    }

    override fun setExposure(
        duration: Long,
        durationUnit: TimeUnit
    ): Boolean {

        val value = convertFromTimeUnit(duration, durationUnit)

        camera.setExposureManual(value.toInt())

        return exposureProperty.get() == value.toInt()
    }

    override fun isExposureSupported(): Boolean {
        return exposureProperty.kind != VideoProperty.Kind.kNone
    }

    override fun getAePriority(): Boolean {
        throw UnsupportedOperationException(
            "AE priority is not supported by CSCore"
        )
    }

    override fun setAePriority(priority: Boolean): Boolean {
        throw UnsupportedOperationException(
            "AE priority is not supported by CSCore"
        )
    }

    /**
     * CSCore exposure values are typically milliseconds-ish integers,
     * not true nanosecond durations.
     *
     * FTC ExposureControl expects time units.
     *
     * You may need platform-specific scaling here.
     */
    private fun convertExposure(
        value: Long,
        resultUnit: TimeUnit
    ): Long {
        return resultUnit.convert(value, TimeUnit.MILLISECONDS)
    }

    private fun convertFromTimeUnit(
        duration: Long,
        unit: TimeUnit
    ): Long {
        return TimeUnit.MILLISECONDS.convert(duration, unit)
    }
}