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

package com.github.serivesmejia.eocvsim.input.source;

import com.github.serivesmejia.eocvsim.input.InputSource;
import com.github.serivesmejia.eocvsim.input.InputSourceInitializer;
import com.google.gson.annotations.Expose;
import io.github.deltacv.visionloop.io.MjpegHttpReader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileFilter;
import java.util.Iterator;

public class HttpSource extends InputSource {

    @Expose
    protected String url;

    private transient MjpegHttpReader mjpegHttpReader;

    private static final Logger logger = LoggerFactory.getLogger(HttpSource.class);

    private transient MatOfByte buf;
    private transient Mat img;

    private transient Iterator<byte[]> iterator;

    private transient long capTimeNanos = 0;

    public HttpSource(String url) {
        this.url = url;
    }

    @Override
    public boolean init() {
        buf = new MatOfByte();
        img = new Mat();

        try {
            mjpegHttpReader = new MjpegHttpReader(url);
            mjpegHttpReader.start();
        } catch (Exception e) {
            logger.error("Error while initializing MjpegHttpReader", e);
            return false;
        }

        try {
            iterator = mjpegHttpReader.iterator();
        } catch (Exception e) {
            logger.error("Error while getting MjpegHttpReader iterator", e);
            return false;
        }

        logger.info("HttpSource initialized");

        return mjpegHttpReader != null && iterator != null;
    }

    byte[] frame;

    @Override
    public Mat update() {
        if (mjpegHttpReader == null) return null;

        boolean result = InputSourceInitializer.INSTANCE.runWithTimeout(name, () -> {
            frame = iterator.next();
            return frame != null;
        });

        if(!result) {
            return null;
        }

        if(!dataIsValidJPEG(frame)) {
            logger.error("Received data is not a valid JPEG image");
            return null;
        }

        buf.fromArray(frame);

        if(buf.empty()) {
            return null;
        }

        Mat mat = Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_COLOR);
        Imgproc.cvtColor(mat, img, Imgproc.COLOR_BGR2RGBA);

        mat.release();

        capTimeNanos = System.nanoTime();

        return img;
    }

    @Override
    public void reset() {
        if (mjpegHttpReader != null) {
            mjpegHttpReader.stop();
            mjpegHttpReader = null;
        }
    }

    @Override
    public void close() {
        reset();
    }

    @Override
    public void onPause() {
        if (mjpegHttpReader != null) {
            reset();
        }
    }

    @Override
    public void onResume() {
        InputSourceInitializer.INSTANCE.runWithTimeout(name, eocvSim.inputSourceManager, this::init);
    }

    @Override
    protected InputSource internalCloneSource() {
        return new HttpSource(url);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public FileFilter getFileFilters() {
        return null;
    }

    @Override
    public long getCaptureTimeNanos() {
        return capTimeNanos;
    }

    @Override
    public String toString() {
        return "HttpSource(" + url + ")";
    }

    private static boolean dataIsValidJPEG(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }

        int totalBytes = getJPEGSize(data, data.length);

        if (totalBytes == -1) {
            return false;
        }

        return (data[0] == (byte) 0xFF &&
                data[1] == (byte) 0xD8 &&
                data[totalBytes - 2] == (byte) 0xFF &&
                data[totalBytes - 1] == (byte) 0xD9);
    }

    private static int getJPEGSize(byte[] data, int maxLength) {
        if (data == null || maxLength < 4) {
            return -1; // Invalid or too small to be a JPEG
        }

        // Check for SOI marker
        if (data[0] != (byte) 0xFF || data[1] != (byte) 0xD8) {
            return -1; // Not a JPEG
        }

        int pos = 2; // Start after SOI

        while (pos < maxLength - 2) {
            // Look for the next marker (0xFF xx)
            if (data[pos] == (byte) 0xFF) {
                byte marker = data[pos + 1];

                // End of Image (EOI) found
                if (marker == (byte) 0xD9) {
                    return pos + 2; // JPEG size
                }

                // Skip padding bytes (some JPEGs use 0xFF 0x00)
                if (marker == (byte) 0x00) {
                    pos++;
                    continue;
                }

                // Most markers have a 2-byte length field
                if ((marker >= (byte) 0xC0 && marker <= (byte) 0xFE) && marker != (byte) 0xD9) {
                    if (pos + 3 >= maxLength) {
                        return -1; // Incomplete JPEG
                    }

                    // Read segment length (big-endian)
                    int segmentLength = ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);

                    if (segmentLength < 2 || pos + segmentLength >= maxLength) {
                        return -1; // Corrupt or incomplete JPEG
                    }

                    pos += segmentLength; // Move to next marker
                } else {
                    pos++; // Skip unknown byte
                }
            } else {
                pos++; // Continue searching
            }
        }

        return -1; // No valid JPEG end found
    }
}