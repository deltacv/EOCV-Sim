package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.type.GenValue
import io.github.deltacv.easyvision.node.DrawNode

enum class Colors {
    RGB, BGR, HSV, YCrCb, LAB, GRAY
}

class CvtColorNode : DrawNode<CvtColorNode.Session>("Convert Color") {

    val input  = MatAttribute(INPUT, "Input")
    val output = MatAttribute(OUTPUT, "Output")

    val convertTo = EnumAttribute(INPUT, Colors.values(), "Convert To")

    override fun onEnable() {
        + input
        + convertTo

        + output
    }

    override fun genCode(codeGen: CodeGen) = codeGen {
        val session = Session()

        val inputMat = input.value(codeGen)

        processFrame {

        }

        session
    }

    override fun getOutputValueOf(codeGen: CodeGen, attrib: Attribute): GenValue {
        genCodeIfNecessary(codeGen)

        if(attrib == output) {
            return genSession!!.outputMatValue
        }

        raise("Attribute $attrib is not an output of this node or not handled by this")
    }

    class Session : CodeGenSession() {
        lateinit var outputMatValue: GenValue.Mat
    }

}