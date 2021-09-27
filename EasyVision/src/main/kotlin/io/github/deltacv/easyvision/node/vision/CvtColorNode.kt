package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.type.GenValue
import io.github.deltacv.easyvision.node.DrawNode

enum class Colors {
    RGB, BGR, HSV, YCrCb, LAB, GRAY
}

class CvtColorNode : DrawNode("Convert Color") {

    val input  = MatAttribute(INPUT, "Input")
    val output = MatAttribute(OUTPUT, "Output")

    val convertTo = EnumAttribute(INPUT, Colors.values(), "Convert To")

    override fun onEnable() {
        + input
        + convertTo

        + output
    }

    override fun genCode(codeGen: CodeGen) {
        TODO("Not yet implemented")
    }

    override fun getOutputValueOf(codeGen: CodeGen, index: Int): GenValue {
        TODO("Not yet implemented")
    }

}