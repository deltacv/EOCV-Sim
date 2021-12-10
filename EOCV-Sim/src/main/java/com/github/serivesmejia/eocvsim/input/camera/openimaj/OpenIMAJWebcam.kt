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

package com.github.serivesmejia.eocvsim.input.camera.openimaj

import com.github.serivesmejia.eocvsim.input.camera.WebcamBase
import com.github.serivesmejia.eocvsim.input.camera.WebcamRotation
import com.github.serivesmejia.eocvsim.util.Log
import com.github.serivesmejia.eocvsim.util.fps.FpsLimiter
import org.bridj.Pointer
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.openftc.easyopencv.MatRecycler
import org.openimaj.video.capture.Device
import org.openimaj.video.capture.VideoCapture
import java.util.concurrent.ArrayBlockingQueue
import kotlin.experimental.and
import kotlin.system.measureTimeMillis

class OpenIMAJWebcam @JvmOverloads constructor(
    private val device: Device,
    resolution: Size = Size(320.0, 240.0),
    rotation: WebcamRotation = WebcamRotation.UPRIGHT,
    fps: Double = 30.0
) : WebcamBase(rotation) {

    companion object {
        @JvmStatic
        val availableWebcams: MutableList<Device>
            get() = VideoCapture.getVideoDevices()

        const val TAG = "OpenIMAJWebcam"
    }

    override val isOpen get() = videoCapture != null && videoCapture!!.hasNextFrame()

    override var resolution = resolution
        set(value) {assertNotOpen("change resolution")
            field = value
        }


    // openimaj doesn't have an api to get the actual supported resolutions
    // that sucks. however, we can return a list of common resolutions which isn't too bad
    override val supportedResolutions = listOf(
        Size(176.0, 144.0),
        Size(320.0, 240.0),
        Size(640.0, 360.0),
        Size(640.0, 480.0),
        Size(960.0, 540.0),
        Size(1024.0, 768.0),
        Size(1280.0, 720.0),
        Size(1280.0, 1024.0)
    )

    override val index get() = availableWebcams.indexOf(device)
    override val name: String get() = device.nameStr

    override var fps = fps
        set(value) {
            assertNotOpen("change fps")
            field = value
        }

    var videoCapture: VideoCapture? = null
        private set
    private var grabber: ReflectOpenIMAJGrabber? = null

    private var stream: WebcamStream? = null
    private var streamThread: Thread? = null

    private var frameMat: Mat? = null
    private var closeLock = Any()

    override fun open() {
        assertNotOpen("open camera")

        try {
            val capture = VideoCapture(
                resolution.width.toInt(),
                resolution.height.toInt(),
                fps, device
            )
            grabber = ReflectOpenIMAJGrabber(capture)

            // set the actual resolution to the size of the
            // native videoData to avoid jvm crash due to
            // requesting more bytes than we have
            resolution = grabber!!.resolution

            // avoid triggering exception in the resolution setter
            // by defining videocapture after setting actual resolution
            videoCapture = capture

            frameMat = Mat(
                resolution.height.toInt(),
                resolution.width.toInt(),
                CvType.CV_8UC3
            )

            closeLock = Any()

            // creating webcam stream and starting it in another thread
            stream = WebcamStream(this, closeLock)
            streamThread = Thread(stream!!, "WebcamStream-\"$name\"-Thread")

            streamThread!!.start()
        } catch(e: Exception) {
            Log.error(TAG, "Error while opening camera", e)

            videoCapture = null
            streamThread?.interrupt()
        }
    }

    override fun internalRead(mat: Mat) {
        if(!streamThread!!.isAlive) {
            Log.warn(TAG, "The WebcamStream thread mysteriously disappeared, probably due to an exception. Closing webcam \"$name\".")
            close()
            return
        }

        val queue = stream!!.queue

        if (!queue.isEmpty()) {
            val webcamFrameMat = queue.poll()

            webcamFrameMat.copyTo(frameMat)
            webcamFrameMat.copyTo(mat)

            webcamFrameMat.returnMat()
        } else {
            frameMat!!.copyTo(mat)
        }
    }

    override fun close() {
        assertOpen()

        synchronized(closeLock) {
            videoCapture!!.stopCapture()
            videoCapture = null
            streamThread!!.interrupt()
        }
    }

    private class WebcamStream(
        val webcam: OpenIMAJWebcam,
        val lock: Any,
        matQueueSize: Int = 2,
    ) : Runnable {
        private val grabber = webcam.grabber!!
        private val fpsLimiter = FpsLimiter(webcam.fps)

        private val width = webcam.resolution.width.toInt()
        private val height = webcam.resolution.height.toInt()

        val queue = EvictingBlockingQueue(ArrayBlockingQueue<MatRecycler.RecyclableMat>(matQueueSize))

        private val recycler = MatRecycler(matQueueSize + 2, height, width, CvType.CV_8UC3)
        private val pixels = ByteArray(width * height * 3)

        override fun run() {
            queue.setEvictAction {
                it.returnMat()
            }

            while (!Thread.interrupted() && webcam.isOpen && webcam.videoCapture!!.hasNextFrame()) {
                synchronized(lock) {
                    // When closing the camera, main thread claims the lock and takes a little while.
                    // Due to this, the while loop continues to the next iteration and waits for the
                    // close method to free the lock. Once it's freed and the camera has been closed,
                    // the iteration continues even after the thread has been interrupted.
                    //
                    // This causes grabber.nextFrame to be called below and hang for a little before
                    // it timeouts and ends the thread, but in some apparently random case, it can
                    // cause a native jvm crash for some reason.
                    //
                    // We check if the thread has been interrupted right after the lock is claimed,
                    // and if it was, break out of the loop immediately by interrupting the thread
                    // and exiting from synchronized() to make sure we end the thread right after
                    // close() is called and the lock is reclaimed. Thanks for coming to my ted talk.
                    if(Thread.interrupted()) {
                        Thread.currentThread().interrupt()
                        return@synchronized // break; because the thread has been interrupted
                    }

                    val err = grabber.nextFrame()

                    // copied from the original source code of VideoCapture.
                    // no idea what the < -1 error codes stand for.
                    if (err == -1) {
                        Log.warn(TAG, "Timed out waiting for next frame of \"${webcam.name}\"")
                        return@synchronized
                    } else if (err < -1) {
                        throw RuntimeException("Error occurred getting next frame (code: $err)")
                    }

                    val image = grabber.image ?: return@synchronized // continue; if the image ptr was null

                    val resolution = grabber.resolution

                    // Another instance of an apparently random jvm crash:
                    // Sometimes the jvm ends unexpectedly with an access violation exit code (0xC0000374)
                    // i suspect that it's due to the fact that we read more bytes than we have available
                    // in the call to image.getByteBuffer(), apparently it doesn't implement any sort of
                    // check so if we overflow it instantly results in a crash.
                    //
                    // It *shouldn't* happen since we don't allow the resolution to be changed in the middle of
                    // a running camera stream, but we implement this check regardless to see if it fixes the issue
                    if(resolution.width.toInt() != width || resolution.height.toInt() != height) {
                        Log.warn(TAG, "Grabber currently has a resolution different than the initial one ($resolution vs initial Size($width, $height))")
                        return@synchronized // continue;
                    }

                    val mat = recycler.takeMat()

                    // most sensitive call in the code: Requesting more bytes than what the native
                    // Pointer has, will result in a jvm crash. See the check implemented before
                    val buffer = image.getByteBuffer((width * height * 3).toLong())

                    // getByteBuffer returns a Direct ByteBuffer, which means that it directly maps
                    // to an area of native memory. Calling buffer.get(array) on a direct buffer,
                    // directly copies the pixels using memcpy under the hood, so this is about the
                    // fastest we can get with the openimaj driver in pure kotlin. (this could probably
                    // be reduced to zero or one copies instead of two using native c++ but i don't
                    // really wanna go through that pain again...)
                    //
                    // We reuse the same pixel array every loop so that the gc doesn't panic and lets
                    // us run smoothly. Benchmarks performed at some point revealed that creating a
                    // new array every loop caused spikes of up to 300-500ms constantly, reducing our
                    // frame rate by a non-negligible amount. Thankfully the direct byte buffer created
                    // every frame doesn't make the gc panic because of the same fact that it's just
                    // mapped to bytes in the native memory, outside of the gc's managed memory.
                    buffer.get(pixels)

                    // OpenIMAJGrabber already stores pixels under the hood
                    // in a continuous 1D array in the rgb format, that's
                    // where (width * height * 3) comes from, the "3" constant
                    // stands for the three channels in the rgb color space.
                    //
                    // That is extremely convenient, it directly allows us to
                    // copy the pixels array into the opencv mat. Thank goodness.
                    mat.put(0, 0, pixels)

                    queue.offer(mat)
                }

                fpsLimiter.sync()
            }

            recycler.releaseAll()
        }

    }

}