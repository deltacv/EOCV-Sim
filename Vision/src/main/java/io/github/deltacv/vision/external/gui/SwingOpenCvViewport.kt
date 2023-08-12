/*
 * Copyright (c) 2023 OpenFTC Team & Sebastian Erives
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
package io.github.deltacv.vision.external.gui

import android.graphics.Bitmap
import android.graphics.Canvas
import io.github.deltacv.common.image.DynamicBufferedImageRecycler
import io.github.deltacv.common.image.MatPoster
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue
import org.jetbrains.skia.Color
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.GenericSkikoView
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoView
import org.jetbrains.skiko.swing.SkiaSwingLayer
import org.opencv.core.Mat
import org.opencv.core.Size
import org.openftc.easyopencv.MatRecycler
import org.openftc.easyopencv.OpenCvCamera.ViewportRenderingPolicy
import org.openftc.easyopencv.OpenCvViewRenderer
import org.openftc.easyopencv.OpenCvViewport
import org.openftc.easyopencv.OpenCvViewport.OptimizedRotation
import org.openftc.easyopencv.OpenCvViewport.RenderHook
import org.slf4j.LoggerFactory
import java.awt.GridBagLayout
import java.awt.image.BufferedImage
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class SwingOpenCvViewport(size: Size, fpsMeterDescriptor: String = "deltacv Vision") : OpenCvViewport, MatPoster {

    private val syncObj = Any()

    @Volatile
    private var userRequestedActive = false

    @Volatile
    private var userRequestedPause = false
    private val needToDeactivateRegardlessOfUser = false
    private var surfaceExistsAndIsReady = false

    @Volatile
    private var useGpuCanvas = false

    private enum class RenderingState {
        STOPPED,
        ACTIVE,
        PAUSED
    }

    private val visionPreviewFrameQueue = EvictingBlockingQueue(ArrayBlockingQueue<MatRecycler.RecyclableMat>(VISION_PREVIEW_FRAME_QUEUE_CAPACITY))
    private var framebufferRecycler: MatRecycler? = null

    @Volatile
    private var internalRenderingState = RenderingState.STOPPED
    val renderer: OpenCvViewRenderer = OpenCvViewRenderer(false, fpsMeterDescriptor)

    private val skiaLayer = SkiaLayer()
    val component: JComponent get() = skiaLayer

    var logger = LoggerFactory.getLogger(javaClass)

    private var renderHook: RenderHook? = null

    init {
        visionPreviewFrameQueue.setEvictAction { value: MatRecycler.RecyclableMat? ->
            /*
             * If a Mat is evicted from the queue, we need
             * to make sure to return it to the Mat recycler
             */
            framebufferRecycler!!.returnMat(value)
        }

        skiaLayer.skikoView = GenericSkikoView(skiaLayer, object: SkikoView {
            override fun onRender(canvas: org.jetbrains.skia.Canvas, width: Int, height: Int, nanoTime: Long) {
                renderCanvas(Canvas(canvas, width, height))
            }
        })


        setSize(size.width.toInt(), size.height.toInt())
    }

    var shouldPaintOrange = true

    fun attachTo(component: Any) {
        skiaLayer.attachTo(component)

        SwingUtilities.invokeLater {
            skiaLayer.needRedraw()
        }
    }

    fun skiaPanel() = SkiaPanel(skiaLayer)

    override fun setSize(width: Int, height: Int) {
        synchronized(syncObj) {
            check(internalRenderingState == RenderingState.STOPPED) { "Cannot set size while renderer is active!" }

            //Make sure we don't have any mats hanging around
            //from when we might have been running before
            visionPreviewFrameQueue.clear()
            framebufferRecycler = MatRecycler(FRAMEBUFFER_RECYCLER_CAPACITY)

            SwingUtilities.invokeLater {
                skiaLayer.setSize(width, height)
                skiaLayer.repaint()
            }

            surfaceExistsAndIsReady = true
            checkState()
        }
    }

    override fun setFpsMeterEnabled(enabled: Boolean) {}
    override fun resume() {
        synchronized(syncObj) {
            userRequestedPause = false
            checkState()
        }
    }

    override fun pause() {
        synchronized(syncObj) {
            userRequestedPause = true
            checkState()
        }
    }

    /***
     * Activate the render thread
     */
    @Synchronized
    override fun activate() {
        synchronized(syncObj) {
            userRequestedActive = true
            checkState()
        }
    }

    /***
     * Deactivate the render thread
     */
    override fun deactivate() {
        synchronized(syncObj) {
            userRequestedActive = false
            checkState()
        }
    }

    override fun setOptimizedViewRotation(rotation: OptimizedRotation) {}
    override fun notifyStatistics(fps: Float, pipelineMs: Int, overheadMs: Int) {
        renderer.notifyStatistics(fps, pipelineMs, overheadMs)
    }

    override fun setRecording(recording: Boolean) {}
    override fun post(mat: Mat, userContext: Any) {
        synchronized(syncObj) {
            //did they give us null?
            requireNotNull(mat) {
                //ugh, they did
                "cannot post null mat!"
            }

            //Are we actually rendering to the display right now? If not,
            //no need to waste time doing a memcpy
            if (internalRenderingState == RenderingState.ACTIVE) {
                /*
                 * We need to copy this mat before adding it to the queue,
                 * because the pointer that was passed in here is only known
                 * to be pointing to a certain frame while we're executing.
                 */

                /*
                 * Grab a framebuffer Mat from the recycler
                 * instead of doing a new alloc and then having
                 * to free it after rendering/eviction from queue
                 */
                val matToCopyTo = framebufferRecycler!!.takeMat()

                mat.copyTo(matToCopyTo)
                matToCopyTo.context = userContext

                visionPreviewFrameQueue.offer(matToCopyTo)
            }
        }
    }

    /*
     * Called with syncObj held
     */
    fun checkState() {
        /*
         * If the surface isn't ready, don't do anything
         */
        if (!surfaceExistsAndIsReady) {
            logger.info("CheckState(): surface not ready or doesn't exist")
            return
        }

        /*
         * Does the user want us to stop?
         */if (!userRequestedActive || needToDeactivateRegardlessOfUser) {
            if (needToDeactivateRegardlessOfUser) {
                logger.info("CheckState(): lifecycle mandates deactivation regardless of user")
            } else {
                logger.info("CheckState(): user requested that we deactivate")
            }

            /*
             * We only need to stop the render thread if it's not
             * already stopped
             */if (internalRenderingState != RenderingState.STOPPED) {
                logger.info("CheckState(): deactivating viewport")

                /*
                 * Wait for him to die non-interuptibly
                 */
                internalRenderingState = RenderingState.STOPPED
            } else {
                logger.info("CheckState(): already deactivated")
            }
        } else if (userRequestedActive) {
            logger.info("CheckState(): user requested that we activate")

            /*
             * We only need to start the render thread if it's
             * stopped.
             */if (internalRenderingState == RenderingState.STOPPED) {
                logger.info("CheckState(): activating viewport")
                internalRenderingState = RenderingState.PAUSED
                internalRenderingState = if (userRequestedPause) {
                    RenderingState.PAUSED
                } else {
                    RenderingState.ACTIVE
                }
            } else {
                logger.info("CheckState(): already activated")
            }
        }
        if (internalRenderingState != RenderingState.STOPPED) {
            if (userRequestedPause && internalRenderingState != RenderingState.PAUSED
                    || !userRequestedPause && internalRenderingState != RenderingState.ACTIVE) {
                internalRenderingState = if (userRequestedPause) {
                    logger.info("CheckState(): pausing viewport")
                    RenderingState.PAUSED
                } else {
                    logger.info("CheckState(): resuming viewport")
                    RenderingState.ACTIVE
                }

                /*
                 * Interrupt him so that he's not stuck looking at his frame queue.
                 * (We stop filling the frame queue if the user requested pause so
                 * we aren't doing pointless memcpys)
                 */
            }
        }
    }

    private val canvasLock = Any()
    private lateinit var lastFrame: MatRecycler.RecyclableMat

    private fun renderCanvas(canvas: Canvas) {
        if(!::lastFrame.isInitialized) {
            lastFrame = framebufferRecycler!!.takeMat()
        }

        synchronized(canvasLock) {
            when (internalRenderingState) {
                RenderingState.ACTIVE -> {
                    shouldPaintOrange = true

                    val mat: MatRecycler.RecyclableMat = try {
                        //Grab a Mat from the frame queue
                        val frame = visionPreviewFrameQueue.poll(10, TimeUnit.MILLISECONDS) ?: lastFrame

                        frame
                    } catch (e: InterruptedException) {

                        //Note: we actually don't re-interrupt ourselves here, because interrupts are also
                        //used to simply make sure we properly pick up a transition to the PAUSED state, not
                        //just when we're trying to close. If we're trying to close, then exitRequested will
                        //be set, and since we break immediately right here, the close will be handled cleanly.
                        //Thread.currentThread().interrupt();
                        return
                    }

                    mat.copyTo(lastFrame)

                    if (mat.empty()) {
                        return // nope out
                    }

                    /*
                 * For some reason, the canvas will very occasionally be null upon closing.
                 * Stack Overflow seems to suggest this means the canvas has been destroyed.
                 * However, surfaceDestroyed(), which is called right before the surface is
                 * destroyed, calls checkState(), which *SHOULD* block until we die. This
                 * works most of the time, but not always? We don't yet understand...
                 */
                    if (canvas != null) {
                        renderer.render(mat, canvas, renderHook, mat.context)
                    } else {
                        logger.info("Canvas was null")
                    }

                    //We're done with that Mat object; return it to the Mat recycler so it can be used again later
                    if (mat !== lastFrame) {
                        framebufferRecycler!!.returnMat(mat)
                    }
                }

                RenderingState.PAUSED -> {
                    if (shouldPaintOrange) {
                        shouldPaintOrange = false

                        /*
                     * For some reason, the canvas will very occasionally be null upon closing.
                     * Stack Overflow seems to suggest this means the canvas has been destroyed.
                     * However, surfaceDestroyed(), which is called right before the surface is
                     * destroyed, calls checkState(), which *SHOULD* block until we die. This
                     * works most of the time, but not always? We don't yet understand...
                     */
                        if (canvas != null) {
                            renderer.renderPaused(canvas)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    fun clearViewport() {
        visionPreviewFrameQueue.clear()

        synchronized(canvasLock) {
            lastFrame.release()
        }
    }

    override fun setRenderingPolicy(policy: ViewportRenderingPolicy) {}
    override fun setRenderHook(renderHook: RenderHook) {
        this.renderHook = renderHook
    }

    companion object {
        private const val VISION_PREVIEW_FRAME_QUEUE_CAPACITY = 2
        private const val FRAMEBUFFER_RECYCLER_CAPACITY = VISION_PREVIEW_FRAME_QUEUE_CAPACITY + 4 //So that the evicting queue can be full, and the render thread has one checked out (+1) and post() can still take one (+1).
    }
}