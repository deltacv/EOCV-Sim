package io.github.deltacv.easyvision.node.math

import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.math.IntAttribute

class SumIntegerNode : DrawNode("Sum Integer") {

    override fun onEnable() {
        + IntAttribute(INPUT, "A")
        + IntAttribute(INPUT, "B")

        + IntAttribute(OUTPUT, "Result")
    }

}