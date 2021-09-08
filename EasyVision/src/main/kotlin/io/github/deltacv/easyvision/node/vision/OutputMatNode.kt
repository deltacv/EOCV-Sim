package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.vision.MatAttribute

class OutputMatNode : DrawNode("Pipeline Output", allowDelete = false) {

    override fun onEnable() {
        + MatAttribute(INPUT, "Output")
    }

}