package io.github.deltacv.eocvsim.ipc

import com.github.serivesmejia.eocvsim.util.CvUtil
import com.github.serivesmejia.eocvsim.util.extension.aspectRatio
import com.github.serivesmejia.eocvsim.util.extension.clipTo
import com.github.serivesmejia.eocvsim.util.image.BufferedImageRecycler
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import java.awt.Dimension
import java.awt.image.DataBufferByte

class IpcImageStreamer(
    val resolution: Size,
    val opcode: Byte,
    val server: IpcServer
) {

    private val matRecycler = MatRecycler(2)

    private val bytes = ByteArray(resolution.width.toInt() * resolution.height.toInt() * 3)

    fun sendFrame(
        id: Int,
        img: Mat,
        cvtCode: Int?
    ) {
        if(img.empty()) return

        val scaledImg = matRecycler.takeMat()
        scaledImg.release()

        try {
            if (img.size() == resolution) { //nice, the mat size is the exact same as the video size
                img.copyTo(scaledImg)
            } else { //uh oh, this might get a bit harder here...
                val targetR = resolution.aspectRatio()
                val inputR = img.aspectRatio()

                //ok, we have the same aspect ratio, we can just scale to the required size
                if (targetR == inputR) {
                    Imgproc.resize(img, scaledImg, resolution, 0.0, 0.0, Imgproc.INTER_AREA)
                } else { //hmm, not the same aspect ratio, we'll need to do some fancy stuff here...
                    val inputW = img.size().width
                    val inputH = img.size().height

                    val widthRatio = resolution.width / inputW
                    val heightRatio = resolution.height / inputH
                    val bestRatio = widthRatio.coerceAtMost(heightRatio)

                    val newSize = Size(inputW * bestRatio, inputH * bestRatio).clipTo(resolution)

                    //get offsets so that we center the image instead of leaving it at (0,0)
                    //(basically the black bars you see)
                    val xOffset = (resolution.width - newSize.width) / 2
                    val yOffset = (resolution.height - newSize.height) / 2

                    val resizedImg = matRecycler.takeMat()

                    Imgproc.resize(img, resizedImg, newSize, 0.0, 0.0, Imgproc.INTER_AREA)

                    //get submat of the exact required size and offset position from the "videoMat",
                    //which has the user-defined size of the current video.
                    val submat = scaledImg.submat(Rect(Point(xOffset, yOffset), newSize))

                    //then we copy our adjusted mat into the gotten submat. since a submat is just
                    //a reference to the parent mat, when we copy here our data will be actually
                    //copied to the actual mat, and so our new mat will be of the correct size and
                    //centered with the required offset
                    resizedImg.copyTo(submat)

                    resizedImg.returnMat()
                }
            }

            if(cvtCode != null) {
                Imgproc.cvtColor(scaledImg, scaledImg, cvtCode)
            }
        } catch(e: Exception) {
            scaledImg.returnMat()
            return
        }

        synchronized(bytes) {
            scaledImg.get(0, 0, bytes)
            scaledImg.returnMat()

            server.broadcastBinary(opcode, id, bytes)
        }
    }

}