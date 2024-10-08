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

import android.graphics.Canvas;
import org.opencv.core.Mat;

public abstract class OpenCvPipeline {

    private Object userContext;
    private boolean isFirstFrame = true;
    private long firstFrameTimestamp;

    public abstract Mat processFrame(Mat input);

    public void onViewportTapped() { }

    public void init(Mat mat) { }
    public Object getUserContextForDrawHook()
    {
        return userContext;
    }

    /**
     * Call this during processFrame() to request a hook during the viewport's
     * drawing operation of the current frame (which will happen asynchronously
     * at some future time) using the Canvas API.
     *
     * If you call this more than once during processFrame(), the last call takes
     * precedence. You will only get a single draw hook for a given frame.
     *
     * @param userContext anything you want :monkey: will be passed back to you
     * in {@link #onDrawFrame(Canvas, int, int, float, float, Object)}. You can
     * use this to store information about what you found in the frame, so that
     * you know what to draw when it's time. (Otherwise how the heck would you
     * know what to draw??).
     */
    public void requestViewportDrawHook(Object userContext)
    {
        this.userContext = userContext;
    }

    /**
     * Called during the viewport's frame rendering operation at some later point after
     * you called called {@link #requestViewportDrawHook(Object)} during processFrame().
     * Allows you to use the Canvas API to draw annotations on the frame, rather than
     * using OpenCV calls. This allows for more eye-candy-y annotations since you've got
     * a high resolution canvas to work with rather than, say, a 320x240 image.
     *
     * Note that this is NOT called from the same thread that calls processFrame()!
     * And may actually be called from the UI thread depending on the viewport renderer.
     *
     * @param canvas the canvas that's being drawn on NOTE: Do NOT get dimensions from it, use below
     * @param onscreenWidth the width of the canvas that corresponds to the image
     * @param onscreenHeight the height of the canvas that corresponds to the image
     * @param scaleBmpPxToCanvasPx multiply pixel coords by this to scale to canvas coords
     * @param scaleCanvasDensity a scaling factor to adjust e.g. text size. Relative to Nexus5 DPI.
     * @param userContext whatever you passed in when requesting the draw hook :monkey:
     */
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {};

    Mat processFrameInternal(Mat input)
    {
        if(isFirstFrame)
        {
            init(input);

            firstFrameTimestamp = System.currentTimeMillis();
            isFirstFrame = false;
        }

        return processFrame(input);
    }

}