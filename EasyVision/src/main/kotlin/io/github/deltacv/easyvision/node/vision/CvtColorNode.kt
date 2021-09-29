package io.github.deltacv.easyvision.node.vision

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.codegen.*
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.build.*
import io.github.deltacv.easyvision.node.DrawNode

enum class Colors(val channels: Int, val channelNames: Array<String>) {
    RGBA(4,  arrayOf("R", "G", "B", "A")),
    RGB(3,   arrayOf("R", "G", "B")),
    BGR(3,   arrayOf("B", "G", "R")),
    HSV(3,   arrayOf("H", "S", "V")),
    YCrCb(3, arrayOf("Y", "Cr", "Cb")),
    LAB(3,   arrayOf("L", "A", "B")),
    GRAY(1,  arrayOf("GRAY"))
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

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val inputMat = input.value(current)

        val targetColor = convertTo.value(current).value
        val matColor = inputMat.color

        import("org.opencv.imgproc.Imgproc")

        if(matColor != targetColor) {
            val matName = tryName("${targetColor.name.lowercase()}Mat")

            // create mat instance variable
            private(matName, new("Mat"))

            current.scope { // add a cvtColor step in processFrame
                "Imgproc.cvtColor"(inputMat.value, matName.v, cvtColorValue(matColor, targetColor))
            }

            session.outputMatValue = GenValue.Mat(matName.v, targetColor) // store data in the current session
        } else {
            // we don't need to do any processing if the mat is
            // already of the color the user specified to convert to
            session.outputMatValue = inputMat
        }

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return genSession!!.outputMatValue
        }

        raise("Attribute $attrib is not an output of this node or not handled by this")
    }

    class Session : CodeGenSession {
        lateinit var outputMatValue: GenValue.Mat
    }

}