package io.github.deltacv.vision.external

import android.graphics.Canvas
import org.openftc.easyopencv.OpenCvViewport
import org.openftc.easyopencv.OpenCvViewport.RenderHook

object PipelineRenderHook : RenderHook {
    override fun onDrawFrame(canvas: Canvas, onscreenWidth: Int, onscreenHeight: Int, scaleBmpPxToCanvasPx: Float, canvasDensityScale: Float, userContext: Any) {
        val frameContext = userContext as OpenCvViewport.FrameContext

        // We must make sure that we call onDrawFrame() for the same pipeline which set the
        // context object when requesting a draw hook. (i.e. we can't just call onDrawFrame()
        // for whatever pipeline happens to be currently attached; it might have an entirely
        // different notion of what to expect in the context object)
        if (frameContext.generatingPipeline != null) {
            frameContext.generatingPipeline.onDrawFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, canvasDensityScale, frameContext.userContext)
        }
    }
}
