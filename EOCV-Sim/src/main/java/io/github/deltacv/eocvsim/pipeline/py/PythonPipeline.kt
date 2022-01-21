/*
 * Copyright (c) 2022 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.deltacv.eocvsim.pipeline.py

import io.github.deltacv.eocvsim.pipeline.StreamableOpenCvPipeline
import io.github.deltacv.eocvsim.virtualreflect.py.PyWrapper
import io.github.deltacv.eocvsim.virtualreflect.py.enableLabeling
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.opencv.core.Mat
import org.openftc.easyopencv.OpenCvPipeline
import org.python.core.Py
import org.python.core.PyFunction
import org.python.core.PyObject
import org.python.util.PythonInterpreter

class PythonPipeline(
    override val name: String,
    val source: String,
    val telemetry: Telemetry
) : PyWrapper, StreamableOpenCvPipeline() {

    var initFunction: PyFunction? = null
        private set
    var processFrameFunction: PyFunction
        private set
    var onViewportTappedFunction: PyFunction? = null
        private set

    private lateinit var matPyObject: PyObject

    override val interpreter = PythonInterpreter().apply {
        enableLabeling()
        set("telemetry", Py.java2py(telemetry))
        set("stream", Streamer(this@PythonPipeline))

        exec(source)
    }

    init {
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
    }

    override fun init(mat: Mat) {
        matPyObject = Py.java2py(mat)
        initFunction?.__call__(matPyObject)
    }

    override fun processFrame(input: Mat): Mat {
        return processFrameFunction.__call__(matPyObject).__tojava__(Mat::class.java) as Mat
    }

    override fun onViewportTapped() {
        onViewportTappedFunction?.__call__()
    }

    // class for exposing the streamFrame function to python
    // in the shape of "stream(Int, Mat, Int)"
    // or "stream(Int, Mat)"
    class Streamer(private val pipeline: PythonPipeline) : PyObject() {
        override fun __call__(id: PyObject, image: PyObject): PyObject {
            pipeline.streamFrame(id.asInt().toShort(), Py.tojava(image, Mat::class.java))
            return Py.None
        }

        override fun __call__(id: PyObject, image: PyObject, cvtColor: PyObject): PyObject {
            pipeline.streamFrame(id.asInt().toShort(), Py.tojava(image, Mat::class.java), cvtColor.asInt())
            return Py.None
        }

        override fun __call__(args: Array<out PyObject>): PyObject {
            if(args.size < 2) {
                throw Py.TypeError("stream() missing ${2 - args.size} required positional argument(s)")
            } else if(args.size > 3) {
                throw Py.TypeError("stream() takes 2 or 3 positional arguments but ${args.size} were given")
            }

            val id = args[0]
            val image = args[1]
            val cvtColor = if(args.size == 3) args[2] else null

            pipeline.streamFrame(id.asInt().toShort(), Py.tojava(image, Mat::class.java), cvtColor?.asInt())

            return Py.None
        }
    }

}