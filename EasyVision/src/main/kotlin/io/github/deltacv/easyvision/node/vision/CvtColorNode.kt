package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.node.DrawNode

enum class Colors {
    RGB, BGR, HSV, YCrCb, LAB, GRAY
}

class CvtColorNode : DrawNode("Convert Color") {

    override fun onEnable() {
        + MatAttribute(INPUT, "Input")
        + EnumAttribute(INPUT, Colors.values(), "Convert To")

        + MatAttribute(OUTPUT, "Output")
    }

}