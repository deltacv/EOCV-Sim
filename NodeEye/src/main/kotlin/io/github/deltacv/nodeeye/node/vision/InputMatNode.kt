package io.github.deltacv.nodeeye.node.vision

import io.github.deltacv.nodeeye.node.DrawNode
import io.github.deltacv.nodeeye.attribute.vision.MatAttribute

class InputMatNode : DrawNode("Pipeline Input", allowDelete = false) {

    override fun onEnable() {
        + MatAttribute(OUTPUT, "Input")
    }

}