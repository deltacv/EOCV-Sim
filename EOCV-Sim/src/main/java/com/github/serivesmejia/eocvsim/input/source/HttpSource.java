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
import io.github.deltacv.papervision.plugin.ipc.stream.MjpegHttpReader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.Videoio;
import org.openftc.easyopencv.MatRecycler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.util.Iterator;

public class HttpSource extends InputSource {

    String url;

    private volatile transient MatRecycler matRecycler = new MatRecycler(6);

    MjpegHttpReader mjpegHttpReader = null;

    Logger logger = LoggerFactory.getLogger(HttpSource.class);

    MatOfByte buf = new MatOfByte();
    Mat img = new Mat();

    Iterator<byte[]> iterator;

    long capTimeNanos = 0;

    public HttpSource(String url) {
        this.url = url;
    }

    @Override
    public boolean init() {
        try {
            mjpegHttpReader = new MjpegHttpReader(url);
            mjpegHttpReader.start();
        } catch (IOException e) {
            logger.error("Error while initializing MjpegHttpReader", e);
        }

        iterator = mjpegHttpReader.iterator();

        return mjpegHttpReader != null;
    }

    @Override
    public Mat update() {
        if (mjpegHttpReader == null) return null;

        if (img == null) return null;

        byte[] frame = iterator.next();
        buf.put(0, 0, frame);

        Mat mat = Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_UNCHANGED);
        mat.copyTo(img);
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
}