/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.output

import com.github.serivesmejia.eocvsim.gui.util.MatPoster
import com.github.serivesmejia.eocvsim.util.StrUtil
import com.github.serivesmejia.eocvsim.util.extension.aspectRatio
import com.github.serivesmejia.eocvsim.util.extension.clipTo
import com.github.serivesmejia.eocvsim.util.fps.FpsCounter
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.videoio.VideoWriter
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.roundToInt

class VideoRecordingSession(val videoFps: Double = 30.0, val videoSize: Size = Size(320.0, 240.0), val isFramesRgb: Boolean = true) {

    private val videoWriter = VideoWriter()
    private val tempFile = File.createTempFile(StrUtil.random(), ".avi")

    @Volatile private var videoMat: Mat? = null

    val matPoster = MatPoster("VideoRec", videoFps.toInt())

    private val fpsCounter = FpsCounter()

    @Volatile var hasStarted = false
        private set
    @Volatile var hasStopped = false
        private set

    val isRecording: Boolean
        get() {
            return hasStarted && !hasStopped
        }

    init {
        matPoster.addPostable { postMat(it) }
    }

    fun startRecordingSession() {
        videoWriter.open(tempFile.toString(), VideoWriter.fourcc('M', 'J', 'P', 'G'), videoFps, videoSize)
        hasStarted = true;
    }

    fun stopRecordingSession() {
        videoWriter.release(); videoMat?.release(); matPoster.stop()
        hasStopped = true
    }

    fun saveTo(file: File) {
        if(!hasStopped) return
        Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.delete(tempFile.toPath())
    }

    fun discardVideo() {
        if(tempFile.exists())
            Files.delete(tempFile.toPath())
    }

    @Synchronized fun postMatAsync(inputMat: Mat) {
        if(!videoWriter.isOpened) return
        matPoster.post(inputMat)
    }

    @Synchronized fun postMat(inputMat: Mat) {
        if(!videoWriter.isOpened) return

        if(videoMat == null)
            videoMat = Mat(videoSize, inputMat.type())
        else
            videoMat!!.setTo(Scalar(0.0, 0.0, 0.0))

        //we need BGR frames
        if(isFramesRgb) {
            Imgproc.cvtColor(inputMat, inputMat, Imgproc.COLOR_RGB2BGR)
        }

        if(inputMat.size() == videoSize) { //nice, the mat size is the exact same as the video size
            compensateFpsWrite(inputMat, fpsCounter.fps.toDouble(), videoFps)
        } else { //uh oh, this might get a bit harder here...
            val videoR = videoSize.aspectRatio()
            val inputR = inputMat.aspectRatio()

            //ok, we have the same aspect ratio, we can just scale to the required size
            if(videoR == inputR) {
                Imgproc.resize(inputMat, videoMat, videoSize, 0.0, 0.0, Imgproc.INTER_AREA)
                compensateFpsWrite(videoMat!!, fpsCounter.fps.toDouble(), videoFps)
            } else { //hmm, not the same aspect ratio, we'll need to do some fancy stuff here...
                val inputW = inputMat.size().width
                val inputH = inputMat.size().height

                val widthRatio = videoSize.width / inputW
                val heightRatio = videoSize.height / inputH
                val bestRatio = widthRatio.coerceAtMost(heightRatio)

                val newSize = Size(inputW * bestRatio, inputH * bestRatio).clipTo(videoSize)

                //get offsets so that we center the image instead of leaving it at (0,0)
                //(basically the black bars you see)
                val xOffset = (videoSize.width - newSize.width) / 2
                val yOffset = (videoSize.height - newSize.height) / 2

                Imgproc.resize(inputMat, inputMat, newSize, 0.0, 0.0, Imgproc.INTER_AREA)

                //get submat of the exact required size and offset position from the "videoMat",
                //which has the user-defined size of the current video.
                val submat = videoMat!!.submat(Rect(Point(xOffset, yOffset), newSize))

                //then we copy our adjusted mat into the gotten submat. since a submat is just
                //a reference to the parent mat, when we copy here our data will be actually
                //copied to the actual mat, and so our new mat will be of the correct size and
                //centered with the required offset
                inputMat.copyTo(submat);

                compensateFpsWrite(videoMat!!, fpsCounter.fps.toDouble(), videoFps)
            }

            fpsCounter.update()
        }
    }

    //compensating for variable fps, we write the same mat multiple
    //times so that our video stays in sync with the correct speed.
    @Synchronized private fun compensateFpsWrite(mat: Mat, currentFps: Double, targetFps: Double) {
        if (currentFps < targetFps && currentFps > 0) {
            repeat((targetFps / currentFps).roundToInt()) {
                videoWriter.write(mat)
            }
        } else {
            videoWriter.write(mat)
        }
    }

}