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

package com.github.serivesmejia.eocvsim.input.source;

import com.github.serivesmejia.eocvsim.gui.Visualizer;
import com.github.serivesmejia.eocvsim.input.InputSource;
import com.github.serivesmejia.eocvsim.util.FileFilters;
import com.google.gson.annotations.Expose;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.openftc.easyopencv.MatRecycler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileFilter;
import java.util.Objects;

public class VideoSource extends InputSource {

    @Expose
    private String videoPath = null;

    private transient VideoCapture video = null;

    private transient MatRecycler.RecyclableMat lastFramePaused = null;
    private transient MatRecycler.RecyclableMat lastFrame = null;

    private transient boolean initialized = false;

    @Expose
    private volatile Size size;

    private volatile transient MatRecycler matRecycler = null;

    private transient double lastFramePosition = 0;

    private transient long capTimeNanos = 0;

    Logger logger = LoggerFactory.getLogger(getClass());

    public VideoSource() {}

    public VideoSource(String videoPath, Size size) {
        this.videoPath = videoPath;
        this.size = size;
    }

    @Override
    public boolean init() {

        if (initialized) return false;
        initialized = true;

        video = new VideoCapture();
        video.open(videoPath);

        if (!video.isOpened()) {
            logger.error("Unable to open video " + videoPath);
            return false;
        }

        if (matRecycler == null) matRecycler = new MatRecycler(4);

        MatRecycler.RecyclableMat newFrame = matRecycler.takeMat();
        newFrame.release();

        video.read(newFrame);

        if (newFrame.empty()) {
            logger.error("Unable to open video " + videoPath + ", returned Mat was empty.");
            return false;
        }

        newFrame.release();
        matRecycler.returnMat(newFrame);

        return true;

    }

    @Override
    public void reset() {

        if (!initialized) return;

        if (video != null && video.isOpened()) video.release();

        if(lastFrame != null && lastFrame.isCheckedOut())
            lastFrame.returnMat();
        if(lastFramePaused != null && lastFramePaused.isCheckedOut())
            lastFramePaused.returnMat();

        matRecycler.releaseAll();

        video = null;
        initialized = false;

    }

    @Override
    public void close() {

        if(video != null && video.isOpened()) video.release();
        if(lastFrame != null) lastFrame.returnMat();

        if (lastFramePaused != null) {
            lastFramePaused.returnMat();
            lastFramePaused = null;
        }

    }

    @Override
    public Mat update() {

        if (isPaused) {
            return lastFramePaused;
        } else if (lastFramePaused != null) {
            lastFramePaused.returnMat();
            lastFramePaused = null;
        }

        if (lastFrame == null) lastFrame = matRecycler.takeMat();
        if (video == null) return lastFrame;

        MatRecycler.RecyclableMat newFrame = matRecycler.takeMat();

        video.read(newFrame);
        capTimeNanos = System.nanoTime();

        //with videocapture for video files, when an empty mat is returned
        //the most likely reason is that the video ended, so we set the
        //playback position back to 0 for looping in here and start over
        //in next update
        if (newFrame.empty()) {
            newFrame.returnMat();
            video.set(Videoio.CAP_PROP_POS_FRAMES, 0);
            return lastFrame;
        }

        if (size == null) size = lastFrame.size();

        Imgproc.cvtColor(newFrame, lastFrame, Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(lastFrame, lastFrame, size, 0.0, 0.0, Imgproc.INTER_AREA);

        matRecycler.returnMat(newFrame);

        return lastFrame;

    }

    @Override
    public void onPause() {

        if (lastFrame != null) lastFrame.release();
        if (lastFramePaused == null) lastFramePaused = matRecycler.takeMat();

        video.read(lastFramePaused);

        Imgproc.cvtColor(lastFramePaused, lastFramePaused, Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(lastFramePaused, lastFramePaused, size, 0.0, 0.0, Imgproc.INTER_AREA);

        update();

        lastFramePosition = video.get(Videoio.CAP_PROP_POS_FRAMES);

        video.release();
        video = null;

    }

    @Override
    public void onResume() {
        video = new VideoCapture();
        video.open(videoPath);
        video.set(Videoio.CAP_PROP_POS_FRAMES, lastFramePosition);
    }

    @Override
    protected InputSource internalCloneSource() {
        return new VideoSource(videoPath, size);
    }

    @Override
    public FileFilter getFileFilters() {
        return FileFilters.videoMediaFilter;
    }

    @Override
    public long getCaptureTimeNanos() {
        return capTimeNanos;
    }

    @Override
    public String toString() {
        return "VideoSource(" + videoPath + ", " + (size != null ? size.toString() : "null") + ")";
    }

}