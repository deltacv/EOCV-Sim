package io.github.deltacv.nodeeye.node.math

import io.github.deltacv.nodeeye.node.DrawNode
import io.github.deltacv.nodeeye.attribute.AttributeMode
import io.github.deltacv.nodeeye.attribute.math.IntAttribute

class SumIntegerNode : DrawNode("Sum Integer") {

    override fun onEnable() {
        nodeAttributes.add(IntAttribute(AttributeMode.INPUT, "A"))
        nodeAttributes.add(IntAttribute(AttributeMode.INPUT, "B"))
        nodeAttributes.add(IntAttribute(AttributeMode.OUTPUT, "Result"))
    }

}