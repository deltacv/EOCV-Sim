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

import com.github.sarxos.webcam.Webcam;
import com.github.serivesmejia.eocvsim.gui.Visualizer;
import com.github.serivesmejia.eocvsim.input.InputSource;
import com.github.serivesmejia.eocvsim.util.Log;
import com.github.serivesmejia.eocvsim.util.StrUtil;
import com.google.gson.annotations.Expose;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.openftc.easyopencv.MatRecycler;

import javax.swing.filechooser.FileFilter;
import java.util.List;

public class CameraSource extends InputSource {

    // for global use, -1 means no webcam currently in use
    public static int currentWebcamIndex = -1;

    protected int webcamIndex;

    @Expose
    protected String webcamName = null;

    private transient VideoCapture camera = null;

    private transient MatRecycler.RecyclableMat lastFramePaused = null;
    private transient MatRecycler.RecyclableMat lastFrame = null;

    private transient boolean initialized = false;

    protected boolean isLegacyByIndex = false;

    @Expose
    protected volatile Size size;

    private volatile transient MatRecycler matRecycler;

    private transient long capTimeNanos = 0;

    public CameraSource(String webcamName, Size size) {
        this.webcamName = webcamName;
        this.size = size;
    }

    public CameraSource(int webcamIndex, Size size) {
        this.webcamIndex = webcamIndex;
        this.size = size;
        isLegacyByIndex = true;
    }

    @Override
    public boolean init() {

        if (initialized) return false;
        initialized = true;

        if(webcamName != null) {
            Webcam.resetDriver();
            List<Webcam> webcams = Webcam.getWebcams();

            boolean foundWebcam = false;

            for(int i = 0 ; i < webcams.size() ; i++) {
                String name = webcams.get(i).getName();
                double similarity = StrUtil.similarity(name, webcamName);

                if(name.equals(webcamName) || similarity > 0.6) {
                    System.out.println(name + " " + webcamName + " similarity " + similarity + " " + i);
                    webcamIndex = i;
                    foundWebcam = true;
                    break;
                }
            }

            if(!foundWebcam) {
                Log.error("CameraSource", "Could not find webcam " + webcamName);
                return false;
            }
        }

        camera = new VideoCapture();
        camera.open(webcamIndex);

        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, size.width);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, size.height);

        if (!camera.isOpened()) {
            Log.error("CameraSource", "Unable to open camera " + webcamIndex);
            return false;
        }

        if (matRecycler == null) matRecycler = new MatRecycler(4);

        MatRecycler.RecyclableMat newFrame = matRecycler.takeMat();

        camera.read(newFrame);

        if (newFrame.empty()) {
            Log.error("CameraSource", "Unable to open camera " + webcamIndex + ", returned Mat was empty.");
            newFrame.release();
            return false;
        }

        matRecycler.returnMat(newFrame);

        currentWebcamIndex = webcamIndex;

        return true;

    }

    @Override
    public void reset() {

        if (!initialized) return;
        if (camera != null && camera.isOpened()) camera.release();

        if(lastFrame != null && lastFrame.isCheckedOut())
            lastFrame.returnMat();
        if(lastFramePaused != null && lastFramePaused.isCheckedOut())
            lastFramePaused.returnMat();

        camera = null;
        initialized = false;

    }

    @Override
    public void close() {
        if (camera != null && camera.isOpened()) camera.release();
        currentWebcamIndex = -1;
    }

    @Override
    public Mat update() {

        if (isPaused) {
            return lastFramePaused;
        } else if (lastFramePaused != null) {
            lastFramePaused.release();
            lastFramePaused.returnMat();
            lastFramePaused = null;
        }

        if (lastFrame == null) lastFrame = matRecycler.takeMat();
        if (camera == null) return lastFrame;

        MatRecycler.RecyclableMat newFrame = matRecycler.takeMat();

        camera.read(newFrame);
        capTimeNanos = System.nanoTime();

        if (newFrame.empty()) {
            newFrame.returnMat();
            return lastFrame;
        }

        if (size == null) size = lastFrame.size();

        Imgproc.cvtColor(newFrame, lastFrame, Imgproc.COLOR_BGR2RGB);

        newFrame.release();
        newFrame.returnMat();

        return lastFrame;

    }

    @Override
    public void onPause() {

        if (lastFrame != null) lastFrame.release();
        if (lastFramePaused == null) lastFramePaused = matRecycler.takeMat();

        camera.read(lastFramePaused);

        Imgproc.cvtColor(lastFramePaused, lastFramePaused, Imgproc.COLOR_BGR2RGB);

        update();

        camera.release();
        camera = null;

        currentWebcamIndex = -1;
    }

    @Override
    public void onResume() {

        Visualizer.AsyncPleaseWaitDialog apwdCam = eocvSim.inputSourceManager.showApwdIfNeeded(name);

        camera = new VideoCapture();
        camera.open(webcamIndex);

        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, size.width);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, size.height);

        currentWebcamIndex = webcamIndex;

        apwdCam.destroyDialog();
    }

    @Override
    protected InputSource internalCloneSource() {
        return new CameraSource(webcamName, size);
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
        if (size == null) size = new Size();
        return "CameraSource(" + webcamName + ", " + (size != null ? size.toString() : "null") + ")";
    }

}