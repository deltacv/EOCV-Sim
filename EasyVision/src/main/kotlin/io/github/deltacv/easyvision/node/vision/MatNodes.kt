package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.codegen.parse.v

class InputMatNode : DrawNode<NoSession>("Pipeline Input", allowDelete = false) {

    override fun onEnable() {
        + MatAttribute(OUTPUT, "Input")
    }

    override fun genCode(current: CodeGen.Current) = NoSession

    fun startGen(current: CodeGen.Current) {
        propagate(current)
    }

    val value = GenValue.Mat("input".v, Colors.RGBA)

    override fun getOutputValueOf(current: CodeGen.Current,
                                  attrib: Attribute) = value
}

class OutputMatNode : DrawNode<NoSession>("Pipeline Output", allowDelete = false) {

    val input = MatAttribute(INPUT, "Output")

    override fun onEnable() {
        + input
    }

    override fun genCode(current: CodeGen.Current) = current {
        current.scope {
            returnMethod(input.value(current).value) // start code gen!
            appendWhiteline = false
        }

        NoSession
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute) = GenValue.None
}