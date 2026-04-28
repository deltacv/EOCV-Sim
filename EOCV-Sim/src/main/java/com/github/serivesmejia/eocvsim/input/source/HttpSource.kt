/*
 * Copyright (c) 2025 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.input.source

import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer

import com.google.gson.annotations.Expose
import io.github.deltacv.visionloop.io.MjpegHttpReader
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import javax.swing.filechooser.FileFilter

class HttpSource @JvmOverloads constructor(
    @Expose @JvmField var url: String = ""
) : InputSource() {

    override val hasSlowInitialization: Boolean get() = true

    @Transient private var mjpegHttpReader: MjpegHttpReader? = null

    @Transient private var buf: MatOfByte? = null
    @Transient private var img: Mat? = null

    @Transient private var iterator: Iterator<ByteArray>? = null

    @Transient private var capTimeNanos: Long = 0

    @Transient private val logger = LoggerFactory.getLogger(javaClass)

    override fun init(): Boolean {
        buf = MatOfByte()
        img = Mat()

        try {
            mjpegHttpReader = MjpegHttpReader(url)
            mjpegHttpReader!!.start()
        } catch (e: Exception) {
            logger.error("Error while initializing MjpegHttpReader", e)
            return false
        }

        try {
            iterator = mjpegHttpReader!!.iterator()
        } catch (e: Exception) {
            logger.error("Error while getting MjpegHttpReader iterator", e)
            return false
        }

        logger.info("HttpSource initialized")

        return mjpegHttpReader != null && iterator != null
    }

    @Transient private var frame: ByteArray? = null

    override fun update(): Mat? {
        if (mjpegHttpReader == null || iterator == null) return null

        val result = InputSourceInitializer.runWithTimeout(name) {

            frame = iterator!!.next()
            frame != null
        }

        if (result != InputSourceInitializer.Result.SUCCESS || frame == null) {
            return null
        }

        val frameData = frame!!

        if (!dataIsValidJPEG(frameData)) {
            logger.error("Received data is not a valid JPEG image")
            return null
        }

        buf!!.fromArray(*frameData)

        if (buf!!.empty()) {
            return null
        }

        val mat = Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_COLOR)
        Imgproc.cvtColor(mat, img, Imgproc.COLOR_BGR2RGBA)

        mat.release()

        capTimeNanos = System.nanoTime()

        return img
    }

    override fun reset() {
        mjpegHttpReader?.stop()
        mjpegHttpReader = null
    }

    override fun close() {
        reset()
    }

    override fun onPause() {
        if (mjpegHttpReader != null) {
            reset()
        }
    }

    override fun onResume() {
        InputSourceInitializer.runWithTimeout(this) {


            init()
        }
    }

    override fun internalCloneSource(): InputSource = HttpSource(url)

    override val fileFilters: FileFilter? get() = null
    override val captureTimeNanos: Long get() = capTimeNanos


    override fun toString(): String {
        return "HttpSource($url)"
    }

    companion object {
        private fun dataIsValidJPEG(data: ByteArray?): Boolean {
            if (data == null || data.size < 2) {
                return false
            }

            val totalBytes = getJPEGSize(data, data.size)

            if (totalBytes == -1) {
                return false
            }

            return (data[0] == 0xFF.toByte() &&
                    data[1] == 0xD8.toByte() &&
                    data[totalBytes - 2] == 0xFF.toByte() &&
                    data[totalBytes - 1] == 0xD9.toByte())
        }

        private fun getJPEGSize(data: ByteArray?, maxLength: Int): Int {
            if (data == null || maxLength < 4) {
                return -1 // Invalid or too small to be a JPEG
            }

            // Check for SOI marker
            if (data[0] != 0xFF.toByte() || data[1] != 0xD8.toByte()) {
                return -1 // Not a JPEG
            }

            var pos = 2 // Start after SOI

            while (pos < maxLength - 2) {
                // Look for the next marker (0xFF xx)
                if (data[pos] == 0xFF.toByte()) {
                    val marker = data[pos + 1]

                    // End of Image (EOI) found
                    if (marker == 0xD9.toByte()) {
                        return pos + 2 // JPEG size
                    }

                    // Skip padding bytes (some JPEGs use 0xFF 0x00)
                    if (marker == 0x00.toByte()) {
                        pos++
                        continue
                    }

                    // Most markers have a 2-byte length field
                    if ((marker >= 0xC0.toByte() && marker <= 0xFE.toByte()) && marker != 0xD9.toByte()) {
                        if (pos + 3 >= maxLength) {
                            return -1 // Incomplete JPEG
                        }

                        // Read segment length (big-endian)
                        val segmentLength = ((data[pos + 2].toInt() and 0xFF) shl 8) or (data[pos + 3].toInt() and 0xFF)

                        if (segmentLength < 2 || pos + segmentLength >= maxLength) {
                            return -1 // Corrupt or incomplete JPEG
                        }

                        pos += segmentLength // Move to next marker
                    } else {
                        pos++ // Skip unknown byte
                    }
                } else {
                    pos++ // Continue searching
                }
            }

            return -1 // No valid JPEG end found
        }
    }
}
