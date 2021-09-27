package io.github.deltacv.easyvision.node.math

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.type.GenValue

class SumIntegerNode : DrawNode<SumIntegerNode.Session>("Sum Integer") {

    override fun onEnable() {
        + ListAttribute(INPUT, IntAttribute, "Numbers")

        + IntAttribute(OUTPUT,"Result")
    }

    class Session : CodeGenSession() {

    }

    override fun genCode(codeGen: CodeGen): Session {
        TODO("Not yet implemented")
    }

    override fun getOutputValueOf(codeGen: CodeGen, attrib: Attribute): GenValue {
        TODO("Not yet implemented")
    }

}