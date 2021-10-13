package io.github.deltacv.easyvision.node.vision.overlay

import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.attribute.vision.PointsAttribute
import io.github.deltacv.easyvision.attribute.vision.ScalarAttribute
import io.github.deltacv.easyvision.attribute.vision.ScalarRangeAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.parse.new
import io.github.deltacv.easyvision.codegen.parse.v
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.vision.Colors

@RegisterNode(
    name = "Draw Contours on Image",
    category = Category.OVERLAY,
    description = "Draws the contours list in the passed image."
)
class DrawContoursOnImageNode : DrawNode<DrawContoursOnImageNode.Session>("Draw Contours on Image")  {

    val drawMat = MatAttribute(INPUT, "Drawing On")
    val contours = ListAttribute(INPUT, PointsAttribute, "Contours")

    val lineColor = ScalarAttribute(INPUT, Colors.RGB,"Line Color")
    val lineThickness = IntAttribute(INPUT, "Line Thickness")

    override fun onEnable() {
        + drawMat
        + contours

        + lineColor
        + lineThickness

        lineThickness.value.set(1)
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val color = lineColor.value(current)
        val colorScalar = tryName("contoursColor")

        val inputMat = drawMat.value(current)
        val contoursList = contours.value(current)
        val thickness = lineThickness.value(current).value

        if(contoursList !is GenValue.RuntimeListOf<*>) {
            contours.raise("")
        }

        // add necessary imports
        import("org.opencv.imgproc.Imgproc")
        import("org.opencv.core.Scalar")

        public(colorScalar,
            new("Scalar",
                color.a.toString(),
                color.b.toString(),
                color.c.toString(),
                color.d.toString(),
            )
        )

        current.scope {
            "Imgproc.drawContours"(
                inputMat.value,
                (contoursList as GenValue.RuntimeListOf<*>).value,
                (-1).v, colorScalar.v, thickness.v
            )
        }

        session
    }

    class Session : CodeGenSession {

    }

}