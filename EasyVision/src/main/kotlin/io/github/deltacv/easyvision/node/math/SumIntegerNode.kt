package io.github.deltacv.easyvision.node.math

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue

class SumIntegerNode : DrawNode<SumIntegerNode.Session>("Sum Integer") {

    val numbers = ListAttribute(INPUT, IntAttribute, "Numbers")
    val result  = IntAttribute(OUTPUT,"Result")

    override fun onEnable() {
        + numbers
        + result
    }

    class Session : CodeGenSession() {

    }

    override fun genCode(current: CodeGen.Current): Session {
        TODO("Not yet implemented")
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        TODO("Not yet implemented")
    }

}