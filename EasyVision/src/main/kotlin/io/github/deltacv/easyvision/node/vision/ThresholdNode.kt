package io.github.deltacv.easyvision.node.vision

import imgui.ImGui
import imgui.type.ImInt
import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.EnumAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.attribute.vision.RangeAttribute
import io.github.deltacv.easyvision.attribute.vision.ScalarAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.NoSession
import io.github.deltacv.easyvision.codegen.build.cvtColorValue
import io.github.deltacv.easyvision.codegen.build.new
import io.github.deltacv.easyvision.codegen.build.v
import io.github.deltacv.easyvision.gui.ExtraWidgets
import io.github.deltacv.easyvision.node.DrawNode

class ThresholdNode : DrawNode<ThresholdNode.Session>("Color Threshold") {

    val input = MatAttribute(INPUT, "Input")
    val scalar = ScalarAttribute(INPUT, Colors.values()[0], "Test")
    val output = MatAttribute(OUTPUT, "Output")

    override fun onEnable() {
        + input
        + scalar
        + output
    }

    val colorValue = ImInt()

    private var lastColor = Colors.values()[0]

    override fun drawNode() {
        input.drawHere()

        ImGui.newLine()
        ImGui.text("(Enum) Color Space")

        ImGui.pushItemWidth(110.0f)
        val color = ExtraWidgets.enumCombo(Colors.values(), colorValue)
        ImGui.popItemWidth()

        ImGui.newLine()

        if(color != lastColor) {
            scalar.color = color
        }

        lastColor = color
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val range = scalar.value(current)

        var inputMat = input.value(current)
        
        var matColor = inputMat.color
        var targetColor = lastColor

        if(matColor != targetColor) {
            if(matColor == Colors.RGBA && targetColor != Colors.RGB) {
                matColor = Colors.RGB
            } else if(matColor != Colors.RGB && targetColor == Colors.RGBA) {
                targetColor = Colors.RGB
            }
        }
        
        val needsCvt = matColor != targetColor

        val cvtMat = tryName("${targetColor.name.lowercase()}Mat")
        val thresholdTargetMat = tryName("${targetColor.name.lowercase()}BinaryMat")

        val lowerScalar = tryName("lower${targetColor.name}")
        val upperScalar = tryName("upper${targetColor.name}")

        // add necessary imports
        import("org.opencv.imgproc.Imgproc")
        import("org.opencv.core.Scalar")
        import("org.opencv.core.Core")

        // lower color scalar
        public(lowerScalar,
            new("Scalar",
                range.a.min.toString(),
                range.b.min.toString(),
                range.c.min.toString(),
                range.d.min.toString(),
            )
        )

        // upper color scalar
        public(upperScalar,
            new("Scalar",
                range.a.max.toString(),
                range.b.max.toString(),
                range.c.max.toString(),
                range.d.max.toString(),
            )
        )

        if(needsCvt) {
            private(cvtMat, new("Mat"))
        }
        // output mat target
        private(thresholdTargetMat, new("Mat"))

        current.scope {
            if(needsCvt) {
                "Imgproc.cvtColor"(inputMat.value, cvtMat.v, cvtColorValue(matColor, targetColor))
                inputMat = GenValue.Mat(cvtMat.v, targetColor)
            }

            "Core.inRange"(inputMat.value, lowerScalar.v, upperScalar.v, thresholdTargetMat.v)
        }

        session.outputMat = GenValue.Mat(thresholdTargetMat.v, targetColor)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == output) {
            return genSession!!.outputMat
        }

        raise("Attribute $attrib is not an output of this node or not handled by this")
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}