package io.github.deltacv.nodeeye.node.vision

import io.github.deltacv.nodeeye.node.DrawNode
import io.github.deltacv.nodeeye.attribute.AttributeMode
import io.github.deltacv.nodeeye.attribute.vision.MatAttribute

class InputMatNode : DrawNode("Pipeline Input") {

    override fun onEnable() {
        nodeAttributes.add(MatAttribute(AttributeMode.OUTPUT, "Input"))
    }

}