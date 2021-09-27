package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.type.GenValue
import io.github.deltacv.easyvision.codegen.v

class InputMatNode : DrawNode("Pipeline Input", allowDelete = false) {

    override var genSession: CodeGenSession? = null

    override fun onEnable() {
        + MatAttribute(OUTPUT, "Input")
    }

    override fun genCode(codeGen: CodeGen): CodeGenSession {
        raise("Input Mat node cannot generate code")
    }

    override fun getOutputValueOf(codeGen: CodeGen, index: Int) = GenValue.Mat("input".v, Colors.RGB)
}

class OutputMatNode : DrawNode("Pipeline Output", allowDelete = false) {

    val input = MatAttribute(INPUT, "Output")

    override fun onEnable() {
        + input
    }

    override var genSession: CodeGenSession? = null

    override fun genCode(codeGen: CodeGen) = codeGen {
        processFrame {
            returnMethod(input.value(codeGen).value) // start code gen!
        }

        CodeGenSession()
    }

    override fun getOutputValueOf(codeGen: CodeGen, index: Int) = GenValue.None
}