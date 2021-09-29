package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.parse.new
import io.github.deltacv.easyvision.codegen.parse.v
import io.github.deltacv.easyvision.node.DrawNode

class MaskNode : DrawNode<MaskNode.Session>("Binary Mask"){

    val inputMat = MatAttribute(INPUT, "Input")
    val maskMat  = MatAttribute(INPUT, "Binary Mask")

    val outputMat = MatAttribute(OUTPUT, "Output")

    override fun onEnable() {
        + inputMat
        + maskMat

        + outputMat
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val input = inputMat.value(current)
        input.requireNonBinary(inputMat)

        val mask = maskMat.value(current)
        mask.requireBinary(maskMat)

        val output = tryName("${input.value.value!!}Mask")

        import("org.opencv.core.Core")

        private(output, new("Mat"))

        current.scope {
            "$output.release"()
            "Core.bitwise_and"(input.value, input.value, output.v, mask.value)
        }

        session.outputMat = GenValue.Mat(output.v, input.color)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputMat) {
            return genSession!!.outputMat
        }

        raise("Attribute $attrib is not an output of this node or not handled by this")
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}