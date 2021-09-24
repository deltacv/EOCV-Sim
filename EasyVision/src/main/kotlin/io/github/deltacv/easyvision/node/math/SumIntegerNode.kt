package io.github.deltacv.easyvision.node.math

import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute

class SumIntegerNode : DrawNode("Sum Integer") {

    override fun onEnable() {
        + ListAttribute(INPUT, IntAttribute, "Numbers")

        + IntAttribute(OUTPUT,"Result")
    }

}