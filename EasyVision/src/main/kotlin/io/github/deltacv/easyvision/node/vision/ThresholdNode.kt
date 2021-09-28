package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.vision.RangeAttribute
import io.github.deltacv.easyvision.attribute.vision.ScalarAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.node.DrawNode

class ThresholdNode : DrawNode<NoSession>("Color Threshold") {

    val threshColor = EnumAttribute(INPUT, Colors.values(), "Space")

    override fun onEnable() {
        + ScalarAttribute(INPUT, Colors.values()[0], "Test")
    }

    override fun genCode(current: CodeGen.Current): NoSession {
        TODO("Not yet implemented")
    }

}