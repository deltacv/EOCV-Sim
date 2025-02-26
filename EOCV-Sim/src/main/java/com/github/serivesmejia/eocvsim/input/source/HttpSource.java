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
import com.google.gson.annotations.Expose;
import io.github.deltacv.papervision.plugin.ipc.stream.MjpegHttpReader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.Videoio;
import org.openftc.easyopencv.MatRecycler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
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

    @Override
    public Mat update() {
        if (mjpegHttpReader == null) return null;

        byte[] frame = iterator.next();

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
            mjpegHttpReader.stop();
        }
    }

    @Override
    public void onResume() {
        if (mjpegHttpReader != null) {
            mjpegHttpReader.start();
        }
    }

    @Override
    protected InputSource internalCloneSource() {
        return new HttpSource(url);
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

        int totalBytes = data.length;
        return (data[0] == (byte) 0xFF &&
                data[1] == (byte) 0xD8 &&
                data[totalBytes - 2] == (byte) 0xFF &&
                data[totalBytes - 1] == (byte) 0xD9);
    }
}