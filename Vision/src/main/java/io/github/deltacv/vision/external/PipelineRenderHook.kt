/*
 * Copyright (c) 2023 Sebastian Erives
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

package io.github.deltacv.vision.external

import android.graphics.Canvas
import org.openftc.easyopencv.OpenCvViewport
import org.openftc.easyopencv.OpenCvViewport.RenderHook

object PipelineRenderHook : RenderHook {
    override fun onDrawFrame(canvas: Canvas, onscreenWidth: Int, onscreenHeight: Int, scaleBmpPxToCanvasPx: Float, canvasDensityScale: Float, userContext: Any) {
        val frameContext = userContext as OpenCvViewport.FrameContext

        // We must make sure that we call onDrawFrame() for the same pipeline that set the
        // context object when requesting a draw hook. (i.e. we can't just call onDrawFrame()
        // for whatever pipeline happens to be currently attached; it might have an entirely
        // different notion of what to expect in the context object)
        if (frameContext.generatingPipeline != null) {
            frameContext.generatingPipeline.onDrawFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, canvasDensityScale, frameContext.userContext)
        }
    }
}
