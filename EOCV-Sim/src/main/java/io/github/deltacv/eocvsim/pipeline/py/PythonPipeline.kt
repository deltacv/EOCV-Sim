package io.github.deltacv.eocvsim.pipeline.py

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.opencv.core.Mat
import org.openftc.easyopencv.OpenCvPipeline
import org.python.core.Py
import org.python.core.PyFunction
import org.python.core.PyObject
import org.python.util.PythonInterpreter

class PythonPipeline(
    val name: String,
    val source: String,
    val telemetry: Telemetry
) : OpenCvPipeline() {

    var initFunction: PyFunction? = null
        private set
    lateinit var processFrameFunction: PyFunction
        private set
    var onViewportTappedFunction: PyFunction? = null
        private set

    private lateinit var matPyObject: PyObject

    private val interpreter = PythonInterpreter()

    override fun init(mat: Mat) {
        interpreter.set("telemetry", Py.java2py(telemetry))
        interpreter.exec(source)

        val initFunc = interpreter.get("init")
        if(initFunc is PyFunction) {
            initFunction = initFunc
        }

        val processFrameFunc = interpreter.get("processFrame")
        if(processFrameFunc is PyFunction) {
            processFrameFunction = processFrameFunc
        } else throw NoSuchMethodException("processFrame function was not found in the python script")

        val onViewportTappedFunc = interpreter.get("onViewportTapped")
        if(onViewportTappedFunc is PyFunction) {
            onViewportTappedFunction = onViewportTappedFunc
        }

        matPyObject = Py.java2py(mat)

        initFunction?.__call__(matPyObject)
    }

    override fun processFrame(input: Mat): Mat {
        return processFrameFunction.__call__(matPyObject).__tojava__(Mat::class.java) as Mat
    }

    override fun onViewportTapped() {
        onViewportTappedFunction?.__call__()
    }

}