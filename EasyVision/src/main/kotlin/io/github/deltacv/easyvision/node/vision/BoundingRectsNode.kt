package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode

@RegisterNode(
    name = "Bounding Rectangles",
    category = Category.CV_BASICS,
    description = "Finds all the contours (list of points) of a given binary image."
)
class BoundingRectsNode : DrawNode<BoundingRectsNode.Session>("Bounding Rectangles") {

    val inputContours = PointsAttribute(INPUT, "Contours")


    override fun genCode(current: CodeGen.Current): Session {
        TODO("Not yet implemented")
    }

    class Session : CodeGenSession {

    }

}