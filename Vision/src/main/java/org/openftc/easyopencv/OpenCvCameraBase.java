/*
 * Copyright (c) 2019 OpenFTC Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openftc.easyopencv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.MovingStatistics;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public abstract class OpenCvCameraBase implements OpenCvCamera {

    private OpenCvPipeline pipeline = null;

    private MovingStatistics msFrameIntervalRollingAverage;
    private MovingStatistics msUserPipelineRollingAverage;
    private MovingStatistics msTotalFrameProcessingTimeRollingAverage;
    private ElapsedTime timer;

    private OpenCvViewport viewport;
    private OpenCvCameraRotation rotation;

    private final Object pipelineChangeLock = new Object();

    private Mat rotatedMat = new Mat();
    private Mat matToUseIfPipelineReturnedCropped;
    private Mat croppedColorCvtedMat = new Mat();

    private ViewportRenderer desiredViewportRenderer = ViewportRenderer.SOFTWARE;
    ViewportRenderingPolicy desiredRenderingPolicy = ViewportRenderingPolicy.MAXIMIZE_EFFICIENCY;
    boolean fpsMeterDesired = true;

    private Scalar brown = new Scalar(82, 61, 46, 255);

    private int frameCount = 0;
    private float avgFps;
    private int avgPipelineTime;
    private int avgOverheadTime;
    private int avgTotalFrameTime;
    private long currentFrameStartTime;

    @Override
    public void showFpsMeterOnViewport(boolean show) {
        viewport.setFpsMeterEnabled(show);
    }

    @Override
    public void pauseViewport() {
        viewport.pause();
    }

    @Override
    public void resumeViewport() {
        viewport.resume();
    }

    @Override
    public void setViewportRenderingPolicy(ViewportRenderingPolicy policy) {
        viewport.setRenderingPolicy(policy);
    }

    @Override
    public void setViewportRenderer(ViewportRenderer renderer) {
        this.desiredViewportRenderer = renderer;
    }

    @Override
    public void setPipeline(OpenCvPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public int getFrameCount() {
        return 0;
    }

    @Override
    public float getFps() {
        return 0;
    }

    @Override
    public int getPipelineTimeMs() {
        return 0;
    }

    @Override
    public int getOverheadTimeMs() {
        return 0;
    }

    @Override
    public int getTotalFrameTimeMs() {
        return 0;
    }

    @Override
    public int getCurrentPipelineMaxFps() {
        return 0;
    }

    @Override
    public void startRecordingPipeline(PipelineRecordingParameters parameters) {

    }

    @Override
    public void stopRecordingPipeline() {

    }

    protected synchronized void handleFrameUserCrashable(Mat frame, long timestamp) {
        msFrameIntervalRollingAverage.add(timer.milliseconds());
        timer.reset();
        double secondsPerFrame = msFrameIntervalRollingAverage.getMean() / 1000d;
        avgFps = (float) (1d / secondsPerFrame);
        Mat userProcessedFrame = null;

        int rotateCode = mapRotationEnumToOpenCvRotateCode(rotation);

        if (rotateCode != -1) {
            /*
             * Rotate onto another Mat rather than doing so in-place.
             *
             * This does two things:
             *     1) It seems that rotating by 90 or 270 in-place
             *        causes the backing buffer to be re-allocated
             *        since the width/height becomes swapped. This
             *        causes a problem for user code which makes a
             *        submat from the input Mat, because after the
             *        parent Mat is re-allocated the submat is no
             *        longer tied to it. Thus, by rotating onto
             *        another Mat (which is never re-allocated) we
             *        remove that issue.
             *
             *     2) Since the backing buffer does need need to be
             *        re-allocated for each frame, we reduce overhead
             *        time by about 1ms.
             */
            Core.rotate(frame, rotatedMat, rotateCode);
            frame = rotatedMat;
        }

        final OpenCvPipeline pipelineSafe;

        // Grab a safe reference to what the pipeline currently is,
        // since the user is allowed to change it at any time
        synchronized (pipelineChangeLock) {
            pipelineSafe = pipeline;
        }

        if (pipelineSafe != null) {
            if (pipelineSafe instanceof TimestampedOpenCvPipeline) {
                ((TimestampedOpenCvPipeline) pipelineSafe).setTimestamp(timestamp);
            }

            long pipelineStart = System.currentTimeMillis();
            userProcessedFrame = pipelineSafe.processFrameInternal(frame);
            msUserPipelineRollingAverage.add(System.currentTimeMillis() - pipelineStart);
        }

        // Will point to whatever mat we end up deciding to send to the screen
        final Mat matForDisplay;

        if (pipelineSafe == null) {
            matForDisplay = frame;
        } else if (userProcessedFrame == null) {
            throw new OpenCvCameraException("User pipeline returned null");
        } else if (userProcessedFrame.empty()) {
            throw new OpenCvCameraException("User pipeline returned empty mat");
        } else if (userProcessedFrame.cols() != frame.cols() || userProcessedFrame.rows() != frame.rows()) {
            /*
             * The user didn't return the same size image from their pipeline as we gave them,
             * ugh. This makes our lives interesting because we can't just send an arbitrary
             * frame size to the viewport. It re-uses framebuffers that are of a fixed resolution.
             * So, we copy the user's Mat onto a Mat of the correct size, and then send that other
             * Mat to the viewport.
             */

            if (userProcessedFrame.cols() > frame.cols() || userProcessedFrame.rows() > frame.rows()) {
                /*
                 * What on earth was this user thinking?! They returned a Mat that's BIGGER in
                 * a dimension than the one we gave them!
                 */

                throw new OpenCvCameraException("User pipeline returned frame of unexpected size");
            }

            //We re-use this buffer, only create if needed
            if (matToUseIfPipelineReturnedCropped == null) {
                matToUseIfPipelineReturnedCropped = frame.clone();
            }

            //Set to brown to indicate to the user the areas which they cropped off
            matToUseIfPipelineReturnedCropped.setTo(brown);

            int usrFrmTyp = userProcessedFrame.type();

            if (usrFrmTyp == CvType.CV_8UC1) {
                /*
                 * Handle 8UC1 returns (masks and single channels of images);
                 *
                 * We have to color convert onto a different mat (rather than
                 * doing so in place) to avoid breaking any of the user's submats
                 */
                Imgproc.cvtColor(userProcessedFrame, croppedColorCvtedMat, Imgproc.COLOR_GRAY2RGBA);
                userProcessedFrame = croppedColorCvtedMat; //Doesn't affect user's handle, only ours
            } else if (usrFrmTyp != CvType.CV_8UC4 && usrFrmTyp != CvType.CV_8UC3) {
                /*
                 * Oof, we don't know how to handle the type they gave us
                 */
                throw new OpenCvCameraException("User pipeline returned a frame of an illegal type. Valid types are CV_8UC1, CV_8UC3, and CV_8UC4");
            }

            //Copy the user's frame onto a Mat of the correct size
            userProcessedFrame.copyTo(matToUseIfPipelineReturnedCropped.submat(
                    new Rect(0, 0, userProcessedFrame.cols(), userProcessedFrame.rows())));

            //Send that correct size Mat to the viewport
            matForDisplay = matToUseIfPipelineReturnedCropped;
        } else {
            /*
             * Yay, smart user! They gave us the frame size we were expecting!
             * Go ahead and send it right on over to the viewport.
             */
            matForDisplay = userProcessedFrame;
        }

        if (viewport != null) {
            viewport.post(matForDisplay, new OpenCvViewport.FrameContext(pipelineSafe, pipelineSafe != null ? pipelineSafe.getUserContextForDrawHook() : null));
        }

        avgPipelineTime = (int) Math.round(msUserPipelineRollingAverage.getMean());
        avgTotalFrameTime = (int) Math.round(msTotalFrameProcessingTimeRollingAverage.getMean());
        avgOverheadTime = avgTotalFrameTime - avgPipelineTime;

        if (viewport != null) {
            viewport.notifyStatistics(avgFps, avgPipelineTime, avgOverheadTime);
        }

        frameCount++;

        msTotalFrameProcessingTimeRollingAverage.add(System.currentTimeMillis() - currentFrameStartTime);
    }


    protected OpenCvViewport.OptimizedRotation getOptimizedViewportRotation(OpenCvCameraRotation streamRotation) {
        if (!cameraOrientationIsTiedToDeviceOrientation()) {
            return OpenCvViewport.OptimizedRotation.NONE;
        }

        if (streamRotation == OpenCvCameraRotation.SIDEWAYS_LEFT || streamRotation == OpenCvCameraRotation.SENSOR_NATIVE) {
            return OpenCvViewport.OptimizedRotation.ROT_90_COUNTERCLOCWISE;
        } else if (streamRotation == OpenCvCameraRotation.SIDEWAYS_RIGHT) {
            return OpenCvViewport.OptimizedRotation.ROT_90_CLOCKWISE;
        } else if (streamRotation == OpenCvCameraRotation.UPSIDE_DOWN) {
            return OpenCvViewport.OptimizedRotation.ROT_180;
        } else {
            return OpenCvViewport.OptimizedRotation.NONE;
        }
    }

    protected abstract OpenCvViewport setupViewport();

    protected abstract OpenCvCameraRotation getDefaultRotation();

    protected abstract int mapRotationEnumToOpenCvRotateCode(OpenCvCameraRotation rotation);

    protected abstract boolean cameraOrientationIsTiedToDeviceOrientation();

    protected abstract boolean isStreaming();

}
