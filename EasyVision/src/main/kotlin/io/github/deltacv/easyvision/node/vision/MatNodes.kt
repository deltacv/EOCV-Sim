package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.type.GenValue
import io.github.deltacv.easyvision.codegen.v

class InputMatNode : DrawNode("Pipeline Input", allowDelete = false) {
    override fun onEnable() {
        + MatAttribute(OUTPUT, "Input")
    }

    override fun genCode(codeGen: CodeGen) {
    }

    override fun getOutputValueOf(codeGen: CodeGen, index: Int) = GenValue.Mat("input".v)
}

class OutputMatNode : DrawNode("Pipeline Output", allowDelete = false) {

    val input = MatAttribute(INPUT, "Output")

    override fun onEnable() {
        + input
    }

    override fun genCode(codeGen: CodeGen) = codeGen {
        processFrame {
            returnMethod(input.value(codeGen).value) // start code gen!
        }
    }

    override fun getOutputValueOf(codeGen: CodeGen, index: Int) = GenValue.None
}