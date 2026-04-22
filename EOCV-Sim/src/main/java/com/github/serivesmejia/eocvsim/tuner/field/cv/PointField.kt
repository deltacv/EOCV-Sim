package com.github.serivesmejia.eocvsim.tuner.field.cv

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.tuner.TunableField
import com.github.serivesmejia.eocvsim.tuner.TunableNumber
import io.github.deltacv.eocvsim.virtualreflect.VirtualField
import org.opencv.core.Point

class PointField(
    instance: Any,
    reflectionField: VirtualField
) : TunableField<Point>(instance, reflectionField, AllowMode.ONLY_NUMBERS_DECIMAL) {


    private var point: Point = if (initialFieldValue != null) {
        val p = initialFieldValue as Point
        Point(p.x, p.y)
    } else {
        Point(0.0, 0.0)
    }

    private val xValue by lazy { TunableNumber(point.x, { point.x }, { updatePoint(0, it) }) }
    private val yValue by lazy { TunableNumber(point.y, { point.y }, { updatePoint(1, it) }) }

    override val tunableValues by lazy { listOf(xValue, yValue) }

    private fun updatePoint(index: Int, newValue: Double) {
        if (index == 0) point.x = newValue else point.y = newValue
        setPipelineFieldValue(point)
    }

    override fun init() {
        reflectionField.set(point)
    }

    override fun refreshPipelineObject() {
        val current = reflectionField.get() as Point
        point.x = current.x
        point.y = current.y
    }

    override val value: Point
        get() = point
}
