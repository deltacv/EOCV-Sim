package io.github.deltacv.nodeeye.node.math

import io.github.deltacv.nodeeye.node.DrawNode
import io.github.deltacv.nodeeye.attribute.math.IntAttribute

class SumIntegerNode : DrawNode("Sum Integer") {

    override fun onEnable() {
        + IntAttribute(INPUT, "A")
        + IntAttribute(INPUT, "B")

        + IntAttribute(OUTPUT, "Result")
    }

}