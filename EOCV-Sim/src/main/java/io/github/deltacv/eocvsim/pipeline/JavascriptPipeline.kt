package io.github.deltacv.eocvsim.pipeline

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.ScriptableObject
import org.opencv.core.Mat
import org.openftc.easyopencv.OpenCvPipeline
import java.lang.IllegalArgumentException

class JavascriptPipeline(
    val name: String,
    val source: String,
    val telemetry: Telemetry
) : OpenCvPipeline() {

    val jsContext: Context by lazy { Context.enter() }
    val jsScope: ScriptableObject by lazy {
        jsContext.initStandardObjects(ImporterTopLevel(jsContext), false)
    }

    var initFunction: Function? = null
        private set

    lateinit var processFrameFunction: Function
        private set

    var onViewportTappedFunction: Function? = null
        private set
    
    private lateinit var inputMatArray: Array<Any>

    override fun init(mat: Mat) {
        // making telemetry available in the js pipeline
        val telemetryObject = Context.toObject(telemetry, jsScope)
        ScriptableObject.putProperty(jsScope, "telemetry", telemetryObject)

        // initializing rhyno and interpreting source code
        jsContext.evaluateString(jsScope, source, name, 0, null)

        val initFunc = jsScope.get("init", jsScope)
        initFunction = if(initFunc is Function) {
            initFunc
        } else null

        val processFrameFunc = jsScope.get("processFrame", jsScope)
        processFrameFunction = if(processFrameFunc is Function) {
            processFrameFunc
        } else throw IllegalArgumentException("Script does not define a processFrame() function")

        val onViewportTappedFunc = jsScope.get("onViewportTapped", jsScope)
        onViewportTappedFunction = if(onViewportTappedFunc is Function) {
            onViewportTappedFunc
        } else null
        
        // calling init()
        inputMatArray = arrayOf(mat)
        initFunction?.call(jsContext, jsScope, jsScope, inputMatArray)
    }

    override fun processFrame(input: Mat): Mat {
        val result = processFrameFunction.call(jsContext, jsScope, jsScope, inputMatArray)
        if(result !is Mat) {
            throw IllegalArgumentException("JavaScript processFrame didn't return a Mat")
        }

        return result
    }

    private val emptyArray = arrayOf<Any>()

    override fun onViewportTapped() {
        onViewportTappedFunction?.call(jsContext, jsScope, jsScope, emptyArray)
    }

}