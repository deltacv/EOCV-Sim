package io.github.deltacv.nodeeye.node.vision

import io.github.deltacv.nodeeye.node.DrawNode
import io.github.deltacv.nodeeye.attribute.AttributeMode
import io.github.deltacv.nodeeye.attribute.vision.MatAttribute

class OutputMatNode : DrawNode("Pipeline Output", allowDelete = false) {

    override fun onEnable() {
        + MatAttribute(INPUT, "Output")
    }

}