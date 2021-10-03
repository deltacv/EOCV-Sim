package io.github.deltacv.easyvision.node.math

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.Category

@RegisterNode(
    name = "Sum Integers",
    category = Category.MATH,
    description = "Sums a list of integers and outputs the result"
)
class SumIntegerNode : DrawNode<SumIntegerNode.Session>("Sum Integers") {

    val numbers = ListAttribute(INPUT, IntAttribute, "Numbers")
    val result  = IntAttribute(OUTPUT, "Result")

    override fun onEnable() {
        + numbers
        + result
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()



        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == result) {
            return genSession!!.result
        }

        raise("Attribute $attrib is not an output of this node or not handled by this")
    }

    class Session : CodeGenSession {
        lateinit var result: GenValue.Int
    }

}