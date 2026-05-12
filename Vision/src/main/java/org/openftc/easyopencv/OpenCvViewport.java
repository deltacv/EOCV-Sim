/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv;

import android.graphics.Canvas;

import org.opencv.core.Mat;

public interface OpenCvViewport
{
    enum OptimizedRotation
    {
        NONE(0),
        ROT_90_COUNTERCLOCWISE(90),
        ROT_90_CLOCKWISE(-90),
        ROT_180(180);

        int val;

        OptimizedRotation(int val)
        {
            this.val = val;
        }
    }

    interface RenderHook
    {
        void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float canvasDensityScale, Object userContext);
    }

    void setFpsMeterEnabled(boolean enabled);
    void pause();
    void resume();

    void activate();
    void deactivate();

    void setSize(int width, int height);
    void setOptimizedViewRotation(OptimizedRotation rotation);

    void notifyStatistics(float fps, int pipelineMs, int overheadMs);
    void setRecording(boolean recording);

    void post(Mat frame, Object userContext);
    void setRenderingPolicy(OpenCvCamera.ViewportRenderingPolicy policy);
    void setRenderHook(RenderHook renderHook);

    class FrameContext
    {
        public OpenCvPipeline generatingPipeline;
        public Object userContext;

        public FrameContext(OpenCvPipeline generatingPipeline, Object userContext)
        {
            this.generatingPipeline = generatingPipeline;
            this.userContext = userContext;
        }
    }
}
